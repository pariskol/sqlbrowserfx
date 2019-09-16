package gr.sqlbrowserfx.nodes;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LineChartBox extends VBox {

	private String displayColMessage = "Select display column";
	private String plotColMessage = "Select plot column";
	private ComboBox<String> displayColComboBox;
	private TextField valueField;
	private ComboBox<String> plotColxComboBox;
	private ComboBox<String> plotColyComboBox;

	public LineChartBox(List<String> columnNames) {
		super();

		displayColComboBox = new ComboBox<String>(FXCollections.observableArrayList(columnNames));
		valueField = new TextField();
		plotColxComboBox = new ComboBox<String>(FXCollections.observableArrayList(columnNames));
		plotColyComboBox = new ComboBox<String>(FXCollections.observableArrayList(columnNames));

		this.getChildren().addAll(new Label(displayColMessage), displayColComboBox, valueField, new Label("Select x column"),
				plotColxComboBox, new Label("Select y column"), plotColyComboBox);
		
		displayColComboBox.prefWidthProperty().bind(this.widthProperty());
		valueField.prefWidthProperty().bind(this.widthProperty());
		plotColxComboBox.prefWidthProperty().bind(this.widthProperty());
		plotColyComboBox.prefWidthProperty().bind(this.widthProperty());
	}

	public String getPlotColumn1() {
		return plotColxComboBox.getSelectionModel().getSelectedItem();
	}

	public String getPlotColumn2() {
		return plotColyComboBox.getSelectionModel().getSelectedItem();
	}
	
	public String getKeyColumn() {
		return displayColComboBox.getSelectionModel().getSelectedItem();
	}

	public String getValue() {
		return valueField.getText();
	}
}
