package gr.sqlbrowserfx.nodes.sqlpane;

import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

public class SqlTableTab extends Tab {

	SqlTableView sqlTableView;
	SplitPane splitPane;
	TabPane recordsTabPane;
	Label label;
//	TODO store columns box

	public SqlTableTab() {
		super();
	}

	public SqlTableTab(String text, final SqlTableView sqlTableView) {
		super(text, sqlTableView);
		this.sqlTableView = sqlTableView;

		Node graphic = JavaFXUtils.icon("/icons/table-e.png");
		label = new Label(text, graphic);
		label.textProperty().bind(this.getSqlTableView().titleProperty());
		this.setText(null);
		this.setGraphic(label);
	}

	public SqlTableView getSqlTableView() {
		return sqlTableView;
	}

	public void setSqlTableView(final SqlTableView sqlTableView) {
		this.sqlTableView = sqlTableView;
	}

	public SplitPane getSplitPane() {
		return splitPane;
	}

	public void setSplitPane(final SplitPane splitPane) {
		this.splitPane = splitPane;
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
	
	public Node getCustomGraphic() {
		return label;
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
