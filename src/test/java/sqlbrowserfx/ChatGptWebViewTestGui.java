package sqlbrowserfx;

import java.io.IOException;

import gr.sqlbrowserfx.nodes.codeareas.java.JavaCodeArea;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ChatGptWebViewTestGui extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
	    var webView = new WebView();
	    var webEngine = webView.getEngine();
	    // Force dark color scheme via CSS injection
	    String darkModeCSS = """
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
	                    
	                }
	            `;
	            document.documentElement.appendChild(style);
	        })();
	    """;

	    // Load ChatGPT and then apply dark mode styling
	    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
	        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
	            webEngine.executeScript(darkModeCSS);
	        }
	    });
	    webEngine.load("https://chatgpt.com/");
	    
	    var pasteButton = new Button("test");
	    pasteButton.setOnAction(event -> {
	    	String safeText = "This is a test text pasted from java"
	    	        .replace("\\", "\\\\")
	    	        .replace("\"", "\\\"")
	    	        .replace("\n", "\\n")
	    	        .replace("\r", "");

	    	    String js = """
	    	    (function() {
	    	    		const el = document.getElementById('prompt-textarea');
	    	            el.innerText = "%s";
	    	            setTimeout(() => {
	   	    	            const btn = document.getElementById('composer-submit-button');
    	    				btn.click();
	    	            }, 500);

	    	    })();
	    	    """.formatted(safeText);

	    	    webEngine.executeScript(js);
	    });
		Scene scene = new Scene(
				new VBox(pasteButton, webView)
		);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
