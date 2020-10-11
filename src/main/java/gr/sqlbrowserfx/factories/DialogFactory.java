package gr.sqlbrowserfx.factories;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class DialogFactory {

	private static String DEFAULT_STYLESHEET;
	private static final boolean ENABLE_JMETRO = System.getProperty("sqlbrowsefx.jmetro.theme") != null;
	private static final String JMETRO = System.getProperty("sqlbrowsefx.jmetro.theme");
	
	public static void createErrorDialog(Throwable e) {
		createErrorDialog(e, null);
	}
	
	public static void createErrorDialog(Throwable e, String stylesheet) {

		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR);
//			alert.setTitle("SQL Exception");
			alert.setHeaderText(e.getClass().getSimpleName());
			alert.setContentText(e.getMessage());

			// Create expandable Exception.
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String exceptionText = sw.toString();

			Label label = new Label("The exception stacktrace was:");

			TextArea textArea = new TextArea(exceptionText);
			textArea.setEditable(false);
			textArea.setWrapText(true);

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			VBox.setVgrow(textArea, Priority.ALWAYS);
			VBox expContent = new VBox(label,textArea);

			LoggerFactory.getLogger("sqlbrowserfx").error(e.getMessage(), e);
			alert.getDialogPane().setExpandableContent(expContent);
			if (stylesheet != null)
				alert.getDialogPane().getStylesheets().add(stylesheet);
			else if (DEFAULT_STYLESHEET != null)
				alert.getDialogPane().getStylesheets().add(DEFAULT_STYLESHEET);
			
			if (ENABLE_JMETRO && JMETRO.equals("dark"))
				new JMetro(Style.DARK).setParent(alert.getDialogPane());	
			else if (ENABLE_JMETRO && JMETRO.equals("light"))
				new JMetro(Style.LIGHT).setParent(alert.getDialogPane());
			
			alert.showAndWait();
		});
	}
	
	public static int createConfirmationDialog(String title, String message) {
		return createConfirmationDialog(title, message, null);
	}
	
	public static int createConfirmationDialog(String title, String message, String stylesheet) {
		AtomicInteger result = new AtomicInteger(0);
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		if (stylesheet != null)
			alert.getDialogPane().getStylesheets().add(stylesheet);
		else if (DEFAULT_STYLESHEET != null)
			alert.getDialogPane().getStylesheets().add(DEFAULT_STYLESHEET);
		
		if (ENABLE_JMETRO && JMETRO.equals("dark"))
			new JMetro(Style.DARK).setParent(alert.getDialogPane());	
		else if (ENABLE_JMETRO && JMETRO.equals("light"))
			new JMetro(Style.LIGHT).setParent(alert.getDialogPane());
		
		Optional<ButtonType> res = alert.showAndWait();
		if (res.get() == ButtonType.OK){
		    result.set(1);
		} else {
		    result.set(0);
		}
		
		return result.get();
	}
	
	public static void createInfoDialog(String title, String message) {
		createInfoDialog(title, message, null);
	}
	
	public static void createInfoDialog(String title, String message, String stylesheet) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(message);
			if (stylesheet != null)
				alert.getDialogPane().getStylesheets().add(stylesheet);
			else if (DEFAULT_STYLESHEET != null)
				alert.getDialogPane().getStylesheets().add(DEFAULT_STYLESHEET);
			
			if (ENABLE_JMETRO && JMETRO.equals("dark"))
				new JMetro(Style.DARK).setParent(alert.getDialogPane());	
			else if (ENABLE_JMETRO && JMETRO.equals("light"))
				new JMetro(Style.LIGHT).setParent(alert.getDialogPane());

			LoggerFactory.getLogger("sqlbrowserfx").info(message);
			alert.showAndWait();
		});
	}
	
	public static String createTextInputDialog(String title, String header, String promt) {
		TextInputDialog dialog = new TextInputDialog(promt);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText("Please enter :");

		// Traditional way to get the response value.
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()){
		   return result.get();
		}
		
		return null;
	}
	
	public static int createDeleteDialog(Node owner, ObservableList<MapTableViewRow> rows, String message) {
		return createDeleteDialog(owner, rows, message, null);
	}
	
	public static int createDeleteDialog(Node owner, ObservableList<MapTableViewRow> rows, String message, String stylesheet) {
		SqlTableView sqlTableView = new SqlTableView();
		sqlTableView.createColumns(rows.get(0).getColumns());
		sqlTableView.setItems(rows);
		
		return createDialogWithContent(owner, sqlTableView, message, stylesheet);
	}
	
	public static int createDialogWithContent(Node owner, Node content, String message, String stylesheet) {

        final Stage dialog = new Stage();
        dialog.setTitle("Confirmation");
        Button yes = new Button("Yes", JavaFXUtils.createIcon("/icons/yes.png"));
        Button no = new Button("No", JavaFXUtils.createIcon("/icons/no.png"));

        Label displayLabel = new Label(message);
        displayLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

        dialog.initModality(Modality.NONE);
        dialog.initOwner((Stage) owner.getScene().getWindow());

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.BASELINE_RIGHT);

        VBox dialogVbox = new VBox();
        dialogVbox.setAlignment(Pos.CENTER);


        buttonBox.getChildren().addAll(yes,no);
        dialogVbox.getChildren().addAll(displayLabel, content, buttonBox);

        AtomicInteger result = new AtomicInteger(0);
        yes.addEventHandler(ActionEvent.ACTION,
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        result.set(1);
                    	dialog.close();
                    }
                });
        no.addEventHandler(ActionEvent.ACTION,
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        result.set(0);
                        dialog.close();
                    }
                });

		if (ENABLE_JMETRO && JMETRO.equals("dark"))
			new JMetro(Style.DARK).setParent(dialogVbox);	
		else if (ENABLE_JMETRO && JMETRO.equals("light"))
			new JMetro(Style.LIGHT).setParent(dialogVbox);
		
        Scene dialogScene = new Scene(dialogVbox, 400, 200);
        if (stylesheet != null)
        	dialogScene.getStylesheets().add(stylesheet);
        else if (DEFAULT_STYLESHEET != null)
        	dialogScene.getStylesheets().add(DEFAULT_STYLESHEET);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
        return result.get();
    }

	public static String getDialogStyleSheet() {
		return DEFAULT_STYLESHEET;
	}

	public static void setDialogStyleSheet(String dialogStyleSheet) {
		DialogFactory.DEFAULT_STYLESHEET = dialogStyleSheet;
	}
	
	
}
