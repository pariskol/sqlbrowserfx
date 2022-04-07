package gr.sqlbrowserfx.nodes;

public class MySqlConfigBox extends DbConfigBox {

	public String getHistoryQuery() {
		return "select url, user, database, timestamp, id from connections_history_localtime"
				+ " where database_type = 'mysql' order by timestamp desc";
	}
	

	@Override
	public String getSaveType() {
		return "mysql";
	}
	
}
