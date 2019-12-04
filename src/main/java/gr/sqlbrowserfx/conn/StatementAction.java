package gr.sqlbrowserfx.conn;

import java.sql.Statement;

@FunctionalInterface
public interface StatementAction {

	public void use(Statement stmt);
}
