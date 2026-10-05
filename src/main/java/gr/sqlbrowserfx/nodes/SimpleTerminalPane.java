package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.codeareas.SearchableCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class SimpleTerminalPane extends BorderPane implements ToolbarOwner, InputMapOwner {

    private final SearchableCodeArea historyArea = new SearchableCodeArea();
    private final TextField commandLineField = new TextField();
    private final ListView<String> historyListView = new ListView<>();

    private String currentDirectory = SystemUtils.USER_HOME;

    private final Executor commandExecutor = Executors.newSingleThreadExecutor();

    public SimpleTerminalPane() {
        this(SystemUtils.USER_HOME);
    }

    public SimpleTerminalPane(String initialDirectory) {

        if (initialDirectory != null && !initialDirectory.isBlank()) {
            this.currentDirectory = Paths.get(initialDirectory)
                    .toAbsolutePath()
                    .normalize()
                    .toString();
        } else {
            this.currentDirectory = SystemUtils.USER_HOME;
        }

        commandLineField.setPromptText("Enter command here...");
        setCommandLineFieldAction();

        historyArea.setEditable(false);
        historyArea.setFocusTraversable(false);
        historyArea.prefWidthProperty().bind(this.widthProperty());

        setTop(createToolbar());
        setCenter(new VirtualizedScrollPane<>(historyArea));
        setBottom(commandLineField);

        setInputMap();
    }

    private void setCommandLineFieldAction() {
        commandLineField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                commandLineField.setDisable(true);
                historyArea.appendText(commandLineField.getText() + "\n");
                historyArea.requestFollowCaret();
                executeCommand();
            }
        });
    }

    private void executeCommand() {
        commandExecutor.execute(() -> {
            String command = commandLineField.getText().trim();

            try {

                // Handle cd internally (cannot be done via ProcessBuilder)
                if (command.startsWith("cd")) {

                    var parts = command.split("\\s+", 2);
                    var newDirectory = parts.length > 1 ? parts[1] : SystemUtils.USER_HOME;

                    if (newDirectory.startsWith("~")) {
                        newDirectory = newDirectory.replace("~", SystemUtils.USER_HOME);
                    }

                    var newPath = Paths.get(newDirectory);

                    if (!newPath.isAbsolute()) {
                        newPath = Paths.get(currentDirectory).resolve(newPath);
                    }

                    newPath = newPath.normalize().toAbsolutePath();

                    if (Files.exists(newPath) && Files.isDirectory(newPath)) {
                        currentDirectory = newPath.toString();
                        var finalDir = currentDirectory;

                        Platform.runLater(()
                                -> historyArea.appendText("Directory changed to: " + finalDir + "\n"));
                    } else {
                        var finalNewDirectory = newDirectory;
                        Platform.runLater(()
                                -> historyArea.appendText("Directory not found: " + finalNewDirectory + "\n"));
                    }

                } else {

                    var arguments = createProcessArguments();

                    var processBuilder = new ProcessBuilder(arguments);
                    processBuilder.directory(new File(currentDirectory));

                    var process = processBuilder.start();

                    var output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8)
                            .replaceAll("\\u001B\\[[;\\d]*[ -/]*[@-~]", "");
                    var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8)
                            .lines()
                            .map(line -> "[ERROR] " + line)
                            .collect(Collectors.joining(System.lineSeparator()));

                    process.waitFor();

                    if (!output.isEmpty()) {
                        Platform.runLater(() -> historyArea.appendText(output));
                    }

                    if (!error.isEmpty()) {
                        Platform.runLater(() -> historyArea.appendText(error));
                    }
                }

            } catch (IOException | InterruptedException e) {
                DialogFactory.createErrorDialog(e);
            } finally {
                Platform.runLater(() -> {
                    historyListView.getItems().add(commandLineField.getText());
                    commandLineField.clear();
                    historyArea.appendText(currentDirectory + "\n");
                    historyArea.requestFollowCaret();
                    commandLineField.setDisable(false);
                    commandLineField.requestFocus();
                });
            }
        });
    }

    private String[] createProcessArguments() {
        var isWindows = SystemUtils.OS_NAME.toLowerCase().contains("windows");

        if (isWindows) {
            return new String[]{
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "-Command",
                commandLineField.getText()
            };
        } else {
            return new String[]{
                "/bin/bash",
                "-c",
                commandLineField.getText()
            };
        }
    }

    @Override
    public void setInputMap() {
        var fetchPreviousCommand = InputMap.consume(
                EventPattern.keyPressed(KeyCode.UP, KeyCombination.CONTROL_DOWN),
                action -> {
                    if (!historyListView.getItems().isEmpty()) {
                        commandLineField.setText(historyListView.getItems().get(historyListView.getItems().size() - 1));
                    }
                });

        var copyToClipboardFromList = InputMap.consume(
                EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN),
                action -> {
                    var selectedItem = historyListView.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        StringSelection stringSelection = new StringSelection(selectedItem);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, null);
                    }
                });

        Nodes.addInputMap(commandLineField, fetchPreviousCommand);
        Nodes.addInputMap(historyListView, copyToClipboardFromList);
    }

    @Override
    public FlowPane createToolbar() {
        var clearHistoryButton = new Button("", JavaFXUtils.createIcon("/icons/clear.png"));
        clearHistoryButton.setTooltip(new Tooltip("Clear history"));
        clearHistoryButton.setOnAction(event -> {
            historyArea.clear();
            historyListView.getItems().clear();
        });

        var toggleHistoryListButton = new Button("", JavaFXUtils.createIcon("/icons/monitor.png"));
        toggleHistoryListButton.setTooltip(new Tooltip("Toggle commands history"));
        toggleHistoryListButton.setOnAction(event -> {
            if (historyListView.getParent() == null) {
                setRight(historyListView);
            } else {
                setRight(null);
            }
        });

        var toolbar = new CustomFlowPane(
                clearHistoryButton,
                toggleHistoryListButton
        );
        return toolbar;
    }
}
