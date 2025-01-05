package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
            try {
                var arguments = createProcessArguments();
                var processBuilder = new ProcessBuilder(arguments);
                processBuilder.directory(new File(currentDirectory));
                var process = processBuilder.start();
                var output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);

                if (!output.isEmpty()) {
                    Platform.runLater(() -> historyArea.appendText(output));
                }
                if (!error.isEmpty()) {
                    Platform.runLater(() -> historyArea.appendText(error));
                }

                var processSucceeded = process.exitValue() == 0;
                var isCdCommand = List.of(arguments).contains("cd");

                if (processSucceeded && isCdCommand) {
                    var newDirectory = arguments[3];

                    var goToHomeDirectory = newDirectory.startsWith("~");
                    if (goToHomeDirectory) {
                        newDirectory = StringUtils.replace(newDirectory, "~", SystemUtils.USER_HOME);
                    }
                    if (!goToHomeDirectory && !newDirectory.startsWith("/")) {
                        newDirectory = "/" + newDirectory;
                    }
                    if (!goToHomeDirectory) {
                        newDirectory = this.currentDirectory + newDirectory;
                    }
                    // set normalized path without '.', '..'
                    this.currentDirectory = Paths.get(new File(newDirectory).getAbsolutePath()).normalize().toAbsolutePath().toString();

                    Platform.runLater(() -> historyArea.appendText("Directory changed to: " + this.currentDirectory + "\n"));
                }
            } catch (IOException e) {
                DialogFactory.createErrorDialog(e);
            } finally {
                Platform.runLater(() -> {
                    historyListView.getItems().add(commandLineField.getText());
                    commandLineField.clear();
                    historyArea.appendText(this.currentDirectory + "\n");
                    historyArea.requestFollowCaret();
                    commandLineField.setDisable(false);
                    commandLineField.requestFocus();
                });
            }
        });
    }
    private String[] createProcessArguments() {
        var isWindows = SystemUtils.OS_NAME.toLowerCase().contains("windows");
        var arguments = new ArrayList<String>();
        if (isWindows) {
            arguments.add("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
            arguments.add("-Command");
        } else {
            arguments.add("/bin/bash");
            arguments.add("-c");
        }
        var split = commandLineField.getText().split(" ");
        Collections.addAll(arguments, split);

        var resultArray = new String[arguments.size()];
        return arguments.toArray(resultArray);
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
            }
            else {
                setRight(null);
            }
        });

        var toolbar = new FlowPane(
                clearHistoryButton,
                toggleHistoryListButton
        );
        return toolbar;
    }
}
