package gr.sqlbrowserfx;

import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;

public class SqlPaneState {

	SqlTableView sqlTableView;
	SqlTableTab tableTab;
	
	public SqlPaneState(final SqlTableView sqlTableView, final SqlTableTab tableTab) {
		this.sqlTableView = sqlTableView;
		this.tableTab = tableTab;
	}

	public SqlTableView getSqlTableView() {
		return sqlTableView;
	}

	public void setSqlTableView(SqlTableView sqlTableView) {
		this.sqlTableView = sqlTableView;
	}

	public SqlTableTab getTableTab() {
		return tableTab;
	}

	public void setTableTab(SqlTableTab tableTab) {
		this.tableTab = tableTab;
	}
	
}
