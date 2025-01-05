package gr.sqlbrowserfx.nodes;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqliteConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TableCreationPane extends BorderPane implements ToolbarOwner, SimpleObservable<String> {
	
	private final FlowPane toolbar;
	private final ListView<ColumnCreationBox> columnBoxesListView;
	private final TextField tableNameField;
	private final SqlConnector sqlConnector;
	private final SqlCodeArea sqlCodeArea;
    protected List<SimpleObserver<String>> listeners;


	public TableCreationPane(SqlConnector sqlConnector) {
		toolbar = this.createToolbar();
		this.sqlConnector = sqlConnector;
		this.listeners = new ArrayList<>();
		this.setTop(toolbar);
		tableNameField = new TextField();
		tableNameField.setPromptText("Enter table name...");
		this.columnBoxesListView = new ListView<>();
		this.columnBoxesListView.setSelectionModel(new NoSelectionModel<>());
		ColumnCreationBox columnCreationBox = new ColumnCreationBox(sqlConnector, columnBoxesListView);
		columnBoxesListView.getItems().add(columnCreationBox);
		sqlCodeArea = new SqlCodeArea();
		SplitPane splitPane = new SplitPane(
				new VBox(tableNameField, columnBoxesListView),
				sqlCodeArea);
		splitPane.setOrientation(Orientation.VERTICAL);
		this.setCenter(splitPane);
		VBox.setVgrow(columnBoxesListView, Priority.ALWAYS);
		
		Button createTableButton = new Button("Execute", JavaFXUtils.createIcon("/icons/play.png"));
		createTableButton.setOnAction(actionEvent -> {
			try {
				sqlConnector.executeUpdate(sqlCodeArea.getText());
				listeners.forEach(listener -> listener.onObservableChange(null));
				DialogFactory.createNotification("Table created", "Table creation succeeded");
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
		createTableButton.setTooltip(new Tooltip("Run generated sql create statement"));
		
		this.setBottom(createTableButton);
	}
	
	@Override
	public FlowPane createToolbar() {
		Button addButton = new Button("Add Column", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnMouseClicked(mouseEvent -> {
			ColumnCreationBox columnCreationBox = new ColumnCreationBox(sqlConnector, columnBoxesListView);
			columnBoxesListView.getItems().add(columnCreationBox);
		});
		addButton.setTooltip(new Tooltip("Add new column"));
		Button createQueryButton = new Button("Generate Sql", JavaFXUtils.createIcon("/icons/details.png"));
		createQueryButton.setOnAction(actionEvent -> {
			sqlCodeArea.clear();
			sqlCodeArea.appendText(createCreateQuery());
		});
		createQueryButton.setTooltip(new Tooltip("Generate sql create statement"));

		FlowPane toolbar = new FlowPane(addButton, createQueryButton);
		toolbar.setPrefWidth(addButton.getWidth());
		return toolbar;
	}
	
	public String createCreateQuery() {
		StringBuilder query = new StringBuilder("CREATE TABLE " + tableNameField.getText() + "\n(\n");
		StringBuilder foreignKeys = new StringBuilder();
		StringBuilder primaryKey = new StringBuilder("    PRIMARY KEY(");
		for (ColumnCreationBox cb : columnBoxesListView.getItems()) {
			query.append("    ").append(cb.getColumnName()).append(" ").append(cb.getColumnType());

			if (cb.isNotNull())
				query.append(" NOT NULL");
			if (cb.isAutoIncrement() && sqlConnector instanceof SqliteConnector)
				query.append(" AUTOINCREMENT");
			if (cb.isAutoIncrement() && !(sqlConnector instanceof SqliteConnector))
				query.append(" AUTO_INCREMENT");
			if (cb.isUnique())
				query.append(" UNIQUE");
			if (cb.isColumnPrimaryKey())
				primaryKey.append(cb.getColumnName()).append(",");
			if (cb.isColumnForeignKey())
				foreignKeys.append("    FOREIGN KEY(").append(cb.getColumnName()).append(") REFERENCES ").append(cb.getReferencedTable()).append("(").append(cb.getReferencedColumn()).append("),\n");
			
			query.append(",\n");
		}
		if (!primaryKey.toString().equals("    PRIMARY KEY(")) {
			primaryKey = new StringBuilder(primaryKey.substring(0, primaryKey.length() - 1));
			primaryKey.append(")");
			if (!foreignKeys.isEmpty())
				primaryKey.append(",");
			primaryKey.append("\n");
			query.append(primaryKey);
		}
		if (!foreignKeys.isEmpty()) {
			foreignKeys = new StringBuilder(foreignKeys.substring(0, foreignKeys.length() - ",\n".length()));
			foreignKeys.append("\n");
			query.append(foreignKeys);
		}
		query.append(")");

		return query.toString();
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservableChange(null));
	}

	@Override
	public void changed(String data) {
		listeners.forEach(listener -> listener.onObservableChange(data));
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		listeners.remove(listener);
	}
	
}
