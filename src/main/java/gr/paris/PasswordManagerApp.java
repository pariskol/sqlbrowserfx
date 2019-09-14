package gr.paris;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.BasicConfigurator;
import org.controlsfx.control.PopOver;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadKeyTemplates;

import animatefx.animation.FadeIn;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqliteConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.sqlTableView.EditBox;
import gr.sqlfx.sqlTableView.SqlTableRow;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class PasswordManagerApp extends Application{

	private Button addButton;
	private PopOver popOver;
	private TextField searchField;
	Timer searchTimer;

	private PasswordTableView passwordTableView;
	private BorderPane bPane;

	public PasswordManagerApp() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws GeneralSecurityException, IOException {
		BasicConfigurator.configure();
	    AeadConfig.register();

	    tink();
		launch(args);
	}
	
	public static void tink() throws GeneralSecurityException, IOException {
//		 // Generate the key material...
//	    KeysetHandle keysetHandle = KeysetHandle.generateNew(
//	        AeadKeyTemplates.AES128_GCM);
//
//	    // and write it to a file.
	    String keysetFilename = "my_keyset.json";
//	    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(
//	        new File(keysetFilename)));
	    
	    KeysetHandle keysetHandle = CleartextKeysetHandle.read(
	            JsonKeysetReader.withFile(new File(keysetFilename)));
	    
	 // 2. Get the primitive.
	    Aead aead = keysetHandle.getPrimitive(Aead.class);

	    // 3. Use the primitive to encrypt a plaintext,
	    byte[] ciphertext = aead.encrypt("yoyoyo".getBytes(), new byte[0]);

	    // ... or to decrypt a ciphertext.
	    byte[] decrypted = aead.decrypt(ciphertext, new byte[0]);
	    System.out.println(new String(decrypted, StandardCharsets.UTF_8));
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("SqlBrowser");

		SqlConnector sqlConnector = new SqliteConnector("./pass.db");
		passwordTableView = new PasswordTableView(sqlConnector);
		sqlConnector.executeQueryRaw("select * from passwords", rset -> {
			passwordTableView.setItems(rset);
		});
		
		popOver = new PopOver();
		addButton = new Button("", JavaFXUtils.icon("/res/add.png"));
		addButton.setOnAction(action -> this.addButtonAction());
		searchField = new TextField();
		searchField.setPromptText("search...");
		searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (searchTimer != null)
				searchTimer.cancel();
			searchTimer = new Timer();
			searchTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (newValue.isEmpty()) {
						Platform.runLater(() -> passwordTableView.setItems(passwordTableView.getSqlTableRows()));
					} else {
						PasswordManagerApp.this.searchFieldAction();
					}
				}
			}, 1000);
		});

		bPane = new BorderPane();
		bPane.setTop(new HBox(addButton, searchField));
		bPane.setCenter(passwordTableView);
		
		searchField.prefWidthProperty().bind(bPane.widthProperty());

		Scene scene = new Scene(bPane, 800, 400);
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();

		primaryStage.getIcons().add(new Image("/res/sqlite.png"));

		primaryStage.show();

		primaryStage.setOnCloseRequest(closeEvent -> {
		
			Platform.exit();
			System.exit(0);
		});
		
	}
	
	protected void addButtonAction() {
		if (addButton.isFocused() && popOver.isShowing())
			return;

		addButton.requestFocus();

		if (passwordTableView.getColumns().size() == 0)
			return;

		EditBox editBox = new EditBox(passwordTableView, null, false);
		editBox.setPadding(new Insets(10));

		try {
			passwordTableView.getSqlConnector().executeQuery("select id from passwords order by id desc limit 1 ", rset -> {
				editBox.getMap().get("ID").setText(String.valueOf(rset.getInt(1) + 1));
			});
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
		
		Button addBtn = new Button("Add", JavaFXUtils.icon("/res/check.png"));
		addBtn.setTooltip(new Tooltip("Add"));
		editBox.getMainBox().getChildren().add(addBtn);


		addBtn.setOnAction(submitEvent -> {
			passwordTableView.getSqlConnector().executeAsync(() -> passwordTableView.insertRecord(editBox));
//			bPane.setLeft(null);
		});
		addBtn.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				addBtn.getOnAction().handle(new ActionEvent());
			}
		});

		if (bPane.getRight() != null) {
			bPane.setRight(null);
		}
		else {
			bPane.setRight(new ScrollPane(editBox));
			new FadeIn(editBox).play();
		}
		addBtn.requestFocus();
	}
	

	private void searchFieldAction() {
		passwordTableView.getSelectionModel().clearSelection();
		// use executor service of sqlConnector
		passwordTableView.getSqlConnector().executeAsync(() -> {
			Platform.runLater(() -> passwordTableView.setItems(passwordTableView.getSqlTableRows()));
			ObservableList<SqlTableRow> searchRows = FXCollections.observableArrayList();

			for (SqlTableRow row : passwordTableView.getSqlTableRows()) {
				for (TableColumn<SqlTableRow, ?> column : passwordTableView.getVisibleLeafColumns()) {
					if (column.getText().equals("APP")) {
						if (row.get(column.getText()) != null) {
							if (row.get(column.getText()).toString().matches("(?i:.*" + searchField.getText() + ".*)")) {
								searchRows.add(new SqlTableRow(row));
								break;
							}
						}
						break;
				}
				
				}
				Platform.runLater(() -> passwordTableView.setItems(searchRows));

			}
		});
	}
}
