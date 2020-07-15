package gr.bashfx;

import java.io.File;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.TreeItem;

public class FileTreeItem extends TreeItem<String> {

	File file;
	
	public FileTreeItem(File file) {
		super(file.getName());
		this.file = file;
		
		if (file.getName().contains(".sh"))
			this.setGraphic(JavaFXUtils.icon("/res/file-sh.png"));
		else if (file.getName().contains(".")) 
			this.setGraphic(JavaFXUtils.icon("/res/file-extension.png"));
		else 
			this.setGraphic(JavaFXUtils.icon("/res/file.png"));
	}

	public File getFile() {
		return file;
	}
	
}
