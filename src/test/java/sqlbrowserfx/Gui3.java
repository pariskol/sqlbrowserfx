package sqlbrowserfx;

import java.io.IOException;

import gr.sqlbrowserfx.nodes.HelpTabPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui3 extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		primaryStage.setScene(new Scene(new HelpTabPane()));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
