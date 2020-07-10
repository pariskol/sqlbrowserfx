package sqlbrowserfx;

import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui2 extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setScene(new Scene(new SqlPane(new SqliteConnector("/home/paris/sqlite-dbs/chinook.db"))));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
