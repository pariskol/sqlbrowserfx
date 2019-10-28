
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
import gr.sqlbrowserfx.dock.nodes.DDBTreeView;
import gr.sqlbrowserfx.dock.nodes.DSqlConsoleView;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.DBTreeView;
import gr.sqlbrowserfx.nodes.MySqlConfigBox;
import gr.sqlbrowserfx.nodes.queriesMenu.QueriesMenu;
import gr.sqlbrowserfx.nodes.sqlCodeArea.CodeAreaKeywords;
import gr.sqlbrowserfx.nodes.sqlPane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;
import gr.sqlbrowserfx.restService.RestServiceConfig;
import gr.sqlbrowserfx.restService.SparkRestService;
import gr.sqlbrowserfx.utils.JavaFXUtils;
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
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserFXApp extends Application {

	private static final String RECENT_DBS_PATH = "./recent-dbs.txt";
	private static final String CSS_THEME = System.getProperty("themeCSS", "/res/basic.css");
	private static final String INTERNAL_DB = "sqlbrowse.db";
	private static String DB;
	private static RestServiceConfig restServiceConfig;

	private Scene primaryScene;
	private Stage primaryStage;
	private DSqlPane mainSqlPane;
	DraggingTabPaneSupport dragingSupport;
	
	private SqlConnector sqlConnector;
	private boolean restServiceStarted;
	private DDBTreeView ddbTreeView;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		DialogFactory.setDialogStyleSheet(CSS_THEME);
		CodeAreaKeywords.onKeywordsBind();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		primaryStage.setTitle("SqlBrowser");

		createDBselectBox();

		primaryStage.setScene(primaryScene);
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
		primaryScene = new Scene(dbTabPane, 600, 400);
		primaryScene.getStylesheets().add(DockPane.class.getResource("default.css").toExternalForm());
		primaryScene.getStylesheets().add(CSS_THEME);
		primaryScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (primaryScene.getFocusOwner() instanceof Button) {
					((Button) primaryScene.getFocusOwner()).getOnAction().handle(new ActionEvent());
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
		createAppView(sqliteConnector);

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
				
				Platform.runLater(() -> createAppView(mysqlConnector));
			});
	}

	private void createAppView(SqlConnector sqlConnector) {
		primaryStage.setMaximized(true);
		DockPane dockPane = new DockPane();
		dockPane.getStylesheets().add(CSS_THEME);

		mainSqlPane = new DSqlPane(sqlConnector);
		SqlBrowserFXAppManager.addSqlPane(mainSqlPane);
		mainSqlPane.asDockNode().setTitle(mainSqlPane.asDockNode().getTitle() + " " + SqlBrowserFXAppManager.getActiveSqlPanes().size());
		mainSqlPane.asDockNode().dock(dockPane, DockPos.CENTER, new double[] {0.8f});
		mainSqlPane.asDockNode().setClosable(false);
		mainSqlPane.showConsole();

		ddbTreeView = new DDBTreeView(DB, sqlConnector);
		CodeAreaKeywords.bind(ddbTreeView.getContentNames());
		ddbTreeView.addListener(value -> CodeAreaKeywords.bind(ddbTreeView.getContentNames()));
		mainSqlPane.getSqlConsoleBox().addListener(ddbTreeView);
		ddbTreeView.asDockNode().dock(dockPane, DockPos.LEFT, new double[] {0.2f});
		ddbTreeView.asDockNode().setClosable(false);
		// fixed size 
		SplitPane.setResizableWithParent(ddbTreeView.asDockNode(), Boolean.FALSE);
		
		MenuBar menuBar = createMenu(dockPane);

		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(menuBar, dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);
		primaryScene.setRoot(vbox);
		primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
			SplitPane.setResizableWithParent(ddbTreeView.asDockNode(), Boolean.TRUE);
			for (SplitPane split : dockPane.getSplitPanes()) {
			    double[] positions = split.getDividerPositions(); // reccord the current ratio
			    Platform.runLater(() -> split.setDividerPositions(positions)); // apply the now former ratio
			}
			SplitPane.setResizableWithParent(ddbTreeView.asDockNode(), Boolean.FALSE);
		});
	}

	private MenuBar createMenu(DockPane dockPane) {
		final Menu menu1 = new Menu("Views", JavaFXUtils.icon("/res/open-view.png"));
		MenuItem sqlPaneViewItem = new MenuItem("Open Table View", JavaFXUtils.icon("/res/database.png"));
		sqlPaneViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlPane newSqlPane = new DSqlPane(sqlConnector);
				newSqlPane.asDockNode().setTitle(newSqlPane.asDockNode().getTitle() + " " + SqlBrowserFXAppManager.getActiveSqlPanes().size());
				newSqlPane.asDockNode().dock(dockPane, DockPos.RIGHT, mainSqlPane.asDockNode());
//FIXME null pointer wxception occures
				//				newSqlPane.getSqlConsoleBox().addListener(ddbTreeView);
				SqlBrowserFXAppManager.addSqlPane(newSqlPane);
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
		
		MenuItem webViewItem = new MenuItem("Open Docs", JavaFXUtils.icon("/res/web.png"));
		webViewItem.setOnAction(event -> {
			WebView docsView = new WebView();
			docsView.getEngine().load("https://www.sqlite.org/index.html");
			DockNode dockNode = new DockNode(docsView, "Docs", JavaFXUtils.icon("/res/web.png"));
			dockNode.dock(dockPane, DockPos.LEFT);	
		});

		menu1.getItems().addAll(sqlPaneViewItem, sqlConsoleViewItem, webViewItem);

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

		Menu menu3 = new Menu();
		menu3.setGraphic(new HBox(JavaFXUtils.icon("res/settings.png"), new Label("Configuration")));
		menu3.getGraphic().setOnMouseClicked(mouseEvent -> {
			DockNode dockNode = new DockNode(new SqlPane(SqlBrowserFXAppManager.getConfigSqlConnector()), "Internal DB");
			dockNode.dock(dockPane, DockPos.BOTTOM, ddbTreeView.asDockNode());
		});
		
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menu1, menu2, new QueriesMenu(), menu3);

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
		for (String styleSheet : primaryScene.getStylesheets())
			scene.getStylesheets().add(styleSheet);
		stage.setTitle("Rest service configuration");
		stage.setScene(scene);
		stage.show();
	}

}
