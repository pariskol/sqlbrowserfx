package gr.sqlbrowserfx.nodes;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

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
import java.sql.Statement;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

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

    @SuppressWarnings("unchecked")
    public SqlConsolePane(SqlConnector sqlConnector) {
        this.sqlConnector = sqlConnector;
        sqlQueryRunning = new AtomicBoolean(false);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(32, 32);
        historyArea = new TextArea();
        listeners = new ArrayList<>();

        queryTabPane = new TabPane();
        var draggingSupport = new DraggingTabPaneSupport("/icons/thunder.png");
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
                        var codeArea = (oldTab.getContent() != null) ? ((VirtualizedScrollPane<CodeArea>) oldTab.getContent()).getContent()
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

        this.setTop(toolbar);
        this.setCenter(splitPane);

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
            var db = event.getDragboard();
            var success = false;
            if (db.hasFiles()) {
                var file = db.getFiles().get(0);
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
        var selectedTab = queryTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == newConsoleTab) {
            this.openNewSqlConsoleTab();
        } else {
            var codeArea = ((VirtualizedScrollPane<CodeArea>) selectedTab.getContent()).getContent();
            if (codeArea instanceof CSqlCodeArea) {
                codeAreaRef = (CSqlCodeArea) codeArea;
            }
        }
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

    private void openNewSqlConsoleTab() {
        var sqlCodeArea = new CSqlCodeArea();
        sqlCodeArea.wrapTextProperty().bind(this.wrapTextCheckBox.selectedProperty());
        sqlCodeArea.showLinesProperty().bind(this.showLinesCheckBox.selectedProperty());
        sqlCodeArea.autoCompleteProperty().bind(this.autoCompleteOnTypeCheckBox.selectedProperty());

        sqlCodeArea.setRunAction(this::executeButtonAction);
        sqlCodeArea.addEventHandler(SimpleEvent.EVENT_TYPE, simpleEvent -> SqlConsolePane.this.changed());

        var scrollPane = new VirtualizedScrollPane<>(sqlCodeArea);
        var newTab = new Tab("query " + queryTabPane.getTabs().size(), scrollPane);
        addTabContextMenu(newTab);
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

        var vsp = new VirtualizedScrollPane<>(codeArea);

        var fileCodeArea = (FileCodeArea) codeArea;

        Tab tab = new Tab(selectedFile.getName(), vsp);
        tab.setOnCloseRequest((event) -> {
            if (fileCodeArea.isTextDirty()) {
                event.consume();

                if (DialogFactory.createConfirmationDialog(
                        "Unsaved work",
                        "Do you want to discard changes ?")) {
                    queryTabPane.getTabs().remove(tab);
                }
            }
        });
        var closeTabItem = new MenuItem("Close Tab", JavaFXUtils.createIcon("/icons/minus.png"));
        closeTabItem.setOnAction(event -> {
            if (fileCodeArea.isTextDirty()) {
                event.consume();

                if (DialogFactory.createConfirmationDialog(
                        "Unsaved work",
                        "Do you want to discard changes ?")) {
                    queryTabPane.getTabs().remove(tab);
                }
            }
        });
        tab.setContextMenu(new ContextMenu(closeTabItem));

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
        var fileChooser = new FileChooser();
        var selectedFile = fileChooser.showOpenDialog(null);
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
            if (popOverIsShowing) {
                return;
            }

            popOverIsShowing = true;
            CustomPopOver popOver = new CustomPopOver(new CustomVBox(autoCompleteOnTypeCheckBox, openInNewTableViewCheckBox, wrapTextCheckBox, showLinesCheckBox));
            popOver.setOnHidden(event -> popOverIsShowing = false);
            popOver.show(settingsButton);
        });
        settingsButton.setTooltip(new Tooltip("Adjust settings"));

        openButton = new Button("", JavaFXUtils.createIcon("/icons/code-file.png"));
        openButton.setOnMouseClicked(mouseEvent -> this.openFileAction());
        openButton.setTooltip(new Tooltip("Open file"));

        var toolbar = new CustomFlowPane(executeButton, stopExecutionButton, settingsButton, openButton);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private final CodeArea getSelectedSqlCodeArea() {
        return ((VirtualizedScrollPane<CodeArea>) queryTabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
    }

    public String executeButtonAction() {
        if (sqlQueryRunning.get()) {
            DialogFactory.createNotification("Query execution in progress", "A query is already running!\n You must wait to finish or cancel it, in order to run a new one!");
            return null;
        }

        var sqlConsoleArea = this.getSelectedSqlCodeArea();
        var query = !sqlConsoleArea.getSelectedText().isEmpty() ? sqlConsoleArea.getSelectedText() : sqlConsoleArea.getText();
        final var fixedQuery = fixQuery(query);

        // fast exit on empty query
        if (fixedQuery.isEmpty()) {
            return null;
        }

        // execute on different thread
        sqlConnector.executeAsync(() -> {
            var start = System.currentTimeMillis();

            sqlQueryRunning.set(true);
            Platform.runLater(() -> executeButton.setDisable(true));

            try {

                if (isReadQuery(fixedQuery)) {
                    handleSelectQuery(fixedQuery);
                } else {
                    handleActionQuery(fixedQuery);
                }

                var duration = System.currentTimeMillis() - start;
                saveHistory(fixedQuery, duration);
            } catch (SQLException e) {
                handleException(e);
            } finally {
                sqlQueryRunning.set(false);
                Platform.runLater(() -> {
                    executeButton.setDisable(false);
                    getSelectedSqlCodeArea().requestFocus();
                });
            }
        });

        return fixedQuery;
    }

    private void handleSelectQuery(String fixedQuery) throws SQLException {
        sqlConnector.executeCancelableQuery(fixedQuery,
                rset -> {
                    handleSelectResult(fixedQuery, rset);
                },
                stmt -> stopExecutionButton.setOnAction(action -> cancelQuery(stmt))
        );
    }

    private void handleActionQuery(String fixedQuery) throws SQLException {
        var rowsAffected = sqlConnector.executeUpdate(fixedQuery);
        handleUpdateResult(rowsAffected);

        if (isSchemaChangeQuery(fixedQuery)) {
            changed(fixedQuery);
        }
    }

    private boolean isReadQuery(String fixedQuery) {
        var trimmed = fixedQuery.toLowerCase().trim();
        return trimmed.startsWith("select")
                || trimmed.startsWith("with")
                || trimmed.startsWith("show")
                || trimmed.startsWith("describe")
                || trimmed.startsWith("desc");
    }

    private boolean isSchemaChangeQuery(String query) {
        var trimmed = query.toLowerCase().trim();
        return trimmed.startsWith("drop")
                || trimmed.startsWith("create")
                || trimmed.startsWith("alter")
                || trimmed.startsWith("grant")
                || trimmed.startsWith("revoke");
    }

    private void cancelQuery(Statement stmt) {
        try {
            stmt.cancel();
        } catch (SQLException e) {
            LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
        }
    }

    private void saveHistory(final String fixedQuery, long queryDuration) {
        try {
            DialogFactory.createNotification("Query executed", "Query execution took " + queryDuration + "ms", 1);
            SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdateAsync("insert into queries_history (query, duration) values (?, ?)",
                    Arrays.asList(fixedQuery, queryDuration));
        } catch (SQLException e) {
            LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
        }
    }

    private String fixQuery(String query) {
        // remove leading/trailing whitespace and replace tabs with spaces
        query = query.trim().replaceAll("\t", " ");

        // remove leading spaces after trim
        var spacesNum = 0;
        while (spacesNum < query.length() && (query.charAt(spacesNum) == ' ' || query.charAt(spacesNum) == '\n')) {
            spacesNum++;
        }
        query = query.substring(spacesNum);

        // remove sql comments
        query = query.replaceAll("--.*\n", "").replaceAll("/\\*.*?\\*/", "");

        return query;
    }

    protected void handleUpdateResult(int rowsAffected) throws SQLException {
        historyArea.appendText("Query OK (" + rowsAffected + " rows affected)\n");
    }

    protected void handleSelectResult(String query, ResultSet rset) throws SQLException {
        var lines = new StringBuilder();
        while (rset.next()) {
            var line = new StringBuilder();
            var rsmd = rset.getMetaData();
            for (var i = 1; i <= rsmd.getColumnCount(); i++) {
                line.append(rsmd.getColumnLabel(i)).append(" : ");
                if (rset.getObject(rsmd.getColumnLabel(i)) != null) {
                    line.append(rset.getObject(rsmd.getColumnLabel(i)).toString()).append(", ");
                }
            }
            line = new StringBuilder(line.substring(0, line.length() - ", ".length()));
            lines.append(line).append("\n");
        }
        historyArea.setText(lines.toString());
    }

    public void handleException(SQLException e) {
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
