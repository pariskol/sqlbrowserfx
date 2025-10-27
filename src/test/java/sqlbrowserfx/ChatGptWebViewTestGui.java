package sqlbrowserfx;

import java.io.IOException;

import org.fxmisc.richtext.CodeArea;

import gr.sqlbrowserfx.nodes.ChatGptWebView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatGptWebViewTestGui extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		var chatGptWebView = new ChatGptWebView();
	    var pasteButton = new Button("test");
	    var codeButton = new Button("code");
	    var codeArea = new CodeArea();
	    codeButton.setOnAction(event -> {
	    	System.out.println(chatGptWebView.getAiGeneratedCode());
	    });
	    pasteButton.setOnAction(event -> {
	    	String safeText = "Generate only code and only one code block. Generate an sql query"
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
	        	new Thread(() -> {
	        		try {
	        			Thread.sleep(3000);
	        			System.out.println("Getting code");
	        			Platform.runLater(() -> {
	        				codeArea.appendText(chatGptWebView.getAiGeneratedCode());
	        			});
	        		} catch(Exception e) {
	        			// Ignore
	        		}
	        	}).start();
	        	
	    });
		Scene scene = new Scene(
				new VBox(pasteButton, codeButton, chatGptWebView, codeArea)
		);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
