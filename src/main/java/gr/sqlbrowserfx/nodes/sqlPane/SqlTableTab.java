package gr.sqlbrowserfx.nodes.sqlPane;

import gr.sqlbrowserfx.nodes.sqlTableView.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class SqlTableTab extends Tab {

	SqlTableView sqlTableView;
	SplitPane splitPane;
	TabPane recordsTabPane;
	Label label;
//	TODO store columns box

	public SqlTableTab() {
		super();
	}

	public SqlTableTab(String text, SqlTableView sqlTableView) {
		super(text, sqlTableView);
		this.sqlTableView = sqlTableView;

		Node graphic = JavaFXUtils.icon("res/table-e.png");
		label = new Label(text, graphic);
		label.textProperty().bind(this.getSqlTableView().titleProperty());
//		this.textProperty().unbind();
		this.setText(null);
		this.setGraphic(label);
	}

	public SqlTableView getSqlTableView() {
		return sqlTableView;
	}

	public void setSqlTableView(SqlTableView sqlTableView) {
		this.sqlTableView = sqlTableView;
	}

	public SplitPane getSplitPane() {
		return splitPane;
	}

	public void setSplitPane(SplitPane splitPane) {
		this.splitPane = splitPane;
	}

	public TabPane getRecordsTabPane() {
		return recordsTabPane;
	}

	public void setRecordsTabPane(TabPane tabPane) {
		this.recordsTabPane = tabPane;
	}
	
	public StringProperty customTextProperty() {
		return label.textProperty();
	}
	
	public Node getCustomGraphic() {
		return label;
	}
	
	public String getCustomText() {
		return label.getText();
	}
	
	public void setCustomGraphic(Node graphic) {
		label.setGraphic(graphic);
	}

}
