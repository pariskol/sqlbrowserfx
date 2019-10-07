package gr.sqlbrowserfx.nodes;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.utils.AppManager;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class QueriesMenu extends Menu{

	HashMap<String, Menu> menuItemsMap;
	HashMap<String, String> queriesMap;
	
	public QueriesMenu() {
		super("Saved queries", JavaFXUtils.icon("/res/thunder.png"));
		menuItemsMap = new HashMap<>();
		queriesMap = new HashMap<>();
		
		MenuItem refreshMenuItem = new MenuItem("Refresh", JavaFXUtils.icon("/res/refresh.png"));
		refreshMenuItem.setOnAction(action -> this.loadQueries());
		this.getItems().add(refreshMenuItem);
		this.loadQueries();
	}
	
	private void loadQueries() {
		this.getItems().removeAll(menuItemsMap.values());
		try {
			SqlConnector sqlConnector = AppManager.getConfigSqlConnector();
			sqlConnector.executeQuery("select distinct category from saved_queries", rset -> {
				String category = rset.getString(1); 
				Menu categorySubMenu =  new Menu(category);
				menuItemsMap.put(category, categorySubMenu);
				this.getItems().add(categorySubMenu);
				
				sqlConnector.executeQuery("select * from saved_queries where category = ?", Arrays.asList(category), rset2 -> {
					try {
						QueryDTO qd = (QueryDTO) DTOMapper.map(rset2, QueryDTO.class);
						MenuItem queryMenuItem = new MenuItem(qd.getDescription());
						queriesMap.put(qd.getDescription(), qd.getSql());
						queryMenuItem.setOnAction(action -> {
							StringSelection stringSelection = new StringSelection(queriesMap.get(qd.getDescription()));
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							clipboard.setContents(stringSelection, null);
						});
						categorySubMenu.getItems().add(queryMenuItem);
					} catch (IllegalAccessException | InstantiationException e) {
						e.printStackTrace();
					}
				});
			});
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
