package sqlbrowserfx;

import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui1 extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setScene(new Scene(new DSqlPane(new SqliteConnector("/home/paris/sqllite-dbs/users.db"))));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
