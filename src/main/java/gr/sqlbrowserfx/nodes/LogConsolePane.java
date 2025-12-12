package gr.sqlbrowserfx.nodes;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.controlsfx.control.PopOver;
import org.fxmisc.flowless.VirtualizedScrollPane;

import gr.sqlbrowserfx.nodes.codeareas.log.CodeAreaTailerListener;
import gr.sqlbrowserfx.nodes.codeareas.log.LogCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class LogConsolePane extends BorderPane implements ToolbarOwner {
	private boolean popOverIsShowing = false;

	private final CheckBox wrapTextCheckBox;
	private final CheckBox showLinesCheckBox;
	private final CheckBox followCarretCheckBox;
	private Button settingsButton;
	private final LogCodeArea logCodeArea;

	private Thread tailerDaemon;
	private Tailer tailer;

	public LogConsolePane() {
		logCodeArea = new LogCodeArea();
		wrapTextCheckBox = new CheckBox("Wrap text");
		showLinesCheckBox = new CheckBox("Show line number");
		followCarretCheckBox = new CheckBox("Scroll lock");
		
		logCodeArea.wrapTextProperty().bind(this.wrapTextCheckBox.selectedProperty());
		logCodeArea.showLinesProperty().bind(this.showLinesCheckBox.selectedProperty());
		logCodeArea.followCarretProperty().bind(this.followCarretCheckBox.selectedProperty());

		wrapTextCheckBox.setSelected(true);
		showLinesCheckBox.setSelected(true);
		followCarretCheckBox.setSelected(true);
		
		this.setTop(this.createToolbar());
		this.setCenter(new VirtualizedScrollPane<>(logCodeArea));
		
		this.startTailing();
	}
	
	protected void startTailing() {
		TailerListener listener = new CodeAreaTailerListener(logCodeArea);
	    tailer = new Tailer(new File("./logs/sqlbrowserfx.log"), listener, 1000);
	    tailerDaemon = new Thread(tailer, "Logfile Tailer Daemon");
	    tailerDaemon.setDaemon(true);
	    tailerDaemon.start();
	}
	
	protected void stopTailing() {
		if (tailer != null) {
			tailer.stop();
			tailerDaemon.interrupt();
		}
	}

	@Override
	public FlowPane createToolbar() {
		settingsButton = new Button("", JavaFXUtils.createIcon("/icons/settings.png"));
		settingsButton.setOnMouseClicked(mouseEvent -> {
			if (!popOverIsShowing) {
				popOverIsShowing = true;
				PopOver popOver = new PopOver(new CustomVBox(wrapTextCheckBox, showLinesCheckBox, followCarretCheckBox));
				popOver.setOnHidden(event -> popOverIsShowing = false);
				popOver.show(settingsButton);
			}
		});
		settingsButton.setTooltip(new Tooltip("Adjust settings"));
		FlowPane toolbar = new CustomFlowPane(settingsButton);
		return toolbar;
	}

}
