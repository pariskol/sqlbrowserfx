package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.util.Arrays;

import org.controlsfx.control.PopOver;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleEvent;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

public class CSqlCodeArea extends SqlCodeArea {

	private PopOver saveQueryPopOver;

	public CSqlCodeArea() {
		super();
	}

	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		MenuItem menuItemSave = new MenuItem("Save Query", JavaFXUtils.createIcon("/icons/thunder.png"));
		menuItemSave.setOnAction(action -> this.saveQueryAction());
		menuItemSave.disableProperty().bind(this.isTextSelectedProperty().not());
		menu.getItems().addAll(menuItemSave);
		return menu;
	}

	@Override
	protected void onMouseClicked() {
		super.onMouseClicked();
		if (saveQueryPopOver != null)
			saveQueryPopOver.hide();
	}
	
	@Override
	public void setInputMap() {
		if (!isEditable())
			return;
		
		super.setInputMap();
		InputMap<Event> saveQuery = InputMap.consume(
				EventPattern.keyPressed(KeyCode.S, KeyCombination.CONTROL_DOWN),
				action -> this.saveQueryAction()
        );
		
        Nodes.addInputMap(this, saveQuery);
	}
	
	protected void saveQueryAction() {
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
			DialogFactory.createErrorNotification(e);
		}

		Button addButton = new Button("Save", JavaFXUtils.createIcon("/icons/check.png"));
		addButton.setOnAction(event -> {
			sqlConnector.executeAsync(() -> {
				try {
					String query = !this.getSelectedText().isEmpty() ? this.getSelectedText() : this.getText();
					sqlConnector.executeUpdate("insert into saved_queries (query,category,description) values (?,?,?)",
							Arrays.asList(query,
									categoryField.getSelectionModel().getSelectedItem(),
									descriptionField.getText()));
					DialogFactory.createNotification("Info", "Query has been saved successfuly");
					this.fireEvent(new SimpleEvent());
				} catch (SQLException e) {
					DialogFactory.createErrorNotification(e);
				}
			});
		});

		VBox vb = new VBox(categoryField, descriptionField, addButton);
		saveQueryPopOver  = new PopOver(vb);
		
		categoryField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				saveQueryPopOver.hide();
			}
		});
		descriptionField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE) {
				saveQueryPopOver.hide();
			}
		});
		saveQueryPopOver.setArrowSize(0);
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		saveQueryPopOver.show(this, boundsInScene.getMinX()+saveQueryPopOver.getWidth()/3, boundsInScene.getMinY()-saveQueryPopOver.getHeight()/2);
	}

}
