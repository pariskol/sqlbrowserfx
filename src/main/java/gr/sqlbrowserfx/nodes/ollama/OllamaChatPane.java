package gr.sqlbrowserfx.nodes.ollama;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.CustomHBox;
import gr.sqlbrowserfx.nodes.CustomVBox;
import gr.sqlbrowserfx.nodes.codeareas.sql.OllamaCodeBlocksCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class OllamaChatPane extends BorderPane {

    private final OllamaCodeBlocksCodeArea chatArea = new OllamaCodeBlocksCodeArea();
    private final TextArea inputArea = new TextArea();
    private final Button sendBtn = new Button("Generate", JavaFXUtils.createIcon("/icons/play.png"));
    private final ComboBox<Conversation> conversationComboBox = new ComboBox<>();
    private final SqlConnector sqlConnector = SqlBrowserFXAppManager.getConfigSqlConnector();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final CustomHBox bottomHBox;
    private final CheckBox autoScrollCheckBox = new CheckBox("Autoscroll");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile String conversationId = null;
    private volatile boolean isAiTurn = false;
    private final OllamaHandler ollama = new OllamaHandler() {
        @Override
        public void broadcast(String conversationId, String type, String content) {
            if (type.equals("conversation_started")) {
                OllamaChatPane.this.conversationId = conversationId;
                setRecentConversations();
                return;
            }
            Platform.runLater(() -> {
                if (isAiTurn && type.equals("response_chunk")) {
                    chatArea.appendText("assistant: ");
                    isAiTurn = false;
                }
                if (type.equals("response_chunk")) {
                    chatArea.appendText(content);
                }
                if (type.equals("response_generated")) {
                    chatArea.appendText("\n\n");
                    inputArea.setDisable(false);
                    setBottom(bottomHBox);
                }
                
                if (autoScrollCheckBox.isSelected()) {
                    chatArea.requestFollowCaret();
                }
            });
        }
    };

    public OllamaChatPane() {
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(500);

        inputArea.setPromptText("Ask something... (Ctrl + ENTER)");
        autoScrollCheckBox.setSelected(true);

        var vbox = new CustomVBox(autoScrollCheckBox, inputArea);
        var split = new SplitPane(
            new VirtualizedScrollPane<>(chatArea), 
            vbox
        );
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.8, 0.2);
        inputArea.prefHeightProperty().bind(vbox.heightProperty());

        var refreshBtn = new Button("", JavaFXUtils.createIcon("/icons/refresh.png"));
        refreshBtn.setTooltip(new Tooltip("Reload Conversation"));
        refreshBtn.setOnAction(e -> setRecentConversations());

        var newBtn = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
        newBtn.setTooltip(new Tooltip("New Conversation"));
        newBtn.setOnAction(e -> {
            conversationId = null;
            chatArea.clear();
            setRecentConversations();
        });
        
        var topHBox = new CustomHBox(newBtn, refreshBtn, conversationComboBox);
        this.setTop(topHBox);
        conversationComboBox.prefWidthProperty().bind(topHBox.widthProperty());
        this.setCenter(split);
        this.bottomHBox = new CustomHBox(sendBtn);
        this.setBottom(this.bottomHBox);

        inputArea.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
        sendBtn.setOnAction(e -> sendMessage());
        
        setRecentConversations();

        conversationComboBox.setPromptText("Select Conversation");
        conversationComboBox.setOnAction(event -> {
            var convo = conversationComboBox.getValue();

            if (convo == null) {
                return;
            }

            conversationId = convo.getId();
            restoreConversation(conversationId);
        });

        progressIndicator.setMaxHeight(40);
        progressIndicator.setMaxWidth(40);

    }

    private void appendMessageToChat(String role, String content) {
        Platform.runLater(() -> chatArea.appendText(role + ": " + content + "\n\n"));
    }

    private void restoreConversation(String convo) {
        Platform.runLater(() -> chatArea.clear());
        executor.submit(() -> {
            try {
                var messages = new ArrayList<Message>();
                sqlConnector.executeQuery(
                        "SELECT * FROM ollama_messages WHERE conversation_id = ? ORDER BY id ASC",
                        Arrays.asList(conversationId),
                        rset -> {
                            try {
                                messages.add((Message) DTOMapper.map(rset, Message.class));
                            } catch (Exception e) {
                                DialogFactory.createErrorNotification(e);
                            }
                        }
                );

                messages.forEach(m -> appendMessageToChat(m.getRole(), m.getContent()));
            } catch (SQLException e) {
                DialogFactory.createErrorNotification(e);
            }
        });
    }

    private void setRecentConversations() {
        Platform.runLater(() -> conversationComboBox.getItems().clear());
        executor.submit(() -> {
            try {
                var conversationHistory = new ArrayList<Conversation>();
                sqlConnector.executeQuery(
                        "SELECT * FROM ollama_conversations ORDER BY created_at DESC LIMIT 20", rset -> {
                            Conversation convo;
                            try {
                                convo = (Conversation) DTOMapper.map(rset, Conversation.class);
                                conversationHistory.add(convo);
                            } catch (Exception e) {
                                DialogFactory.createErrorNotification(e);

                            }
                        });

                Platform.runLater(() -> {
                    conversationComboBox.getItems().addAll(conversationHistory);
                    var currentConversation = conversationHistory.stream()
                            .filter(c -> c.getId().equals(conversationId))
                            .findFirst();

                    if (currentConversation.isPresent()) {
                        conversationComboBox.getSelectionModel().select(currentConversation.get());
                    }
                });
            } catch (SQLException e) {
                DialogFactory.createErrorNotification(e);
            }
        });
    }

    private void sendMessage() {
        String prompt = inputArea.getText().trim();
        if (prompt.isEmpty()) {
            return;
        }

        inputArea.clear();

        var chatAreaWasEmpty = chatArea.getText().isEmpty();
        appendMessageToChat("user", prompt);

        startLoading();

        executor.submit(() -> {
            try {
                if (chatAreaWasEmpty) {
                    ollama.startConversation(prompt);
                    return;
                }

                ollama.sendMessage(conversationId, prompt);
            } catch (SQLException e) {
                DialogFactory.createErrorNotification(e);
            }
        });
    }

    private void startLoading() {
        inputArea.setDisable(true);
        setBottom(progressIndicator);
        isAiTurn = true;
    }

    public void reportSqlSyntaxErrors(String propmt) {
        if (propmt.isEmpty()) {
            return;
        }

        startLoading();
        executor.submit(() -> ollama.reportSqlSyntaxErrors(conversationId, propmt));
    }

    public void explainSql(String propmt) {
        if (propmt.isEmpty()) {
            return;
        }

        startLoading();
        executor.submit(() -> ollama.explainSql(conversationId, propmt));
    }

    public void suggestSql(String propmt) {
        if (propmt.isEmpty()) {
            return;
        }

        startLoading();
        executor.submit(() -> ollama.suggestSqlQuery(conversationId, propmt));
    }

    public void feedSchema(String propmt) {
        if (propmt.isEmpty()) {
            return;
        }

        startLoading();
        executor.submit(() -> ollama.feedSchema(conversationId, propmt));
    }
}
