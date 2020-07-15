
package gr.sqlbrowserfx;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.log4j.BasicConfigurator;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.json.JSONArray;
import org.slf4j.LoggerFactory;

import gr.bashfx.BashFXApp;
import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.DockWeights;
import gr.sqlbrowserfx.dock.nodes.DBTreePane;
import gr.sqlbrowserfx.dock.nodes.DSqlConsoleView;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.MySqlConfigBox;
import gr.sqlbrowserfx.nodes.codeareas.log.LogCodeArea;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeAreaSyntax;
import gr.sqlbrowserfx.nodes.queriesmenu.QueriesMenu;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.nodes.tableviews.MapTableView;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.rest.RESTfulServiceConfig;
import gr.sqlbrowserfx.rest.SparkRESTfulService;
import gr.sqlbrowserfx.utils.HTTPUtils;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.PropertiesLoader;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserFXApp extends Application {

	private static final String RECENT_DBS_PATH = "./recent-dbs.txt";
	private static final String CSS_THEME = (String) PropertiesLoader.getProperty("sqlbrowsefx.css.theme", String.class, "/res/flat-blue.css");
	private static final Boolean AUTO_COMMIT_IS_ENABLED = (Boolean) PropertiesLoader.getProperty("sqlconnector.enable.autocommit", Boolean.class, true);

	private static String DB;
	private static RESTfulServiceConfig restServiceConfig;

	private Scene primaryScene;
	private Stage primaryStage;
	private DSqlPane mainSqlPane;
	DraggingTabPaneSupport dragingSupport;
	
	private SqlConnector sqlConnector;
	private boolean restServiceStarted;
	private DBTreePane ddbTreePane;
	private boolean isInternalDBShowing = false;
	private boolean isRestConfigurationShowing = false;
	private QueriesMenu queriesMenu;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		DialogFactory.setDialogStyleSheet(CSS_THEME);
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
		Label selectedDBtext = new Label("No database selected");
		Button openButton = new Button("Open", new ImageView(new Image("/res/database.png")));
		openButton.setOnAction(actionEvent -> dbSelectionAction(selectedDBtext.getText()));
		HBox bottomBox = new HBox(selectedDBtext, openButton);
		bottomBox.setPadding(new Insets(5));
		bottomBox.setSpacing(5);
		bottomBox.setAlignment(Pos.CENTER_RIGHT);

		VBox rightBox = new VBox();
		Label text = new Label("Browse system for database...");
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

		Label recentDBsText = new Label("Recently opened");
		recentDBsText.setTextAlignment(TextAlignment.CENTER);

		ListView<String> recentDBsListView = new ListView<>();
		try (Stream<String> stream = Files.lines(Paths.get(RECENT_DBS_PATH))) {
			stream.forEach(line -> recentDBsListView.getItems().add(line));
		} catch (IOException e) {
			DialogFactory.createErrorDialog(e);
		}

		recentDBsListView.setOnMouseClicked(
				mouseEvent -> selectedDBtext.setText(recentDBsListView.getSelectionModel().getSelectedItem()));
		VBox leftBox = new VBox(recentDBsText, recentDBsListView);
		leftBox.setAlignment(Pos.CENTER);
		leftBox.setPadding(new Insets(5));
		leftBox.setSpacing(5);

		BorderPane borderPane = new BorderPane();
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
//		mySqlConfigBox.getChildren().add(new ListView<String>(FXCollections.observableArrayList(Arrays.asList("Yesterday", "jdbc:mysql://localhost:3306/sakila?autoReconnect=true&useSSL=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"))));
		Tab mysqlTab = new Tab("MySQL", mySqlConfigBox);
		mysqlTab.setGraphic(JavaFXUtils.createImageView("/res/mysql.png", 28.0, 28.0));
		mysqlTab.setClosable(false);
		TabPane dbTabPane = new TabPane(sqliteTab, mysqlTab);
		
		JavaFXUtils.applyJMetro(dbTabPane);
		
		primaryScene = new Scene(dbTabPane, 600, 400);
		leftBox.prefHeightProperty().bind(primaryScene.heightProperty());
		leftBox.prefWidthProperty().bind(primaryScene.widthProperty().divide(2));
		primaryScene.getStylesheets().add(DockPane.class.getResource("default.css").toExternalForm());
		primaryScene.getStylesheets().add(CSS_THEME);
		primaryScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (primaryScene.getFocusOwner() instanceof Button
						&& ((Button) primaryScene.getFocusOwner()).getOnAction() != null) {
					((Button) primaryScene.getFocusOwner()).getOnAction().handle(new ActionEvent());
				}
			}
		});

	}

	private void dbSelectionAction(String dbPath) {
		if (dbPath.equals("No database selected"))
			return;

		if (Files.notExists(Paths.get(dbPath), LinkOption.NOFOLLOW_LINKS)) {
			DialogFactory.createErrorDialog(new FileNotFoundException("File does not exists"));
			return;
		}
		DB = dbPath;
		restServiceConfig = new RESTfulServiceConfig("localhost", 8080, DB);

		SqlConnector sqliteConnector = new SqliteConnector(dbPath);
		sqliteConnector.setAutoCommitModeEnabled(AUTO_COMMIT_IS_ENABLED);
		this.sqlConnector = sqliteConnector;
		createAppView(sqliteConnector);

	}

	private void dbSelectionAction(MySqlConfigBox configBox) {
		
		configBox.getConnectButton().setDisable(true);
		DB = configBox.getDatabaseField().getText();
		restServiceConfig = new RESTfulServiceConfig("localhost", 8080, DB);
			SqlConnector mysqlConnector = new MysqlConnector(configBox.getUrl(), configBox.getDatabaseField().getText(),
					configBox.getUserField().getText(), configBox.getPasswordField().getText());
			this.sqlConnector = mysqlConnector;
			
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					mysqlConnector.setAutoCommitModeEnabled(AUTO_COMMIT_IS_ENABLED);
					mysqlConnector.checkConnection();
//					SqlBrowserFXAppManager.getConfigSqlConnector()
//										  .executeUpdate("insert into mysql_history (date,url,usenrname,database) values (?,?,?)",
//												  Arrays.asList(new Timestamp(System.currentTimeMillis()),
//														  		configBox.getUrl(), configBox.getUserField().getText(), configBox.getDatabaseField().getText()));
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

	private VBox createJsonTableView() {
		MapTableView tableView = new MapTableView();
		tableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.C) {
				StringBuilder content = new StringBuilder();
				for (MapTableViewRow row :tableView.getSelectionModel().getSelectedItems()) {
					content.append(row.toString());
				}
				
				StringSelection stringSelection = new StringSelection(content.toString());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		});
		TextField requestField = new TextField();
		requestField.setPromptText("Enter url...");
		final Executor executor = Executors.newSingleThreadExecutor();
		requestField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				executor.execute(() -> {
					try {
						
						JSONArray jsonArray = new JSONArray(HTTPUtils.GET(requestField.getText()));
						tableView.setItemsLater(jsonArray);
					} catch (Throwable e) {
						DialogFactory.createErrorDialog(e);
					}
				});
			}
		});
		VBox vbox = new VBox(requestField, tableView);
		VBox.setVgrow(tableView, Priority.ALWAYS);
		return vbox;
	}
	
	private void createAppView(SqlConnector sqlConnector) {
		
		primaryStage.setMaximized(true);
		DockPane dockPane = new DockPane();
		MenuBar menuBar = createMenu(dockPane);

		dockPane.getStylesheets().add(CSS_THEME);

		mainSqlPane = new DSqlPane(sqlConnector);
		SqlBrowserFXAppManager.addSqlPane(mainSqlPane);
		mainSqlPane.asDockNode().setTitle(mainSqlPane.asDockNode().getTitle() + " " + SqlBrowserFXAppManager.getActiveSqlPanes().size());
		mainSqlPane.asDockNode().dock(dockPane, DockPos.CENTER, DockWeights.asDoubleArrray(0.8f));
		mainSqlPane.asDockNode().setClosable(false);
		mainSqlPane.showConsole();

		ddbTreePane = new DBTreePane(DB, sqlConnector);
		SqlCodeAreaSyntax.bind(ddbTreePane.getDBTreeView().getContentNames());
		for (String table : ddbTreePane.getDBTreeView().getContentNames()) {
			SqlCodeAreaSyntax.bind(table, ddbTreePane.getDBTreeView().getColumnsForTable(table));
		}
		ddbTreePane.getDBTreeView().addObserver(value -> SqlCodeAreaSyntax.bind(ddbTreePane.getDBTreeView().getContentNames()));
		mainSqlPane.getSqlConsoleBox().addObserver(ddbTreePane.getDBTreeView());
		mainSqlPane.getSqlConsoleBox().addObserver(queriesMenu);
		ddbTreePane.asDockNode().dock(dockPane, DockPos.LEFT, DockWeights.asDoubleArrray(0.2f));
		ddbTreePane.asDockNode().setClosable(false);
		// fixed size 
		SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.FALSE);
		
		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(menuBar, dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);
		
		JavaFXUtils.applyJMetro(vbox);
		
		primaryScene.setRoot(vbox);
		primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
			SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.TRUE);
			for (SplitPane split : dockPane.getSplitPanes()) {
			    double[] positions = split.getDividerPositions(); // reccord the current ratio
			    Platform.runLater(() -> split.setDividerPositions(positions)); // apply the now former ratio
			}
			SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.FALSE);
		});
	}

	private MenuBar createMenu(DockPane dockPane) {
		final Menu menu1 = new Menu("Views", JavaFXUtils.icon("/res/open-view.png"));
		
		MenuItem sqlPaneViewItem = new MenuItem("Open Table View", JavaFXUtils.icon("/res/database.png"));
		sqlPaneViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlPane newSqlPane = new DSqlPane(sqlConnector);
				SqlBrowserFXAppManager.addSqlPane(newSqlPane);
				newSqlPane.asDockNode().setTitle(newSqlPane.asDockNode().getTitle() + " " + SqlBrowserFXAppManager.getActiveSqlPanes().size());
				newSqlPane.asDockNode().dock(dockPane, DockPos.RIGHT);
				newSqlPane.getSqlConsoleBox().addObserver(queriesMenu);
				newSqlPane.getSqlConsoleBox().addObserver(ddbTreePane.getDBTreeView());
			});
		});
		
		MenuItem sqlConsoleViewItem = new MenuItem("Open Console View", JavaFXUtils.icon("/res/console.png"));
		sqlConsoleViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlConsoleView sqlConsoleView = new DSqlConsoleView(sqlConnector);
				sqlConsoleView.asDockNode().dock(dockPane, DockPos.RIGHT);

			});
		});
		MenuItem bashCodeAreaItem = new MenuItem("Open BashFX", JavaFXUtils.icon("/res/console.png"));
		bashCodeAreaItem.setOnAction(event -> {
			Platform.runLater(() -> {
				VBox vb = new BashFXApp().createBashFXAppBox(primaryStage);
			    JavaFXUtils.applyJMetro(vb);
			    Scene scene = new Scene(vb, 800, 600);
			    for (String styleSheet : primaryScene.getStylesheets())
			  	  scene.getStylesheets().add(styleSheet);
			    Stage stage = new Stage();
			    stage.setTitle("BashFX");
			    stage.setScene(scene);
			    stage.show();
			});
		});
		MenuItem tablesTreeViewItem = new MenuItem("Open structure tree view", JavaFXUtils.icon("/res/details.png"));
		tablesTreeViewItem.setOnAction(event -> {
			DBTreePane treeView = new DBTreePane(DB, sqlConnector);
			DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
			dockNode.dock(dockPane, DockPos.RIGHT);	
		});
		
		MenuItem jsonTableViewItem = new MenuItem("Open JSON Table View", JavaFXUtils.icon("/res/web.png"));
		jsonTableViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				VBox jsonTableView = this.createJsonTableView();

			    JavaFXUtils.applyJMetro(jsonTableView);
			    Scene scene = new Scene(jsonTableView, 800, 600);
			    for (String styleSheet : primaryScene.getStylesheets())
			  	  scene.getStylesheets().add(styleSheet);
			    Stage stage = new Stage();
			    stage.setTitle("SqlBrowserFX Log");
			    stage.setScene(scene);
			    stage.setOnCloseRequest(closeEvent -> {
					DockNode dockNode = new DockNode(jsonTableView, "JSON table", JavaFXUtils.icon("/res/web.png"));
					dockNode.dock(dockPane, DockPos.RIGHT);	
			    });
			    stage.show();
			});
		});
		
		MenuItem webViewItem = new MenuItem("Open Docs", JavaFXUtils.icon("/res/web.png"));
		webViewItem.setOnAction(event -> {
			WebView docsView = new WebView();
			docsView.getEngine().load("https://www.sqlite.org/index.html");
			DockNode dockNode = new DockNode(docsView, "Docs", JavaFXUtils.icon("/res/web.png"));
			dockNode.dock(dockPane, DockPos.RIGHT);	
		});
		
		MenuItem logItem = new MenuItem("Open Log View", JavaFXUtils.icon("/res/monitor.png"));
		logItem.setOnAction(actionEvent -> {
			LogCodeArea logArea = new LogCodeArea();
			TailerListener listener = new CodeAreaTailerListener(logArea);
		    Tailer tailer = new Tailer(new File("./log/sql-browser.log"), listener, 0);

		    Executor executor = Executors.newSingleThreadExecutor();
		    executor.execute(tailer);
		      
		    VirtualizedScrollPane<LogCodeArea> virtualizedScrollPane =new VirtualizedScrollPane<>(logArea);
		    JavaFXUtils.applyJMetro(virtualizedScrollPane);
		    Scene scene = new Scene(virtualizedScrollPane, 800, 600);
		    for (String styleSheet : primaryScene.getStylesheets())
		  	  scene.getStylesheets().add(styleSheet);
		    Stage stage = new Stage();
		    stage.setTitle("SqlBrowserFX Log");
		    stage.setScene(scene);
		    stage.setOnCloseRequest(closeEvent -> {
			    DockNode dockNode = new DockNode(virtualizedScrollPane, "SqlBrowserFX Log", JavaFXUtils.icon("/res/monitor.png"));
				dockNode.dock(dockPane, DockPos.RIGHT);	
		    });
		    stage.show();
		    

		});

		menu1.getItems().addAll(bashCodeAreaItem, logItem, sqlPaneViewItem, jsonTableViewItem);
		if (sqlConnector instanceof SqliteConnector)
			menu1.getItems().add(webViewItem);

		final Menu menu2 = new Menu("Rest Service", new ImageView(new Image("/res/spark.png", 16, 16, false, false)));
		MenuItem restServiceStartItem = new MenuItem("Start Rest Service", JavaFXUtils.createImageView("/res/spark.png", 16.0, 16.0));
		restServiceStartItem.setOnAction(actionEvent -> {
			if (restServiceStarted == false) {
				SparkRESTfulService.configure(restServiceConfig.getIp(), restServiceConfig.getPort());
				SparkRESTfulService.init(sqlConnector);
				SparkRESTfulService.start();
				restServiceStartItem.setText("Stop Rest Service");
				restServiceStarted = true;
			} else {
				SparkRESTfulService.stop();
				restServiceStarted = false;
				restServiceStartItem.setText("Start Rest Service");
			}
		});

		MenuItem restServiceConfigItem = new MenuItem("Configure Rest Service", JavaFXUtils.icon("res/settings.png"));
		restServiceConfigItem.setOnAction(actionEvent -> createRestServiceConfigBox());
		
		menu2.getItems().addAll(restServiceStartItem, restServiceConfigItem);

		Menu menu3 = new Menu();
		HBox customGraphic = new HBox(JavaFXUtils.icon("res/settings.png"), new Label("Configuration"));
		customGraphic.setSpacing(5);
		menu3.setGraphic(customGraphic);
		menu3.getGraphic().setOnMouseClicked(mouseEvent -> {
			if (!isInternalDBShowing) {
				SqlPane sqlPane = new SqlPane(SqlBrowserFXAppManager.getConfigSqlConnector());
				JavaFXUtils.applyJMetro(sqlPane);
				Scene scene = new Scene(sqlPane, 800, 600);
				for (String styleSheet : primaryScene.getStylesheets())
					scene.getStylesheets().add(styleSheet);
				Stage stage = new Stage();
				stage.setTitle("SqlBrowserFX Internal Database");
				stage.setScene(scene);
				stage.show();
				isInternalDBShowing  = true;
				stage.setOnCloseRequest(windowEvent -> {
					isInternalDBShowing = false;
				});
			}
		});
		
		Menu menu4 = new Menu("Transactions");
		MenuItem commitAllItem = new MenuItem("Commit all", JavaFXUtils.icon("res/check.png"));
		commitAllItem.setOnAction(actionEvent -> sqlConnector.commitAll());
		
		MenuItem rollbackAllItem = new MenuItem("Rollback all", JavaFXUtils.icon("res/refresh.png"));
		rollbackAllItem.setOnAction(actionEvent -> sqlConnector.rollbackAll());
		
		menu4.getItems().addAll(commitAllItem, rollbackAllItem);
		if (!sqlConnector.isAutoCommitModeEnabled())
			menu4.setDisable(true);
		
		MenuBar menuBar = new MenuBar();
		queriesMenu = new QueriesMenu();
		menuBar.getMenus().addAll(menu1, menu2, queriesMenu, menu3, menu4);

		return menuBar;
	}

	private void createRestServiceConfigBox() {
		if (isRestConfigurationShowing)
			return;
		
		ImageView bottleLogo = JavaFXUtils.createImageView("/res/spark-logo.png", 0.0, 200.0);
		Label ipLabel = new Label("Ip address");
		TextField ipField = new TextField(restServiceConfig.getIp());
		Label portLabel = new Label("Port");
		TextField portField = new TextField(restServiceConfig.getPort().toString());
		Button saveButton = new Button("Save", JavaFXUtils.icon("/res/check.png"));

		VBox vBox = new VBox(bottleLogo, ipLabel, ipField, portLabel, portField, saveButton);
		JavaFXUtils.applyJMetro(vBox);
		vBox.setPadding(new Insets(15));

		Stage stage = new Stage();
		Scene scene = new Scene(vBox);
		for (String styleSheet : primaryScene.getStylesheets())
			scene.getStylesheets().add(styleSheet);
		stage.setTitle("Rest service configuration");
		stage.setScene(scene);
		stage.show();
		
		saveButton.setOnAction(actionEvent -> {
			restServiceConfig.setIp(ipField.getText());
			restServiceConfig.setPort(Integer.parseInt(portField.getText()));
			stage.close();
		});
		isRestConfigurationShowing = true;
		stage.setOnCloseRequest(windowEvent -> isRestConfigurationShowing  = false);
	}

}
