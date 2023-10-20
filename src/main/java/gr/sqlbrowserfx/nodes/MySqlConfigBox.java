package gr.sqlbrowserfx.nodes;

public class MySqlConfigBox extends DbConfigBox {

	public String getHistoryQuery() {
		return "select url, user, database, timestamp, id from connections_history_localtime"
				+ " where database_type = '" + getSqlConnectorType() + "' order by timestamp desc";
	}
	

	@Override
	public String getSqlConnectorType() {
		return SqlConnectorType.MYSQL.toString().toLowerCase();
	}
	
}
