package sqlbrowserfx;

import gr.sqlbrowserfx.HelpTabPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui3 extends Application {

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setScene(new Scene(new HelpTabPane()));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
