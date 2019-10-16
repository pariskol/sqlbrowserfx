package gr.sqlbrowserfx.nodes.sqlPane;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.controlsfx.control.PopOver;

import gr.sqlbrowserfx.listeners.CloseAction;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.nodes.sqlTableView.SqlTableRow;
import gr.sqlbrowserfx.nodes.sqlTableView.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SqlTableRowEditBox extends BorderPane implements SimpleChangeListener<SqlTableRow> {

	private HashMap<String, TextField> fieldsMap;
	private List<String> columns;
	private CloseAction closeAction;
	private SqlTableRow sqlTableRow;
	private VBox centerBox;
	private FlowPane toolbar;
	private ScrollPane scrollPane;

	public SqlTableRowEditBox(SqlTableView sqlTableView, SqlTableRow sqlTableRow, boolean resizeable) {
		centerBox = new VBox();
		fieldsMap = new HashMap<>();
		columns = sqlTableView.getColumnsNames();
		for (String columnName : columns) {
			Label label = new Label(columnName);
			label.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			label.setTooltip(new Tooltip(columnName));
			label.setAlignment(Pos.CENTER_RIGHT);
			TextField textField = new TextField();
			textField.setAlignment(Pos.CENTER);

			this.sqlTableRow = sqlTableRow;
			if (sqlTableRow != null && sqlTableRow.get(columnName) != null)
				textField.setText(sqlTableRow.get(columnName).toString());
			else
				textField.setText("");

			fieldsMap.put(columnName, textField);

			Button infoButton = new Button("", JavaFXUtils.icon("/res/zoom.png"));
			infoButton.setFocusTraversable(false);
			infoButton.setOnMouseClicked(event2 -> {
				infoButton.requestFocus();
				if (textField.getText().isEmpty())
					return;

				TextArea infoText = new TextArea(textField.getText());
				infoText.setWrapText(true);
				infoText.setPrefColumnCount(20);
				infoText.setPrefRowCount(5);
				Button submitButton = new Button("Save", JavaFXUtils.icon("/res/check.png"));
				submitButton.setOnAction(event -> textField.setText(infoText.getText()));
				PopOver info = new PopOver(new VBox(infoText, submitButton));
//				info.setArrowSize(0);
				info.setDetachable(false);
				info.show(infoButton);
			});

			if (sqlTableRow != null && columnName.equals(sqlTableView.getPrimaryKey())) {
				textField.setEditable(false);
				textField.setTooltip(new Tooltip("Primary key can't be edit"));
				label.setGraphic(JavaFXUtils.icon("/res/primary-key.png"));
			} else if (sqlTableRow != null && sqlTableView.getSqlTable().isForeignKey(columnName)) {
				textField.setTooltip(new Tooltip("Foreign key"));
				label.setGraphic(JavaFXUtils.icon("/res/foreign-key.png"));
			}
			HBox node = new HBox(label, textField, infoButton);

			if (resizeable) {
				label.prefWidthProperty().bind(node.widthProperty().multiply(0.4));
				textField.prefWidthProperty().bind(node.widthProperty().multiply(0.6));
//				infoButton.prefWidthProperty().bind(node.widthProperty().multiply(0.1));
			}

			node.setAlignment(Pos.CENTER_RIGHT);
			centerBox.getChildren().add(node);
		}
		
		if (resizeable) {
			scrollPane = new ScrollPane(centerBox);
			scrollPane.hbarPolicyProperty().set(ScrollBarPolicy.NEVER);
			centerBox.prefWidthProperty().bind(scrollPane.widthProperty());
			centerBox.setPadding(new Insets(10));
			this.setCenter(scrollPane);
		}
		else {
			this.setCenter(centerBox);
		}
	}

	public SqlTableRowEditBox() {
		
	}
	public List<TextField> getTextFields() {
		return new ArrayList<>(fieldsMap.values());
	}

	public HashMap<String, TextField> getMap() {
		return fieldsMap;
	}

	public void clear() {
		for (TextField textField : fieldsMap.values()) {
			textField.clear();
		}
	}
	
	public void refresh() {
		for (String column : columns) {
			if (sqlTableRow != null && sqlTableRow.get(column) != null)
				this.put(column, sqlTableRow.get(column).toString());
			else
				this.put(column, "");
		}
	}

	public void setOnClose(CloseAction closeAction) {
		this.closeAction = closeAction;
	}

	public void close() {
		closeAction.close();
	}
	
	public void put(String columnName, String value) {
		this.fieldsMap.get(columnName).setText(value);;
	}

	public void copy() {
		StringBuilder content = new StringBuilder();
		content.append(sqlTableRow.toString());

		StringSelection stringSelection = new StringSelection(content.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	public FlowPane getToolbar() {
		return toolbar;
	}

	public void setBarLeft(FlowPane bar) {
		this.toolbar = bar;
		this.toolbar.setOrientation(Orientation.VERTICAL);
		this.toolbar.prefHeightProperty().bind(centerBox.heightProperty());
		this.setBottom(null);
		this.setLeft(bar);
	}
	
	public void setBarBottom(FlowPane bar) {
		this.toolbar = bar;
		this.toolbar.setOrientation(Orientation.HORIZONTAL);
		this.toolbar.prefWidthProperty().bind(centerBox.widthProperty());
		this.setLeft(null);
		this.setBottom(bar);
	}

	public ScrollPane getScrollPane() {
		return scrollPane;
	}

	
	public VBox getMainBox() {
		return centerBox;
	}

	public void setScrollPane(ScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	@Override
	public void onChange(SqlTableRow newValue) {
		for (String column : columns) {
			TextField textField = fieldsMap.get(column);
			String newText = newValue.get(column) != null ? newValue.get(column).toString() : null;
			textField.setText(newText);
		}
	}

}
