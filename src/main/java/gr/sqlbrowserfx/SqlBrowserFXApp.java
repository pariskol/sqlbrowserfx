
package gr.sqlbrowserfx;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.dockfx.DockWeights;
import org.json.JSONArray;
import org.slf4j.LoggerFactory;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import com.kodedu.terminalfx.config.TerminalConfig;

import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.PostgreSqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DDBTreePane;
import gr.sqlbrowserfx.dock.nodes.DLogConsolePane;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.HelpTabPane;
import gr.sqlbrowserfx.nodes.MySqlConfigBox;
import gr.sqlbrowserfx.nodes.PostgreSqlConfigBox;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.codeareas.sql.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.sql.KeywordType;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeAreaSyntax;
import gr.sqlbrowserfx.nodes.queriesmenu.QueriesMenu;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.nodes.tableviews.HistorySqlTableView;
import gr.sqlbrowserfx.nodes.tableviews.JSONTableView;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.rest.RESTfulService;
import gr.sqlbrowserfx.rest.RESTfulServiceConfig;
import gr.sqlbrowserfx.utils.HttpClient;
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserFXApp extends Application {

	private static final String CSS_THEME = "/styles/" + (String) PropertiesLoader.getProperty("sqlbrowserfx.css.theme", String.class, "flat-dark") + ".css";
	private static final Boolean AUTO_COMMIT_IS_ENABLED = (Boolean) PropertiesLoader.getProperty("sqlconnector.enable.autocommit", Boolean.class, true);

	private static String DB;
	private static RESTfulServiceConfig restServiceConfig;

	private Scene primaryScene;
	public static Stage STAGE;
	private DSqlPane mainSqlPane;

	private SqlConnector sqlConnector;
	private boolean restServiceStarted;
	private DDBTreePane ddbTreePane;
	private boolean isInternalDBShowing = false;
	private boolean isRestConfigurationShowing = false;
	private QueriesMenu queriesMenu;
	
	@SuppressWarnings("unused")
	private double fontSize;

	public static void main(String[] args) {
		PropertiesLoader.setLogger(LoggerFactory.getLogger(LoggerConf.LOGGER_NAME));
		DialogFactory.setDialogStyleSheet(CSS_THEME);
		DB = args.length > 0 && args[0] != null ? args[0] : null;
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		Font defaultFont = Font.getDefault();
		fontSize = defaultFont.getSize();
		SqlBrowserFXApp.STAGE = primaryStage;
		primaryStage.setTitle("SqlBrowserFX");

		if (DB == null)
			createDBselectBox();
		else
			dbSelectionAction(DB);
		
		primaryStage.setScene(primaryScene);
		primaryStage.sizeToScene();
		primaryStage.getIcons().add(JavaFXUtils.createImage("/icons/sqlbrowser-fx.png"));
		primaryStage.show();

		primaryStage.setOnCloseRequest(closeEvent -> {
			if (sqlConnector instanceof SqliteConnector) 
				saveConnectionToHistory();
			
			Platform.exit();
			System.exit(0);
		});

	}

	private void createDBselectBox() {
		Label selectedDBtext = new Label("No database selected");
		Button openButton = new Button("Open", JavaFXUtils.createIcon("/icons/database.png"));
		openButton.setOnAction(actionEvent -> dbSelectionAction(selectedDBtext.getText()));
		HBox bottomBox = new HBox(selectedDBtext, openButton);
		bottomBox.setPadding(new Insets(5));
		bottomBox.setSpacing(5);
		bottomBox.setAlignment(Pos.CENTER_RIGHT);

		VBox rightBox = new VBox();
		Label text = new Label("Browse system for database...");
		Button fileChooserButton = new Button("Search", JavaFXUtils.createIcon("/icons/magnify.png"));
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

		Label recentDBsText = new Label("History");
		recentDBsText.setTextAlignment(TextAlignment.CENTER);

		HistorySqlTableView recentDBsTableView = new HistorySqlTableView(SqlBrowserFXAppManager.getConfigSqlConnector());
		
		SqlBrowserFXAppManager
			.getConfigSqlConnector()
			.executeQueryRawAsync("select database, timestamp, id from connections_history_localtime where database_type = 'sqlite' order by timestamp desc",
				rset -> recentDBsTableView.setItemsLater(rset)
		);

		recentDBsTableView.setOnMouseClicked(
			mouseEvent -> {
				if (recentDBsTableView.getSelectionModel().getSelectedItem() != null) {
					selectedDBtext.setText(recentDBsTableView.getSelectionModel().getSelectedItem().get("database").toString());
					if (mouseEvent.getClickCount() == 2)
						dbSelectionAction(selectedDBtext.getText());
				}
			});
		VBox leftBox = new VBox(recentDBsText, recentDBsTableView);
		leftBox.setAlignment(Pos.CENTER);
		leftBox.setPadding(new Insets(5));
		leftBox.setSpacing(5);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(rightBox);
		borderPane.setLeft(leftBox);
		borderPane.setBottom(bottomBox);

		Tab sqliteTab = new Tab("Sqlite", borderPane);
		sqliteTab.setGraphic(JavaFXUtils.createImageView("/icons/sqlite.png", 28.0, 28.0));
		sqliteTab.setClosable(false);
		
		MySqlConfigBox mySqlConfigBox = new MySqlConfigBox();
		mySqlConfigBox.getConnectButton().setOnAction(actionEvent -> {
			mySqlConfigBox.showLoader(true);
			dbSelectionAction(mySqlConfigBox);
		});
		Tab mysqlTab = new Tab("MySQL", mySqlConfigBox);
		mysqlTab.setGraphic(JavaFXUtils.createImageView("/icons/mysql.png", 28.0, 28.0));
		mysqlTab.setClosable(false);
		
		MySqlConfigBox mariadbConfigBox = new MySqlConfigBox();
		mariadbConfigBox.getConnectButton().setOnAction(actionEvent -> {
			mariadbConfigBox.showLoader(true);
			dbSelectionAction(mariadbConfigBox);
		});
		Tab mariadbTab = new Tab("MariaDB", mariadbConfigBox);
		mariadbTab.setGraphic(JavaFXUtils.createImageView("/icons/mariadb.png", 28.0, 28.0));
		mariadbTab.setClosable(false);
		
		
		PostgreSqlConfigBox postgreSqlConfigBox = new PostgreSqlConfigBox();
		postgreSqlConfigBox.getConnectButton().setOnAction(actionEvent -> {
			postgreSqlConfigBox.showLoader(true);
			dbSelectionAction(postgreSqlConfigBox);
		});
		Tab postgresqlTab = new Tab("PostgreSQL", postgreSqlConfigBox);
		postgresqlTab.setGraphic(JavaFXUtils.createImageView("/icons/postgre.png", 28.0, 28.0));
		postgresqlTab.setClosable(false);
		
		TabPane dbTabPane = new TabPane(sqliteTab, mysqlTab, mariadbTab, postgresqlTab);
		
		JavaFXUtils.applyJMetro(dbTabPane);
		
		primaryScene = new Scene(dbTabPane, 800, 500);
		leftBox.prefHeightProperty().bind(primaryScene.heightProperty());
		leftBox.prefWidthProperty().bind(primaryScene.widthProperty().divide(2));
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
		if (System.getProperty("sqlbrowserfx.mode", "advanced").equals("simple")) {
			SqlCodeAreaSyntax.init(SqlBrowserFXAppManager.getDBtype());
			primaryScene.setRoot(new SqlConsolePane(sqliteConnector));
			JavaFXUtils.addZoomInOutSupport(primaryScene.getRoot());
			STAGE.setScene(primaryScene);
		}
		else
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
				} catch (SQLException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
					configBox.showLoader(false);
					DialogFactory.createErrorDialog(e);
					configBox.getConnectButton().setDisable(false);
					return;
				}
				
				configBox.saveToHistory();
				Platform.runLater(() -> {
					if (System.getProperty("mode", "normal").equals("simple")) {
						SqlCodeAreaSyntax.init(SqlBrowserFXAppManager.getDBtype());
						primaryScene.setRoot(new SqlConsolePane(mysqlConnector));
						JavaFXUtils.addZoomInOutSupport(primaryScene.getRoot());
						STAGE.setScene(primaryScene);
					}
					else
						createAppView(mysqlConnector);
				});
			});
	}
	
	private void dbSelectionAction(PostgreSqlConfigBox configBox) {
		configBox.getConnectButton().setDisable(true);
		DB = configBox.getDatabaseField().getText();
		restServiceConfig = new RESTfulServiceConfig("localhost", 8080, DB);
			SqlConnector mysqlConnector = new PostgreSqlConnector(configBox.getUrl(), configBox.getDatabaseField().getText(),
					configBox.getUserField().getText(), configBox.getPasswordField().getText());
			this.sqlConnector = mysqlConnector;
			
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					mysqlConnector.setAutoCommitModeEnabled(AUTO_COMMIT_IS_ENABLED);
					mysqlConnector.checkConnection();
				} catch (SQLException e) {
					LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
					configBox.showLoader(false);
					DialogFactory.createErrorDialog(e);
					configBox.getConnectButton().setDisable(false);
					return;
				}
				
				configBox.saveToHistory();
				Platform.runLater(() -> {
					if (System.getProperty("mode", "normal").equals("simple")) {
						SqlCodeAreaSyntax.init(SqlBrowserFXAppManager.getDBtype());
						primaryScene.setRoot(new SqlConsolePane(mysqlConnector));
						JavaFXUtils.addZoomInOutSupport(primaryScene.getRoot());
						STAGE.setScene(primaryScene);
					}
					else
						createAppView(mysqlConnector);
				});
			});
	}

	private VBox createJsonTableView() {
		JSONTableView tableView = new JSONTableView();
		
		TextField requestField = new TextField();
		requestField.setPromptText("Enter url...");
		final Executor executor = Executors.newSingleThreadExecutor();
		requestField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				executor.execute(() -> {
					try {
						
						JSONArray jsonArray = new JSONArray(HttpClient.GET(requestField.getText()));
						tableView.setItemsLater(jsonArray);
					} catch (Throwable e) {
						DialogFactory.createErrorNotification(e);
					}
				});
			}
		});
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
			else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.F) {
				requestField.requestFocus();
				requestField.selectAll();
			}
		});
		VBox vbox = new VBox(requestField, tableView);
		VBox.setVgrow(tableView, Priority.ALWAYS);
		return vbox;
	}
	
	private String determineDBType(SqlConnector sqlConnector) {
		String dbType = null;
		if (sqlConnector instanceof SqliteConnector)
			dbType = "sqlite";
		else if (sqlConnector instanceof MysqlConnector)
			dbType = "mysql";
		else if (sqlConnector instanceof PostgreSqlConnector)
			dbType = "mysql";
		return dbType;
	}
	
	private void createAppView(SqlConnector sqlConnector) {
		
		SqlBrowserFXAppManager.setDBtype(determineDBType(sqlConnector));
		SqlCodeAreaSyntax.init(SqlBrowserFXAppManager.getDBtype());
		
		STAGE.setMaximized(true);
		DockPane dockPane = new DockPane();
		MenuBar menuBar = createMenu(dockPane);

		dockPane.getStylesheets().add(CSS_THEME);

		mainSqlPane = new DSqlPane(sqlConnector);
		SqlBrowserFXAppManager.registerDSqlPane(mainSqlPane);
		mainSqlPane.asDockNode().setTitle(mainSqlPane.asDockNode().getTitle() + " " + SqlBrowserFXAppManager.getActiveSqlPanes().size());
		mainSqlPane.asDockNode().dock(dockPane, DockPos.CENTER, DockWeights.asDoubleArrray(0.8f));
		mainSqlPane.asDockNode().setClosable(false);
		mainSqlPane.showConsole();

		ddbTreePane = new DDBTreePane(DB, sqlConnector);
		SqlBrowserFXAppManager.registerDDBTreeView(ddbTreePane.getDBTreeView());
		ddbTreePane.getDBTreeView().asDockNode().setOnClose(() -> SqlBrowserFXAppManager.unregisterDDBTreeView(ddbTreePane.getDBTreeView()));
		
		ddbTreePane.getDBTreeView().addObserver(value -> {
			SqlCodeAreaSyntax.bind(ddbTreePane.getDBTreeView().getContentNames().stream().map(kw -> new Keyword(kw, KeywordType.TABLE)).collect(Collectors.toList()));
//			SqlCodeAreaSyntax.bind(ddbTreePane.getDBTreeView().getContentNames().stream().map(x -> x.toUpperCase() + "@").collect(Collectors.toList()));
		});
		mainSqlPane.getSqlConsoleBox().addObserver(ddbTreePane.getDBTreeView());
		ddbTreePane.asDockNode().dock(dockPane, DockPos.LEFT, DockWeights.asDoubleArrray(0.2f));
		ddbTreePane.asDockNode().setClosable(false);
		// fixed size 
		SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.FALSE);
		
		VBox vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
//		TabPane tp = new TabPane(new Tab("SqlBrowserFX", dockPane), new Tab("BashFX", new BashFXApp().createBashFXAppBox(primaryStage)));
		vbox.getChildren().addAll(menuBar, dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);
		
		JavaFXUtils.applyJMetro(vbox);
		JavaFXUtils.addZoomInOutSupport(vbox);

		if (primaryScene == null) {
			primaryScene = new Scene(vbox);
			STAGE.setScene(primaryScene);
			primaryScene.getStylesheets().add(CSS_THEME);
		}

		primaryScene.setRoot(vbox);
		STAGE.heightProperty().addListener((obs, oldVal, newVal) -> {
			SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.TRUE);
			for (SplitPane split : dockPane.getSplitPanes()) {
			    double[] positions = split.getDividerPositions(); // reccord the current ratio
			    Platform.runLater(() -> split.setDividerPositions(positions)); // apply the now former ratio
			}
			SplitPane.setResizableWithParent(ddbTreePane.asDockNode(), Boolean.FALSE);
		});
		
	}

	private MenuBar createMenu(DockPane dockPane) {
		final Menu menu1 = new Menu("Views", JavaFXUtils.createIcon("/icons/open-view.png"));
		
		MenuItem sqlPaneViewItem = new MenuItem("Open Table View", JavaFXUtils.createIcon("/icons/database.png"));
		sqlPaneViewItem.setOnAction(event -> {
			Platform.runLater(() -> {
				DSqlPane newSqlPane = new DSqlPane(sqlConnector);
				newSqlPane.asDockNode().setTitle(newSqlPane.asDockNode().getTitle() + " " + (SqlBrowserFXAppManager.getActiveSqlPanes().size() + 1));
				newSqlPane.asDockNode().setDockPane(dockPane);
				newSqlPane.asDockNode().setFloating(true);
				SqlBrowserFXAppManager.registerDSqlPane(newSqlPane);
			});
		});
		
		MenuItem sqlConsoleViewItem = new MenuItem("Open Simple Console View", JavaFXUtils.createIcon("/icons/console.png"));
		sqlConsoleViewItem.setOnAction(event -> {
			new DockNode(dockPane, new SqlConsolePane(sqlConnector), "Simple SqlConsole", JavaFXUtils.createIcon("/icons/console.png"));
		});
//		MenuItem bashCodeAreaItem = new MenuItem("Open BashFX", JavaFXUtils.createIcon("/icons/console.png"));
//		bashCodeAreaItem.setOnAction(event -> {
//			Node vb = new BashFXApp().createBashFXAppBox(true);
//		    JavaFXUtils.applyJMetro(vb);
//			new DockNode(dockPane, vb, "BashFX", JavaFXUtils.createIcon("/icons/console.png"));
//		});
		MenuItem tablesTreeViewItem = new MenuItem("Open structure tree view", JavaFXUtils.createIcon("/icons/details.png"));
		tablesTreeViewItem.setOnAction(event -> {
			DDBTreePane treeView = new DDBTreePane(DB, sqlConnector);
			DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.createIcon("/icons/details.png"));
			dockNode.dock(dockPane, DockPos.RIGHT);	
		});
		
		MenuItem jsonTableViewItem = new MenuItem("Open JSON Table View", JavaFXUtils.createIcon("/icons/web.png"));
		jsonTableViewItem.setOnAction(event -> {
			VBox jsonTableView = this.createJsonTableView();
		    JavaFXUtils.applyJMetro(jsonTableView);
			new DockNode(dockPane, jsonTableView, "JSON table", JavaFXUtils.createIcon("/icons/web.png"));
		});
		
//		MenuItem webViewItem = new MenuItem("Open Docs", JavaFXUtils.createIcon("/icons/web.png"));
//		webViewItem.setOnAction(event -> {
//			WebView docsView = new WebView();
//			docsView.getEngine().load("https://www.sqlite.org/index.html");
//			DockNode dockNode = new DockNode(docsView, "Docs", JavaFXUtils.createIcon("/icons/web.png"));
//			dockNode.dock(dockPane, DockPos.RIGHT);	
//		});
		
		MenuItem logItem = new MenuItem("Open Log View", JavaFXUtils.createIcon("/icons/monitor.png"));
		logItem.setOnAction(actionEvent -> new DLogConsolePane(dockPane).asDockNode());
		
		MenuItem terminalItem = new MenuItem("Open Terminal View", JavaFXUtils.createIcon("/icons/console.png"));
		terminalItem.setOnAction(event -> {
			TerminalConfig darkConfig = new TerminalConfig();
			darkConfig.setBackgroundColor(Color.rgb(16, 16, 16));
			darkConfig.setForegroundColor(Color.rgb(240, 240, 240));
			darkConfig.setCursorColor(Color.rgb(255, 0, 0, 0.5));

			TerminalBuilder terminalBuilder = new TerminalBuilder(darkConfig);
			TerminalTab terminal = terminalBuilder.newTerminal();

			TabPane tabPane = new TabPane();
			new DraggingTabPaneSupport().addSupport(tabPane);
			tabPane.getTabs().add(terminal);
			
			new DockNode(dockPane, tabPane, "Terminal", JavaFXUtils.createIcon("/icons/console.png"));
		});

		menu1.getItems().addAll(sqlPaneViewItem, jsonTableViewItem, logItem, terminalItem, sqlConsoleViewItem);

		final Menu menu2 = new Menu("Restful Service", JavaFXUtils.createIcon("/icons/web.png"));
		MenuItem restServiceStartItem = new MenuItem("Start Restful Service", JavaFXUtils.createIcon("/icons/play.png"));
		restServiceStartItem.setOnAction(actionEvent -> {
			if (restServiceStarted == false) {
				try {
					RESTfulService.configure(restServiceConfig.getIp(), restServiceConfig.getPort());
					RESTfulService.init(sqlConnector);
					RESTfulService.start();
					restServiceStartItem.setGraphic(JavaFXUtils.createIcon("/icons/stop.png"));
					restServiceStartItem.setText("Stop Restful Service");
					restServiceStarted = true;
					DialogFactory.createNotification("Restful Service", "Restful Service started !");
				} catch(Exception e) {
					DialogFactory.createErrorNotification(e);
				}
			} else {
				RESTfulService.stop();
				restServiceStarted = false;
				DialogFactory.createNotification("Restful Service", "Restful Service stopped !");
				restServiceStartItem.setGraphic(JavaFXUtils.createIcon("/icons/play.png"));
				restServiceStartItem.setText("Start Restful Service");
			}
		});

		MenuItem restServiceConfigItem = new MenuItem("Configure Restful Service", JavaFXUtils.createIcon("/icons/settings.png"));
		restServiceConfigItem.setOnAction(actionEvent -> createRestServiceConfigBox());
		
		menu2.getItems().addAll(restServiceStartItem, restServiceConfigItem);

		Menu menu3 = new Menu();
		HBox customGraphic = new HBox(JavaFXUtils.createIcon("/icons/settings.png"), new Label("Internal DB"));
		customGraphic.setSpacing(5);
		menu3.setGraphic(customGraphic);
		menu3.getGraphic().setOnMouseClicked(mouseEvent -> {
			if (!isInternalDBShowing) {
				DSqlPane newSqlPane = new DSqlPane(SqlBrowserFXAppManager.getConfigSqlConnector());
				newSqlPane.asDockNode().setTitle("SqlBrowserFX Internal Database");
				newSqlPane.asDockNode().setDockPane(dockPane);
				newSqlPane.asDockNode().setFloating(true);
				newSqlPane.createSqlTableTabWithDataUnsafe("connections_history");
				newSqlPane.createSqlTableTabWithDataUnsafe("saved_queries");
//				newSqlPane.createSqlTableTabWithDataUnsafe("autocomplete");
//				newSqlPane.createSqlTableTabWithDataUnsafe("queries_history");
				isInternalDBShowing  = true;
				newSqlPane.asDockNode().setOnClose(() -> isInternalDBShowing = false);
			}
		});
		
		Menu menu4 = new Menu("Transactions", JavaFXUtils.createIcon("/icons/transaction.png"));
		MenuItem commitAllItem = new MenuItem("Commit all", JavaFXUtils.createIcon("/icons/check.png"));
		commitAllItem.setOnAction(actionEvent -> sqlConnector.commitAll());
		
		MenuItem rollbackAllItem = new MenuItem("Rollback all", JavaFXUtils.createIcon("/icons/refresh.png"));
		rollbackAllItem.setOnAction(actionEvent -> sqlConnector.rollbackAll());
		
		menu4.getItems().addAll(commitAllItem, rollbackAllItem);
		if (sqlConnector.isAutoCommitModeEnabled())
			menu4.setDisable(true);
		
		Menu menu5 = new Menu();
		customGraphic = new HBox(JavaFXUtils.createIcon("/icons/help.png"), new Label("Help"));
		customGraphic.setSpacing(5);
		menu5.setGraphic(customGraphic);
		menu5.getGraphic().setOnMouseClicked(mouseEvent -> {
			new DockNode(dockPane, new HelpTabPane(), "Help", null);
		});

		MenuBar menuBar = new MenuBar();
		queriesMenu = new QueriesMenu();
		menuBar.getMenus().addAll(menu1, menu2, queriesMenu, menu4, menu3, menu5);

		return menuBar;
	}

	private void createRestServiceConfigBox() {
		if (isRestConfigurationShowing)
			return;
		
		ImageView bottleLogo = JavaFXUtils.createImageView("/icons/javalin-logo.png", 0.0, 200.0);
		Label ipLabel = new Label("Ip address");
		TextField ipField = new TextField(restServiceConfig.getIp());
		Label portLabel = new Label("Port");
		TextField portField = new TextField(restServiceConfig.getPort().toString());
		Button saveButton = new Button("Save", JavaFXUtils.createIcon("/icons/check.png"));

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
			isRestConfigurationShowing  = false;
			stage.close();
		});
		isRestConfigurationShowing = true;
		stage.setOnCloseRequest(windowEvent -> isRestConfigurationShowing  = false);
	}
	
	private void saveConnectionToHistory() {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeAsync(() -> {
			try {
				String query = "insert into connections_history (database, database_type) values (?, ?)";
				SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdate(query,
						Arrays.asList(DB, "sqlite"));
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}

}
