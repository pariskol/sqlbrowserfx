
package gr.sqlbrowserfx;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.dock.nodes.DDBTreePane;
import gr.sqlbrowserfx.dock.nodes.DSqlConsolePaneNH;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.MySqlConfigBox;
import gr.sqlbrowserfx.nodes.SqlConsolePane;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.sqlpane.SimpleSqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.nodes.tableviews.HistorySqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.PropertiesLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SqlBrowserFXAppWithoutDocking extends Application {

	private static final String CSS_THEME = "/styles/"
			+ (String) PropertiesLoader.getProperty("sqlbrowserfx.css.theme", String.class, "flat-dark") + ".css";
	private static final Boolean AUTO_COMMIT_IS_ENABLED = (Boolean) PropertiesLoader
			.getProperty("sqlconnector.enable.autocommit", Boolean.class, true);

	private static String DB;

	private Scene primaryScene;
	public static Stage STAGE;
	private SqlPane mainSqlPane;

	private SqlConnector sqlConnector;
	private DDBTreePane ddbTreePane;

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
		var defaultFont = Font.getDefault();
		fontSize = defaultFont.getSize();
		SqlBrowserFXAppWithoutDocking.STAGE = primaryStage;
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
		var selectedDBtext = new Label("No database selected");
		var openButton = new Button("Open", JavaFXUtils.createIcon("/icons/database.png"));
		openButton.setOnAction(actionEvent -> dbSelectionAction(selectedDBtext.getText()));
		var bottomBox = new HBox(selectedDBtext, openButton);
		bottomBox.setPadding(new Insets(5));
		bottomBox.setSpacing(5);
		bottomBox.setAlignment(Pos.CENTER_RIGHT);

		var rightBox = new VBox();
		var text = new Label("Browse system for database...");
		var fileChooserButton = new Button("Search", JavaFXUtils.createIcon("/icons/magnify.png"));
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

		var recentDBsText = new Label("History");
		recentDBsText.setTextAlignment(TextAlignment.CENTER);

		var recentDBsTableView = new HistorySqlTableView(
				SqlBrowserFXAppManager.getConfigSqlConnector());

		SqlBrowserFXAppManager.getConfigSqlConnector().executeQueryRawAsync(
				"select database, timestamp, id from connections_history_localtime where database_type = 'sqlite' order by timestamp desc",
				rset -> recentDBsTableView.setItemsLater(rset));

		recentDBsTableView.setOnMouseClicked(mouseEvent -> {
			if (recentDBsTableView.getSelectionModel().getSelectedItem() != null) {
				selectedDBtext
						.setText(recentDBsTableView.getSelectionModel().getSelectedItem().get("database").toString());
				if (mouseEvent.getClickCount() == 2)
					dbSelectionAction(selectedDBtext.getText());
			}
		});
		var leftBox = new VBox(recentDBsText, recentDBsTableView);
		leftBox.setAlignment(Pos.CENTER);
		leftBox.setPadding(new Insets(5));
		leftBox.setSpacing(5);

		var borderPane = new BorderPane();
		borderPane.setCenter(rightBox);
		borderPane.setLeft(leftBox);
		borderPane.setBottom(bottomBox);

		var sqliteTab = new Tab("Sqlite", borderPane);
		sqliteTab.setGraphic(JavaFXUtils.createImageView("/icons/sqlite.png", 28.0, 28.0));
		sqliteTab.setClosable(false);
		MySqlConfigBox mySqlConfigBox = new MySqlConfigBox();
		mySqlConfigBox.getConnectButton().setOnAction(actionEvent -> {
			mySqlConfigBox.showLoader(true);
			dbSelectionAction(mySqlConfigBox);
		});
//		mySqlConfigBox.getChildren().add(new ListView<String>(FXCollections.observableArrayList(Arrays.asList("Yesterday", "jdbc:mysql://localhost:3306/sakila?autoReconnect=true&useSSL=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"))));
		var mysqlTab = new Tab("MySQL", mySqlConfigBox);
		mysqlTab.setGraphic(JavaFXUtils.createImageView("/icons/mysql.png", 28.0, 28.0));
		mysqlTab.setClosable(false);
		var dbTabPane = new TabPane(sqliteTab, mysqlTab);

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
		var sqliteConnector = new SqliteConnector(dbPath);
		sqliteConnector.setAutoCommitModeEnabled(AUTO_COMMIT_IS_ENABLED);
		this.sqlConnector = sqliteConnector;
		if (System.getProperty("mode", "normal").equals("simple")) {
			SqlCodeAreaSyntaxProvider.init(SqlBrowserFXAppManager.getDBtype());
			primaryScene.setRoot(new SqlConsolePane(sqliteConnector));
			JavaFXUtils.addZoomInOutSupport(primaryScene.getRoot());
			STAGE.setScene(primaryScene);
		} else
			createAppView(sqliteConnector);

	}

	private void dbSelectionAction(MySqlConfigBox configBox) {
		configBox.getConnectButton().setDisable(true);
		DB = configBox.getDatabaseField().getText();
		var mysqlConnector = new MysqlConnector(configBox.getUrl(), configBox.getDatabaseField().getText(),
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
					SqlCodeAreaSyntaxProvider.init(SqlBrowserFXAppManager.getDBtype());
					primaryScene.setRoot(new SqlConsolePane(mysqlConnector));
					JavaFXUtils.addZoomInOutSupport(primaryScene.getRoot());
					STAGE.setScene(primaryScene);
				} else
					createAppView(mysqlConnector);
			});
		});
	}

	private String determineDBType(SqlConnector sqlConnector) {
		String dbType = null;
		if (sqlConnector instanceof SqliteConnector)
			dbType = "sqlite";
		else if (sqlConnector instanceof MysqlConnector)
			dbType = "mysql";
		return dbType;
	}

	private void createAppView(SqlConnector sqlConnector) {

		SqlBrowserFXAppManager.setDBtype(determineDBType(sqlConnector));
		SqlCodeAreaSyntaxProvider.init(SqlBrowserFXAppManager.getDBtype());
		
		STAGE.setMaximized(true);

		mainSqlPane = new SimpleSqlPane(sqlConnector);
		SqlBrowserFXAppManager.registerSqlPane(mainSqlPane);
		ddbTreePane = new DDBTreePane(DB, sqlConnector);
		var sqlConsolePane = new DSqlConsolePaneNH(sqlConnector, mainSqlPane);
		sqlConsolePane.addObserver(ddbTreePane.getDBTreeView());
		SqlBrowserFXAppManager.registerDDBTreeView(ddbTreePane.getDBTreeView());
		ddbTreePane.getDBTreeView().asDockNode()
				.setOnClose(() -> SqlBrowserFXAppManager.unregisterDDBTreeView(ddbTreePane.getDBTreeView()));

		ddbTreePane.getDBTreeView().addObserver(value -> {
			SqlCodeAreaSyntaxProvider.bind(ddbTreePane.getDBTreeView().getContentNames().stream().map(kw -> new Keyword(kw, KeywordType.TABLE))
					.collect(Collectors.toList()));
		});

		var verticalSp = new SplitPane();
		verticalSp.getItems().addAll(sqlConsolePane, mainSqlPane);
		verticalSp.setOrientation(Orientation.VERTICAL);
		verticalSp.setDividerPositions(0.3, 0.7);

		var tp = new TabPane(new Tab("DB View", ddbTreePane), new Tab("History View", sqlConsolePane.getHistoryBox()));
		tp.getTabs().forEach(tab -> tab.setClosable(false));
		var mainSp = new SplitPane(tp, verticalSp);
		mainSp.setDividerPositions(0.2, 0.8);
;
		var vbox = new VBox();
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(mainSp);
		VBox.setVgrow(mainSp, Priority.ALWAYS);

		JavaFXUtils.applyJMetro(vbox);
		JavaFXUtils.addZoomInOutSupport(vbox);

		if (primaryScene == null) {
			primaryScene = new Scene(vbox);
			STAGE.setScene(primaryScene);
			primaryScene.getStylesheets().add(CSS_THEME);
		}

		primaryScene.setRoot(vbox);
	}

	private void saveConnectionToHistory() {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeAsync(() -> {
			try {
				String query = "insert into connections_history (database, database_type) values (?, ?)";
				SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdate(query, Arrays.asList(DB, "sqlite"));
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}

}
