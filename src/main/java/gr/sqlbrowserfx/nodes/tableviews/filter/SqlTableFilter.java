package gr.sqlbrowserfx.nodes.tableviews.filter;

import gr.sqlbrowserfx.nodes.CustomHBox;
import gr.sqlbrowserfx.nodes.tableviews.MapTableViewRow;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

public class SqlTableFilter extends TableFilter<MapTableViewRow>{

	private static final int MAX_FILTERABLE_VALUES = 50;


	 protected SqlTableFilter(TableView<MapTableViewRow> tableView) {
			this(tableView, false);
	}
	 
	protected SqlTableFilter(TableView<MapTableViewRow> tableView, boolean isLazy) {
		super(tableView, isLazy);
	}

	@Override
    protected boolean isColumnFilterable(TableColumn<?, ?> tableColumn) {
		((SqlTableView) tableColumn.getTableView()).resetColumnGraphic(tableColumn);
		boolean isFilterable = ((SqlTableView)tableColumn.getTableView()).getUniqueEntriesForColumn(tableColumn.getText()) < MAX_FILTERABLE_VALUES;
    	if (isFilterable) {
    		Node graphic = tableColumn.getGraphic() != null ? new CustomHBox(tableColumn.getGraphic(),JavaFXUtils.createIcon("/icons/filter.png")) : JavaFXUtils.createIcon("/icons/filter.png"); 
			tableColumn.setGraphic(graphic);
    	}
		return isFilterable;
    }
	
	public static void apply(SqlTableView sqlTableView) {
		new SqlTableFilter(sqlTableView);
		// in order to use controlsfx 11 implementation use line bellow 
//		org.controlsfx.control.table.TableFilter.forTableView(sqlTableView).apply();
	}
	
}
