package gr.bashfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import gr.bashfx.codeareas.bash.BashCodeArea;
import gr.sqlbrowserfx.nodes.sqlpane.DraggingTabPaneSupport;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BashFXApp extends Application{
	
	public static void main(String[] args) {
//		BasicConfigurator.configure();
		launch(args);
	}

	private TabPane tabPane;
	private TreeView<String> dirTreeView;

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("BashFX");
		
		Scene scene = new Scene(createBashFXAppBox(primaryStage), 1000, 600);
		scene.getStylesheets().add("flat-blue.css");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public VBox createBashFXAppBox(Stage primaryStage) {
		tabPane = new TabPane();
		new DraggingTabPaneSupport().addSupport(tabPane);
		MenuBar menu = createMenuBar(primaryStage, tabPane);
		dirTreeView = fillTreeView(null);
		SplitPane splitPane = new SplitPane(dirTreeView, tabPane);
	    splitPane.setDividerPositions(0.3f, 0.7f);
		SplitPane.setResizableWithParent(dirTreeView, false);
		VBox.setVgrow(splitPane, Priority.ALWAYS);
		VBox vBox = new VBox(menu,splitPane);
		return vBox;
	}
	
	private MenuBar createMenuBar(Stage primaryStage, TabPane tabPane) {
		MenuBar menu = new MenuBar();
		Menu fileMenu = new Menu("File");
		MenuItem open = new MenuItem("Open");
		fileMenu.getItems().add(open);
		open.setOnAction(actionEvent -> openFileAction(primaryStage, tabPane));
		MenuItem openFolderItem = new MenuItem("Open Folder");
		fileMenu.getItems().add(openFolderItem);
		openFolderItem.setOnAction(actionEvent -> openFolderAction(primaryStage));
		
		MenuItem newItem = new MenuItem("New");
//		newItem.setOnAction(arg0);
		fileMenu.getItems().add(newItem);
		MenuItem saveItem = new MenuItem("Save Ctrl+S");
		saveItem.setOnAction(actionEvent -> getSelectedCodeArea().saveFileAction());
		fileMenu.getItems().add(saveItem);
		MenuItem saveAsItem = new MenuItem("Save As...");
		saveAsItem.setOnAction(actionEvent -> {
			
		});
		fileMenu.getItems().add(saveAsItem);

		Menu editMenu = new Menu("Edit");
		MenuItem editMenuItem = new MenuItem("Copy Ctrl+C");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().copy());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Cut Ctrl+X");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().cut());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Paste Ctrl+V");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().paste());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Undo Ctrl+Z");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().undo());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Redo Ctrl+Shift+Z");
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().redo());
		editMenu.getItems().add(editMenuItem);
		editMenuItem = new MenuItem("Find/Replace Ctrl+F");
		editMenu.getItems().add(editMenuItem);
		editMenuItem.setOnAction(actionEvent -> getSelectedCodeArea().showSearchAndReplacePopup());
		Menu helpMenu = new Menu("Help");
		
		menu.getMenus().addAll(fileMenu, editMenu, helpMenu);

		return menu;
	}

	
	@SuppressWarnings("unchecked")
	private BashCodeArea getSelectedCodeArea() {
		return (BashCodeArea)((VirtualizedScrollPane<CodeArea>) tabPane.getSelectionModel().getSelectedItem().getContent()).getContent();
	}
	
	private static void openFileAction(Stage primaryStage, TabPane tabPane) {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(primaryStage);
		openInNewTab(tabPane, selectedFile);
		
	}
	
	private void openFolderAction(Stage primaryStage) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		File selectedDir = dirChooser.showDialog(primaryStage);
		fillTreeView(selectedDir.getAbsolutePath());
	}

	private static void openInNewTab(TabPane tabPane, File selectedFile) {
		BashCodeArea bashCodeArea = new BashCodeArea(selectedFile.getAbsolutePath());
		VirtualizedScrollPane<CodeArea> vsp = new VirtualizedScrollPane<CodeArea>(bashCodeArea);
		String fileContent = null;
		try {
			fileContent = StringUtils.join(
				Files.lines(Paths.get(selectedFile.getAbsolutePath()))
				 	 .collect(Collectors.toList()), "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		bashCodeArea.replaceText(fileContent);
		Tab tab = new Tab(selectedFile.getName(),vsp);
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

	public static void createTree(File file, TreeItem<String> parent) {
	    if (file.isDirectory()) {
	        TreeItem<String> treeItem = new TreeItem<>(file.getName(), JavaFXUtils.icon("/res/folder.png"));
	        parent.getChildren().add(treeItem);
	        for (File f : file.listFiles()) {
	            createTree(f, treeItem);
	        }
	    }
	    else {
	    	TreeItem<String> treeItem = new FileTreeItem(file);
	    	parent.getChildren().add(treeItem);
	    }
	}
	
	public TreeView<String> fillTreeView(String inputDirectoryLocation) {
	    if (dirTreeView == null) {
	    	dirTreeView = new TreeView<>();
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
	    }
		// Creates the root item.
	    TreeItem<String> rootItem = new TreeItem<>(inputDirectoryLocation);

	    // Get a list of files.
	    if (inputDirectoryLocation != null) {
		    File fileInputDirectoryLocation = new File(inputDirectoryLocation);
		    File fileList[] = fileInputDirectoryLocation.listFiles();
	
		    // create tree
		    for (File file : fileList) {
		        createTree(file, rootItem);
		    }
	    }
	    dirTreeView.setRoot(rootItem);
	    return dirTreeView;
	}
	
}
