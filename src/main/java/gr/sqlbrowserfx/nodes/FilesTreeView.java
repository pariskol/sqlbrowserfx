package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FilesTreeView extends TreeView<TreeViewFile> implements ContextMenuOwner, InputMapOwner {

	private String rootPath;
	private TreeItem<TreeViewFile> rootItem, selectedRootItem;
	
	private SimpleBooleanProperty isFileProperty = new SimpleBooleanProperty(false);
	private SimpleBooleanProperty isLeafMenuProperty = new SimpleBooleanProperty(false);

	
	private TextField searchField;
	private Label searResultsLabel;
	private Integer lastSelectedItemPos = 0;
	private List<TreeItem<TreeViewFile>> searchResultsTreeItemsList = new ArrayList<>();
	private Button nextSearchResultButton;
	private ListView<TreeViewFile> searchResultsListView;
	private CustomPopOver popOver;

	public FilesTreeView(String rootPath) {
		this.rootPath = rootPath;
		this.refresh();
		this.setContextMenu(this.createContextMenu());

		searResultsLabel = new Label("No results");
		searchField = new TextField();
		searchField.setPromptText("Search...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				this.searchFieldAction();
			}
		});
		nextSearchResultButton = new Button("", JavaFXUtils.createIcon("/icons/next.png"));
		nextSearchResultButton.setTooltip(new Tooltip("Go to next tree item"));
		nextSearchResultButton.setOnAction(event -> {
			if (searchResultsTreeItemsList.isEmpty())
				return;
			lastSelectedItemPos = lastSelectedItemPos == searchResultsTreeItemsList.size() - 1 ? 0 : ++lastSelectedItemPos;
			this.getSelectionModel().clearSelection();
			this.getSelectionModel().select(searchResultsTreeItemsList.get(lastSelectedItemPos));
			int row = this.getRow(searchResultsTreeItemsList.get(lastSelectedItemPos));
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
			if (popOver != null && popOver.isShowing()) {
				popOver.hide();
			}
			
			if (event.getClickCount() == 2) {
				var file = this.getSelectionModel().getSelectedItem().getValue().asFile();
				
				if (file.isFile()) {
					var sqlConsolePane = SqlBrowserFXAppManager.getFirstActiveDSqlConsolePane();
					sqlConsolePane.openNewFileTab(file);
				}
			}
			
			event.consume();
		});
		
		this.getSelectionModel().selectedItemProperty().addListener((ob, ov, nv) -> {
			if (nv == null) return;
			this.isFileProperty.set(nv.getValue().isFile());
			this.isLeafMenuProperty.set(!nv.isLeaf());
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
		this.searchResultsTreeItemsList.clear();
		this.getSelectionModel().clearSelection();

		Executors.newSingleThreadExecutor().execute(() -> {
			searchRootItem(this.getRoot());
			Platform.runLater(() -> {
				this.searResultsLabel.setText(this.searchResultsTreeItemsList.size() + " results");
				var l = this.searchResultsTreeItemsList.stream().filter(ti -> ti.getValue().isFile()).map(ti -> ti.getValue())
						.collect(Collectors.toList());
				l.sort((a, b) -> a.getName().compareTo(b.getName()));
				this.searchResultsListView.setItems(FXCollections.observableArrayList(l));
			});
		});
	}

	private void searchRootItem(TreeItem<TreeViewFile> rootItem) {
		if (rootItem == null)
			return;

		for (var treeItem : rootItem.getChildren()) {
			if (treeItem.getValue().isDirectory()) {
				searchRootItem(treeItem);
			}
			
			if (treeItem.getValue().getName().matches("(?i:.*" + searchField.getText() + ".*)")) {
				searchResultsTreeItemsList.add(treeItem);
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

	private void createSearPopOver() {
		var vb = new VBox(
				new Label("Type and press enter"),
				new HBox(searchField, nextSearchResultButton),
				searResultsLabel,
				searchResultsListView
			);
		
		popOver = new CustomPopOver(vb);
	}
	
	public void showSearchPopup() {
		var boundsInScene = this.localToScreen(this.getBoundsInLocal());

		if (searchResultsListView == null) {
			searchResultsListView = new ListView<TreeViewFile>();
			searchResultsListView.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2) {
					var file = searchResultsListView.getSelectionModel().getSelectedItem().asFile();

					if (file.isFile()) {
						var sqlConsolePane = SqlBrowserFXAppManager.getFirstActiveDSqlConsolePane();
						sqlConsolePane.openNewFileTab(file);
					}
				}

				event.consume();
			});
		}
		
		if (popOver == null) {
			createSearPopOver();
		}
		
		popOver.show(getParent(), boundsInScene.getMaxX(), boundsInScene.getMinY());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var contextMenu = new ContextMenu();

		var menuItemSearch = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		menuItemSearch.setOnAction(event -> this.showSearchPopup());
		var menuItemCollapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		menuItemCollapseAll.disableProperty().bind(this.isLeafMenuProperty.not());
		menuItemCollapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		var menuItemRefresh = new MenuItem("Refresh", JavaFXUtils.createIcon("/icons/refresh.png"));
		menuItemRefresh.setOnAction(event -> refresh());
		
		var menuItemNewRoot = new MenuItem("Select As Root", JavaFXUtils.createIcon("/icons/folder.png"));
		menuItemNewRoot.disableProperty().bind(this.isFileProperty);
		menuItemNewRoot.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() == null)
				return;
			
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
