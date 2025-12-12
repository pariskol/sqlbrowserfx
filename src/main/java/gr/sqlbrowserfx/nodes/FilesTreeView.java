package gr.sqlbrowserfx.nodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.factories.DialogFactory;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;

public class FilesTreeView extends TreeView<TreeViewFile> implements ContextMenuOwner, InputMapOwner {

	private final String rootPath;
	private TreeItem<TreeViewFile> rootItem, selectedRootItem;

	private final SimpleBooleanProperty isFileProperty = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty hasFilesToCopyProperty = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty isLeafMenuProperty = new SimpleBooleanProperty(false);


	private final TextField searchField;
	private final Label searResultsLabel;
	private Integer lastSelectedItemPos = 0;
	private final List<TreeItem<TreeViewFile>> searchResultsTreeItemsList = new ArrayList<>();
	private final Button nextSearchResultButton;
	private ListView<TreeViewFile> searchResultsListView;
	private CustomPopOver popOver;

	private List<File> filesToCopy = new ArrayList<>();

	public FilesTreeView(String rootPath) {
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
		        content.putFiles(List.of(selectedFile));
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
				openSelectedFile();
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

	private void openSelectedFile() {
		var file = this.getSelectionModel().getSelectedItem().getValue().asFile();

		if (file.isFile()) {
			var sqlConsolePane = SqlBrowserFXAppManager.getFirstActiveDSqlConsolePane();
			sqlConsolePane.openNewFileTab(file);
		}
	}

	private void copyFiles() {
		filesToCopy = this.getSelectionModel().getSelectedItems().stream().map(ti -> ti.getValue().asFile()).toList();
		if (!filesToCopy.isEmpty()) {
			this.hasFilesToCopyProperty.set(true);
		}
	}

	private void pasteFiles() {
		var treeItem = getSelectionModel().getSelectedItem();
		var selectedFile = treeItem.getValue().asFile();
		if (!selectedFile.isDirectory()) return;

		Executors.newSingleThreadExecutor().execute(() -> {
			filesToCopy.forEach(file -> {
				try {
					var path = Paths.get(file.getAbsolutePath());
					var newPath = Paths.get(selectedFile.getAbsolutePath(), file.getName());
					newPath = !Files.exists(newPath) ? newPath : Paths.get(selectedFile.getAbsolutePath(), "copy_" + file.getName());
					Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
					Platform.runLater(() -> refresh(treeItem));
				} catch (IOException e) {
					DialogFactory.createErrorNotification(e);
				}
			});
			// clear pending files to copy
			filesToCopy = null;
			this.hasFilesToCopyProperty.set(false);
		});
	}

	private void deleteSelectedFiles() {
		var treeItems = this.getSelectionModel().getSelectedItems();
		var fileNames = treeItems.stream().map(ti -> ti.getValue().getName()).toList();
		var doDelete = DialogFactory.createConfirmationDialog("Delete file", "Are you sure you want to delete the following files: \n" + StringUtils.join(fileNames, "\n"));
		if (doDelete) {
			treeItems.forEach(this::deleteFileTreeItem);
		}
	}
	
	private void deleteFileTreeItem(TreeItem<TreeViewFile> treeItem) {
		var file = treeItem.getValue().asFile();

		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				deleteFilesRecursively(file);
				// refresh its parent node
				Platform.runLater(() -> refresh(treeItem.getParent()));
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
	}
	
	private void deleteFilesRecursively(File file) throws IOException {
		var files = file.isDirectory()
				? Arrays.asList(file.listFiles(f -> !f.isHidden() && !f.getName().startsWith(".")))
				: Arrays.asList(file);

		for (var newFile : files) {
			if (newFile.isDirectory()) {
				deleteFilesRecursively(newFile);
			} else {
				Files.delete(Paths.get(newFile.getAbsolutePath()));
			}
		}
		
		// after deletion of files also delete parent directory
		if (file.isDirectory()) {
			Files.delete(Paths.get(file.getAbsolutePath()));
		}

	}

	private void createNewFile() {
		var treeItem = this.getSelectionModel().getSelectedItem();
		var file = treeItem.getValue().asFile();
		if (!file.isDirectory()) return;

		String newFileName = DialogFactory.createTextInputDialog("New file", "Enter new file name");
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				var newPath = Paths.get(file.getAbsolutePath(), newFileName);
				Files.createFile(newPath);
				Platform.runLater(() -> refresh(treeItem));
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
	}
	
	private void createNewDirectory() {
		var treeItem = this.getSelectionModel().getSelectedItem();
		var file = treeItem.getValue().asFile();
		if (!file.isDirectory()) return;

		String newFileName = DialogFactory.createTextInputDialog("New Folder", "Enter new folder name");
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				var newPath = Paths.get(file.getAbsolutePath(), newFileName);
				Files.createDirectory(newPath);
				Platform.runLater(() -> refresh(treeItem));
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
	}

	private void renameFile() {
		var treeItem = this.getSelectionModel().getSelectedItem();
		var file = treeItem.getValue().asFile();

		String newFileName = DialogFactory.createTextInputDialog("Rename file", "Enter new file name");
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				var path = Paths.get(file.getAbsolutePath());
				Files.move(path, path.resolveSibling(newFileName));
				// since is a file we need to refresh its parent node
				// which will be a directory
				Platform.runLater(() -> refresh(treeItem.getParent()));
			} catch (IOException e) {
				DialogFactory.createErrorDialog(e);
			}
		});
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
				var l = this.searchResultsTreeItemsList.stream().filter(ti -> ti.getValue().isFile()).map(TreeItem::getValue).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());
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

	
	private TreeItem<TreeViewFile> fillTreeView(String path) {
		var file = new TreeViewFile(path);
		var rootItem = new TreeItem<>(file, JavaFXUtils.createIcon("/icons/folder.png"));

		var files = Arrays.asList(file.listFiles(f -> !f.isHidden() && !f.getName().startsWith(".")));
		files.sort((a,b) -> {
			if (a.isDirectory() && b.isFile()) return - 1;
			if (a.isDirectory() && b.isDirectory()) return a.getName().compareTo(b.getName());
			if (a.isFile() && b.isFile()) return a.getName().compareTo(b.getName());
			return 1;
		});

		if (!files.isEmpty()) {
			for (var newFile : files) {
				if (newFile.isDirectory()) {
					var childItem = fillTreeView(newFile.getAbsolutePath());
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

	@Override
	public void refresh() {
		this.setRoot(this.fillTreeView(rootPath));
		rootItem = this.getRoot();
		rootItem.setExpanded(true);
	}

	private void refresh(TreeItem<TreeViewFile> treeItem) {
		var targetPath = treeItem.getValue().asFile().getAbsolutePath();
		treeItem.getChildren().clear();
		treeItem.getChildren().addAll(this.fillTreeView(targetPath).getChildren());
		treeItem.setExpanded(true);
		this.getSelectionModel().select(treeItem);
	}

	private void createSearPopOver() {
		var vb = new CustomVBox(
				new Label("Type and press enter"),
				new CustomHBox(searchField, nextSearchResultButton),
				searResultsLabel,
				searchResultsListView
			);

		popOver = new CustomPopOver(vb);
	}

	private void openSearchResultsListViewFile() {
		var file = searchResultsListView.getSelectionModel().getSelectedItem().asFile();

		if (file.isFile()) {
			var sqlConsolePane = SqlBrowserFXAppManager.getFirstActiveDSqlConsolePane();
			sqlConsolePane.openNewFileTab(file);
		}
	}

	private void createSearchResultsListView() {
		searchResultsListView = new ListView<>();
		searchResultsListView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				openSearchResultsListViewFile();
			}
		});
		searchResultsListView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				openSearchResultsListViewFile();
			}

			event.consume();
		});
	}

	public void showSearchPopup() {
		var boundsInScene = this.localToScreen(this.getBoundsInLocal());

		if (searchResultsListView == null) {
			createSearchResultsListView();
		}

		if (popOver == null) {
			createSearPopOver();
		}

		// show to parent node to resolve font rendering issue
		popOver.show(getParent(), boundsInScene.getMaxX(), boundsInScene.getMinY());
	}

	@Override
	public ContextMenu createContextMenu() {
		var contextMenu = new ContextMenu();

		var openFile = new MenuItem("Open", JavaFXUtils.createIcon("/icons/code-file.png"));
		openFile.disableProperty().bind(this.isFileProperty.not());
		openFile.setOnAction(event -> this.openSelectedFile());

		var newFile = new MenuItem("New File", JavaFXUtils.createIcon("/icons/add.png"));
		newFile.disableProperty().bind(this.isFileProperty);
		newFile.setOnAction(event -> this.createNewFile());
		var newDir = new MenuItem("New Folder", JavaFXUtils.createIcon("/icons/add.png"));
		newDir.disableProperty().bind(this.isFileProperty);
		newDir.setOnAction(event -> this.createNewDirectory());

		var copyFiles = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
		copyFiles.setOnAction(event -> this.copyFiles());

		var pasteFiles = new MenuItem("Paste", JavaFXUtils.createIcon("/icons/paste.png"));
		pasteFiles.setOnAction(event -> this.pasteFiles());
		pasteFiles.disableProperty().bind(this.isFileProperty.or(this.hasFilesToCopyProperty.not()));

		var deleteFile = new MenuItem("Delete", JavaFXUtils.createIcon("/icons/minus.png"));
		deleteFile.setOnAction(event -> this.deleteSelectedFiles());

		var renameFile = new MenuItem("Rename", JavaFXUtils.createIcon("/icons/edit.png"));
		renameFile.setOnAction(event -> this.renameFile());

		var search = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
		search.setOnAction(event -> this.showSearchPopup());

		var collapseAll = new MenuItem("Collapse All", JavaFXUtils.createIcon("/icons/collapse.png"));
		collapseAll.disableProperty().bind(this.isLeafMenuProperty.not());
		collapseAll.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() != null)
				this.collapseAll(this.getSelectionModel().getSelectedItem());
		});

		var refresh = new MenuItem("Refresh Folder", JavaFXUtils.createIcon("/icons/refresh.png"));
		refresh.setOnAction(event -> refresh(this.getSelectionModel().getSelectedItem()));
		refresh.disableProperty().bind(this.isFileProperty);

		var setAsRoot = new MenuItem("Select As Root", JavaFXUtils.createIcon("/icons/folder.png"));
		setAsRoot.disableProperty().bind(this.isFileProperty);
		setAsRoot.setOnAction(event -> {
			if (this.getSelectionModel().getSelectedItem() == null)
				return;

			this.selectedRootItem = this.getSelectionModel().getSelectedItem();
			this.setRoot(this.selectedRootItem);
			this.selectedRootItem.setExpanded(true);
		});
		setAsRoot.disableProperty().bind(this.getSelectionModel().selectedItemProperty().isNull().or(this.isFileProperty));

		var restoreRoot = new MenuItem("Restore Project Root", JavaFXUtils.createIcon("/icons/refresh.png"));
		restoreRoot.setOnAction(event -> this.setRoot(this.rootItem));
		restoreRoot.disableProperty().bind(this.getSelectionModel().selectedItemProperty().isEqualTo(this.rootItem));

		contextMenu.getItems().addAll(openFile, new SeparatorMenuItem(),
				newFile, newDir, renameFile, copyFiles, pasteFiles, deleteFile, new SeparatorMenuItem(),
				search, collapseAll, new SeparatorMenuItem(), setAsRoot, refresh, new SeparatorMenuItem(),
				restoreRoot);

		return contextMenu;
	}
}
