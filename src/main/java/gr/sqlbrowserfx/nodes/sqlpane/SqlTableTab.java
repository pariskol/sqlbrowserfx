package gr.sqlbrowserfx.nodes.sqlpane;

import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

public class SqlTableTab extends Tab {

	private SqlTableView sqlTableView;
	private TabPane recordsTabPane;
	private Label label;
//	TODO store columns box

	public SqlTableTab() {
		super();
	}

	public SqlTableTab(String text, final SqlTableView sqlTableView) {
		super(text, sqlTableView);
		this.sqlTableView = sqlTableView;

		Node graphic = JavaFXUtils.createIcon("/icons/table-e.png");
		label = new Label(text, graphic);
		label.textProperty().bind(this.getSqlTableView().titleProperty());
		this.setText(null);
		this.setGraphic(label);
		this.setOnClosed(event -> {
			this.sqlTableView = null;
			System.gc();
		});
	}

	public SqlTableView getSqlTableView() {
		return sqlTableView;
	}

	public TabPane getRecordsTabPane() {
		return recordsTabPane;
	}

	public void setRecordsTabPane(final TabPane tabPane) {
		this.recordsTabPane = tabPane;
	}
	
	public StringProperty customTextProperty() {
		return label.textProperty();
	}
	
	public String getCustomText() {
		return label.getText();
	}
	
	public void setCustomGraphic(Node graphic) {
		label.setGraphic(graphic);
	}
	
	public void startLoading() {
		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxHeight(40);
		progressIndicator.setMaxWidth(40);
		this.setContent(new StackPane(progressIndicator));
	}
	
	public void load() {
		this.setContent(sqlTableView);
	}

}
