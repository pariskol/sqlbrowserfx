package gr.sqlbrowserfx;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dockfx.DockNode;
import org.dockfx.Dockable;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;

public class DDbDiagramPane extends DbDiagramPane implements Dockable {

	private DockNode thisDockNode = null;
	
	public DDbDiagramPane(SqlConnector sqlConnector) {
		super();
		setLoading(true);
		sqlConnector.executeAsync(() -> {
			var sqlTables = new ArrayList<SqlTable>();
			try {
				sqlConnector.getContents(rset -> {
					String name = rset.getString(1);
					String type = rset.getString(2);
					
					if (type.toLowerCase().contains("table")) {
						sqlConnector.executeQueryRaw("select * from " + name + " where 1 = 2", rset2 -> {
							SqlTable sqlTable = new SqlTable(rset2.getMetaData());
							sqlTable.setPrimaryKey(sqlConnector.findPrimaryKey(name));
							List<Map<String, String>> fkeys = sqlConnector.findFoireignKeyReferences(name);
							sqlTable.setForeignKeys(
									fkeys.stream().map(x -> x.get(SqlConnector.FOREIGN_KEY)).collect(Collectors.toList()));
							sqlTable.setRelatedTables(fkeys.stream().map(x -> x.get(SqlConnector.REFERENCED_TABLE)).collect(Collectors.toList()));
							sqlTables.add(sqlTable);
						});
					}
				});
			} catch (SQLException e) {
				DialogFactory.createErrorDialog(e);
			}
			
			Platform.runLater(() -> init(sqlTables));
		});
	}
	
	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "DB Diagram", JavaFXUtils.createIcon("/icons/diagram.png"));
		}
		return thisDockNode;
	}
}
