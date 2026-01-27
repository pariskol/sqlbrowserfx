package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;

public class SearchAndReplacePopOver extends CustomPopOver implements SimpleObservable<String> {

	private final CodeArea codeArea;

	protected TextField findField;
	protected TextField replaceField;
	protected Button findButton;
	protected Button replaceButton;
	private List<SimpleObserver<String>> listeners;
	private final Button replaceAllButton;
	private final CheckBox wholeWordCheckBox;
	private final CheckBox caseInsensitiveCheckBox;
	private final Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	private volatile boolean javafxThreadRunning = false;
	private final Object javafxThreadRunningLock = new Object();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
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
				return;
			} else if (keyEvent.getCode() == KeyCode.ENTER) {
				this.findButtonAction();
			} 
			
			keyEvent.consume();

		});
		replaceField = new TextField();
		replaceField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				return;
			}
			else if (keyEvent.getCode() == KeyCode.ENTER) {
				this.replaceButtonAction();
			}
			
			keyEvent.consume();
		});
		replaceField.setPromptText("Replace...");

		findButton = new Button("Find", JavaFXUtils.createIcon("/icons/magnify.png"));
		findButton.setOnAction(event -> this.findButtonAction());
		replaceButton = new Button("Replace", JavaFXUtils.createIcon("/icons/replace.png"));
		replaceButton.setOnAction(event -> this.replaceButtonAction());
		
		replaceAllButton = new Button("Replace all", JavaFXUtils.createIcon("/icons/replace.png"));
		replaceAllButton.setOnAction(event -> this.replaceAllButtonAction());

		wholeWordCheckBox = new CheckBox("w");
		wholeWordCheckBox.setTooltip(new Tooltip("Whole Word"));
		wholeWordCheckBox.setFocusTraversable(false);
		caseInsensitiveCheckBox = new CheckBox("ci");
		caseInsensitiveCheckBox.setTooltip(new Tooltip("Case Insensitive"));
		caseInsensitiveCheckBox.setFocusTraversable(false);
		
		if (enableReplace) {
			this.setContentNode(new CustomHBox(new CustomVBox(findField, replaceField, new CustomHBox(findButton, replaceButton, replaceAllButton)),
										 new CustomVBox(wholeWordCheckBox, caseInsensitiveCheckBox))
					);
		}
		else {
			this.setContentNode(new CustomHBox(new CustomVBox(findField, findButton),
										 new CustomVBox(wholeWordCheckBox, caseInsensitiveCheckBox)));
		}
	}
	
	private int findButtonActionImpl() {
	    String pattern = findField.getText();
	    if (pattern.isEmpty()) return 0;

	    // timeout check
	    if (System.currentTimeMillis() >= terminationTime) {
	        logger.debug("Find action for '" + pattern + "' timed out");
	        return 0;
	    }

	    String text = codeArea.getText();

	    // handle case-insensitive
	    boolean caseInsensitive = caseInsensitiveCheckBox.isSelected();
	    String searchPattern = pattern;
	    if (caseInsensitive) {
	        searchPattern = pattern.toLowerCase();
	        text = text.toLowerCase();
	    }

	    // start searching from caret
	    int pos = text.indexOf(searchPattern, codeArea.getCaretPosition());

	    while (pos != -1) {
	        // handle whole-word
	        if (wholeWordCheckBox.isSelected()) {
	            boolean leftBoundary = (pos == 0) ||
	                    !Character.isLetterOrDigit(text.charAt(pos - 1));
	            boolean rightBoundary = (pos + searchPattern.length() >= text.length()) ||
	                    !Character.isLetterOrDigit(text.charAt(pos + searchPattern.length()));

	            if (!(leftBoundary && rightBoundary)) {
	                // not a whole word, continue searching
	                pos = text.indexOf(searchPattern, pos + 1);
	                continue;
	            }
	        }

	        // found a match, select it in JavaFX thread
	        final int finalPos = pos;
	        final String finalPattern = pattern;
	        javafxThreadRunning = true;
	        Platform.runLater(() -> {
	            synchronized (javafxThreadRunningLock) {
	                selectMatchingWord(finalPattern, finalPos);
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

	        return 1; // match found
	    }

	    // if no match found, wrap search to start of document
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

	    // optional small pause before recursive search
	    try { Thread.sleep(100); } catch (InterruptedException ignored) {}

	    return this.findButtonActionImpl(); // recursive search from start
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
			} finally {
				Platform.runLater(() -> this.disableButtons(false));
			}
		});
		
	}
	
	private void resetSearchTerminationTime() {
		terminationTime = System.currentTimeMillis() + 3000;
	}
	
	private void selectMatchingWord(String pattern, int position) {
	    // move caret to the end of the match
	    codeArea.moveTo(position + pattern.length());
	    codeArea.requestFollowCaret();

	    // select the matching range
	    codeArea.selectRange(position, position + pattern.length());
	}

	private void replaceButtonAction() {
		this.resetSearchTerminationTime();
		this.disableButtons(true);
		executor.execute(() -> {
			try {
				SearchAndReplacePopOver.this.replaceButtonActionImpl();
			} catch (Exception e) {
				logger.error(e.getMessage());
			} finally {
				Platform.runLater(() -> this.disableButtons(false));
			}
		});
	}
	
	private void replaceButtonActionImpl() {
	    // Timeout check
	    if (terminationTime <= System.currentTimeMillis()) {
	        logger.debug("Replace action timed out");
	        return;
	    }

	    String replacement = replaceField.getText();
	    String selectedText = codeArea.getSelectedText();

	    // Only replace if there is a selection and it's different
	    if (!selectedText.isEmpty() && !replacement.equals(selectedText)) {
	        String oldValue = selectedText;

	        javafxThreadRunning = true;
	        Platform.runLater(() -> {
	            synchronized (javafxThreadRunningLock) {
	                // Replace selected text
	                codeArea.replaceSelection(replacement);

	                // Move caret / select replaced text
	                int caretPos = codeArea.getCaretPosition() - replacement.length();
	                selectMatchingWord(replacement, caretPos);

	                // Notify change listener
	                this.changed(oldValue + ">" + replacement);

	                // Release lock
	                javafxThreadRunning = false;
	                javafxThreadRunningLock.notify();
	            }
	        });

	        // Wait for JavaFX thread to finish
	        while (javafxThreadRunning) {
	            synchronized (javafxThreadRunningLock) {
	                try {
	                    javafxThreadRunningLock.wait(100);
	                } catch (InterruptedException e) {
	                    LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
	                }
	            }
	        }
	    } else {
	        // No selection, find next match and replace
	        if (findButtonActionImpl() != 0) {
	            replaceButtonActionImpl(); // recursive call
	        }
	    }
	}

	
	public void replaceAllButtonAction() {
	    String searchText = findField.getText();
	    if (searchText.isEmpty()) return;

	    String replacementText = replaceField.getText();

	    // Build regex safely
	    String regex = Pattern.quote(searchText); // escape special characters

	    if (wholeWordCheckBox.isSelected() && searchText.matches("\\w+")) {
	    	regex = "\\b" + regex + "\\b"; // add word boundaries
	    }

	    if (caseInsensitiveCheckBox.isSelected()) {
	        regex = "(?i)" + regex; // case-insensitive
	    }

	    final String finalRegex = regex;
	    // Run replacement on JavaFX thread
	    Platform.runLater(() -> {
	        String originalText = codeArea.getText();
	        String replacedText = originalText.replaceAll(finalRegex, Matcher.quoteReplacement(replacementText));	        codeArea.replaceText(replacedText);
	        codeArea.moveTo(0);
	        // Notify change
	        this.changed(searchText + ">" + replacementText);
	    });
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
				 .execute(() -> listeners.forEach(l -> l.onObservableChange(data)));
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
