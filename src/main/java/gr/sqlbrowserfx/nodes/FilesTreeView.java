package gr.sqlbrowserfx.nodes;

import java.util.List;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;

public class FilesTreeView extends TreeView<TreeViewFile> implements ContextMenuOwner {

	private String rootPath;

	public FilesTreeView(String rootPath) {
		this.rootPath = rootPath;
		this.refresh();
		this.setContextMenu(this.createContextMenu());

		// FIXME: drag and drop does not work from here to sql console pane
		this.setCellFactory(tv -> {
			var cell = new TreeCell<TreeViewFile>() {
				@Override
				protected void updateItem(TreeViewFile file, boolean empty) {
					super.updateItem(file, empty);
					if (file != null && !empty) {
						setText(file.toString());
					}
				}
			};
			cell.setOnDragDetected(event -> {
				var selectedItem = cell.getText();
				if (selectedItem != null) {
					var dragboard = cell.startDragAndDrop(TransferMode.ANY);
					var content = new ClipboardContent();
					content.putString(selectedItem);
					dragboard.setContent(content);
					dragboard.setDragView(cell.snapshot(null, null));
				}
				event.consume();
			});

			return cell;
		});
	}

	private TreeItem<TreeViewFile> createTreeView(String path) {
		var file = new TreeViewFile(path);
		var rootItem = new TreeItem<>(file);

		var files = file.listFiles();
		if (files != null) {
			for (var newFile : files) {
				if (newFile.isDirectory()) {
					var childItem = createTreeView(newFile.getAbsolutePath());
					rootItem.getChildren().add(childItem);
				} else {
					var childItem = new TreeItem<>(new TreeViewFile(newFile.getAbsolutePath()));
					rootItem.getChildren().add(childItem);
				}
			}
		}

		return rootItem;
	}

	private void collapseAll(TreeItem<TreeViewFile> treeItem) {
		treeItem.getChildren().forEach(this::collapseAll);
		treeItem.setExpanded(false);
	}

	public void refresh() {
		this.setRoot(this.createTreeView(rootPath));
		this.getRoot().setExpanded(true);
	}

	@Override
	public ContextMenu createContextMenu() {
		var contextMenu = new ContextMenu();

		var menuItemCollapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		menuItemCollapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		var menuItemRefresh = new MenuItem("Refresh", JavaFXUtils.createIcon("/icons/refresh.png"));
		menuItemRefresh.setOnAction(event -> refresh());

		contextMenu.getItems().addAll(menuItemCollapseAll, menuItemRefresh);

		return contextMenu;
	}
}
