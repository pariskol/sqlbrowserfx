package sqlbrowserfx;

import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SqlPaneTestGui extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Scene scene = new Scene(new SqlPane(new SqliteConnector("/home/paris/sqlite-dbs/chinook.db")), 800, 600);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
