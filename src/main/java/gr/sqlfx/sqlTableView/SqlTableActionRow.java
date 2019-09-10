package gr.sqlfx.sqlTableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.controlsfx.control.PopOver;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.factories.DialogFactory;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

@SuppressWarnings("unused")
public class SqlTableActionRow extends SqlTableRow{

	private String primaryKey;
	private ObservableList<SqlTableRow> rows; //reference
	private String tableName;
	private final SqlConnector sqlConnector;
	private Button editButton;
	private Button deleteButton;
	
	private HBox btnBox;
	private ToolBar toolbar;
	private ButtonsProperty buttonsProperty;

	public SqlTableActionRow(SqlTableView parentTable, Map<String, Object> entry) {
		super(entry);
		this.rows = parentTable.getSqlTableRows();
//		this.tableName = parentTable.getTableName();
		this.sqlConnector = parentTable.getSqlConnector();
//		this.primaryKey = parentTable.getPrimaryKey();		
		editButton = new Button("",new ImageView(new Image("/res/edit.png")));
		deleteButton = new Button("",new ImageView(new Image("/res/close.png")));
		
		toolbar = new ToolBar(editButton,deleteButton);
		toolbar.setBackground(Background.EMPTY);
		toolbar.setPadding(Insets.EMPTY);
		btnBox = new HBox(toolbar);
		btnBox.setAlignment(Pos.CENTER);
		
		editButton.setOnMouseClicked(event -> {
			editButton.requestFocus();
			VBox editBox = new VBox();
			editBox.setPadding(new Insets(5,20,5,5));
			for (String column: columns) {
				Label label = new Label(column);
				TextField textField = new TextField();
				textField.setAlignment(Pos.CENTER);
				
				if (propertiesMap.get(column).getValue() != null) textField.setText(propertiesMap.get(column).getValue().toString());
				else textField.setText("");
				
				Button infoButton = new Button("", new ImageView(new Image("/res/zoom.png")));
				infoButton.setFocusTraversable(false);
				infoButton.setOnMouseClicked(event2 -> {
					if (textField.getText().isEmpty())
						return;
					
					Text infoText = new Text(textField.getText());
					if (infoText.getText().length() > 200)
						infoText.setWrappingWidth(200);
					PopOver info = new PopOver(infoText);
//					info.setArrowSize(0);
					info.show(infoButton);
				});
				
				if (column.equals(primaryKey)) {
					textField.setEditable(false);
					textField.setTooltip(new Tooltip("Primary key can't be edit"));
				}
				
				HBox node = new HBox(label,textField,infoButton);
				node.setAlignment(Pos.CENTER_RIGHT);;
				editBox.getChildren().add(node);

			}
			
			ScrollPane scrollPane = new ScrollPane(editBox);
			scrollPane.setFocusTraversable(false);
			scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
			
			PopOver popOver = new PopOver(scrollPane);
			popOver.setPrefHeight(50);
			
			if (primaryKey != null) {
				Button submitBtn = new Button("Ok", new ImageView(new Image("/res/check.png")));
				submitBtn.setOnAction(submitEvent -> {
					for (int i=0; i< editBox.getChildren().size();i++) {
						if (editBox.getChildren().get(i) instanceof HBox) {
							HBox node = (HBox) editBox.getChildren().get(i);
							Label label = (Label) node.getChildren().get(0);
							TextField value = (TextField) node.getChildren().get(1);
		
								if (value != null) {
									propertiesMap.get(label.getText())
										.setValue(value.getText());
							}
						}
					}
					
					String sqlQuery = "update " + tableName + " set ";
					List<Object> params = new ArrayList<>();
					for (String column : columns) {
						if (!column.equals(primaryKey)) {
							sqlQuery += column + "= ? ,";
							params.add(propertiesMap.get(column).getValue());
						}
					}
					sqlQuery = sqlQuery.substring(0,sqlQuery.length()-1);
					sqlQuery += " where " + primaryKey + "=\"" + propertiesMap.get(primaryKey).getValue() + "\""; 
					System.out.println(sqlQuery);
					final String query = sqlQuery;
					new Thread(() -> { 
						try {
							sqlConnector.executeUpdate(query,params);
						} catch (Exception e) {
							DialogFactory.createErrorDialog(e);
						}
					}).start();
					popOver.hide();
				});
				submitBtn.setOnKeyPressed(keyEvent -> {
					if (keyEvent.getCode() == KeyCode.ENTER) {
						submitBtn.getOnAction().handle(new ActionEvent());
					}
				});
				
				
				editBox.getChildren().add(submitBtn);
			}
			
//			popOver.setArrowSize(0);;
			popOver.setDetachable(false);
			popOver.show(editButton);
			//submitBtn.requestFocus();
		});
		//remove this person object from list
		deleteButton.setOnAction(event -> {
			this.rows.remove(this);
			String sqlQuery = "delete from " + tableName + " where ";
			List<Object> params = new ArrayList<>();
			for (String column : columns) {
				sqlQuery += column + "= ? and ";
				params.add(propertiesMap.get(column).getValue());
			}
			sqlQuery = sqlQuery.substring(0,sqlQuery.length()-5);
			System.out.println(sqlQuery);
			final String query = sqlQuery;
			new Thread(() -> {
				try {
					sqlConnector.executeUpdate(query,params);
				} catch (Exception e) {
					DialogFactory.createErrorDialog(e);
				}
			}).start();
			});
		
		buttonsProperty = new ButtonsProperty(btnBox);
	}

	public ButtonsProperty getButtonsProperty() {
		return buttonsProperty;
	}

	
}
