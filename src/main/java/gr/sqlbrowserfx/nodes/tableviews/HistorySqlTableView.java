package gr.sqlbrowserfx.nodes.tableviews;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class HistorySqlTableView extends SqlTableView implements ContextMenuOwner {

	public HistorySqlTableView(SqlConnector sqlConnector) {
		super(sqlConnector);
		this.setContextMenu(createContextMenu());
		this.enableColumnFiltering(true);
	}

	@Override
	public ContextMenu createContextMenu() {
		MenuItem menuItemDelete = new MenuItem("Delete", JavaFXUtils.createIcon("/icons/minus.png"));
		menuItemDelete.setOnAction(event -> {
			sqlConnector.executeAsync(() ->
				this.getSelectionModel().getSelectedItems().forEach(this::deleteRecord)
			);
		});

		MenuItem menuItemCopy = new MenuItem("Copy row", JavaFXUtils.createIcon("/icons/copy.png"));
		menuItemCopy.setOnAction(actionEvent -> {
			StringBuilder content = new StringBuilder();

			this.getSelectionModel().getSelectedItems().forEach(row -> content.append(row.toString()).append("\n"));

			StringSelection stringSelection = new StringSelection(content.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		});
		return new ContextMenu(menuItemDelete, menuItemCopy);
	}
}
