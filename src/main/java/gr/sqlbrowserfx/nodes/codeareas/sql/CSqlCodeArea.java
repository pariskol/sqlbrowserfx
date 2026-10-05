package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.util.Arrays;

import org.controlsfx.control.PopOver;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.nodes.CustomVBox;
import gr.sqlbrowserfx.nodes.ollama.OllamaHandler;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.SqlBrowserFXThreadUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class CSqlCodeArea extends SqlCodeArea {

    private PopOver saveQueryPopOver;

    @Override
    public ContextMenu createContextMenu() {
        var menu = super.createContextMenu();
        var menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/thunder.png"));
        menuItemSave.setOnAction(action -> this.saveQueryAction());
        menuItemSave.disableProperty().bind(this.isTextSelectedProperty().not());
        menu.getItems().addAll(menuItemSave);

        var executorRunning = new SimpleBooleanProperty(false);
        var ollama = new OllamaHandler() {
            @Override
            public void broadcast(String conversationId, String type, String content) {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }
        };

        var menuItemCheckSyntax = new MenuItem("(AI) Check Syntax", JavaFXUtils.createIcon("/icons/suggestion.png"));
        menuItemCheckSyntax.textProperty().bind(executorRunning.map(running -> running ? "(AI) Check Syntax (Running...)" : "(AI) Check Syntax"));
        menuItemCheckSyntax.disableProperty().bind(executorRunning);
        menuItemCheckSyntax.setOnAction(action -> {
            executorRunning.set(true);
            SqlBrowserFXThreadUtils.createDaemonThread(() -> {
                ollama.reportSqlSyntaxErrors(getText().isEmpty() ? getSelectedText() : getText());
                Platform.runLater(() -> executorRunning.set(false));
            }, "ai-syntax-check-thread");
        });

        var menuItemExplainSql = new MenuItem("(AI) Explain Sql", JavaFXUtils.createIcon("/icons/suggestion.png"));
        menuItemExplainSql.textProperty().bind(executorRunning.map(running -> running ? "(AI) Explain Sql (Running...)" : "(AI) Explain Sql"));
        menuItemExplainSql.disableProperty().bind(executorRunning);
        menuItemExplainSql.setOnAction(action -> {
            executorRunning.set(true);
            SqlBrowserFXThreadUtils.createDaemonThread(() -> {
                ollama.explainSql(getText().isEmpty() ? getSelectedText() : getText());
                Platform.runLater(() -> executorRunning.set(false));
            }, "ai-explain-thread");
        });

        var menuItemSqlSuggestions = new MenuItem("(AI) Sql Suggestions", JavaFXUtils.createIcon("/icons/suggestion.png"));
        menuItemSqlSuggestions.textProperty().bind(executorRunning.map(running -> running ? "(AI) Sql Suggestions (Running...)" : "(AI) Sql Suggestions"));
        menuItemSqlSuggestions.disableProperty().bind(executorRunning);
        menuItemSqlSuggestions.setOnAction(action -> {
            executorRunning.set(true);
            SqlBrowserFXThreadUtils.createDaemonThread(() -> {
                ollama.suggestSqlQuery(getText().isEmpty() ? getSelectedText() : getText());
                Platform.runLater(() -> executorRunning.set(false));
            }, "ai-suggestion-thread");
        });

        menu.getItems().addAll(
                new SeparatorMenuItem(), menuItemCheckSyntax, menuItemExplainSql, menuItemSqlSuggestions
        );

        return menu;
    }

    @Override
    protected void onMouseClicked() {
        super.onMouseClicked();
        if (saveQueryPopOver != null) {
            saveQueryPopOver.hide();
        }
    }

    @Override
    public void setInputMap() {
        if (!isEditable()) {
            return;
        }

        super.setInputMap();
        var saveQuery = InputMap.consume(
                EventPattern.keyPressed(KeyCode.S, KeyCombination.CONTROL_DOWN),
                action -> this.saveQueryAction()
        );

        Nodes.addInputMap(this, saveQuery);
    }

    protected void saveQueryAction() {
        final var sqlConnector = SqlBrowserFXAppManager.getConfigSqlConnector();
        var descriptionField = new TextField();
        descriptionField.setPromptText("Description");

        var categoryField = new ComboBox<String>();
        categoryField.setEditable(true);
        try {
            sqlConnector.executeQuery("select distinct category from saved_queries", rset -> categoryField.getItems().add(rset.getString(1)));
        } catch (SQLException e) {
            DialogFactory.createErrorNotification(e);
        }

        var addButton = new Button("Save", JavaFXUtils.createIcon("/icons/save.png"));

        categoryField.setPromptText("Write a name to create new...");
        var vb = new CustomVBox(
                new Label("Choose category"),
                categoryField,
                descriptionField,
                addButton);
        vb.setPrefWidth(300);

        addButton.setOnAction(event -> {
            sqlConnector.executeAsync(() -> {
                try {
                    var query = !this.getSelectedText().isEmpty() ? this.getSelectedText() : this.getText();
                    sqlConnector.executeUpdate("insert into saved_queries (query,category,description) values (?,?,?)",
                            Arrays.asList(query,
                                    categoryField.getSelectionModel().getSelectedItem(),
                                    descriptionField.getText()));
                    DialogFactory.createNotification("Info", "Query has been saved successfuly");
                    this.fireEvent(new SimpleEvent());
                    saveQueryPopOver.hide();
                } catch (SQLException e) {
                    DialogFactory.createErrorNotification(e);
                }
            });
        });

        categoryField.prefWidthProperty().bind(vb.widthProperty());
        saveQueryPopOver = new PopOver(vb);

        categoryField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                saveQueryPopOver.hide();
            }
        });
        descriptionField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                saveQueryPopOver.hide();
            }
        });
        saveQueryPopOver.setArrowSize(0);
        var boundsInScene = this.localToScreen(this.getBoundsInLocal());
        saveQueryPopOver.show(getParent(), boundsInScene.getMinX() + saveQueryPopOver.getWidth() / 3,
                boundsInScene.getMinY() - saveQueryPopOver.getHeight() / 2);
    }

}
