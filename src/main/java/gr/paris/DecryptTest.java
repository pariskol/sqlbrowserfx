package gr.paris;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqliteConnector;

public class DecryptTest {

	public static void main(String[] args) throws SQLException, GeneralSecurityException {
		SqlConnector sqlConnector = new SqliteConnector("./pass.db");
		for (int i=0;i<=10; i++) {
			List<Object> params = new ArrayList<>();
			params.add("paris");
			params.add(Encrypter.encrypt("test"+i));
			params.add("app"+i);
			sqlConnector.executeUpdate("insert into passwords (username, password, app) values (?,?,?)", params);
			sqlConnector.executeQuery("select * from passwords", rset -> {
				try {
					System.out.println(Encrypter.decrypt(rset.getBytes("PASSWORD")));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			});
		}
	}
}
