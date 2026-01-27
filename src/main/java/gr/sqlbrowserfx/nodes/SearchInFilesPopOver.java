package gr.sqlbrowserfx.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.nodes.codeareas.FileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.SimpleFileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.java.FileJavaCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.FileSqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.typescript.FileTypeScriptCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.FileInfo;
import gr.sqlbrowserfx.utils.FilesUtils;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.LineMatch;
import gr.sqlbrowserfx.utils.PropertiesLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class SearchInFilesPopOver extends CustomPopOver {

	private String rootPath = ((String) PropertiesLoader.getProperty("sqlbrowserfx.root.path", String.class, "~/"))
			.replaceAll("\"", "");
	
	private CodeArea codeArea = new CodeArea();
	TextField searchField;
	private TextField extensionField;
	private TableView<FileInfo> filesTableView;
	private ListView<LineMatch> linesListView;	
	private CheckBox wholeWordCheckBox;
	private CheckBox caseInsensitiveCheckBox;
	private SplitPane vSplit;
	
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	
	public SearchInFilesPopOver() {
		
		var fileSearchBox = this.createFileSearchBox();
		var linesListBox = this.createLinesListBox();
		var hSplit = new SplitPane(fileSearchBox, new CustomVBox(new Label("Lines Matches"),linesListBox));
		hSplit.setOrientation(Orientation.HORIZONTAL);
		hSplit.setDividerPositions(0.7f, 0.3f); 
		

		vSplit = new SplitPane(hSplit, new VirtualizedScrollPane<CodeArea>(codeArea));
		vSplit.setOrientation(Orientation.VERTICAL);
		
		var borderPane = new BorderPane(vSplit);
		this.setContentNode(borderPane);
		this.setPrefSize(1000, 800);
		// Add ESC key handler to close the stage
		borderPane.setOnKeyPressed(event -> {
	    	if (event.getCode() == KeyCode.ESCAPE) {
	    		PropertiesLoader.storeProperty("./sqlbrowserfx.properties", "sqlbrowserfx.root.path", this.rootPath);
	    		this.hide();
	    	}
	    });
	}

	
	private VBox createLinesListBox() {
		this.createLinesListView();
		
		var nextBtn = new Button("Next >");
		nextBtn.setOnAction(evetn -> {
			var idx = linesListView.getSelectionModel().getSelectedIndex();
			if (idx < linesListView.getItems().size() - 1) {
				idx++;
			}
			
			linesListView.getSelectionModel().select(idx);
			selectLineMatch(linesListView.getSelectionModel().getSelectedItem());
		});
		
		var prevBtn = new Button("< Prev");
		prevBtn.setOnAction(evetn -> {
			var idx = linesListView.getSelectionModel().getSelectedIndex();
			if (idx > 0) {
				idx--;
			}
			
			linesListView.getSelectionModel().select(idx);
			selectLineMatch(linesListView.getSelectionModel().getSelectedItem());
		});
		
		return new VBox(new CustomHBox(prevBtn, nextBtn), linesListView);
	}

	private VBox createFileSearchBox() {
		var vbox = new CustomVBox();
		var openButton = new Button("", JavaFXUtils.createIcon("/icons/code-file.png"));
		openButton.setTooltip(new Tooltip("Open file"));
		
		extensionField = new TextField();
		extensionField.setPromptText("File Extension...");
		searchField = new TextField();
//		searchField.setPrefWidth(576);
		searchField.setPromptText("Pattern...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				search();
			}
			
			if (keyEvent.getCode() != KeyCode.ESCAPE) {
				keyEvent.consume();
			}
		});
		wholeWordCheckBox = new CheckBox("ww");
		wholeWordCheckBox.setTooltip(new Tooltip("Whole Word"));
		wholeWordCheckBox.setFocusTraversable(false);
		caseInsensitiveCheckBox = new CheckBox("ci");
		caseInsensitiveCheckBox.setTooltip(new Tooltip("Case Insensitive"));
		caseInsensitiveCheckBox.setFocusTraversable(false);

		var descLabel = new Label("File Search in: " + rootPath);

		var settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
		settingsButton.setOnMouseClicked(event -> {
			var dirChooser = new DirectoryChooser();
			File initialDir = new File(this.rootPath);
			dirChooser.setInitialDirectory(initialDir);
			var selectedDir = dirChooser.showDialog(vbox.getScene().getWindow());
			if (selectedDir != null) {
				this.rootPath = selectedDir.getAbsolutePath();
				descLabel.setText("File Search in: " + rootPath);
			}
		});
		settingsButton.setTooltip(new Tooltip("Click to change root path"));
		
		this.createFilesTableView();
		
		var searchButton = new Button("Search", JavaFXUtils.createIcon("/icons/magnify.png"));
		searchButton.setOnAction(event -> this.search());
		
		vbox.getChildren().addAll(
			new CustomHBox(settingsButton, descLabel),
			new CustomHBox(
				searchField, 
				caseInsensitiveCheckBox,
				wholeWordCheckBox,
				extensionField,
				searchButton
			), 
			filesTableView);
		return vbox;
	}

	private void createLinesListView() {
		linesListView = new ListView<>();
		linesListView.setOnMouseClicked(event -> {
		    if (event.getClickCount() == 1) {
		        var selectedItem = linesListView.getSelectionModel().getSelectedItem();
		        this.selectLineMatch(selectedItem);
		    }
		});
		
	    
		linesListView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER
			        || keyEvent.getCode() == KeyCode.UP
			        || keyEvent.getCode() == KeyCode.DOWN) {
				var selectedItem = linesListView.getSelectionModel().getSelectedItem();
				this.selectLineMatch(selectedItem);
			}
		});
		
		VBox.setVgrow(linesListView, Priority.ALWAYS);
	}

	private void createFilesTableView() {
		filesTableView = new TableView<>();
		var nameColumn = new TableColumn<FileInfo, String>("File");
		nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
		filesTableView.getColumns().add(nameColumn);
		nameColumn.setPrefWidth(400);

		var countColumn = new TableColumn<FileInfo, String>("Matches");
		countColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMatchCount().toString()));
		filesTableView.getColumns().add(countColumn);

		var pathColumn = new TableColumn<FileInfo, String>("Relative Path");
		pathColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getRelativePath()));
		filesTableView.getColumns().add(pathColumn);
		pathColumn.setPrefWidth(400);
		filesTableView.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(filesTableView, Priority.ALWAYS);
		
		filesTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER
			        || keyEvent.getCode() == KeyCode.UP
			        || keyEvent.getCode() == KeyCode.DOWN) {
				this.selectFile();
			}
		});
		filesTableView.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getButton() == MouseButton.PRIMARY) {
				this.selectFile();
			}
		});
		
		var menuItemCopy = new MenuItem("Copy Absolute Path", JavaFXUtils.createIcon("/icons/copy.png"));
        menuItemCopy.setOnAction(event -> {
			var selectedItem = filesTableView.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				var content = new ClipboardContent();
	            content.putString(selectedItem.getAbsolutePath());
	            Clipboard.getSystemClipboard().setContent(content);
			}
        });
        
        var openInVSCode = new MenuItem("Open in VS Code", JavaFXUtils.createIcon("/icons/code-file.png"));
        openInVSCode.setOnAction(e -> {
            var selected = filesTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openInVSCode(selected.getAbsolutePath(), false);
            }
        });
        var openInActiveVSCode = new MenuItem("Open in Active VS Code", JavaFXUtils.createIcon("/icons/code-file.png"));
        openInActiveVSCode.setOnAction(e -> {
            var selected = filesTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openInVSCode(selected.getAbsolutePath(), true);
            }
        });
        
		filesTableView.setContextMenu(new ContextMenu(menuItemCopy, openInVSCode, openInActiveVSCode));
	}

	private void selectFile() {
		var fileInfo = filesTableView.getSelectionModel().getSelectedItem();
		if (fileInfo == null) {
			return;
		}
		this.openFile(fileInfo.getAbsolutePath());
		this.linesListView.setItems(FXCollections.observableArrayList(fileInfo.getLineMatches()));
		this.selectLineMatch(this.linesListView.getItems().get(0));
	}

	private void selectLineMatch(LineMatch selectedItem) {
		if (selectedItem == null) {
			return;
		}
	    int line = selectedItem.getLineNumber() - 1; // CodeArea lines are 0-based
	    int position = codeArea.position(line, 0).toOffset();
	    codeArea.moveTo(position);

	    int lineEnd = codeArea.position(line + 1, 0).toOffset();
	    codeArea.selectRange(position, lineEnd);
	    
	    codeArea.requestFollowCaret();
	}
	
	
	private void openFile(String absolutePath) {
		if (codeArea instanceof FileCodeArea && absolutePath.equals(((FileCodeArea) this.codeArea).getPath())) {
			return;
		}
		
        var file = new File(absolutePath);
        if (file.getName().endsWith(".java")) {
            this.codeArea = new FileJavaCodeArea(file);
        } else if (file.getName().endsWith(".sql")) {
            this.codeArea = new FileSqlCodeArea(file);
        } else if (file.getName().endsWith(".ts")) {
            this.codeArea = new FileTypeScriptCodeArea(file);
        } else if (file.getName().endsWith(".html")) {
            this.codeArea = new FileTypeScriptCodeArea(file);
        } 
        else {
            this.codeArea = new SimpleFileCodeArea(file);
        }
        
        this.vSplit.getItems().remove(1);
        this.vSplit.getItems().add(new VirtualizedScrollPane<CodeArea>(codeArea));
	}
	
	private void openInVSCode(String filePath, boolean reuseWindow) {
	    try {
	        var command = new ArrayList<String>();
	        command.add("codium");

	        if (reuseWindow) {
	            command.add("-r"); // reuse active window
	        }

	        command.add(filePath);

	        new ProcessBuilder(command)
	                .redirectErrorStream(true)
	                .start();

	    } catch (IOException ex) {
	    	System.err.println("Failed to open file in VS Code: " + ex.getMessage());
	    }
	}

	private void search() {
	    searchField.setDisable(true);
	    filesTableView.setDisable(true);

	    var rawPattern = searchField.getText();
	    if (rawPattern.isEmpty()) {
	        searchField.setDisable(false);
	        filesTableView.setDisable(false);
	        return;
	    }

	    // Build regex safely
	    var pattern = Pattern.quote(rawPattern);
	    if (wholeWordCheckBox.isSelected() && rawPattern.matches("\\w+")) {
	        pattern = "\\b" + pattern + "\\b";
	    }

	    if (caseInsensitiveCheckBox.isSelected()) {
	        pattern = "(?i)" + pattern;
	    }

	    final var finalPattern = pattern;
	    final var extension = extensionField.getText();
	    
	    executor.schedule(() -> {
	        var searchResults = FilesUtils.walkContentsWithLines(rootPath, finalPattern, extension, 10);

	        Platform.runLater(() -> {
	            filesTableView.setItems(FXCollections.observableArrayList(searchResults));
	            searchField.setDisable(false);
	            filesTableView.setDisable(false);
	        });
	    }, 0, TimeUnit.SECONDS);
	}

}
