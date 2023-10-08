package gr.sqlbrowserfx.nodes.codeareas;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

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
			setText(item.getKeyword());
			if(item.isFunction())
				this.setGraphic(JavaFXUtils.createIcon("/icons/function.png"));
			else if (item.isTable())
				this.setGraphic(JavaFXUtils.createIcon("/icons/table.png"));
			else if (item.isQuery())
				this.setGraphic(JavaFXUtils.createIcon("/icons/thunder.png"));
			else if (item.isColumn())
				this.setGraphic(JavaFXUtils.createIcon("/icons/blue.png"));
			else if (item.isVariable())
				this.setGraphic(JavaFXUtils.createIcon("/icons/var.png"));
			else if (item.isAlias())
				this.setGraphic(JavaFXUtils.createIcon("/icons/table-y.png"));
			else
				this.setGraphic(JavaFXUtils.createIcon("/icons/green.png"));
			
			widthProperty().addListener(this::widthChanged);
		}
	}
	
	private void widthChanged(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		double width = getWidth() + listView.getInsets().getLeft() + listView.getInsets().getRight();
	    listView.setPrefWidth(Math.max(listView.getPrefWidth(), width));

	}      
}
