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

package gr.sqlbrowserfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DBTreeView;
import gr.sqlbrowserfx.dock.nodes.DSqlConsoleView;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.MySqlConfigBox;
import gr.sqlbrowserfx.rest.service.RestServiceConfig;
import gr.sqlbrowserfx.rest.service.SparkRestService;
import gr.sqlbrowserfx.sqlPane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.Keywords;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserFXApp extends Application {

	private static final String RECENT_DBS_PATH = "./recent-dbs.txt";
	private static String DB = "/home/paris/sqllite-dbs/users.db";
	private static RestServiceConfig restServiceConfig;

	private Scene scene;
	private Stage stage;
	private DSqlPane mainSqlPane;
	DraggingTabPaneSupport dragingSupport;
	
	private SqlConnector sqlConnector;
	private boolean restServiceStarted;

	public static void main(String[] args) {
		BasicConfigurator.configure();

		Keywords.onKeywordsBind();
		launch(args);
	}

//	@Override
//	public void start(Stage primaryStage) {
//		stage = primaryStage;
//		primaryStage.setTitle("SqlBrowser");
//
//		scene = new Scene(new BashCodeArea());
//		scene.getStylesheets().add(DockPane.class.getResource("default.css").toExternalForm());
//		scene.getStylesheets().add("/res/basic.css");
//		primaryStage.setScene(scene);
//		primaryStage.sizeToScene();
//
//		primaryStage.getIcons().add(new Image("/res/sqlite.png"));
//
//		primaryStage.show();
//
//		primaryStage.setOnCloseRequest(closeEvent -> {
//			boolean exists = false;
//			try (Stream<String> stream = Files.lines(Paths.get(RECENT_DBS_PATH))) {
//				exists = stream.anyMatch(line -> line.equals(DB));
//				if (!exists)
//					Files.write(Paths.get(RECENT_DBS_PATH), (DB + "\n").getBytes(), StandardOpenOption.APPEND);
//			} catch (IOException e) {
//				DialogFactory.createErrorDialog(e);
//			}
//			Platform.exit();
//			System.exit(0);
//		});
//
//	}
	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		primaryStage.setTitle("SqlBrowser");

		createDBselectBox();

		primaryStage.setScene(scene);
		primaryStage.sizeToScene();

		primaryStage.getIcons().add(new Image("/res/sqlbrowser-fx.png"));

		primaryStage.show();

		primaryStage.setOnCloseRequest(closeEvent -> {
			if (sqlConnector instanceof SqliteConnector) {
				boolean exists = false;
				try (Stream<String> stream = Files.lines(Paths.get(RECENT_DBS_PATH))) {
					exists = stream.anyMatch(line -> line.equals(DB));
					if (!exists)
						Files.write(Paths.get(RECENT_DBS_PATH), (DB + "\n").getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					DialogFactory.createErrorDialog(e);
				}
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
		sqliteTab.setGraphic(JavaFXUtils.createImageView("/res/sqlite.png", 28.0, 28.0));
		sqliteTab.setClosable(false);
		MySqlConfigBox mySqlConfigBox = new MySqlConfigBox();
		mySqlConfigBox.getConnectButton().setOnAction(actionEvent -> {
			mySqlConfigBox.showLoader(true);
			dbSelectionAction(mySqlConfigBox);
		});
		Tab mysqlTab = new Tab("MySQL", mySqlConfigBox);
		mysqlTab.setGraphic(JavaFXUtils.createImageView("/res/mysql.png", 28.0, 28.0));
		mysqlTab.setClosable(false);
		TabPane dbTabPane = new TabPane(sqliteTab, mysqlTab);
		scene = new Scene(dbTabPane, 600, 400);
		scene.getStylesheets().add(DockPane.class.getResource("default.css").toExternalForm());
		scene.getStylesheets().add("/res/basic.css");
		scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (scene.getFocusOwner() instanceof Button) {
					((Button) scene.getFocusOwner()).getOnAction().handle(new ActionEvent());
				}
			}
		});

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
		
		configBox.getConnectButton().setDisable(true);
		DB = configBox.getDatabaseField().getText();
		restServiceConfig = new RestServiceConfig("localhost", 8080, DB);
			SqlConnector mysqlConnector = new MysqlConnector(configBox.getDatabaseField().getText(),
					configBox.getUserField().getText(), configBox.getPasswordField().getText());
			this.sqlConnector = mysqlConnector;
			
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					mysqlConnector.checkConnection();
				} catch (SQLException e) {
					LoggerFactory.getLogger("SQLBROWSER").error(e.getMessage(), e);
					configBox.showLoader(false);
					DialogFactory.createErrorDialog(e);
					configBox.getConnectButton().setDisable(false);
					return;
				}
				
				Platform.runLater(() -> dbSelectionAction(mysqlConnector));
			});
	}

	private void dbSelectionAction(SqlConnector sqlConnector) {
		stage.setMaximized(true);
		DockPane dockPane = new DockPane();

		mainSqlPane = new DSqlPane(sqlConnector);
		mainSqlPane.asDockNode().dock(dockPane, DockPos.CENTER, new double[] {0.8f});
		mainSqlPane.asDockNode().setClosable(false);
		mainSqlPane.sqlConsoleButtonAction();

		DBTreeView treeView = new DBTreeView(DB, sqlConnector);
		Keywords.bind(treeView.getContentNames());
		treeView.addListener(value -> Keywords.bind(treeView.getContentNames()));
		mainSqlPane.getSqlConsoleBox().addListener(treeView);
		DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
		dockNode.dock(dockPane, DockPos.LEFT, new double[] {0.2f});
		dockNode.setClosable(false);
		// fixed size 
		SplitPane.setResizableWithParent(dockNode, Boolean.FALSE);
		
		MenuBar menuBar = createMenu(dockPane);

		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(menuBar, dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);
		scene.setRoot(vbox);
		stage.heightProperty().addListener((obs, oldVal, newVal) -> {
			SplitPane.setResizableWithParent(dockNode, Boolean.TRUE);
			for (SplitPane split : dockPane.getSplitPanes()) {
			    double[] positions = split.getDividerPositions(); // reccord the current ratio
			    Platform.runLater(() -> split.setDividerPositions(positions)); // apply the now former ratio
			}
			SplitPane.setResizableWithParent(dockNode, Boolean.FALSE);
		});
	}

	private MenuBar createMenu(DockPane dockPane) {
		final Menu menu1 = new Menu("Views", JavaFXUtils.icon("/res/open-view.png"));
		MenuItem sqlPaneViewItem = new MenuItem("Open Table View", JavaFXUtils.icon("/res/database.png"));
		sqlPaneViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlPane newSqlPane = new DSqlPane(sqlConnector);
				newSqlPane.asDockNode().dock(dockPane, DockPos.RIGHT, mainSqlPane.asDockNode());

			});
		});
		MenuItem sqlConsoleViewItem = new MenuItem("Open Console View", JavaFXUtils.icon("/res/console.png"));
		sqlConsoleViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlConsoleView sqlConsoleView = new DSqlConsoleView(sqlConnector);
				sqlConsoleView.asDockNode().dock(dockPane, DockPos.RIGHT);

			});
		});
		MenuItem tablesTreeViewItem = new MenuItem("Open structure tree view", JavaFXUtils.icon("/res/details.png"));
		tablesTreeViewItem.setOnAction(event -> {
			TreeView<String> treeView = new DBTreeView(DB, sqlConnector);
			DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
			dockNode.dock(dockPane, DockPos.LEFT);	
		});

		menu1.getItems().addAll(sqlPaneViewItem, sqlConsoleViewItem);

		final Menu menu2 = new Menu("Rest Service", new ImageView(new Image("/res/spark.png", 16, 16, false, false)));
		MenuItem restServiceStartItem = new MenuItem("Start Rest Service", JavaFXUtils.createImageView("/res/spark.png", 16.0, 16.0));
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

		MenuItem restServiceConfigItem = new MenuItem("Configure Rest Service", JavaFXUtils.icon("res/settings.png"));
		restServiceConfigItem.setOnAction(actionEvent -> createRestServiceConfigBox());

		menu2.getItems().addAll(restServiceStartItem, restServiceConfigItem);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menu1, menu2);

		return menuBar;
	}

	private void createRestServiceConfigBox() {
		ImageView bottleLogo = JavaFXUtils.createImageView("/res/spark-logo.png", 0.0, 200.0);
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
