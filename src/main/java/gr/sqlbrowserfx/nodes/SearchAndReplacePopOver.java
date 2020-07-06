package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SearchAndReplacePopOver extends PopOver implements SimpleObservable<String> {

	private CodeArea codeArea;
	private volatile int lastPos;

	protected TextField findField;
	protected TextField replaceField;
	protected Button findButton;
	protected Button replaceButton;
	private List<SimpleChangeListener<String>> listeners;
	
	public SearchAndReplacePopOver(CodeArea codeArea) {
		this(codeArea, true);
	}
	@SuppressWarnings("unchecked")
	public SearchAndReplacePopOver(CodeArea codeArea, boolean enableReplace) {
		this.codeArea = codeArea;
		if (codeArea instanceof SimpleChangeListener) {
			this.addListener((SimpleChangeListener<String>)codeArea);
		}
		findField = new TextField();
		findField.setPromptText("Search...");
		findField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				this.hide();
			} else if (keyEvent.getCode() == KeyCode.ENTER) {
				this.findButtonAction();
			}
		});
		replaceField = new TextField();
		replaceField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				this.hide();
			}
//			else if (keyEvent.getCode() == KeyCode.ENTER) {
//				this.replaceButtonAction();
//			}
		});
		replaceField.setPromptText("Replace...");

		findButton = new Button("Find", JavaFXUtils.icon("/res/magnify.png"));
		findButton.setOnMouseClicked(mouseEvent -> this.findButtonAction());
		findButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.findButtonAction();
				keyEvent.consume();
			}
		});
		replaceButton = new Button("Replace", JavaFXUtils.icon("/res/replace.png"));
		replaceButton.setOnMouseClicked(mouseEvent -> this.replaceButtonAction());
		replaceButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.replaceButtonAction();
				keyEvent.consume();
			}
		});
//		findButton.prefWidthProperty().bind(findField.widthProperty());
//		replaceButton.prefWidthProperty().bind(findField.widthProperty());

		this.setArrowSize(0);
		if (enableReplace)
			this.setContentNode(new VBox(findField, replaceField, new HBox(findButton, replaceButton)));
		else
			this.setContentNode(new VBox(findField, findButton));

		this.setAutoHide(true);
	}
	
	private void findButtonAction() {
		if (!findField.getText().isEmpty()) {
			lastPos = codeArea.getText().indexOf(findField.getText(), codeArea.getCaretPosition());
			if (lastPos != -1) {
				codeArea.moveTo(lastPos + findField.getText().length());
				codeArea.requestFollowCaret();
				codeArea.selectRange(lastPos, lastPos + findField.getText().length());
			} else if (lastPos == -1) {
				lastPos = 0;
				codeArea.moveTo(0);
			}
		}
	}

	private void replaceButtonAction() {
		if (!replaceField.getText().isEmpty() && !codeArea.getSelectedText().isEmpty()) {
			String oldValue = codeArea.getSelectedText();
			codeArea.replaceSelection(replaceField.getText());
			this.changed(oldValue + ">" + replaceField.getText());
		}
	}
	
	public TextField getFindField() {
		return findField;
	}
	@Override
	public void changed() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void changed(String data) {
		Executors.newSingleThreadExecutor()
				 .execute(() -> listeners.forEach(l -> l.onChange(data)));
		
	}
	@Override
	public void addListener(SimpleChangeListener<String> listener) {
		if (this.listeners == null)
			this.listeners = new ArrayList<>();
		this.listeners.add(listener);
		
	}
	@Override
	public void removeListener(SimpleChangeListener<String> listener) {
		this.listeners.remove(listener);
	}
	
}
