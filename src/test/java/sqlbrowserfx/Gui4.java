package sqlbrowserfx;

import java.io.IOException;

import gr.sqlbrowserfx.nodes.codeareas.java.JavaCodeArea;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui4 extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		Scene scene = new Scene(new JavaCodeArea(), 800, 600);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
