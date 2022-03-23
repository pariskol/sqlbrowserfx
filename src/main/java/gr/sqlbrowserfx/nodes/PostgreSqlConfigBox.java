package gr.sqlbrowserfx.nodes;

public class PostgreSqlConfigBox extends MySqlConfigBox {

	@Override
	public String getHistoryQuery() {
		return "select url, user, database, timestamp, id from connections_history_localtime"
				+ " where database_type = 'postgresql' order by timestamp desc";
	}

	@Override
	public String getSaveType() {
		return "postgresql";
	}
}
