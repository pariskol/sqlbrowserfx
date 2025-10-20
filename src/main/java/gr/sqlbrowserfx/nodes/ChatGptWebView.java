package gr.sqlbrowserfx.nodes;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class ChatGptWebView extends BorderPane implements ContextMenuOwner, InputMapOwner {
	
	private final String darkModeCssJs = """
        (function() {
            const style = document.createElement('style');
            style.textContent = `
                :root {
                    color-scheme: dark;
                    background-color: #222222 !important;
                    color: #e0e0e0 !important;
                }
                html, body *:not(pre):not(pre *) {
                    background-color: #222222 !important;
                    color: #e0e0e0 !important;
                    margin: 0 !important;
                    box-shadow: none !important;
					text-shadow: none !important;
                }
                pre * {
                    background-color: #2d2d2d !important;
                    color: #e0e0e0 !important;
                    border-radius: 5px !important;
                }
			    button {
			    	border-radius: 5px !important;
			    	border-width: 2px !important;
			    	border-color: #2d2d2d !important;
				}
				button:hover {
					border-color: #196de3 !important;
				}
				form {
                    border-radius: 5px !important;
					border-width: 2px !important;
					border-color: #2d2d2d !important;
				}
            `;
            document.documentElement.appendChild(style);
        })();
    """;
	private final String selectedTextJs = """
		(function() {
    		let text = "";

		    if (window.getSelection) {
		        text = window.getSelection().toString();
		    } else if (document.selection && document.selection.type != "Control") {
		        text = document.selection.createRange().text;
		    }
		
		    return text;
        })();
	""";
	
	private final String clickAskButtonJs = """
		(function() {
    		const btn = document.getElementById('composer-submit-button');
			btn.click();
        })();
	""";

	private final WebView webView = new WebView();;
	
	public ChatGptWebView() {	    
	    var webEngine = webView.getEngine();
	    // Load ChatGPT and then apply dark mode styling
	    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
	        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
	            webEngine.executeScript(this.darkModeCssJs);
	        }
	    });
	    webEngine.load("https://chatgpt.com/");
	    
	    this.setContextMenu();
	    this.setInputMap();
	    this.setCenter(webView);
	}
	
	public WebEngine getEngine() {
		return this.webView.getEngine();
	}
	
	private void setContextMenu() {
		var contextMenu = this.createContextMenu();
	    webView.setContextMenuEnabled(false); // disable native WebView menu
	    webView.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
	        if (mouseEvent.getButton() == MouseButton.SECONDARY) { // right-click
	            contextMenu.show(webView, mouseEvent.getScreenX(), mouseEvent.getScreenY());
	            mouseEvent.consume(); // prevent default
	        } else {
	            contextMenu.hide();
	        }
	    });
	}

	@Override
    public void setInputMap() {
        var copy = InputMap.consume(
                EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN),
                action -> this.copySelectedTextToClipoboard());
        
        var ask = InputMap.consume(
                EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                action -> this.clickAskButton());
        
        Nodes.addInputMap(this.webView, ask);
        Nodes.addInputMap(this.webView, copy);
        Nodes.addInputMap(this, copy);
    }
    
	private String pasteAndAskJs(String question) {
		var safeText = question.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\n", "\\n").replace("\r", "");
		
		return """
				(function() {
				const el = document.getElementById('prompt-textarea');
		        el.innerText = "%s";
		        setTimeout(() => {
		            const btn = document.getElementById('composer-submit-button');
		btn.click();
		        }, 500);

		})();
		""".formatted(safeText);
	}
	
	public void askChatGpt(String question) {
		this.getEngine().executeScript(this.pasteAndAskJs(question));
	}
	
	private void clickAskButton() {
		this.getEngine().executeScript(this.clickAskButtonJs);
	}
	
	private void copySelectedTextToClipoboard() {
		var text = (String) webView.getEngine().executeScript(this.selectedTextJs);
	    var clipboard = Clipboard.getSystemClipboard();
	    var content = new ClipboardContent();
	    content.putString(text);
	    clipboard.setContent(content);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var copySelectedHtmlText = new MenuItem("Copy Selected Text", JavaFXUtils.createIcon("/icons/copy.png"));
	    copySelectedHtmlText.setOnAction(copyAction -> this.copySelectedTextToClipoboard());
	    
	    var refresh = new MenuItem("Refresh", JavaFXUtils.createIcon("/icons/refresh.png"));
	    refresh.setOnAction(copyAction -> this.getEngine().reload());
	    
	    return new ContextMenu(copySelectedHtmlText, refresh);
	}

}
