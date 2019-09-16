package gr.sqlbrowserfx.conn;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetAction {

	public void use(ResultSet rset) throws SQLException;
}
