package sqlbrowserfx;

import java.sql.SQLException;

import org.json.JSONArray;

import gr.sqlbrowserfx.nodes.sqlTableView.MapTableView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kong.unirest.Unirest;

public class TestJsonTableView extends Application{

	public static void main(String[] args) {
//		BasicConfigurator.configure();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("SqlBrowser");
		JSONArray jsonArray = new JSONArray(Unirest.get("https://www.psantamouris.gr/get/customers").asString().getBody());
		MapTableView tableView = new MapTableView();
		try {
			tableView.setItemsLater(jsonArray);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		primaryStage.setScene(new Scene(tableView));
		primaryStage.show();

	}
}
