package sqlbrowserfx;

import java.io.IOException;

import gr.sqlbrowserfx.nodes.ChatGptWebView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatGptWebViewTestGui extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		var chatGptWebView = new ChatGptWebView();
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

	    	    chatGptWebView.getEngine().executeScript(js);
	    });
		Scene scene = new Scene(
				new VBox(pasteButton, chatGptWebView)
		);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
