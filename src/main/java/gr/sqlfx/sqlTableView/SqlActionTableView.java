package gr.sqlfx.sqlTableView;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqliteConnector;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

public class SqlActionTableView extends SqlTableView {
	List<String> columns;

	public SqlActionTableView(SqlConnector sqlConnector) {
		super();
		this.sqlConnector = sqlConnector;
		columns = new ArrayList<>();
	}

	public SqlActionTableView(SqliteConnector sqlConnector, ResultSet rs) throws SQLException {
		this(sqlConnector);
		this.setItems(rs);
	}

	@Override
	public synchronized void setItems(ResultSet rs) throws SQLException {

		this.clear();

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();

		for (int i = 1; i <= columnCount; i++) {
			columns.add(rsmd.getColumnName(i));
		}
		
		while (rs.next()) {
			Map<String, Object> entry = new HashMap<>();
			for (String columnLabel : columns) {
				entry.put(columnLabel, rs.getString(columnLabel));
			}

			rows.add(new SqlTableActionRow(this, entry));

		}

		super.setItems(rows);
		this.setStyle("-fx-selection-bar: #bae5ff;");

		if (sqlConnector != null) {
			TableColumn<SqlTableRow, HBox> btnColumn = new TableColumn<>("Actions");
			btnColumn.setStyle("-fx-alignment: CENTER;");
			btnColumn.setCellValueFactory(param -> {
				((SqlTableActionRow) param.getValue()).getButtonsProperty().getValue().prefWidthProperty()
						.bind(btnColumn.widthProperty());
				return ((SqlTableActionRow) param.getValue()).getButtonsProperty();
			});
			this.getColumns().add(btnColumn);
		}

		for (String column : columns) {
			TableColumn<SqlTableRow, Object> col = new TableColumn<>(column);
			col.setCellValueFactory(param -> {
				return param.getValue().getObjectProperty(column);
			});
			col.setCellFactory(callback -> {
				return new SqlTableViewEditCell(this);
			});
			this.getColumns().add(col);
		}

		this.autoResizedColumns(autoResize);
	}
}
