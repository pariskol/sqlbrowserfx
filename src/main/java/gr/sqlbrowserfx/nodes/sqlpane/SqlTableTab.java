package gr.sqlbrowserfx.nodes.sqlpane;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

public class SqlTableTab extends Tab implements ContextMenuOwner {

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

		var graphic = JavaFXUtils.createIcon("/icons/table-e.png");
		label = new Label(text, graphic);
		label.textProperty().bind(this.getSqlTableView().titleProperty());
		this.setText(null);
		this.setGraphic(label);
		this.setOnClosed(event -> {
			this.sqlTableView = null;
			System.gc();
		});
		this.setContextMenu(this.createContextMenu());
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
		var progressIndicator = new ProgressIndicator();
		progressIndicator.setMaxHeight(40);
		progressIndicator.setMaxWidth(40);
		this.setContent(new StackPane(progressIndicator));
	}
	
	public void load() {
		this.setContent(sqlTableView);
	}

	@Override
	public ContextMenu createContextMenu() {
		var showQuery = new MenuItem("Show Query", JavaFXUtils.createIcon("/icons/thunder.png"));
		showQuery.setOnAction(event -> {
			new CustomPopOver(new Label(this.sqlTableView.getQuery())).show(this.getGraphic());
		});
		var copyQuery = new MenuItem("Copy Query", JavaFXUtils.createIcon("/icons/copy.png"));
		copyQuery.setOnAction(event -> {
            var content = new ClipboardContent();
            content.putString(this.sqlTableView.getQuery());
            Clipboard.getSystemClipboard().setContent(content);
		});
		var closeTab = new MenuItem("Close Tab", JavaFXUtils.createIcon("/icons/minus.png"));
		closeTab.setOnAction(event -> {
            this.getTabPane().getTabs().remove(this);
		});
		return new ContextMenu(showQuery, copyQuery, closeTab);
	}

}
