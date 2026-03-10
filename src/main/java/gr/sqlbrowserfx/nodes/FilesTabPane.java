package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.FileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.SimpleFileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.java.FileJavaCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.CSqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.FileSqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableRowEditBox;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;

public class FilesTabPane extends TabPane {

    private CustomPopOver fileSearchPopOver;
    private SqlConnector sqlConnector;

	public FilesTabPane() {
		super();
		var draggingSupport = new DraggingTabPaneSupport("/icons/file.png");
        draggingSupport.addSupport(this);
        
        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                /* allow for both copying and moving, whatever user chooses */
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
        	var db = event.getDragboard();
        	var success = false;
            if (db.hasFiles()) {
            	var file = db.getFiles().get(0);
                this.openNewFileTab(file);
                success = true;
            }
            /*
             * let the source know whether the string was successfully transferred and used
             */
            event.setDropCompleted(success);

            event.consume();
        });
        
        Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.O, KeyCombination.CONTROL_DOWN),
                action -> this.showFileSearchPopOver()));
	}
	
    public void setSqlConnector(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
	}

	private String fixQuery(String query) {
        int spacesNum = 0;
        query = query.trim().replaceAll("\t", "    ");
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == ' ' || query.charAt(i) == '\n') {
                spacesNum++;
            } else {
                break;
            }
        }
        query = query.substring(spacesNum);
        //FIXME find right pattern to ignore comments
        query = query.replaceAll("--.*\n", "");
        return query;
    }
    
	public void openQueryTabArea() {
		var sqlCodeArea = new CSqlCodeArea();
    	var tableView = new SqlTableView(this.sqlConnector);
    	var split = new SplitPane(sqlCodeArea, tableView);
    	split.setOrientation(Orientation.VERTICAL);
    	var bPane = new BorderPane(split);
    	
    	Nodes.addInputMap(tableView, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN),
                 action -> {
					if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
						return;
					}
					var selectedItems = tableView.getSelectionModel().getSelectedItems();
					if (selectedItems != null) {
						var joined = StringUtils.join(selectedItems.stream().map(i -> i.toString()).toList(), "\n");
						var stringSelection = new StringSelection(joined);
						var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(stringSelection, null);
					}
                 }));
    	 
    	var popOver =new CustomPopOver();
    	tableView.setOnMouseClicked(event -> {
    		if (event.getClickCount() < 2) {
    			return;
    		}
    		
    		var sqlTableRow = tableView.getSelectionModel().getSelectedItem();
    		if (sqlTableRow == null)
    			return;

    		var editBox = new SqlTableRowEditBox(tableView, sqlTableRow, false);
    		var sp = new ScrollPane(editBox);
    		sp.setMaxHeight(800);
    		sp.setFitToWidth(true);
    		
    		popOver.setContentNode(sp);
    		popOver.show(tableView, event.getScreenX(), event.getScreenY());
    	});
    	
        var stopBtn = new Button("", JavaFXUtils.createIcon("/icons/stop.png"));

        var sqlQueryRunning = new AtomicBoolean(false);
        sqlCodeArea.setRunAction(() -> {
            final var fixedQuery = this.fixQuery(sqlCodeArea.getSelectedText().isBlank() ? sqlCodeArea.getText() : sqlCodeArea.getSelectedText());
            sqlCodeArea.setDisable(true);
            tableView.setDisable(true);

            bPane.setBottom(new CustomHBox(new Label("Running query. Please wait..."), stopBtn));
            var queryDuration = new AtomicLong(System.currentTimeMillis());
            if (sqlConnector != null && (fixedQuery.toLowerCase().startsWith("select") || fixedQuery.toLowerCase().startsWith("show"))) {
                sqlConnector.executeAsync(() -> {
                    sqlQueryRunning.set(true);
                    try {
                        sqlConnector.executeCancelableQuery(fixedQuery, rset -> {
                            queryDuration.set(System.currentTimeMillis() - queryDuration.get());
                            LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("\n" + fixedQuery + "\n execution took  " + queryDuration.get() + "ms");
                            DialogFactory.createNotification("Query executed", "Query execution took " + queryDuration.get() + "ms", 1);
							tableView.setItemsLater(rset);
                        }, stmt -> {
                        	stopBtn.setOnAction(action -> {
                                try {
                                    stmt.cancel();
                                } catch (SQLException e) {
                                    DialogFactory.createErrorDialog(e);
                                }
                            });
                        });

                    } catch (SQLException e) {
                        DialogFactory.createErrorDialog(e);
                    } finally {
                        sqlQueryRunning.set(false);
                        Platform.runLater(() -> {
                        	sqlCodeArea.setDisable(false);
                        	tableView.setDisable(false);
                        	bPane.setBottom(new Label(tableView.getSqlTableRows().size() + " rows"));
                        });
                    }
                });
            }
            else if (!fixedQuery.isEmpty()) {
                sqlConnector.executeAsync(() -> {
                    sqlQueryRunning.set(true);
                    try {
                        int rowsAffected = sqlConnector.executeUpdate(fixedQuery);
                        queryDuration.set(System.currentTimeMillis() - queryDuration.get());
                        LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("\n" + fixedQuery + "\n execution took  " + queryDuration.get() + "ms");
                        DialogFactory.createNotification("Query executed", "Query execution took " + queryDuration.get() + "ms\n(" + rowsAffected + ") rows affected" , 3);

                    } catch (SQLException e) {
                        DialogFactory.createErrorDialog(e);
                    } finally {
                    	Platform.runLater(() -> sqlCodeArea.setDisable(false));
                        sqlQueryRunning.set(false);
                    }
                });
            }
        });
        
        
    	var tab = new Tab("Sql Query", bPane);
    	tab.setGraphic(JavaFXUtils.createIcon("/icons/thunder.png"));
    	addTabContextMenu(tab);
        this.getTabs().add(tab);
        this.getSelectionModel().select(tab);
	}
	
    private void addTabContextMenu(Tab tab) {
		var closeTabItem = new MenuItem("Close Tab", JavaFXUtils.createIcon("/icons/minus.png"));
		closeTabItem.setOnAction(event -> tab.getTabPane().getTabs().remove(tab));
		
		var renameTabItem = new MenuItem("Rename Tab", JavaFXUtils.createIcon("/icons/edit.png"));
		renameTabItem.setOnAction(event -> {
			var tabGraphic = tab.getGraphic();
			var textField = new TextField();
			textField.setPromptText("Enter new name");
			textField.setOnKeyPressed(keyEvent -> {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					// graphic is label because we are using DragTabPaneSupport util
					var label = (Label) tabGraphic;
					label.setText(textField.getText());
					tab.setGraphic(tabGraphic);
				}
				if (keyEvent.getCode() == KeyCode.ESCAPE) {
					tab.setGraphic(tabGraphic);
				}
				
				keyEvent.consume();
			});
			tab.setGraphic(textField);
			textField.requestFocus();
		});
		
		tab.setContextMenu(new ContextMenu(closeTabItem, renameTabItem));
    }
	
	public void openNewFileTab(File selectedFile) {
        var tab = new Tab(selectedFile.getName());

        AutoCompleteCodeArea<?> codeArea;
        if (selectedFile.getName().endsWith(".java")) {
            codeArea = new FileJavaCodeArea(selectedFile);
        } else if (selectedFile.getName().endsWith(".sql")) {
            codeArea = new FileSqlCodeArea(selectedFile);;
        } else {
            codeArea = new SimpleFileCodeArea(selectedFile);
        }


        var vsp = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(vsp);
        var fileCodeArea = (FileCodeArea) codeArea;

        tab.setOnCloseRequest((event) -> {
            if (fileCodeArea.isTextDirty()) {
                event.consume();

                if (DialogFactory.createConfirmationDialog(
                        "Unsaved work",
                        "Do you want to discard changes ?")
                ) {
                    this.getTabs().remove(tab);
                }
            }
        });
		var closeTabItem = new MenuItem("Close Tab", JavaFXUtils.createIcon("/icons/minus.png"));
		closeTabItem.setOnAction(event -> {
            if (fileCodeArea.isTextDirty()) {
                event.consume();

                if (DialogFactory.createConfirmationDialog(
                        "Unsaved work",
                        "Do you want to discard changes ?")
                ) {
                    this.getTabs().remove(tab);
                }
            }
		});
		tab.setContextMenu(new ContextMenu(closeTabItem));
		
        tab.setGraphic(JavaFXUtils.createIcon("/icons/code-file.png"));
        this.getTabs().add(tab);
        this.getSelectionModel().select(tab);

        codeArea.requestFocus();
    }
	
    private void showFileSearchPopOver() {
    	if (this.fileSearchPopOver == null) {
            this.fileSearchPopOver = new FileSearchPopOver(this::openNewFileTab);
    	}
    	
        if (this.fileSearchPopOver.isShowing()) {
        	return;
        }

        var boundsInScene = this.localToScreen(this.getBoundsInLocal());
        fileSearchPopOver.show(this, boundsInScene.getMaxX() - 620,
                boundsInScene.getMinY());
    }
}
