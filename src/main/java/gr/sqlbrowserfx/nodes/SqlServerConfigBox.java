package gr.sqlbrowserfx.nodes;

public class SqlServerConfigBox extends DbConfigBox {

	@Override
	public String getHistoryQuery() {
		return "select url, user, database, timestamp, id from connections_history_localtime"
				+ " where database_type = '"  + getSqlConnectorType() + "' order by timestamp desc";
	}

	@Override
	public String getSqlConnectorType() {
		return SqlConnectorType.SQLSERVER.toString().toLowerCase();
	}
}
