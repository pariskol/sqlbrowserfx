/**
 * @file DockFX.java
 * @brief Driver demonstrating basic dock layout with prototypes. Maintained in a separate package
 *        to ensure the encapsulation of org.dockfx private package members.
 *
 * @section License
 *
 *          This file is a part of the DockFX Library. Copyright (C) 2015 Robert B. Colton
 *
 *          This program is free software: you can redistribute it and/or modify it under the terms
 *          of the GNU Lesser General Public License as published by the Free Software Foundation,
 *          either version 3 of the License, or (at your option) any later version.
 *
 *          This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *          WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *          PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 *          You should have received a copy of the GNU Lesser General Public License along with this
 *          program. If not, see <http://www.gnu.org/licenses/>.
 **/

package gr.paris;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.paris.dock.nodes.DBTreeView;
import gr.paris.dock.nodes.DSqlConsoleView;
import gr.paris.dock.nodes.DSqlPane;
import gr.paris.nodes.Keywords;
import gr.paris.nodes.MySqlConfigBox;
import gr.paris.rest.service.RestServiceConfig;
import gr.paris.rest.service.SparkRestService;
import gr.sqlfx.conn.MysqlConnector;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqliteConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserApp extends Application {

	private static final String RECENT_DBS_PATH = "./recent-dbs.txt";
	private static String DB = "/home/paris/sqllite-dbs/users.db";
	private static RestServiceConfig restServiceConfig;
	private static Logger logger = LoggerFactory.getLogger("SPARK");;

	private Scene scene;
	private Stage stage;
	private SqlConnector sqlConnector;
	private boolean restServiceStarted;

	public static void main(String[] args) {
		Keywords.onKeywordsBind();
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("log4j.properties"));
			PropertyConfigurator.configure(props);
		} catch (Exception e) {
			BasicConfigurator.configure();
		}
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		primaryStage.setTitle("SqlBrowser");

		createDBselectBox();

		primaryStage.setScene(scene);
		primaryStage.sizeToScene();

		primaryStage.getIcons().add(new Image("/res/sqlite.png"));

		primaryStage.show();

		primaryStage.setOnCloseRequest(closeEvent -> {
			boolean exists = false;
			try (Stream<String> stream = Files.lines(Paths.get(RECENT_DBS_PATH))) {
				exists = stream.anyMatch(line -> line.equals(DB));
				if (!exists)
					Files.write(Paths.get(RECENT_DBS_PATH), (DB + "\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
			Platform.exit();
			System.exit(0);
		});

	}

	private void createDBselectBox() {
		Text selectedDBtext = new Text("No database selected");
		Button openButton = new Button("Open", new ImageView(new Image("/res/database.png")));
		openButton.setOnAction(actionEvent -> dbSelectionAction(selectedDBtext.getText()));
		HBox bottomBox = new HBox(selectedDBtext, openButton);
		bottomBox.setPadding(new Insets(5));
		bottomBox.setSpacing(5);
		bottomBox.setAlignment(Pos.CENTER_RIGHT);

		VBox rightBox = new VBox();
		Text text = new Text("Browse system for database...");
		Button fileChooserButton = new Button("Search", new ImageView(new Image("/res/magnify.png")));
		fileChooserButton.setOnAction(actionEvent -> {
			FileChooser fileChooser = new FileChooser();
			File selectedFile = fileChooser.showOpenDialog(null);

			if (selectedFile != null) {
				selectedDBtext.setText(selectedFile.getAbsolutePath());
			}
		});
		rightBox.getChildren().addAll(text, fileChooserButton);
		rightBox.setAlignment(Pos.CENTER);
		rightBox.setSpacing(5);

		Text recentDBsText = new Text("Recently opened");
		recentDBsText.setTextAlignment(TextAlignment.CENTER);

		ListView<String> recentDBsList = new ListView<>();
		try (Stream<String> stream = Files.lines(Paths.get(RECENT_DBS_PATH))) {
			stream.forEach(line -> recentDBsList.getItems().add(line));
		} catch (IOException e) {
			DialogFactory.createErrorDialog(e);
		}

		recentDBsList.setOnMouseClicked(
				mouseEvent -> selectedDBtext.setText(recentDBsList.getSelectionModel().getSelectedItem()));
		VBox leftBox = new VBox(recentDBsText, recentDBsList);
		leftBox.setAlignment(Pos.CENTER);
		leftBox.setPadding(new Insets(5));
		leftBox.setSpacing(5);

		BorderPane borderPane = new BorderPane();
		borderPane.setPrefSize(600, 400);
		borderPane.setCenter(rightBox);
		borderPane.setLeft(leftBox);
		borderPane.setBottom(bottomBox);

		Tab sqliteTab = new Tab("Sqlite", borderPane);
		sqliteTab.setClosable(false);
		MySqlConfigBox mySqlConfigBox = new MySqlConfigBox();
		mySqlConfigBox.getSubmitButton().setOnAction(actionEvent -> {
			dbSelectionAction(mySqlConfigBox);
		});
		Tab mysqlTab = new Tab("MySQL", mySqlConfigBox);
		mysqlTab.setClosable(false);
		TabPane dbTabPane = new TabPane(sqliteTab, mysqlTab);
		scene = new Scene(dbTabPane, 600, 400);
		scene.getStylesheets().add(DockPane.class.getResource("default.css").toExternalForm());
		scene.getStylesheets().add("/res/basic.css");

	}

	private void dbSelectionAction(String dbPath) {
		if (dbPath.equals("No database selected"))
			return;

		DB = dbPath;
		restServiceConfig = new RestServiceConfig("localhost", 8080, DB);

		SqlConnector sqliteConnector = new SqliteConnector(dbPath);
		this.sqlConnector = sqliteConnector;
		dbSelectionAction(sqliteConnector);

	}

	private void dbSelectionAction(MySqlConfigBox configBox) {
		
		DB = configBox.getDatabaseField().getText();
		restServiceConfig = new RestServiceConfig("localhost", 8080, DB);
		try {
			SqlConnector mysqlConnector = new MysqlConnector(configBox.getDatabaseField().getText(),
					configBox.getUserField().getText(), configBox.getPasswordField().getText());
			this.sqlConnector = mysqlConnector;

			dbSelectionAction(mysqlConnector);
		} catch (Exception e) {
			DialogFactory.createErrorDialog(e);
		}
	}

	private void dbSelectionAction(SqlConnector sqlConnector) {
		DockPane dockPane = new DockPane();

		DSqlPane sqlPane = new DSqlPane(sqlConnector);
		sqlPane.asDockNode().setPrefSize(scene.getWidth()/2, scene.getHeight());
		sqlPane.asDockNode().dock(dockPane, DockPos.CENTER, 0.8f);
		sqlPane.sqlConsoleButtonAction();

//		DSqlPane sqlPane2 = new DSqlPane(sqlConnector);
//		sqlPane2.asDockNode().setPrefSize(scene.getWidth()/4, scene.getHeight());
//		sqlPane2.asDockNode().dock(dockPane, DockPos.RIGHT, sqlPane.asDockNode());

//		DSqlConsoleView dSqlConsoleView = new DSqlConsoleView(sqlConnector);
//		dSqlConsoleView.asDockNode().setPrefSize(scene.getWidth() / 4, scene.getHeight());
//		dSqlConsoleView.asDockNode().dock(dockPane, DockPos.BOTTOM, sqlPane2.asDockNode());

		DBTreeView treeView = new DBTreeView(DB, sqlConnector);
		Keywords.bind(treeView.getContentNames());
		treeView.addListener(value -> Keywords.bind(treeView.getContentNames()));
		sqlPane.getSqlConsoleBox().addListener(treeView);
		DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
		dockNode.dock(dockPane, DockPos.LEFT, 0.2f);
		// fixed size 
		//		SplitPane.setResizableWithParent(dockNode, Boolean.FALSE);
		
		MenuBar menuBar = createMenu(dockPane);

//		Button addTableButton = new Button("Add table", JavaFXUtils.createImageView("/res/add.png"));
//		Button deleteTableButton = new Button("Delete table", JavaFXUtils.createImageView("/res/close.png"));
//		Button editTableButton = new Button("Edit table", JavaFXUtils.createImageView("/res/edit.png"));
//		ToolBar toolBar = new ToolBar(addTableButton, editTableButton, deleteTableButton);

//		HBox statusBar = new HBox(new Label("database connection"), new Button("on"), new Label("rest service"), new Button("on"));
//		statusBar.setAlignment(Pos.CENTER_RIGHT);
		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(menuBar, dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);
		scene.setRoot(vbox);
		stage.heightProperty().addListener((obs, oldVal, newVal) -> {
			for (SplitPane split : dockPane.getSplitPanes()) {
			    double[] positions = split.getDividerPositions(); // reccord the current ratio
			    Platform.runLater(() -> split.setDividerPositions(positions)); // apply the now former ratio
			}
		});
	}

	private MenuBar createMenu(DockPane dockPane) {
		final Menu menu1 = new Menu("Views", JavaFXUtils.icon("/res/open-view.png"));
		MenuItem sqlPaneViewItem = new MenuItem("Open Table View", JavaFXUtils.icon("/res/database.png"));
		sqlPaneViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlPane newSqlPane = new DSqlPane(sqlConnector);
//				newSqlPane.asDockNode().setPrefSize(scene.getWidth() / 2, scene.getHeight() / 2);
				newSqlPane.asDockNode().dock(dockPane, DockPos.RIGHT);

			});
		});
//		MenuItem tabedSqlPaneViewItem = new MenuItem("Open Tabed Table View", JavaFXUtils.icon("/res/m-database.png"));
//		tabedSqlPaneViewItem.setOnAction(event -> {
//			Platform.runLater(() -> {
//				DTabSqlPane newSqlPane = new DTabSqlPane(sqlConnector);
////				newSqlPane.asDockNode().setPrefSize(scene.getWidth() / 2, scene.getHeight() / 2);
//				newSqlPane.asDockNode().dock(dockPane, DockPos.RIGHT);
//
//			});
//		});
		MenuItem sqlConsoleViewItem = new MenuItem("Open Console View", JavaFXUtils.icon("/res/console.png"));
		sqlConsoleViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlConsoleView sqlConsoleView = new DSqlConsoleView(sqlConnector);
//				sqlConsoleView.asDockNode().setPrefSize(scene.getWidth() / 2, scene.getHeight() / 2);
				sqlConsoleView.asDockNode().dock(dockPane, DockPos.RIGHT);

			});
		});
		MenuItem tablesTreeViewItem = new MenuItem("Open structure tree view", JavaFXUtils.icon("/res/details.png"));
		tablesTreeViewItem.setOnAction(event -> {
			TreeView<String> treeView = new DBTreeView(DB, sqlConnector);
			DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
			dockNode.dock(dockPane, DockPos.LEFT);	
		});

		menu1.getItems().addAll(sqlPaneViewItem, sqlConsoleViewItem, tablesTreeViewItem);

		final Menu menu2 = new Menu("Rest Service", new ImageView(new Image("/res/spark.png", 16, 16, false, false)));
		MenuItem restServiceStartItem = new MenuItem("Start Rest Service");
		restServiceStartItem.setOnAction(actionEvent -> {
			if (restServiceStarted == false) {
				SparkRestService.configure(restServiceConfig.getIp(), restServiceConfig.getPort());
				SparkRestService.init(sqlConnector);
				SparkRestService.start();
				restServiceStartItem.setText("Stop Rest Service");
				restServiceStarted = true;
			} else {
				SparkRestService.stop();
				restServiceStarted = false;
				restServiceStartItem.setText("Start Rest Service");
			}
		});

		MenuItem restServiceConfigItem = new MenuItem("Configure Rest Service");
		restServiceConfigItem.setOnAction(actionEvent -> createRestServiceConfigBox());

		menu2.getItems().addAll(restServiceStartItem, restServiceConfigItem);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menu1, menu2);

		return menuBar;
	}

	private void createRestServiceConfigBox() {
		ImageView bottleLogo = JavaFXUtils.createImageView("/res/spark-logo.png", 100.0, 50.0);
		Label ipLabel = new Label("Ip address");
		TextField ipField = new TextField(restServiceConfig.getIp());
		Label portLabel = new Label("Port");
		TextField portField = new TextField(restServiceConfig.getPort().toString());
		Button saveButton = new Button("Save", JavaFXUtils.icon("/res/check.png"));
		saveButton.setOnAction(actionEvent -> {
			restServiceConfig.setIp(ipField.getText());
			restServiceConfig.setPort(Integer.parseInt(portField.getText()));
		});

		VBox vBox = new VBox(bottleLogo, ipLabel, ipField, portLabel, portField, saveButton);
		vBox.setPadding(new Insets(15));

		Stage stage = new Stage();
		Scene scene = new Scene(vBox);
		stage.setTitle("Rest service configuration");
		stage.setScene(scene);
		stage.show();
	}

}
