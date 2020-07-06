package gr.sqlbrowserfx;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.factories.DialogFactory;
import javafx.application.Platform;

public class CodeAreaTailerListener implements TailerListener {

	private CodeArea codeArea;

	public CodeAreaTailerListener(CodeArea codeArea) {
		this.codeArea = codeArea;
	}
	@Override
	public void init(Tailer tailer) {
	}

	@Override
	public void fileNotFound() {
	}

	@Override
	public void fileRotated() {
	}

	@Override
	public void handle(String line) {
		Platform.runLater(() -> codeArea.appendText(line + "\n"));
	}

	@Override
	public void handle(Exception ex) {
		DialogFactory.createErrorDialog(ex);
	}

}
