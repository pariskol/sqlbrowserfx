package gr.sqlbrowserfx.nodes;


import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlConsolePane;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TableCreationPane extends BorderPane implements ToolbarOwner, SimpleObservable<String> {
	
	private FlowPane toolbar;
	private ListView<ColumnCreationBox> columnBoxesListView;
	private TextField tableNameField;
	private SqlConnector sqlConnector;
	private SqlCodeArea sqlCodeArea;
	private DSqlConsolePane sqlConsolePane;

	public TableCreationPane(SqlConnector sqlConnector) {
		toolbar = this.createToolbar();
		this.sqlConnector = sqlConnector;
		this.setLeft(toolbar);
		tableNameField = new TextField();
		tableNameField.setPromptText("Enter table name...");
		this.columnBoxesListView = new ListView<>();
		ColumnCreationBox columnCreationBox = new ColumnCreationBox(sqlConnector);
		columnBoxesListView.getItems().add(columnCreationBox);
		sqlConsolePane = new DSqlConsolePane(sqlConnector, null);
		sqlCodeArea = (SqlCodeArea) sqlConsolePane.getCodeAreaRef();
		SplitPane splitPane = new SplitPane(
				new VBox(tableNameField, columnBoxesListView),
				sqlCodeArea);
		splitPane.setOrientation(Orientation.VERTICAL);
		this.setCenter(splitPane);
		VBox.setVgrow(columnBoxesListView, Priority.ALWAYS);
	}
	
	@Override
	public FlowPane createToolbar() {
		Button addButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
		addButton.setOnMouseClicked(mouseEvent -> {
			ColumnCreationBox columnCreationBox = new ColumnCreationBox(sqlConnector);
			columnBoxesListView.getItems().add(columnCreationBox);
		});
		Button deleteButton = new Button("", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteButton.setOnAction(actionEvent -> {
			if (columnBoxesListView.getSelectionModel().getSelectedItem() != null)
				columnBoxesListView.getItems().remove(columnBoxesListView.getSelectionModel().getSelectedIndex());	
		});
		Button createQueryButton = new Button("", JavaFXUtils.createIcon("/icons/details.png"));
		createQueryButton.setOnAction(actionEvent -> {
			sqlCodeArea.clear();
			sqlCodeArea.appendText(createCreateQuery());
		});
		Button createTableButton = new Button("", JavaFXUtils.createIcon("/icons/check.png"));
		createTableButton.setOnAction(actionEvent -> {
			sqlConsolePane.executeButonAction();
		});
		FlowPane toolbar = new FlowPane(addButton, deleteButton, createQueryButton, createTableButton);
		toolbar.setPrefWidth(addButton.getWidth());
		return toolbar;
	}
	
	public String createCreateQuery() {
		String query = "CREATE TABLE " + tableNameField.getText() + "\n(\n";
		String foreignKeys = "";
		String primaryKey = "    PRIMARY KEY(";
		for (ColumnCreationBox cb : columnBoxesListView.getItems()) {
			query += "    " + cb.getColumnName() + " " + cb.getColumnType();
			
			if (cb.isNotNull())
				query += " NOT NULL";
			if (cb.isUnique())
				query += " UNIQUE";
			if (cb.isColumnPrimaryKey())
				primaryKey += cb.getColumnName() + ",";
			if (cb.isColumnForeignKey())
				foreignKeys += "    FOREIGN KEY(" + cb.getColumnName() + ") REFERENCES " + cb.getReferencedTable() + "(" + cb.getReferencedColumn() + "),\n";
			
			query += ",\n";
		}
		if (!primaryKey.equals("    PRIMARY KEY(")) {
			primaryKey = primaryKey.substring(0, primaryKey.length() - 1);
			primaryKey += ")";
			if (!foreignKeys.isEmpty())
				primaryKey += ",";
			primaryKey += "\n";
			query += primaryKey;
		}
		if (!foreignKeys.isEmpty()) {
			foreignKeys = foreignKeys.substring(0, foreignKeys.length() - ",\n".length());
			foreignKeys += "\n";
			query += foreignKeys;
		}
		query += ")";

		return query;
	}

	@Override
	public void changed() {
		sqlConsolePane.getListeners().forEach(listener -> listener.onObservaleChange(null));
	}

	@Override
	public void changed(String data) {
		sqlConsolePane.getListeners().forEach(listener -> listener.onObservaleChange(data));
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		sqlConsolePane.getListeners().add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		sqlConsolePane.getListeners().remove(listener);
	}
	
}
