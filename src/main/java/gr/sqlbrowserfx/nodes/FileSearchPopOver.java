package gr.sqlbrowserfx.nodes;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import gr.sqlbrowserfx.nodes.sqlpane.CustomPopOver;
import gr.sqlbrowserfx.utils.FilesUtils;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.PropertiesLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class FileSearchPopOver extends CustomPopOver {

	@FunctionalInterface
	public interface Action {
		public void run(File selectedFile);
	}

	private Action action;
	private ScheduledExecutorService executor;
	private TextField searchField;
	private ListView<String> filesListView;
	private String rootPath = ((String) PropertiesLoader.getProperty("sqlbrowserfx.root.path", String.class, "~/"))
			.replaceAll("\"", "");

	public FileSearchPopOver(Action action) {
		super();

		this.action = action;
		var openButton = new Button("", JavaFXUtils.createIcon("/icons/code-file.png"));
		openButton.setOnMouseClicked(mouseEvent -> this.openFileAction());
		openButton.setTooltip(new Tooltip("Open file"));
		
		filesListView = new ListView<String>();
		filesListView.setCellFactory(param -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					// other stuff to do...

				} else {

					// set the width's
					setMinWidth(param.getWidth());
					setMaxWidth(param.getWidth());
					setPrefWidth(param.getWidth());

					// allow wrapping
					setWrapText(true);

					setText(item.toString());

				}
			}
		});
		filesListView.setPrefSize(600, 400);

		searchField = new TextField();
//		searchField.setPrefWidth(576);
		searchField.setPromptText("Search for file...");
		searchField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				search();
			}
			
			if (keyEvent.getCode() != KeyCode.ESCAPE) {
				keyEvent.consume();
			}
		});

		// TODO: add open button if has any value
		this.setContentNode(new VBox(new Label("File Search"), searchField, filesListView));
		this.setOnHidden(event -> {
			if (executor != null) {
				executor.shutdownNow();
			}
		});

		this.setOnShown(event -> searchField.requestFocus());
		this.setHideOnEscape(true);
		
		filesListView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				var filePath = filesListView.getSelectionModel().getSelectedItem();
				action.run(new File(filePath));
			} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				this.hide();
			}
		});
		filesListView.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
				var filePath = filesListView.getSelectionModel().getSelectedItem();
				action.run(new File(filePath));
			}
		});
	}

	private void openFileAction() {
		var fileChooser = new FileChooser();
		var selectedFile = fileChooser.showOpenDialog(null);
		action.run(selectedFile);
	}
	
	private void search() {
		searchField.setDisable(true);
		filesListView.setDisable(true);
		
		var pattern = searchField.getText();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.schedule(() -> {
			var filesPathsFound = FilesUtils.walk(rootPath, pattern);
			Platform.runLater(() -> {
				filesListView.setItems(FXCollections.observableArrayList(filesPathsFound));
				searchField.setDisable(false);
				filesListView.setDisable(false);
			});
		}, 0, TimeUnit.SECONDS);
	}
}
