package gr.sqlbrowserfx.nodes.tableviews;

import java.util.ArrayList;
import java.util.List;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.json.JSONArray;

import gr.sqlbrowserfx.nodes.InputMapOwner;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class JSONTableView extends TableView<MapTableViewRow> implements InputMapOwner {

	protected List<String> columns;
	protected ObservableList<MapTableViewRow> rows;
	double minWidth, prefWidth, maxWidth;
	protected boolean autoResize;
	protected final static int NOT_SET = 0;
	int currentColumnPos = 0;


	public JSONTableView() {

		rows = FXCollections.observableArrayList();
		autoResize = false;
		minWidth = 0;
		prefWidth = 0;
		maxWidth = 0;

		this.setInputMap();
		
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private void setKeys() {
		this.setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				if (keyEvent.getCode() == KeyCode.LEFT) {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));
				}
				if (keyEvent.getCode() == KeyCode.RIGHT) {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));
				}
			}
		});
	}

	@Override
	public void setInputMap() {
		Nodes.addInputMap(this, 
				InputMap.consume(
				EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN),
				action -> {
					if (currentColumnPos < getColumns().size() - 1)
						this.scrollToColumn(getColumns().get(currentColumnPos++));
				}
        ));
		Nodes.addInputMap(this, 
				InputMap.consume(
				EventPattern.keyPressed(KeyCode.LEFT, KeyCombination.CONTROL_DOWN),
				action -> {
					if (currentColumnPos > 0)
						this.scrollToColumn(getColumns().get(currentColumnPos--));
				}
        ));
	}
	
	public synchronized void setItemsLater(JSONArray jsonArray) {

		rows.clear();
		columns = new ArrayList<>();
		jsonArray.getJSONObject(0).keys().forEachRemaining(columns::add);

		for (int i = 0; i < jsonArray.length(); i++) {
			rows.add(new MapTableViewRow(jsonArray.getJSONObject(i).toMap()));
		}

		Platform.runLater(() -> {
			super.setItems(FXCollections.emptyObservableList());
			this.getColumns().clear();

			for (String column : columns) {
				TableColumn<MapTableViewRow, Object> col = new TableColumn<>(column);
				col.setCellValueFactory(param -> param.getValue().getObjectProperty(column));
				col.setCellFactory(callback -> new EditableCell(this));
				this.getColumns().add(col);
			}

			this.autoResizedColumns(autoResize);
			this.setColumnWidth(10, NOT_SET, 300);
			this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			super.setItems(rows);
		});
	}

	public boolean areCellsEditableByClick() {
		return true;
	}
	
	// 0 for no set
	public void setColumnWidth(double min, double pref, double max) {
		this.minWidth = min;
		this.prefWidth = pref;
		this.maxWidth = max;

		for (TableColumn<?, ?> column : this.getColumns()) {
			if (min > NOT_SET)
				column.setMinWidth(minWidth);
			if (pref > NOT_SET)
				column.setPrefWidth(prefWidth);
			if (max > NOT_SET)
				column.setMaxWidth(maxWidth);
		}
	}

	public void autoResizedColumns(boolean autoResize) {
		this.autoResize = autoResize;
		if (autoResize) {
			this.setColumnWidth(NOT_SET, NOT_SET, NOT_SET);
			for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
				column.prefWidthProperty().bind(this.widthProperty().divide(this.getVisibleLeafColumns().size()));
			}
		} else {
			for (TableColumn<?, ?> column : this.getVisibleLeafColumns()) {
				column.prefWidthProperty().unbind();
			}
			this.setColumnWidth(minWidth, prefWidth, maxWidth);
		}
	}

}
