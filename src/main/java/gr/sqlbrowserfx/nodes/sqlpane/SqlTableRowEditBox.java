package gr.sqlbrowserfx.nodes.sqlpane;

import java.awt.TextField;
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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SqlTableRowEditBox extends BorderPane implements SimpleObserver<MapTableViewRow> {

	private LinkedHashMap<String, TextArea> fieldsMap;
	private List<String> columns;
	private Runnable closeAction;
	private MapTableViewRow sqlTableRow;
	private VBox centerBox;
	private FlowPane toolbar;
	private ScrollPane scrollPane;
	private Label messageLabel;
	private Button actionButton;
	private TextArea detailsArea;
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
			label.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			label.setTooltip(new Tooltip(columnName));
			label.setAlignment(Pos.CENTER_RIGHT);
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
					int i = 0;
					for (i = 0; i < l.size(); i++) {
						if (l.get(i).equals(textArea))
							break;
					}
					if (event.isShiftDown() && i > 0)
						l.get(i - 1).requestFocus();
					else if (actionButton != null && (i == l.size() - 1 || (event.isShiftDown() && i == 0)))
						this.actionButton.requestFocus();
					else if (i < l.size() - 1)
						l.get(i + 1).requestFocus();
				}
				else if(event.getCode() == KeyCode.DOWN) {
					List<TextArea> l = new ArrayList<>(fieldsMap.values());
					int i = 0;
					for (i = 0; i < l.size(); i++) {
						if (l.get(i).equals(textArea))
							break;
					}
					if (i < l.size() - 1)
						l.get(i + 1).requestFocus();
				}
				else if(event.getCode() == KeyCode.UP) {
					List<TextArea> l = new ArrayList<>(fieldsMap.values());
					int i = 0;
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
			textArea.textProperty().addListener((obs, o, n) -> {
				detailsArea.setText(n);
			});
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
					PopOver info = new SqlPanePopOver(new VBox(infoText));
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
				textArea.setTooltip(new Tooltip("Primary key can't be edit"));
				label.setGraphic(JavaFXUtils.createIcon("/icons/primary-key.png"));
			} else if (sqlTableRow != null && sqlTableView.getSqlTable().isForeignKey(columnName)) {
				textArea.setTooltip(new Tooltip("Foreign key"));
				label.setGraphic(JavaFXUtils.createIcon("/icons/foreign-key.png"));
			}
			HBox node = new HBox(label, textArea, infoButton);

			if (resizeable) {
				label.prefWidthProperty().bind(node.widthProperty().multiply(0.4));
				textArea.prefWidthProperty().bind(node.widthProperty().multiply(0.6));
//				infoButton.prefWidthProperty().bind(node.widthProperty().multiply(0.1));
			}

			node.setAlignment(Pos.CENTER_RIGHT);
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

	public List<TextArea> getTextFields() {
		return new ArrayList<>(fieldsMap.values());
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
	
	public void updateMessageLabel(String text) {
		Platform.runLater(() -> messageLabel.setText(text));
		
	}

	
	public VBox getMainBox() {
		return centerBox;
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

	public void setScrollPane(ScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	@Override
	public void onObservaleChange(MapTableViewRow newValue) {
		for (String column : columns) {
			TextArea textField = fieldsMap.get(column);
			String newText = newValue.get(column) != null ? newValue.get(column).toString() : null;
			textField.setText(newText);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		for (String key : fieldsMap.keySet()) {
			sb.append(key + " : " + fieldsMap.get(key).getText() + "\n");
		}
		return sb.toString();
	}
	
	private boolean isAdvancedMode() {
		return (System.getProperty("sqlbrowserfx.mode", "advanced").equals("advanced"));
	}

}
