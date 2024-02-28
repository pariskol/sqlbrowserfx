package gr.sqlbrowserfx.nodes;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.codeareas.AutoCompleteCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.FileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.SimpleFileCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.TextAnalyzer;
import gr.sqlbrowserfx.nodes.codeareas.java.FileJavaCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.CSqlCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.FileSqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SqlConsolePane extends BorderPane implements ToolbarOwner, SimpleObservable<String> {

    private final TextArea historyArea;
    private final TabPane queryTabPane;
    private final ProgressIndicator progressIndicator;
    private final Tab newConsoleTab;
    private Button executeButton;
    private CSqlCodeArea codeAreaRef;
    private final CheckBox autoCompleteOnTypeCheckBox;
    private final CheckBox openInNewTableViewCheckBox;
    private final CheckBox wrapTextCheckBox;
    private final CheckBox showLinesCheckBox;
    private final FlowPane toolbar;
    private final SqlConnector sqlConnector;
    protected AtomicBoolean sqlQueryRunning;
    protected List<SimpleObserver<String>> listeners;
    private Button stopExecutionButton;
    private Button settingsButton;
    private boolean popOverIsShowing = false;
    private SplitPane splitPane;
    private Button openButton;
    private Button searchButton;
    private CustomPopOver fileSearchPopOver;


    @SuppressWarnings("unchecked")
    public SqlConsolePane(SqlConnector sqlConnector) {
        this.sqlConnector = sqlConnector;
        sqlQueryRunning = new AtomicBoolean(false);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(32, 32);
        historyArea = new TextArea();
        listeners = new ArrayList<>();

        queryTabPane = new TabPane();
        DraggingTabPaneSupport draggingSupport = new DraggingTabPaneSupport("/icons/thunder.png");
        draggingSupport.addSupport(queryTabPane);
        newConsoleTab = new Tab("");
        newConsoleTab.setGraphic(JavaFXUtils.createIcon("/icons/add.png"));
        queryTabPane.setOnMouseClicked(MouseEvent -> addTab());
        newConsoleTab.setClosable(false);
        queryTabPane.getTabs().add(newConsoleTab);
        queryTabPane.setOnKeyPressed(keyEvent -> {
            if (keyEvent.isControlDown()) {
                switch (keyEvent.getCode()) {
                    case N:
                        this.openNewSqlConsoleTab();
                        break;
                    default:
                        break;
                }
            }
        });

        Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.O, KeyCombination.CONTROL_DOWN),
                action -> this.showFileSearchPopOver()));

        splitPane = new SplitPane(queryTabPane, historyArea);
        splitPane.setOrientation(Orientation.VERTICAL);
        historyArea.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.65));
        queryTabPane.prefHeightProperty().bind(splitPane.heightProperty().multiply(0.35));

        autoCompleteOnTypeCheckBox = new CheckBox("Autocomplete on type");
        autoCompleteOnTypeCheckBox.setSelected(true);

        openInNewTableViewCheckBox = new CheckBox("Open in new table");
        openInNewTableViewCheckBox.setSelected(false);

        queryTabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, oldTab, newTab) -> {
                    if (newTab.getContent() != null) {
                        CodeArea codeArea = (oldTab.getContent() != null) ? ((VirtualizedScrollPane<CodeArea>) oldTab.getContent()).getContent()
                                : null;

                        if (codeArea instanceof TextAnalyzer) {
                            ((TextAnalyzer) codeArea).stopTextAnalyzerDaemon();
                        }

                        codeArea = ((VirtualizedScrollPane<CodeArea>) newTab.getContent()).getContent();
                        if (codeArea instanceof TextAnalyzer) {
                            ((TextAnalyzer) codeArea).startTextAnalyzerDaemon();
                        }
                    }
                });

        wrapTextCheckBox = new CheckBox("Wrap text");
        showLinesCheckBox = new CheckBox("Show line number");
        showLinesCheckBox.setSelected(true);

        toolbar = this.createToolbar();

        this.setCenter(splitPane);
        this.setLeft(toolbar);

        // initial create one tab
        this.addTab();

        this.setOnDragOver(event -> {
            if (event.getGestureSource() != SqlConsolePane.this && event.getDragboard().hasFiles()) {
                /* allow for both copying and moving, whatever user chooses */
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                SqlConsolePane.this.openNewFileTab(file);
                success = true;
            }
            /*
             * let the source know whether the string was successfully transferred and used
             */
            event.setDropCompleted(success);

            event.consume();
        });
    }

    public void destroySplitPane() {
        this.splitPane = null;
    }

    @SuppressWarnings("unchecked")
    private void addTab() {
        Tab selectedTab = queryTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == newConsoleTab) {
            this.openNewSqlConsoleTab();
        } else {
            CodeArea codeArea = ((VirtualizedScrollPane<CodeArea>) selectedTab.getContent()).getContent();
            if (codeArea instanceof CSqlCodeArea) {
                codeAreaRef = (CSqlCodeArea) codeArea;
            }
        }
    }

    private void createFileSearchPopover() {
        if (this.fileSearchPopOver != null) return;

        this.fileSearchPopOver = new FileSearchPopOver(this::openNewFileTab);
    }

    private void showFileSearchPopOver() {
        if (popOverIsShowing) return;

        createFileSearchPopover();

        Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
        fileSearchPopOver.show(searchButton, boundsInScene.getMaxX() - 620,
                boundsInScene.getMinY());
    }

    private void openNewSqlConsoleTab() {
        CSqlCodeArea sqlCodeArea = new CSqlCodeArea();
        sqlCodeArea.wrapTextProperty().bind(this.wrapTextCheckBox.selectedProperty());
        sqlCodeArea.showLinesProperty().bind(this.showLinesCheckBox.selectedProperty());
        sqlCodeArea.autoCompleteProperty().bind(this.autoCompleteOnTypeCheckBox.selectedProperty());

        sqlCodeArea.setRunAction(this::executeButtonAction);
        sqlCodeArea.addEventHandler(SimpleEvent.EVENT_TYPE, simpleEvent -> SqlConsolePane.this.changed());

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(sqlCodeArea);
        Tab newTab = new Tab("query " + queryTabPane.getTabs().size(), scrollPane);
        newTab.setOnClosed(event -> sqlCodeArea.stopTextAnalyzerDaemon());

        queryTabPane.getTabs().add(newTab);
        queryTabPane.getSelectionModel().select(newTab);
        codeAreaRef = sqlCodeArea;
        sqlCodeArea.requestFocus();
    }

    public void openNewFileTab(File selectedFile) {
        AutoCompleteCodeArea<?> codeArea;
        if (selectedFile.getName().endsWith(".java")) {
            codeArea = new FileJavaCodeArea(selectedFile);
        } else if (selectedFile.getName().endsWith(".sql")) {
            codeArea = new FileSqlCodeArea(selectedFile);
        } else {
            codeArea = new SimpleFileCodeArea(selectedFile);
        }

        codeArea.wrapTextProperty().bind(this.wrapTextCheckBox.selectedProperty());
        codeArea.showLinesProperty().bind(this.showLinesCheckBox.selectedProperty());
        codeArea.autoCompleteProperty().bind(this.autoCompleteOnTypeCheckBox.selectedProperty());

        VirtualizedScrollPane<CodeArea> vsp = new VirtualizedScrollPane<>(codeArea);

        FileCodeArea fileCodeArea = (FileCodeArea) codeArea;

        Tab tab = new Tab(selectedFile.getName(), vsp);
        tab.setOnCloseRequest((event) -> {

            if (fileCodeArea.isTextDirty()) {
                event.consume();

                if (DialogFactory.createConfirmationDialog(
                        "Unsaved work",
                        "Do you want to discard changes ?")
                ) {
                    queryTabPane.getTabs().remove(tab);
                }
            }
        });
        tab.setGraphic(JavaFXUtils.createIcon("/icons/code-file.png"));
        queryTabPane.getTabs().add(tab);
        queryTabPane.getSelectionModel().select(tab);

        if (codeArea instanceof CSqlCodeArea casted) {
            codeAreaRef = casted;
            codeArea.requestFocus();
            casted.setRunAction(this::executeButtonAction);
        }
    }

    private void openFileAction() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        openNewFileTab(selectedFile);
    }

    @Override
    public FlowPane createToolbar() {
        executeButton = new Button("", JavaFXUtils.createIcon("/icons/play.png"));
        executeButton.setTooltip(new Tooltip("Execute"));
        executeButton.setOnAction(actionEvent -> executeButtonAction());

        stopExecutionButton = new Button("", JavaFXUtils.createIcon("/icons/stop.png"));
        stopExecutionButton.setTooltip(new Tooltip("Stop execution"));

        settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
        settingsButton.setOnMouseClicked(mouseEvent -> {
            if (popOverIsShowing) return;

            popOverIsShowing = true;
            CustomPopOver popOver = new CustomPopOver(new VBox(autoCompleteOnTypeCheckBox, openInNewTableViewCheckBox, wrapTextCheckBox, showLinesCheckBox));
            popOver.setOnHidden(event -> popOverIsShowing = false);
            popOver.show(settingsButton);
        });
        settingsButton.setTooltip(new Tooltip("Adjust settings"));

        openButton = new Button("", JavaFXUtils.createIcon("/icons/code-file.png"));
        openButton.setOnMouseClicked(mouseEvent -> this.openFileAction());
        openButton.setTooltip(new Tooltip("Open file"));

        searchButton = new Button("", JavaFXUtils.createIcon("/icons/magnify.png"));
        searchButton.setOnMouseClicked(mouseEvent -> this.showFileSearchPopOver());
        searchButton.setTooltip(new Tooltip("Search file"));

        FlowPane toolbar = new FlowPane(searchButton, executeButton, stopExecutionButton, settingsButton, openButton);
        toolbar.setOrientation(Orientation.VERTICAL);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private final CodeArea getSelectedSqlCodeArea() {
        return ((VirtualizedScrollPane<CodeArea>) queryTabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
    }

    // TODO: needs polishing
    public String executeButtonAction() {
        if (sqlQueryRunning.get()) {
            DialogFactory.createNotification("Query execution in progress", "A query is already running!\n You must wait to finish or cancel it, in order to run a new one!");
            return null;
        }

        CodeArea sqlConsoleArea = this.getSelectedSqlCodeArea();
        String query = !sqlConsoleArea.getSelectedText().isEmpty() ? sqlConsoleArea.getSelectedText() : sqlConsoleArea.getText();
        final String fixedQuery = this.fixQuery(query);

        AtomicLong queryDuration = new AtomicLong(System.currentTimeMillis());
        if (fixedQuery.toLowerCase().startsWith("select")
                || fixedQuery.toLowerCase().startsWith("show")) {
            sqlConnector.executeAsync(() -> {
                sqlQueryRunning.set(true);
                Platform.runLater(() -> {
                    executeButton.setDisable(true);
//					this.setCenter(new StackPane(progressIndicator));
                });
                try {
                    sqlConnector.executeCancelableQuery(fixedQuery, rset -> {
                        queryDuration.set(System.currentTimeMillis() - queryDuration.get());
                        LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("\n" + fixedQuery + "\n execution took  " + queryDuration.get() + "ms");
                        DialogFactory.createNotification("Query executed", "Query execution took " + queryDuration.get() + "ms", 1);
                        handleSelectResult(fixedQuery, rset);
                    }, stmt -> stopExecutionButton.setOnAction(action -> {
                        try {
                            stmt.cancel();
                        } catch (SQLException e) {
                            LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
                        }
                    }));

                } catch (SQLException e) {
                    hanldeException(e);
                } finally {
                    SqlConsolePane.this.saveHistory(fixedQuery, queryDuration.get());
                    Platform.runLater(() -> {
                        executeButton.setDisable(false);
//						if (splitPane != null)
//							this.setCenter(splitPane);
//						else
//							this.setCenter(queryTabPane);
                        getSelectedSqlCodeArea().requestFocus();
                    });
                    sqlQueryRunning.set(false);
                }
            });
        } else if (!fixedQuery.isEmpty()) {
            sqlConnector.executeAsync(() -> {
                sqlQueryRunning.set(true);
                Platform.runLater(() -> {
                    executeButton.setDisable(true);
                    this.setCenter(new StackPane(progressIndicator));
                });
                try {
                    int rowsAffected = sqlConnector.executeUpdate(fixedQuery);
                    queryDuration.set(System.currentTimeMillis() - queryDuration.get());
                    LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("\n" + fixedQuery + "\n execution took  " + queryDuration.get() + "ms");
                    DialogFactory.createNotification("Query executed", "Query execution took " + queryDuration.get() + "ms", 1);
                    handleUpdateResult(rowsAffected);

                } catch (SQLException e) {
                    hanldeException(e);
                } finally {
                    SqlConsolePane.this.saveHistory(fixedQuery, queryDuration.get());
                    Platform.runLater(() -> {
                        executeButton.setDisable(false);
                        if (splitPane != null)
                            this.setCenter(splitPane);
                        else
                            this.setCenter(queryTabPane);
                        getSelectedSqlCodeArea().requestFocus();
                    });
                    sqlQueryRunning.set(false);
                }

                String queryToLowerCase = fixedQuery.toLowerCase();
                if (
                    //------------------------------------------
                        (queryToLowerCase.startsWith("drop") ||
                                queryToLowerCase.startsWith("create") ||
                                queryToLowerCase.startsWith("alter")
                        )
                                //------------------------------------------
                                &&
                                //------------------------------------------
                                (queryToLowerCase.contains("table") ||
                                        queryToLowerCase.contains("view") ||
                                        queryToLowerCase.contains("trigger") ||
                                        queryToLowerCase.contains("procedure") ||
                                        queryToLowerCase.contains("function")
                                )
                    //------------------------------------------
                ) {
                    this.changed(fixedQuery);
                }
            });
        }

        return fixedQuery;
    }

    private void saveHistory(final String fixedQuery, long queryDuration) {
        try {
            SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdateAsync("insert into queries_history (query, duration) values (?, ?)",
                    Arrays.asList(fixedQuery, queryDuration));
        } catch (SQLException e) {
            LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
        }
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

    protected void handleUpdateResult(int rowsAffected) throws SQLException {
        historyArea.appendText("Query OK (" + rowsAffected + " rows affected)\n");
    }

    protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
        StringBuilder lines = new StringBuilder();
        while (rset.next()) {
            StringBuilder line = new StringBuilder();
            ResultSetMetaData rsmd = rset.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                line.append(rsmd.getColumnLabel(i)).append(" : ");
                if (rset.getObject(rsmd.getColumnLabel(i)) != null)
                    line.append(rset.getObject(rsmd.getColumnLabel(i)).toString()).append(", ");
            }
            line = new StringBuilder(line.substring(0, line.length() - ", ".length()));
            lines.append(line).append("\n");
        }
        historyArea.setText(lines.toString());
    }

    public void hanldeException(SQLException e) {
        historyArea.appendText(e.getMessage() + "\n");
    }

    @Override
    public void changed() {
        listeners.forEach(listener -> listener.onObservableChange(null));
    }

    @Override
    public void changed(String data) {
        listeners.forEach(listener -> listener.onObservableChange(data));

    }

    @Override
    public void addObserver(SimpleObserver<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeObserver(SimpleObserver<String> listener) {
        listeners.remove(listener);
    }

    public boolean openInNewTableView() {
        return openInNewTableViewCheckBox.isSelected();
    }

    public CodeArea getCodeAreaRef() {
        return codeAreaRef;
    }

    public TabPane getQueryTabPane() {
        return queryTabPane;
    }

    public FlowPane getToolbar() {
        return toolbar;
    }

    public List<SimpleObserver<String>> getListeners() {
        return listeners;
    }


}
