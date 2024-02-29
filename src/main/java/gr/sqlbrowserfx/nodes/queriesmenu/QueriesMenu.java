package gr.sqlbrowserfx.nodes.queriesmenu;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.nodes.DSqlConsolePane;
import gr.sqlbrowserfx.dock.nodes.DSqlPane;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class QueriesMenu extends Menu implements SimpleObserver<String> {

	private final HashMap<String, Menu> menuItemsMap;
	private final HashMap<String, String> queriesMap;
	private long codeAreasAvailable = 0;
	private DSqlConsolePane sqlConsolePane;
	
	public QueriesMenu() {
		super("Saved queries", JavaFXUtils.createIcon("/icons/thunder.png"));
		menuItemsMap = new HashMap<>();
		queriesMap = new HashMap<>();
		
		MenuItem refreshMenuItem = new MenuItem("Refresh Queries", JavaFXUtils.createIcon("/icons/refresh.png"));
		refreshMenuItem.setOnAction(action -> this.loadQueries());
		this.getItems().add(refreshMenuItem);
		this.loadQueries();
		this.startCodeAreasAgent();
	
	}
	
	public QueriesMenu(DSqlConsolePane sqlConsolePane) {
		super("Saved queries", JavaFXUtils.createIcon("/icons/thunder.png"));
		menuItemsMap = new HashMap<>();
		queriesMap = new HashMap<>();
		
		MenuItem refreshMenuItem = new MenuItem("Refresh Queries", JavaFXUtils.createIcon("/icons/refresh.png"));
		refreshMenuItem.setOnAction(action -> this.loadQueries());
		this.getItems().add(refreshMenuItem);
		this.loadQueries();
		this.sqlConsolePane = sqlConsolePane;
		
		if (sqlConsolePane != null)
			this.startCodeAreasAgent();
	
	}

	private void startCodeAreasAgent() {
		Thread t = new Thread(() -> {
			while(!Thread.currentThread().isInterrupted()) {
				try {
					long newCodeAreasAvailable = SqlBrowserFXAppManager.getActiveSqlCodeAreasNum();
					if (newCodeAreasAvailable != codeAreasAvailable) {
						codeAreasAvailable = newCodeAreasAvailable;
						
						for(Menu menu : menuItemsMap.values())
							for (MenuItem item: menu.getItems())
								populateSqlCodeAreasAvailable((Menu) item);
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}, getClass().getSimpleName() + "-codeareas-agent");
		t.setDaemon(true);
		t.start();
	}
	
	private void loadQueries() {
		Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
		this.getItems().removeAll(menuItemsMap.values());
		try {
			SqlConnector sqlConnector = SqlBrowserFXAppManager.getConfigSqlConnector();
			sqlConnector.executeQuery("select distinct category from saved_queries", rset -> {
				String category = rset.getString(1); 
				Menu categorySubMenu =  new Menu(category);
				categorySubMenu.setGraphic(JavaFXUtils.createIcon("/icons/folder.png"));
				menuItemsMap.put(category, categorySubMenu);
				this.getItems().add(categorySubMenu);
				
				sqlConnector.executeQuery("select * from saved_queries where category = ?", Arrays.asList(category), rset2 -> {
					try {
						QueryDTO qd = (QueryDTO) DTOMapper.map(rset2, QueryDTO.class);
						Menu queryMenuItem = new Menu(qd.getDescription());
						queriesMap.put(qd.getDescription(), qd.getSql());
						queryMenuItem.setOnAction(action -> {
							StringSelection stringSelection = new StringSelection(queriesMap.get(qd.getDescription()));
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							clipboard.setContents(stringSelection, null);
						});
						categorySubMenu.getItems().add(queryMenuItem);
						populateSqlCodeAreasAvailable(queryMenuItem);
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				});
			});
			
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void populateSqlCodeAreasAvailable(Menu queryMenuItem) {
		Platform.runLater(() -> {
			queryMenuItem.getItems().clear();

			if (sqlConsolePane == null) {
				for (SqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
					if (sqlPane instanceof DSqlPane) {
						DSqlPane dsqlPane = (DSqlPane) sqlPane;
						if (dsqlPane.getSqlCodeAreaRef() != null) {
							MenuItem sendToCodeArea = new MenuItem("Paste in " + dsqlPane.asDockNode().getTitle());
							sendToCodeArea.setOnAction(action2 -> dsqlPane.getSqlCodeAreaRef().replaceText(queriesMap.get(queryMenuItem.getText())));
							queryMenuItem.getItems().add(sendToCodeArea);
						}
					}
				}
			}
			else {
				MenuItem sendToCodeArea = new MenuItem("Paste in code area");
				sendToCodeArea.setOnAction(action2 -> sqlConsolePane.getCodeAreaRef().replaceText(queriesMap.get(queryMenuItem.getText())));
				queryMenuItem.getItems().add(sendToCodeArea);
			}
			});
	}

	@Override
	public void onObservableChange(String newValue) {
		this.loadQueries();	
	}
}
