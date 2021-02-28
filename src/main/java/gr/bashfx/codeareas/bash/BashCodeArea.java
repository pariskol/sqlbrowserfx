package gr.bashfx.codeareas.bash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.controlsfx.control.Notifications;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;

public class BashCodeArea extends CodeArea{

    private static final int LIST_ITEM_HEIGHT = 30;
    private static final int LIST_MAX_HEIGHT = 120;
    private static final int WORD_LENGTH_LIMIT = 45;
	private SearchAndReplacePopOver searchAndReplacePopOver;
	private String path;
	private Popup autoCompletePopup;
	private String fileName;
    
	public BashCodeArea() {
		this(null);
	}
	
	public BashCodeArea(String path) {
		super();
		this.setContextMenu(this.createContextMenu());
		autoCompletePopup = new Popup();
		this.setOnKeyTyped(event -> this.autoCompleteAction(event));
		this.path = path;

		searchAndReplacePopOver = new SearchAndReplacePopOver(this);

		this.caretPositionProperty().addListener((observable, oldPosition, newPosition) -> {
			if (autoCompletePopup != null)
				autoCompletePopup.hide();
		});

		this.enableHighlighting();
		this.setOnMouseClicked(mouseEvent -> searchAndReplacePopOver.hide());
		this.setOnKeysPressed();
		this.setParagraphGraphicFactory(LineNumberFactory.get(this));

	}
	
	@SuppressWarnings("unused")
	private void enableHighlighting() {
		Subscription subscription = this.multiPlainChanges().successionEnds(java.time.Duration.ofMillis(200))
				.subscribe(ignore -> {
					try {
						this.setStyleSpans(0, computeHighlighting(this.getText()));
					} catch (Exception e) {
						System.out.println("Ignored");
					}
					
				});
	}
	
	private ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy");
		menuItemCopy.setOnAction(event -> this.copy());

		MenuItem menuItemCut = new MenuItem("Cut");
		menuItemCut.setOnAction(event -> this.cut());

		MenuItem menuItemPaste = new MenuItem("Paste");
		menuItemPaste.setOnAction(event -> this.paste());

		menu.getItems().addAll(menuItemCopy, menuItemCut, menuItemPaste);
		return menu;
	}

    public static String getQuery(CodeArea codeArea, int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = codeArea.getText().substring(position - limit, position);
        keywords = keywords.replaceAll("\\p{Punct}", " ").trim();
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1);
    }

    public static ListView<String> getSuggestionsList(String query){
        List<String> suggestions = getQuerySuggestions(query);
        ListView<String> suggestionsList = new ListView<>();
        suggestionsList.getItems().clear();
        suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
        int suggestionsNum = suggestions.size();
        int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT : suggestionsNum * LIST_ITEM_HEIGHT;
        suggestionsList.setPrefHeight(listViewLength);
        return suggestionsList;
    }
    
    public String getPath() {
    	return path;
    }
    
    public String getFileName() {
    	return fileName;
    }

    private static List<String> getQuerySuggestions(String query) {
        List<String> suggestions = BashCodeAreaSyntax.KEYWORDS_lIST.parallelStream()
        							.filter(keyword -> keyword.startsWith(query)).collect(Collectors.toList());
//        suggestions.sort(Comparator.comparing(String::length).thenComparing(String::compareToIgnoreCase));
        return suggestions;
    }
    
	public void showSearchAndReplacePopup() {
		if (!this.getSelectedText().isEmpty()) {
			searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
			searchAndReplacePopOver.getFindField().selectAll();
		}
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		searchAndReplacePopOver.show(this, boundsInScene.getMaxX() - searchAndReplacePopOver.getWidth(),
				boundsInScene.getMinY());
	}
	
	private void setOnKeysPressed() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.S) {
					try {
						this.saveFileAction();
					} catch (Exception e) {
						this.saveAsFileAction();
					}
				}
				else if (keyEvent.getCode() == KeyCode.D) {
					boolean hasInitialSelectedText = false;
					if (this.getSelectedText().isEmpty())
						this.selectLine();
					else
						hasInitialSelectedText = true;
					
					this.replaceSelection("");
					
					if (!hasInitialSelectedText && this.getCaretPosition() != 0) {
						this.deletePreviousChar();
						this.moveTo(this.getCaretPosition() + 1);
					}
				}
				else if (keyEvent.getCode() == KeyCode.F) {
					this.showSearchAndReplacePopup();
				}
			}
		});
	}

	public void saveFileAction() {
		try {
			Files.write(Paths.get(this.path), this.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Notifications.create()
					 .owner(this)
					 .position(Pos.TOP_RIGHT)
					 .darkStyle()
					 .text("File saved at " + new Date().toString())
					 .hideAfter(Duration.millis(1000))
					 .showInformation();
	}
	
	public void saveAsFileAction() {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(null);
		try {
			this.path = selectedFile.getPath();
			this.fileName = selectedFile.getName();
		    Files.createFile(Paths.get(selectedFile.getPath()));
			Files.write(Paths.get(selectedFile.getPath()), this.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Notifications.create()
					 .owner(this)
					 .position(Pos.TOP_RIGHT)
					 .darkStyle()
					 .text("File saved at " + new Date().toString())
					 .hideAfter(Duration.millis(1000))
					 .showInformation();
	} 
	
	private void autoCompleteAction(KeyEvent event) {
		String ch = event.getCharacter();
		// for some reason keycode does not work
		if (Character.isLetter(ch.charAt(0)) || (event.isControlDown() && ch.equals(" "))) {
			int position = this.getCaretPosition();
			String query = getQuery(this, position);
			if (autoCompletePopup == null) {
				autoCompletePopup = new Popup();
			} else {
				autoCompletePopup.hide();
			}
			if (!query.trim().isEmpty()) {
				ListView<String> suggestionsList = getSuggestionsList(query);
				if (suggestionsList.getItems().size() != 0) {
					autoCompletePopup.getContent().clear();
					autoCompletePopup.getContent().add(suggestionsList);
					Bounds pointer = this.caretBoundsProperty().getValue().get();
					autoCompletePopup.show(this, pointer.getMaxX(), pointer.getMinY());
					suggestionsList.setOnKeyPressed(keyEvent -> {
						if (keyEvent.getCode() == KeyCode.ENTER) {
							AtomicReference<String> word = new AtomicReference<>();
							if (suggestionsList.getSelectionModel().getSelectedItem() != null) {
								String selected  = suggestionsList.getSelectionModel().getSelectedItem().toString();
								if (BashCodeAreaSyntax.TEMPLATES_MAP.containsKey(selected))
									selected = BashCodeAreaSyntax.TEMPLATES_MAP.get(selected);
								word.set(selected);
							} else {
								word.set(suggestionsList.getItems().get(0).toString());
							}
							Platform.runLater(() -> {
								this.replaceText(position - query.length(), position, word.get());
								this.moveTo(position + word.get().length() - query.length());
							});
							autoCompletePopup.hide();
						}
						if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.SPACE) {
							autoCompletePopup.hide();
							autoCompletePopup = null;
						}
					});
				}
			} else {
				autoCompletePopup.hide();
			}
		}
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = BashCodeAreaSyntax.PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("FUNCTION") != null ? "function"
							: matcher.group("METHOD") != null ? "method" : matcher.group("PAREN") != null ? "paren"
							: matcher.group("BRACE") != null ? "brace"
									: matcher.group("BRACKET") != null ? "bracket"
									: matcher.group("SEMICOLON") != null ? "semicolon"
											: matcher.group("STRING2") != null ? "string2"
													: matcher.group("STRING") != null ? "string"// : null
															: matcher.group("COMMENT") != null ? "comment"
																: matcher.group("DOLLAR") != null ? "dollar" 
																		: matcher.group("MINUS") != null ? "minus" :
																matcher.group("VAR") != null ? "var" :null;
			/* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}
	
}
