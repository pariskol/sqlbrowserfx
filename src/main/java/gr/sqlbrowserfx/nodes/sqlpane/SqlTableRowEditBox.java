package gr.sqlbrowserfx.nodes.sqlpane;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.controlsfx.control.PopOver;

import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SqlTableRowEditBox extends BorderPane implements SimpleObserver<MapTableViewRow> {

	private final LinkedHashMap<String, TextArea> fieldsMap;
	private final List<String> columns;
	private Runnable closeAction;
	private MapTableViewRow sqlTableRow;
	private final VBox centerBox;
	private FlowPane toolbar;
	private ScrollPane scrollPane;
	private final Label messageLabel;
	private Button actionButton;
	private final TextArea detailsArea;
	private TextArea lastSelectedTextArea;

	public SqlTableRowEditBox(SqlTableView sqlTableView, MapTableViewRow sqlTableRow, boolean resizeable) {
		
		messageLabel = new Label();
		messageLabel.setTextFill(Color.GREEN);
		centerBox = new VBox();
		detailsArea = new TextArea();
		fieldsMap = new LinkedHashMap<>();
		columns = sqlTableView.getColumnsNames();
		for (String columnName : columns) {
			Label label = new Label(columnName);
			label.setTooltip(new Tooltip(columnName));
			TextArea textArea = new TextArea();
			textArea.setPrefRowCount(1);
			textArea.setPrefColumnCount(10);
			textArea.setOnKeyPressed(event -> {
				if (!event.isControlDown())
					return;
				
				if (event.getCode() == KeyCode.ENTER) {
					if (actionButton != null)
						this.actionButton.requestFocus();
				} else if (event.getCode() == KeyCode.TAB) {
					List<TextArea> l = new ArrayList<>(fieldsMap.values());
					int i;
					for (i = 0; i < l.size(); i++) {
						if (l.get(i).equals(textArea))
							break;
					}
					if (event.isShiftDown() && i > 0)
						l.get(i - 1).requestFocus();
					else if (actionButton != null && (i == l.size() - 1 || event.isShiftDown()))
						this.actionButton.requestFocus();
					else if (i < l.size() - 1)
						l.get(i + 1).requestFocus();
				}
				else if(event.getCode() == KeyCode.DOWN) {
					List<TextArea> l = new ArrayList<>(fieldsMap.values());
					int i;
					for (i = 0; i < l.size(); i++) {
						if (l.get(i).equals(textArea))
							break;
					}
					if (i < l.size() - 1)
						l.get(i + 1).requestFocus();
				}
				else if(event.getCode() == KeyCode.UP) {
					List<TextArea> l = new ArrayList<>(fieldsMap.values());
					int i;
					for (i = 0; i < l.size(); i++) {
						if (l.get(i).equals(textArea))
							break;
					}
					if (i > 0)
						l.get(i - 1).requestFocus();
				}
				event.consume();
			});

			this.sqlTableRow = sqlTableRow;
			if (sqlTableRow != null && sqlTableRow.get(columnName) != null) {
				textArea.setText(sqlTableRow.get(columnName).toString());
				if (textArea.getText().contains("\n"))
					textArea.setWrapText(true);
			}
			else
				textArea.setText("");
			//FIXME binding does not work correctly
			textArea.textProperty().addListener((obs, o, n) -> detailsArea.setText(n));
			textArea.focusedProperty().addListener((obs, unfocused, focused) -> {
				if (focused) {
					lastSelectedTextArea = textArea;
					detailsArea.setText(textArea.getText());
				}
			});
			detailsArea.focusedProperty().addListener((obs, unfocused, focused) -> {
				if (focused) {
					detailsArea.textProperty().bindBidirectional(lastSelectedTextArea.textProperty());

				}
				else {
					detailsArea.textProperty().unbindBidirectional(lastSelectedTextArea.textProperty());
					lastSelectedTextArea.textProperty().unbindBidirectional(detailsArea.textProperty());
				}
			});
			fieldsMap.put(columnName, textArea);

			Button infoButton = new Button("", JavaFXUtils.createIcon("/icons/zoom.png"));
			infoButton.setFocusTraversable(false);
			
			if (this.isAdvancedMode()) {
				infoButton.setOnMouseClicked(event2 -> {
					infoButton.requestFocus();
					if (textArea.getText().isEmpty())
						return;
	
					TextArea infoText = new TextArea(textArea.getText());
					infoText.setWrapText(true);
					infoText.setPrefColumnCount(30);
					infoText.setPrefRowCount(12);
					textArea.textProperty().bind(infoText.textProperty());
					PopOver info = new CustomPopOver(new VBox(infoText));
					info.setOnHidden(event-> textArea.textProperty().unbind());
					info.show(infoButton);
					infoText.setOnKeyPressed(event-> {if (event.getCode() == KeyCode.ESCAPE) info.hide();});
				});
			}
			else {
				infoButton.setOnAction(action -> {
					infoButton.requestFocus();
					lastSelectedTextArea = textArea;
					detailsArea.requestFocus();
				});
			}

			if (sqlTableRow != null && sqlTableView.getPrimaryKey() != null && sqlTableView.getPrimaryKey().contains(columnName)) {
				textArea.setEditable(false);
				textArea.setTooltip(new Tooltip("Primary key can't be edited"));
				label.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
			} else if (sqlTableRow != null && sqlTableView.getSqlTable().isForeignKey(columnName)) {
				textArea.setTooltip(new Tooltip("Foreign key"));
				label.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
			}
			HBox node = new HBox(label, textArea, infoButton);

			if (resizeable) {
				label.prefWidthProperty().bind(node.widthProperty().multiply(0.4));
				textArea.prefWidthProperty().bind(node.widthProperty().multiply(0.6));
			}
			else {
				label.setMaxWidth(Double.MAX_VALUE);
				HBox.setHgrow(label, Priority.ALWAYS);
			}

			centerBox.getChildren().add(node);
		}
		
		if (!this.isAdvancedMode()) {
			detailsArea.setPadding(new Insets(5));
			centerBox.getChildren().add(detailsArea);
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

	public HashMap<String, TextArea> getMap() {
		return fieldsMap;
	}

	public void clear() {
		for (TextArea textField : fieldsMap.values()) {
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

	public void setOnClose(Runnable closeAction) {
		this.closeAction = closeAction;
	}

	public void close() {
		closeAction.run();
	}
	
	public void put(String columnName, String value) {
		this.fieldsMap.get(columnName).setText(value);
	}

	public void copy() {
		StringSelection stringSelection = new StringSelection(sqlTableRow.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	public FlowPane getToolbar() {
		return toolbar;
	}

	public void setToolbar(FlowPane bar) {
		this.toolbar = bar;
		this.toolbar.setOrientation(Orientation.HORIZONTAL);
		this.toolbar.prefWidthProperty().bind(centerBox.widthProperty());
		this.setTop(this.toolbar);
	}

	public ScrollPane getScrollPane() {
		return scrollPane;
	}
	
	public void setActionButton(Button button) {
		this.actionButton = button;
		this.actionButton.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.TAB) {
				List<TextArea> fields = new ArrayList<>(fieldsMap.values());
				if (event.isShiftDown())
					fields.get(fields.size() - 1).requestFocus();
				else
					fields.get(0).requestFocus();
				
				event.consume();
			}
		});
		this.centerBox.getChildren().add(actionButton);
	}

	@Override
	public void onObservableChange(MapTableViewRow newValue) {
		for (String column : columns) {
			TextArea textField = fieldsMap.get(column);
			String newText = newValue.get(column) != null ? newValue.get(column).toString() : null;
			textField.setText(newText);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key : fieldsMap.keySet()) {
			sb.append(key).append(" : ").append(fieldsMap.get(key).getText()).append("\n");
		}
		return sb.toString();
	}
	
	private boolean isAdvancedMode() {
		return (System.getProperty("sqlbrowserfx.mode", "advanced").equals("advanced"));
	}

}
