package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
	private List<SimpleObserver<String>> listeners;
	private Button replaceAllButton;
	private CheckBox wholeWordCheckBox;
	private CheckBox caseInsensitiveCheckBox;
	private Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	private volatile boolean javafxThreadRunning = false;
	private Object javafxThreadRunningLock = new Object();
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private long terminationTime;
	
	public SearchAndReplacePopOver(CodeArea codeArea) {
		this(codeArea, true);
	}
	@SuppressWarnings("unchecked")
	public SearchAndReplacePopOver(CodeArea codeArea, boolean enableReplace) {
		this.codeArea = codeArea;
		if (codeArea instanceof SimpleObserver) {
			this.addObserver((SimpleObserver<String>)codeArea);
		}
		findField = new TextField();
		findField.setPromptText("Search...");
		findField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				this.hide();
			} else if (keyEvent.getCode() == KeyCode.ENTER) {
				this.findButtonAction();
			} else {
				keyEvent.consume();
			}
		});
		replaceField = new TextField();
		replaceField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				this.hide();
			}
			else if (keyEvent.getCode() == KeyCode.ENTER) {
				this.replaceButtonAction();
			}
			else {
				keyEvent.consume();
			}
		});
		replaceField.setPromptText("Replace...");

		findButton = new Button("Find", JavaFXUtils.createIcon("/icons/magnify.png"));
		findButton.setOnMouseClicked(mouseEvent -> this.findButtonAction());
		findButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.findButtonAction();
				keyEvent.consume();
			}
			else {
				keyEvent.consume();
			}
		});
		replaceButton = new Button("Replace", JavaFXUtils.createIcon("/icons/replace.png"));
		replaceButton.setOnMouseClicked(mouseEvent -> this.replaceButtonAction());
		replaceButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.replaceButtonAction();
				keyEvent.consume();
			}
			else {
				keyEvent.consume();
			}
		});
		
		replaceAllButton = new Button("Replace all", JavaFXUtils.createIcon("/icons/replace.png"));
		replaceAllButton.setOnMouseClicked(MouseEvent -> this.replaceAllButtonAction());
		replaceAllButton.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.replaceAllButtonAction();
				keyEvent.consume();
			}
			else {
				keyEvent.consume();
			}
		});

		wholeWordCheckBox = new CheckBox("ww");
		wholeWordCheckBox.setFocusTraversable(false);
		caseInsensitiveCheckBox = new CheckBox("ci");
		caseInsensitiveCheckBox.setFocusTraversable(false);
		
		this.setArrowSize(0);
		if (enableReplace) {
			this.setContentNode(new HBox(new VBox(findField, replaceField, new HBox(findButton, replaceButton, replaceAllButton)),
										 new VBox(wholeWordCheckBox, caseInsensitiveCheckBox))
					);
		}
		else {
			this.setContentNode(new HBox(new VBox(findField, findButton),
										 new VBox(wholeWordCheckBox, caseInsensitiveCheckBox)));
		}

		this.setAutoHide(true);
		this.setDetachable(false);
	}
	
	private int findButtonActionImpl() {
//		recursionRound++;
		String pattern = findField.getText();
		if (pattern.isEmpty())
			return 0;
		
		if (System.currentTimeMillis() >= terminationTime) {
			logger.debug("Find action for '" + pattern + "' timed out");
			Platform.runLater(() -> this.disableButtons(false));
			return 0;
		}
		
		String text = codeArea.getText();
		if (caseInsensitiveCheckBox.isSelected()) {
			pattern = pattern.toLowerCase();
			text = text.toLowerCase();
		}
		lastPos = text.indexOf(pattern, codeArea.getCaretPosition());
		
		if (lastPos != -1) {
//FIXME This is no working properly
//---------------------------------------------------------------------				
//				if (wholeWordCheckBox.isSelected()) {
//					if (lastPos != 0 && (text.charAt(lastPos - 1) == ' ' || text.charAt(lastPos - 1) == '\n')
//							&& (text.charAt(lastPos + pattern.length() + 1) == ' ' && text.charAt(lastPos + pattern.length() + 1) == '\n'))
//					else
//						return;
//				}
//---------------------------------------------------------------------				

			javafxThreadRunning = true;
			final String finalPattern = pattern;
			Platform.runLater(() -> {
				synchronized (javafxThreadRunningLock) {
					selectMatchingWord(finalPattern);
					javafxThreadRunning = false;
					javafxThreadRunningLock.notify();
				}
			});
			while (javafxThreadRunning) {
				synchronized (javafxThreadRunningLock) {
					try {
						javafxThreadRunningLock.wait(100);
					} catch (InterruptedException e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
					}
				}
			};
			
//				recursionRound = 0;
			Platform.runLater(() -> this.disableButtons(false));
			return 1;
		}
		else if (lastPos == -1) { // && recursionRound == 1) {
			lastPos = 0;
			javafxThreadRunning = true;
			Platform.runLater(() -> {
				synchronized (javafxThreadRunningLock) {
					codeArea.moveTo(0);
					javafxThreadRunning = false;
					javafxThreadRunningLock.notify();
				}
			});
			
			while (javafxThreadRunning) {
				synchronized (javafxThreadRunningLock) {
					try {
						javafxThreadRunningLock.wait(100);
					} catch (InterruptedException e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
					}
				}
			}
			//recursion
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.findButtonActionImpl();
		}
		
		return 0;
	}
	
	private void disableButtons(boolean disable) {
		this.findButton.setDisable(disable);
		this.replaceButton.setDisable(disable);
		this.replaceAllButton.setDisable(disable);
	}
	
	private void findButtonAction() {
		this.resetSearchTerminationTime();
		this.disableButtons(true);
		executor.execute(() -> {
			try {
				SearchAndReplacePopOver.this.findButtonActionImpl();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		});
		
	}
	
	private void resetSearchTerminationTime() {
		terminationTime = System.currentTimeMillis() + 2000;
	}
	
	private void selectMatchingWord(String pattern) {
		codeArea.moveTo(lastPos + pattern.length());
		codeArea.requestFollowCaret();
		codeArea.selectRange(lastPos, lastPos + pattern.length());
	}

	private void replaceButtonAction() {
		this.resetSearchTerminationTime();
		this.disableButtons(true);
		executor.execute(() -> {
			try {
				SearchAndReplacePopOver.this.replaceButtonActionImpl();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		});
	}
	
	private void replaceButtonActionImpl() {
		if (terminationTime <= System.currentTimeMillis()) {
			logger.debug("Replace action timed out");
			Platform.runLater(() -> this.disableButtons(false));
			return;
		}
		
		String replacement = replaceField.getText();
		if (!codeArea.getSelectedText().isEmpty() && !replacement.equals(codeArea.getSelectedText())) {
			String oldValue = codeArea.getSelectedText();
			javafxThreadRunning = true;
			Platform.runLater(() -> {
				synchronized (javafxThreadRunningLock) {
					codeArea.replaceSelection(replacement);
					selectMatchingWord(replacement);
					this.changed(oldValue + ">" + replacement);
					javafxThreadRunning = false;
					javafxThreadRunningLock.notify();
				}
			});
			
			while (javafxThreadRunning) {
				synchronized (javafxThreadRunningLock) {
					try {
						javafxThreadRunningLock.wait(100);
					} catch (InterruptedException e) {
						LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
					}
				}
			}
		}
		else {
	    	if (findButtonActionImpl() != 0)
	    		replaceButtonActionImpl();
		}
	}
	
	public void replaceAllButtonAction() {
		if (!findField.getText().isEmpty()) {
			String pattern = findField.getText();
			if (wholeWordCheckBox.isSelected())
				pattern = "\\b" + pattern + "\\b";
			if (caseInsensitiveCheckBox.isSelected())
				pattern = "(?i)" + pattern;
			
			String replacement = codeArea.getText().replaceAll(pattern, replaceField.getText());
			codeArea.replaceText(replacement);
			this.changed(findField.getText() + ">" + replaceField.getText());
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
		if (listeners != null) {
			Executors.newSingleThreadExecutor()
				 .execute(() -> listeners.forEach(l -> l.onObservaleChange(data)));
		}
		
	}
	@Override
	public void addObserver(SimpleObserver<String> listener) {
		if (this.listeners == null)
			this.listeners = new ArrayList<>();
		this.listeners.add(listener);
		
	}
	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		this.listeners.remove(listener);
	}
	
}
