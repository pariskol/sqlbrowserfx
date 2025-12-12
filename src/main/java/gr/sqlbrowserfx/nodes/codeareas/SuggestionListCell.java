package gr.sqlbrowserfx.nodes.codeareas;

import gr.sqlbrowserfx.nodes.CustomHBox;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

public class SuggestionListCell extends ListCell<Keyword>{
	
	private final ListView<Keyword> listView;
	
	public SuggestionListCell(ListView<Keyword>  listView) {
		this.listView = listView;
	}

	@Override
	protected void updateItem(Keyword item, boolean empty) {
		super.updateItem(item, empty);
		this.setGraphic(null);
		if (item == null || empty) {
			setText(null);
		} else {
			var text = new StringBuilder(item.getKeyword());
			if (item.getDescription() != null) {
				text.append(" ");
				text.append(item.getDescription());
			}
			if(item.isFunction()) {
				var hint = new Label(item.getDescription());
				hint.setStyle("-fx-text-fill: -fx-secondary-light-color; -fx-font-size: 10pt;");
				
				var hbox = new CustomHBox(
					JavaFXUtils.createIcon("/icons/function.png"),
					new Label(item.getKeyword()),
					hint
				);
		        hbox.setAlignment(Pos.CENTER_LEFT);
			        
				this.setGraphic(hbox);
				setText(null);
			}
			else if (item.isTable()) {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/table.png"));
			}
			else if (item.isQuery()) {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/thunder.png"));
			}
			else if (item.isColumn()) {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
			}
			else if (item.isVariable()) {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/var.png"));
			}
			else if (item.isAlias()) {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/table-y.png"));
			}
			else {
				setText(text.toString());
				this.setGraphic(JavaFXUtils.createIcon("/icons/green.png"));
			}
			
			widthProperty().addListener(this::widthChanged);
		}
	}
	
	private void widthChanged(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		double width = getWidth() + listView.getInsets().getLeft() + listView.getInsets().getRight();
	    listView.setPrefWidth(Math.max(listView.getPrefWidth(), width));

	}      
}
