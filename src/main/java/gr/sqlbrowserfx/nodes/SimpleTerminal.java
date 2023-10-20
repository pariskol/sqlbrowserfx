package gr.sqlbrowserfx.nodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.factories.DialogFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

public class SimpleTerminal extends BorderPane {

	private CodeArea historyArea = new CodeArea();
	private TextField commandLineField = new TextField();

	public SimpleTerminal() {
		commandLineField.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				try {
					var process = new ProcessBuilder(commandLineField.getText());
					var output = IOUtils.toString(process.start().getInputStream(), StandardCharsets.UTF_8);
					historyArea.appendText(output);
				} catch (IOException e) {
					DialogFactory.createErrorDialog(e);
				}
			}
		});
		historyArea.setEditable(false);
		historyArea.setFocusTraversable(false);
		historyArea.requestFollowCaret();
		
		setCenter(historyArea);
		setBottom(commandLineField);
	}
}
