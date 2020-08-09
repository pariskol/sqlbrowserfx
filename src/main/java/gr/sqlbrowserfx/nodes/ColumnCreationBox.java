package gr.sqlbrowserfx.nodes;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.MysqlConnector;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class ColumnCreationBox extends HBox {

	private ComboBox<String> typeComboBox;
	private ComboBox<String> columnsComboBox;
	private ComboBox<String> tablesComboBox;
	private TextField columnNameField;
	private CheckBox fkCheckBox;
	private CheckBox nnCheckBox;
	private CheckBox pkCheckBox;
	private CheckBox uCheckBox;
	private SqlConnector sqlConnector;
	
	public ColumnCreationBox(SqlConnector sqlConnector) {
		this.sqlConnector = sqlConnector;
		List<String> types = this.getTypes();
		typeComboBox = new ComboBox<>();
		typeComboBox.setItems(FXCollections.observableArrayList(types));
		typeComboBox.setPromptText("Column type...");
		
		List<String> tables = null;
		try {
			tables = sqlConnector.getTables();
		} catch (SQLException e) {
			LoggerFactory.getLogger(getClass().getName()).error(e.getMessage(), e);
		}
		columnsComboBox = new ComboBox<>();
		tablesComboBox = new ComboBox<>();
		tablesComboBox.setPromptText("Referenced table...");
		columnsComboBox.setPromptText("Referenced column...");
		tablesComboBox.setItems(FXCollections.observableArrayList(tables));
		tablesComboBox.setOnAction(actionEvent -> {
			sqlConnector.executeQueryRawAsync("select * from " + tablesComboBox.getSelectionModel().getSelectedItem() + " where 1=2",
					rset -> {
						ResultSetMetaData rsmd = rset.getMetaData();
						SqlTable sqlTable = new SqlTable(rsmd);
						Platform.runLater(() -> {
							columnsComboBox.getSelectionModel().clearSelection();
							columnsComboBox.setItems(FXCollections.observableArrayList(sqlTable.getColumns()));
						});	
					});
		});
		fkCheckBox = new CheckBox("FK");
		tablesComboBox.disableProperty().bind(fkCheckBox.selectedProperty().not());
		columnsComboBox.disableProperty().bind(fkCheckBox.selectedProperty().not());
		
		columnNameField = new TextField();
		columnNameField.setPromptText("Column name...");
		pkCheckBox = new CheckBox("PK");
		nnCheckBox = new CheckBox("NN");
		uCheckBox = new CheckBox("U");
		this.getChildren().addAll(columnNameField, typeComboBox, 
				pkCheckBox, fkCheckBox, tablesComboBox, columnsComboBox,  nnCheckBox, uCheckBox);
		this.setSpacing(10);
		this.setAlignment(Pos.BASELINE_LEFT);
	}
	
	private List<String> getTypes() {
		String category = "types";
		Logger logger = LoggerFactory.getLogger(getClass().getName());
		List<String> list = new ArrayList<>();
		try {
			final String dbType = determineDBType();
			SqlBrowserFXAppManager.getConfigSqlConnector()
								  .executeQuery("select name from autocomplete2 where category= ? and description = ? order by name", 
										  Arrays.asList(new String[]{category, dbType}), rset -> {
											try {
												HashMap<String, Object> dto = DTOMapper.map(rset);
												list.add((String)dto.get("name"));
											} catch (Exception e) {
												logger.error(e.getMessage(), e);
											}
			});
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		
		return list;
	}
	
	private String determineDBType() {
		String dbType = null;
		if (sqlConnector instanceof SqliteConnector)
			dbType = "sqlite";
		else if (sqlConnector instanceof MysqlConnector)
			dbType = "mysql";
		return dbType;
	}

	public String getColumnName() {
		return columnNameField.getText();
	}
	
	public String getColumnType() {
		return typeComboBox.getSelectionModel().getSelectedItem();
	}
	
	public Boolean isColumnPrimaryKey() {
		return pkCheckBox.isSelected();
	}
	
	public Boolean isColumnForeignKey() {
		return fkCheckBox.isSelected();
	}
	
	public String getReferencedTable() {
		return tablesComboBox.getSelectionModel().getSelectedItem();
	}
	
	public String getReferencedColumn() {
		return columnsComboBox.getSelectionModel().getSelectedItem();
	}
	
	public Boolean isNotNull() {
		return nnCheckBox.isSelected();
	}
	
	public Boolean isUnique() {
		return uCheckBox.isSelected();
	}
}
