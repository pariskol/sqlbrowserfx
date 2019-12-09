package gr.sqlbrowserfx.nodes.sqlCodeArea;

import java.sql.SQLException;
import java.util.Arrays;

import org.controlsfx.control.PopOver;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

public class CSqlCodeArea extends SqlCodeArea {

	public CSqlCodeArea() {
		super();
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		MenuItem menuItemSave = new MenuItem("Save Query", JavaFXUtils.icon("/res/check.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());

		menu.getItems().addAll(menuItemSave);
		return menu;
	}

	private void saveQueryAction() {
		final SqlConnector sqlConnector = SqlBrowserFXAppManager.getConfigSqlConnector();
		TextField descriptionField = new TextField();
		descriptionField.setPromptText("Description");

		ComboBox<String> categoryField = new ComboBox<>();
		categoryField.setEditable(true);
		try {
			sqlConnector.executeQuery("select distinct category from saved_queries", rset -> {
				categoryField.getItems().add(rset.getString(1));
			});
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}

		Button addButton = new Button("Save", JavaFXUtils.icon("/res/check.png"));
		addButton.setOnAction(event -> {
			try {
				sqlConnector.executeUpdate("insert into saved_queries (query,category,description) values (?,?,?)",
						Arrays.asList(!this.getSelectedText().isEmpty() ? this.getSelectedText() : this.getText(),
								categoryField.getSelectionModel().getSelectedItem(),
								descriptionField.getText()));
				DialogFactory.createInfoDialog("Info", "Query has been saved successfuly");
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			}
		});

		VBox vb = new VBox(categoryField, descriptionField, addButton);
		PopOver popOver = new PopOver(vb);
		
		categoryField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				popOver.hide();
			}
		});
		descriptionField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				popOver.hide();
			}
		});
		popOver.setArrowSize(0);
		Bounds boundsInScene = this.localToScene(this.getBoundsInLocal());
		popOver.show(this, boundsInScene.getMinX()+popOver.getWidth()/3, boundsInScene.getMinY()-popOver.getHeight()/2);

	}
}
