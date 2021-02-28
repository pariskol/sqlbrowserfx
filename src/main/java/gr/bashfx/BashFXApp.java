package gr.bashfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import gr.bashfx.codeareas.bash.BashCodeArea;
import gr.sqlbrowserfx.nodes.ToolbarOwner;
import gr.sqlbrowserfx.nodes.codeareas.sql.FileSqlCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BashFXApp extends Application implements ToolbarOwner {
	
	public static void main(String[] args) {
		launch(args);
	}

	private TabPane tabPane;
	private TreeView<String> dirTreeView;

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("BashFX");
		
		Scene scene = new Scene(createBashFXAppBox(true), 1000, 600);
		scene.getStylesheets().add("/styles/flat-dark.css");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public BorderPane createBashFXAppBox(Boolean openDirView) {
		tabPane = new TabPane();
		new DraggingTabPaneSupport().addSupport(tabPane);
		FlowPane menu = createToolbar();
		BorderPane pane = new BorderPane();
		pane.setLeft(menu);
		
		if (openDirView) {
			dirTreeView = this.createTreeView();
			SplitPane splitPane = new SplitPane(dirTreeView, tabPane);
		    splitPane.setDividerPositions(0.3f, 0.7f);
			SplitPane.setResizableWithParent(dirTreeView, false);
			VBox.setVgrow(splitPane, Priority.ALWAYS);
			pane.setCenter(splitPane);
		}
		else
			pane.setCenter(tabPane);
		
		return pane;
	}
	
	private TreeView<String> createTreeView() {
    	TreeView<String> dirTreeView = new TreeView<>();
    	// Hides the root item of the tree view.
	    dirTreeView.setShowRoot(false);
	    dirTreeView.setOnMouseClicked(mouseEvent -> {
	    	if (mouseEvent.getClickCount() > 1) {
	    		if (dirTreeView.getSelectionModel().getSelectedItem() instanceof FileTreeItem) {
	    			FileTreeItem fileTreeItem = (FileTreeItem) dirTreeView.getSelectionModel().getSelectedItem(); 
	    			openInNewTab(tabPane, fileTreeItem.getFile());
	    		}
    		}
	    });
	return dirTreeView;
	}
	
	@Override
	public FlowPane createToolbar() {
		ContextMenu fileMenu = new ContextMenu();
		MenuItem open = new MenuItem("Open");
		fileMenu.getItems().add(open);
		open.setOnAction(actionEvent -> openFileAction(null, tabPane));
		MenuItem openFolderItem = new MenuItem("Open Folder");
		fileMenu.getItems().add(openFolderItem);
		openFolderItem.setOnAction(actionEvent -> openFolderAction(null));
		
		MenuItem newItem = new MenuItem("New");
		newItem.setOnAction(event -> {
			this.openInNewTab(tabPane);
		});
		fileMenu.getItems().add(newItem);
		MenuItem saveItem = new MenuItem("Save (Ctrl+S)");
		saveItem.setOnAction(actionEvent -> getSelectedCodeArea().saveFileAction());
		fileMenu.getItems().add(saveItem);
		MenuItem saveAsItem = new MenuItem("Save As...");
		saveAsItem.setOnAction(actionEvent -> {
			getSelectedCodeArea().saveAsFileAction();
			getSelectedTab().setText(getSelectedCodeArea().getFileName());
		});
		fileMenu.getItems().add(saveAsItem);

		ContextMenu editMenu = new ContextMenu();
		MenuItem editMenuItem = new MenuItem("Copy (Ctrl+C)");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().copy());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Cut (Ctrl+X)");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().cut());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Paste (Ctrl+V)");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().paste());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Undo (Ctrl+Z)");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().undo());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Redo (Ctrl+Shift+Z)");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().redo());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Find/Replace (Ctrl+F)");
		editMenu.getItems().add(editMenuItem);
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().showSearchAndReplacePopup());

		Button fileMenuButton = new Button("", JavaFXUtils.createIcon("/icons/menu.png"));
		fileMenuButton.setOnMouseClicked(event -> {
			fileMenu.show(fileMenuButton, event.getScreenX(), event.getScreenY());
		});
		Button editMenuButton = new Button("", JavaFXUtils.createIcon("/icons/menu-edit.png"));
		editMenuButton.setOnMouseClicked(event -> {
			editMenu.show(editMenuButton, event.getScreenX(), event.getScreenY());
		});
		
		FlowPane flowPane = new FlowPane(fileMenuButton, editMenuButton);
		flowPane.setOrientation(Orientation.VERTICAL);
		return flowPane;
	}

	
	@SuppressWarnings("unchecked")
	private BashCodeArea getSelectedCodeArea() {
		return (BashCodeArea)((VirtualizedScrollPane<CodeArea>) tabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
	}
	
	private Tab getSelectedTab() {
		return tabPane.getSelectionModel().getSelectedItem();
	}
	
	private void openFileAction(Stage primaryStage, TabPane tabPane) {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(primaryStage);
		openInNewTab(tabPane, selectedFile);
		
	}
	
	private void openFolderAction(Stage primaryStage) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		File selectedDir = dirChooser.showDialog(primaryStage);
		fillTreeView(selectedDir.getAbsolutePath());
	}

	private void openInNewTab(TabPane tabPane, File selectedFile) {
		CodeArea codeArea = null;
		if (selectedFile.getName().endsWith(".sql"))
			codeArea = new FileSqlCodeArea(selectedFile);
		else
			codeArea = new BashCodeArea(selectedFile.getAbsolutePath());
		VirtualizedScrollPane<CodeArea> vsp = new VirtualizedScrollPane<CodeArea>(codeArea);
		String fileContent = null;
		try {
			fileContent = StringUtils.join(
				Files.lines(Paths.get(selectedFile.getAbsolutePath()))
				 	 .collect(Collectors.toList()), "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		codeArea.replaceText(fileContent);
		
		Tab tab = new Tab(selectedFile.getName(),vsp);
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}
	
	private void openInNewTab(TabPane tabPane) {
		BashCodeArea bashCodeArea = new BashCodeArea();
		VirtualizedScrollPane<CodeArea> vsp = new VirtualizedScrollPane<CodeArea>(bashCodeArea);
		Tab tab = new Tab("new",vsp);
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

	private void createTree(File file, TreeItem<String> parent) {
	    if (file.isDirectory()) {
	        TreeItem<String> treeItem = new TreeItem<>(file.getName(), JavaFXUtils.createIcon("/icons/folder.png"));
	        parent.getChildren().add(treeItem);
	        
		    List<File> list = Arrays.asList(file.listFiles());
		    List<File> filesList = list.stream().filter(x -> x.isFile()).collect(Collectors.toList());
		    List<File> dirsList = list.stream().filter(x -> x.isDirectory()).collect(Collectors.toList());
		    
		    Collections.sort(filesList);
		    Collections.sort(dirsList);
		    
		    list = new ArrayList<>();
		    list.addAll(dirsList);
		    list.addAll(filesList);
	        for (File f : list) {
	            createTree(f, treeItem);
	        }
	    }
	    else {
	    	TreeItem<String> treeItem = new FileTreeItem(file);
	    	parent.getChildren().add(treeItem);
	    }
	}
	
	public TreeView<String> fillTreeView(String inputDirectoryLocation) {
		if (dirTreeView == null)
			return dirTreeView;
		
		// Creates the root item.
	    TreeItem<String> rootItem = new TreeItem<>(inputDirectoryLocation);

	    // Get a list of files.
	    if (inputDirectoryLocation != null) {
		    File fileInputDirectoryLocation = new File(inputDirectoryLocation);
		    File[] files = fileInputDirectoryLocation.listFiles();
	
		    List<File> list = Arrays.asList(files);
		    List<File> filesList = list.stream().filter(x -> x.isFile()).collect(Collectors.toList());
		    List<File> dirsList = list.stream().filter(x -> x.isDirectory()).collect(Collectors.toList());
		    
		    Collections.sort(filesList);
		    Collections.sort(dirsList);
		    
		    list = new ArrayList<>();
		    list.addAll(dirsList);
		    list.addAll(filesList);
		    // create tree
		    for (File file : list) {
		        createTree(file, rootItem);
		    }
	    }
	    dirTreeView.setRoot(rootItem);
	    return dirTreeView;
	}
	
}
