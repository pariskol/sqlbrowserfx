package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class FileSqlCodeArea extends CSqlCodeArea {

	private File file;
	
	public FileSqlCodeArea(File file) {
		super();
		this.file = file;
	}
	
    public String getPath() {
    	return file.getPath();
    }
    
    public String getFileName() {
    	return file.getName();
    }
    
	public void saveFileAction() {
		try {
			Files.write(Paths.get(this.getPath()), this.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		DialogFactory.createNotification("File saved", "File saved at " + new Date().toString());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		MenuItem menuItemSave = new MenuItem("Save", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSave.setOnAction(action -> this.saveFileAction());

		menu.getItems().addAll(menuItemSave);
		
		if (getRunAction() == null) {
			menu.getItems().removeAll(menuItemRun);
		}
		return menu;
	}
	
	@Override
	protected void setInputMap() {
		super.setInputMap();
		InputMap<Event> save = InputMap.consume(
				EventPattern.keyPressed(KeyCode.S, KeyCombination.CONTROL_DOWN),
				action -> this.saveFileAction()
        );
		Nodes.addInputMap(this, save);
	}
}
