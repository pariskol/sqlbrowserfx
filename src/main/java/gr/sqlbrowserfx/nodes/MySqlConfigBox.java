package gr.sqlbrowserfx.nodes;

import java.sql.SQLException;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.nodes.tableviews.HistorySqlTableView;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MySqlConfigBox extends VBox {

	private TextField urlField;
	private TextField userField;
	private PasswordField passwordField;
	private TextField databaseField;
	private Button connectButton;
	private ProgressIndicator loader;
	private HistorySqlTableView sqlTableView;
	
	private String lastDatabase = "@database@";


	public MySqlConfigBox() {
		this.setPadding(new Insets(5));
		this.setSpacing(5);
		
		this.getChildren().add(new Label("Databse url"));
		urlField = new TextField();
		urlField.setPromptText("jdbc:mysql://localhost:3306/" + lastDatabase + "?autoReconnect=true&useSSL=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
		urlField.textProperty()
				.addListener((observable, oldValue, newValue) -> {
					String[] split = urlField.getText().split("/");
					if (split.length > 3) {
						databaseField.setText(split[split.length - 1].replaceAll("\\?.*", ""));
					}
				});
		userField = new TextField();
		this.getChildren().add(urlField);
		this.getChildren().add(new Label("Username"));
		userField = new TextField();
		userField.setPromptText("Enter username ...");
		this.getChildren().add(userField);
		this.getChildren().add(new Label("Password"));
		passwordField = new PasswordField();
		passwordField.setPromptText("Enter password ...");
		this.getChildren().add(passwordField);
		this.getChildren().add(new Label("Database"));
		databaseField = new TextField();
		databaseField.setPromptText("Enter database name ...");
		databaseField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.isEmpty()) {
				urlField.setPromptText(urlField.getPromptText().replace(lastDatabase, "@" + newValue + "@"));
				lastDatabase = "@" + newValue + "@";
			}
			else {
				urlField.setPromptText(urlField.getPromptText().replace(lastDatabase, "@database@"));
				lastDatabase = "@database@";
			}
		});
//		this.getChildren().add(databaseField);
		connectButton = new Button("Connect", JavaFXUtils.createIcon("/icons/database.png"));
		
		this.loader = new ProgressIndicator();
		this.loader.setMaxSize(40, 40);
		this.loader.setVisible(false);
		
		HBox hb = new HBox(connectButton, loader);
		hb.setSpacing(5);
		this.getChildren().add(hb);
		
		this.getChildren().add(new Label("History"));
		sqlTableView = new HistorySqlTableView(SqlBrowserFXAppManager.getConfigSqlConnector());
		sqlTableView.setColumnWidth(0, 0, 300);
		this.getChildren().add(sqlTableView);
		sqlTableView.setOnMouseClicked( mouseEvent -> {
			if (sqlTableView.getSelectionModel().getSelectedItem() != null) {
				MapTableViewRow row = sqlTableView.getSelectionModel().getSelectedItem();
				Platform.runLater(() -> {
					urlField.setText(row.get("url").toString());
					userField.setText(row.get("user").toString());
					databaseField.setText(row.get("database").toString());
				});
			}
		});

		SqlBrowserFXAppManager.getConfigSqlConnector()
							  .executeQueryRawAsync(
			this.getHistoryQuery(),
			rset -> {
				sqlTableView.setItemsLater(rset);
			}
		);

		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				if (!connectButton.isFocused()) {
					connectButton.requestFocus();
					keyEvent.consume();
				}
			}
		});
	}

	public MySqlConfigBox(String url) {
		this();
		urlField.setText(url);
	}

	public String getHistoryQuery() {
		return "select url, user, database, timestamp, id from connections_history_localtime"
				+ " where database_type = 'mysql' order by timestamp desc";
	}
	
	public TextField getUserField() {
		return userField;
	}

	public void setUserField(TextField userField) {
		this.userField = userField;
	}

	public PasswordField getPasswordField() {
		return passwordField;
	}

	public void setPasswordField(PasswordField passwordField) {
		this.passwordField = passwordField;
	}

	public TextField getDatabaseField() {
		return databaseField;
	}

	public void setDatabaseField(TextField databaseField) {
		this.databaseField = databaseField;
	}

	public Button getConnectButton() {
		return connectButton;
	}

	public void setConnectButton(Button submitButton) {
		this.connectButton = submitButton;
	}

	public TextField getUrlField() {
		return urlField;
	}

	public void setUrlField(TextField urlField) {
		this.urlField = urlField;
	}
	
	public String getUrl() {
		return urlField.getText().isEmpty() ? urlField.getPromptText().replaceAll("@", "") : urlField.getText();
	}

	public void showLoader(boolean show) {
		Platform.runLater(() -> this.loader.setVisible(show));
	}

	public String getSaveType() {
		return "mysql";
	}
	
	public void saveToHistory() {
		SqlBrowserFXAppManager.getConfigSqlConnector().executeAsync(() -> {
			try {
				String query = "insert into connections_history (url, user, database, database_type) values (?, ?, ?, ?)";
				SqlBrowserFXAppManager.getConfigSqlConnector().executeUpdate(query,
						Arrays.asList(urlField.getText(), userField.getText(), databaseField.getText(), this.getSaveType()));
			} catch (SQLException e) {
				LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage(), e);
			}
		});
	}
}
