package gr.sqlbrowserfx.nodes.codeareas;

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
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class SimpleFileCodeArea extends AutoCompleteCodeArea<SimpleFileCodeAreaSyntaxProvider> implements FileCodeArea {

	private final File file;
	private String lastSavedContent;
	
	public SimpleFileCodeArea(File file) {
		super();
		this.file = file;
		try(var lines = Files.lines(Paths.get(file.getAbsolutePath()))) {
			lastSavedContent = StringUtils.join(lines.collect(Collectors.toList()), "\n");
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
    
	public void saveFileAction() {
		try {
			Files.write(Paths.get(this.getPath()), this.getText().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			this.lastSavedContent = this.getText();
			getUndoManager().forgetHistory();
		} catch (IOException e) {
			DialogFactory.createErrorDialog(e);
		}
		DialogFactory.createNotification("File saved", "File saved at " + new Date());
	}
	
	@Override
	public ContextMenu createContextMenu() {
		var menu = super.createContextMenu();
		var menuItemSave = new MenuItem("Save File", JavaFXUtils.createIcon("/icons/save.png"));
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

	@Override
	protected SimpleFileCodeAreaSyntaxProvider initSyntaxProvider() {
		return new SimpleFileCodeAreaSyntaxProvider();
	}
}
