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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.paris.dock.nodes.DSqlConsoleView;
import gr.paris.dock.nodes.DSqlPane;
import gr.paris.nodes.Keywords;
import gr.paris.nodes.MySqlConfigBox;
import gr.paris.rest.service.RestServiceConfig;
import gr.paris.rest.service.SparkRestService;
import gr.sqlfx.conn.MysqlConnector;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqlTable;
import gr.sqlfx.conn.SqliteConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.utils.DTOMapper;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
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
	private static Logger logger;

	private Scene scene;
	private SqlConnector sqlConnector;
	private boolean restServiceStarted;

	public static void main(String[] args) {
		logger = LoggerFactory.getLogger("SPARK");
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("log4j.properties"));
			PropertyConfigurator.configure(props);
		} catch (Exception e) {
			BasicConfigurator.configure();
		}
        Keywords.onKeywordsBind();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
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
		sqlPane.asDockNode().dock(dockPane, DockPos.CENTER);

//		DSqlPane sqlPane2 = new DSqlPane(sqlConnector);
//		sqlPane2.asDockNode().setPrefSize(scene.getWidth()/4, scene.getHeight());
//		sqlPane2.asDockNode().dock(dockPane, DockPos.RIGHT, sqlPane.asDockNode());

//		DSqlConsoleView dSqlConsoleView = new DSqlConsoleView(sqlConnector);
//		dSqlConsoleView.asDockNode().setPrefSize(scene.getWidth() / 4, scene.getHeight());
//		dSqlConsoleView.asDockNode().dock(dockPane, DockPos.BOTTOM, sqlPane2.asDockNode());

		TreeView<String> treeView = this.createTreeView(dockPane);
		DockNode dockNode = new DockNode(treeView, "Structure", JavaFXUtils.icon("/res/details.png"));
		dockNode.setPrefSize(scene.getWidth()/4, scene.getHeight());
		dockNode.dock(dockPane, DockPos.LEFT);
		
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
			TreeView<String> treeView = this.createTreeView(dockPane);
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
	
	public TreeView<String> createTreeView(DockPane dockPane) {
		{
			TreeItem<String> rootItem = new TreeItem<>(DB, JavaFXUtils.icon("/res/database.png"));
			rootItem.setExpanded(true);

			TreeItem<String> tablesRootItem = new TreeItem<>("Tables", JavaFXUtils.icon("/res/table.png"));
			TreeItem<String> viewsRootItem = new TreeItem<>("Views", JavaFXUtils.icon("/res/view.png"));
			TreeItem<String> indicesRootItem = new TreeItem<>("Indices", JavaFXUtils.icon("/res/index.png"));
			rootItem.getChildren().addAll(tablesRootItem, viewsRootItem, indicesRootItem);

			List<String> tables = new ArrayList<>();
			try {
				sqlConnector.executeQuery("select name,type from sqlite_master", rset -> {
					try {
						HashMap<String, Object> dto = DTOMapper.map(rset);
						tables.add((String) dto.get("name"));
						TreeItem<String> treeItem = new TreeItem<String>((String) dto.get("name"));
						if (dto.get("type").equals("table")) {
							tablesRootItem.getChildren().add(treeItem);
							treeItem.setGraphic(JavaFXUtils.icon("/res/table.png"));
						}
						else if (dto.get("type").equals("view")) {
							viewsRootItem.getChildren().add(treeItem);
							treeItem.setGraphic(JavaFXUtils.icon("/res/view.png"));
						}
						else if (dto.get("type").equals("index")) {
							indicesRootItem.getChildren().add(treeItem);
							treeItem.setGraphic(JavaFXUtils.icon("/res/index.png"));
						}

					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				});
				for (TreeItem<String> table : tablesRootItem.getChildren()) {
					TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.icon("/res/script.png"));
					table.getChildren().add(schemaTree);
					
					sqlConnector.executeQuery("select sql from sqlite_master where name = ?", Arrays.asList(table.getValue()), rset -> {
						String schema = rset.getString("sql");
						schemaTree.getChildren().add(new TreeItem<String>(schema));
					});
					
					TreeItem<String> columnsTree = new TreeItem<>("columns", JavaFXUtils.icon("/res/columns.png"));
					table.getChildren().add(columnsTree);

					sqlConnector.executeQueryRaw("select * from " + table.getValue(), rset -> {
						SqlTable sqlTable = new SqlTable(rset.getMetaData());
						sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(table.getValue()));
						sqlTable.setForeignKeys(sqlConnector.findForeignKeys(table.getValue()));
						sqlTable.getColumns();
						for(String column : sqlTable.getColumns()) {
							TreeItem<String> columnTreeItem = new TreeItem<String>(column);
							if (column.equals(sqlTable.getPrimaryKey()))
								columnTreeItem.setGraphic(JavaFXUtils.icon("/res/primary-key.png"));
							else if (sqlTable.isForeignKey(column))
								columnTreeItem.setGraphic(JavaFXUtils.icon("/res/foreign-key.png"));
							columnsTree.getChildren().add(columnTreeItem);
						}
					});
				}
				for (TreeItem<String> table : indicesRootItem.getChildren()) {
					TreeItem<String> schemaTree = new TreeItem<>("schema", JavaFXUtils.icon("/res/script.png"));
					table.getChildren().add(schemaTree);
					
					sqlConnector.executeQuery("select sql from sqlite_master where name = ?", Arrays.asList(table.getValue()), rset -> {
						String schema = rset.getString("sql");
						schemaTree.getChildren().add(new TreeItem<String>(schema));
					});
				}
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}

			Keywords.bind(tables);


			TreeView<String> treeView = new TreeView<>();
			this.createContextMenu(treeView);
			treeView.setRoot(rootItem);
		
			return treeView;
		}
	}
	
	private ContextMenu createContextMenu(TreeView<String> treeView) {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy text", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copyAction(treeView));


		contextMenu.getItems().addAll(menuItemCopy);
		treeView.setContextMenu(contextMenu);

		return contextMenu;
	}
	
	private void copyAction(TreeView<String> treeView) {
		StringSelection stringSelection = new StringSelection(treeView.getSelectionModel().getSelectedItem().getValue());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

}
