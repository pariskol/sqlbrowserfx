package gr.sqlbrowserfx.nodes;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class MySqlConfigBox extends VBox {

	private TextField userField;
	private PasswordField passwordField;
	private TextField databaseField;
	private Button connectButton;
	private ProgressIndicator loader;
	
	public MySqlConfigBox() {
		this.setPadding(new Insets(5));
		this.setSpacing(5);
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
		this.getChildren().add(databaseField);
		connectButton = new Button("Connect");
		this.getChildren().add(connectButton);

		this.loader = new ProgressIndicator();
		this.loader.setMaxSize(40, 40);
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
	
	public void showLoader(boolean show) {
		if (show) {
			Platform.runLater(() -> this.getChildren().add(loader));
		}
		else {
			Platform.runLater(() -> this.getChildren().remove(loader));
		}
	}
}
