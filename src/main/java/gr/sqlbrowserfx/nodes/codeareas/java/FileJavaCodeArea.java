package gr.sqlbrowserfx.nodes.codeareas.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.nodes.codeareas.FileCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class FileJavaCodeArea extends JavaCodeArea implements FileCodeArea {

	private File file;
	private String lastSavedContent;
	
	public FileJavaCodeArea(File file) {
		super();
		this.file = file;
		try {
			lastSavedContent = StringUtils.join(
				Files.lines(Paths.get(file.getAbsolutePath()))
				 	 .collect(Collectors.toList()), "\n");
		} catch (IOException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("Could not load file " + file.getName(), e);
		}
		this.replaceText(lastSavedContent);
		getUndoManager().forgetHistory();
	}
	
	public boolean isTextDirty() {
		return !this.getText().equals(this.lastSavedContent);
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
			this.lastSavedContent = this.getText();
			getUndoManager().forgetHistory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		DialogFactory.createNotification("File saved", "File saved at " + new Date().toString());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		MenuItem menuItemSave = new MenuItem("Save File", JavaFXUtils.createIcon("/icons/save.png"));
		menuItemSave.setOnAction(action -> this.saveFileAction());

		menu.getItems().addAll(new SeparatorMenuItem(), menuItemSave);
		return menu;
	}
	
	@Override
	public void setInputMap() {
		super.setInputMap();
		InputMap<Event> save = InputMap.consume(
				EventPattern.keyPressed(KeyCode.S, KeyCombination.CONTROL_DOWN),
				action -> this.saveFileAction()
        );
		Nodes.addInputMap(this, save);
	}
}
