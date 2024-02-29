package sqlbrowserfx;

import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.nodes.TableCreationPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TableCreationPaneTestGui extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setScene(new Scene(new TableCreationPane(new SqliteConnector("/home/paris/sqlite-dbs/chinook.db"))));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
