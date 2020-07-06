package gr.sqlbrowserfx.nodes.queriesmenu;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class QueriesMenu extends Menu{

	HashMap<String, Menu> menuItemsMap;
	HashMap<String, String> queriesMap;
	
	public QueriesMenu() {
		super("Saved queries", JavaFXUtils.icon("/res/thunder.png"));
		menuItemsMap = new HashMap<>();
		queriesMap = new HashMap<>();
		
		MenuItem refreshMenuItem = new MenuItem("Refresh Queries", JavaFXUtils.icon("/res/refresh.png"));
		refreshMenuItem.setOnAction(action -> this.loadQueries());
		this.getItems().add(refreshMenuItem);
		this.loadQueries();
	}
	
	private void loadQueries() {
		Logger logger = LoggerFactory.getLogger("SQLBROWSER");
		this.getItems().removeAll(menuItemsMap.values());
		try {
			SqlConnector sqlConnector = SqlBrowserFXAppManager.getConfigSqlConnector();
			sqlConnector.executeQuery("select distinct category from saved_queries", rset -> {
				String category = rset.getString(1); 
				Menu categorySubMenu =  new Menu(category);
				categorySubMenu.setGraphic(JavaFXUtils.icon("/res/folder.png"));
				menuItemsMap.put(category, categorySubMenu);
				this.getItems().add(categorySubMenu);
				
				sqlConnector.executeQuery("select * from saved_queries where category = ?", Arrays.asList(category), rset2 -> {
					try {
						QueryDTO qd = (QueryDTO) DTOMapper.map(rset2, QueryDTO.class);
						Menu queryMenuItem = new Menu();
						queryMenuItem.setGraphic(new Label(qd.getDescription()));
						queriesMap.put(qd.getDescription(), qd.getSql());
						queryMenuItem.setOnAction(action -> {
							StringSelection stringSelection = new StringSelection(queriesMap.get(qd.getDescription()));
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							clipboard.setContents(stringSelection, null);
						});
						categorySubMenu.getItems().add(queryMenuItem);
						
						queryMenuItem.getGraphic().setOnMouseEntered(mouseEvent -> {
							queryMenuItem.getItems().clear();
							for (DSqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
								if (sqlPane.getSqlCodeAreaRef() != null) {
									MenuItem sendToCodeArea = new MenuItem("Paste in " + sqlPane.asDockNode().getTitle());
									sendToCodeArea.setOnAction(action2 -> {
										sqlPane.getSqlCodeAreaRef().clear();
										sqlPane.getSqlCodeAreaRef().appendText(queriesMap.get(qd.getDescription()));
									});
									queryMenuItem.getItems().add(sendToCodeArea);
								}
							}
							queryMenuItem.show();
						});
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				});
			});
			
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
