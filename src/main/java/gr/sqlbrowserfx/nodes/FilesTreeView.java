package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

public class FilesTreeView extends TreeView<TreeViewFile> implements ContextMenuOwner, InputMapOwner {

	private String rootPath;
	private TreeItem<TreeViewFile> rootItem, selectedRootItem;
	
	private TextField searchField;
	private Integer lastSelectedItemPos = 0;
	private List<TreeItem<TreeViewFile>> searchResultsList = new ArrayList<>();
	private Button nextSearchResultButton;

	public FilesTreeView(String rootPath) {
		this.rootPath = rootPath;
		this.refresh();
		this.setContextMenu(this.createContextMenu());

		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.searchFieldAction();
			}
		});
		nextSearchResultButton = new Button("", JavaFXUtils.createIcon("/icons/next.png"));
		nextSearchResultButton.setOnAction(event -> {
			if (searchResultsList.isEmpty())
				return;
			lastSelectedItemPos = lastSelectedItemPos == searchResultsList.size() - 1 ? 0 : ++lastSelectedItemPos;
			this.getSelectionModel().clearSelection();
			this.getSelectionModel().select(searchResultsList.get(lastSelectedItemPos));
			int row = this.getRow(searchResultsList.get(lastSelectedItemPos));
			this.scrollTo(row);
		});
		
		this.setOnDragDetected(event -> {
			var selectedItem = this.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				var selectedFile = selectedItem.getValue().asFile();
				
				if (selectedFile.isDirectory()) return;
				
				var dragboard = this.startDragAndDrop(TransferMode.COPY_OR_MOVE);
				ClipboardContent content = new ClipboardContent();
		        content.putFiles(Arrays.asList(selectedFile));
		        dragboard.setContent(content);
				dragboard.setDragView(selectedItem.getGraphic().snapshot(null, null));
			}
			event.consume();
		});
		
		this.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				var file = this.getSelectionModel().getSelectedItem().getValue().asFile();
				
				if (file.isFile()) {
					var sqlConsolePane = SqlBrowserFXAppManager.getFirstActiveDSqlConsolePane();
					sqlConsolePane.openNewFileTab(file);
				}
			}
			
			event.consume();
		});
		
		this.setInputMap();
	}

	@Override
	public void setInputMap() {
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
				action -> this.showSearchPopup()));
	}
	
	private void searchFieldAction() {
		this.lastSelectedItemPos = -1;
		this.searchResultsList.clear();
		this.getSelectionModel().clearSelection();

		searchRootItem(this.getRoot());
	}

	private void searchRootItem(TreeItem<TreeViewFile> rootItem) {
		if (rootItem == null)
			return;

		for (var t : rootItem.getChildren()) {
			if (t.getValue().getName().matches("(?i:.*" + searchField.getText() + ".*)")) {
				this.getSelectionModel().select(t);
				searchResultsList.add(t);
			}
		}
	}
	
	private String determineIcon(String fileName) {
		var url = "/icons/";
		if (fileName.endsWith(".java")) {
			url += "green.png";
		}
		else if (fileName.endsWith(".sql")) {
			url += "blue.png";
		}
		else {
			url += "red.png";
		}
		
		return url;
	}
	
	private TreeItem<TreeViewFile> createTreeView(String path) {
		var file = new TreeViewFile(path);
		var rootItem = new TreeItem<>(file);

		var files = Arrays.asList(file.listFiles(f -> !f.isHidden() && !f.getName().startsWith(".")));
		files.sort((a,b) -> {
			if (a.isDirectory() && b.isFile()) return - 1;
			if (a.isDirectory() && b.isDirectory()) a.getName().compareTo(b.getName());
			if (a.isFile() && b.isFile()) return a.getName().compareTo(b.getName());
			return 1;
		});
		
		if (files != null) {
			for (var newFile : files) {
				if (newFile.isDirectory()) {
					var childItem = createTreeView(newFile.getAbsolutePath());
					childItem.setGraphic(JavaFXUtils.createIcon("/icons/folder.png"));
					rootItem.getChildren().add(childItem);
				}
				else {
					var childItem = new TreeItem<>(new TreeViewFile(newFile.getAbsolutePath()));
					childItem.setGraphic(JavaFXUtils.createIcon(this.determineIcon(newFile.getName())));
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
		rootItem = this.getRoot();
		rootItem.setExpanded(true);
	}

	public void showSearchPopup() {
		Bounds boundsInScene = this.localToScreen(this.getBoundsInLocal());
		CustomPopOver popOver = new CustomPopOver(new HBox(searchField, nextSearchResultButton));
		popOver.show(this, boundsInScene.getMaxX() - 300, boundsInScene.getMinY());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var contextMenu = new ContextMenu();

		var menuItemSearch = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearch.setOnAction(event -> this.showSearchPopup());
		var menuItemCollapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		menuItemCollapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		var menuItemRefresh = new MenuItem("Refresh", JavaFXUtils.createIcon("/icons/refresh.png"));
		menuItemRefresh.setOnAction(event -> refresh());
		
		var menuItemNewRoot = new MenuItem("Select As Root", JavaFXUtils.createIcon("/icons/folder.png"));
		menuItemNewRoot.setOnAction(event -> {
			this.selectedRootItem = this.getSelectionModel().getSelectedItem();
			this.setRoot(this.selectedRootItem);
			this.selectedRootItem.setExpanded(true);
		});
		
		var menuItemRestoreRoot = new MenuItem("Restore Project Root", JavaFXUtils.createIcon("/icons/refresh.png"));
		menuItemRestoreRoot.setOnAction(event -> this.setRoot(this.rootItem));

		contextMenu.getItems().addAll(menuItemSearch, menuItemCollapseAll, new SeparatorMenuItem(), menuItemNewRoot, menuItemRestoreRoot, new SeparatorMenuItem(), menuItemRefresh);

		return contextMenu;
	}
}
