package gr.sqlbrowserfx.conn;

import java.sql.Statement;

@FunctionalInterface
public interface StatementAction {

	void onStatement(Statement stmt);
}
