package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import gr.sqlbrowserfx.nodes.tableviews.MapTableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class HelpShortcutsTabPane extends TabPane {

	public HelpShortcutsTabPane() {
		super();
		this.getTabs().add(createSqlCodeAreaTab());
		this.getTabs().add(createSqlTableViewTab());
		this.getTabs().add(createDBTreeViewTab());
	}

	private Tab createSqlCodeAreaTab() {
		MapTableView tableView = new MapTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Copy", "Ctrl+C", rows);
		addShortcut("Paste", "Ctrl+P", rows);
		addShortcut("Cut", "Ctrl+X", rows);
		addShortcut("Undo", "Ctrl+Z", rows);
		addShortcut("Redo", "Ctrl+Shift+Z", rows);
		addShortcut("Save Query", "Ctrl+S", rows);
		addShortcut("To Upper Case", "Ctrl+U", rows);
		addShortcut("To Lower Case", "Ctrl+L", rows);
		addShortcut("Add Tabs", "Ctrl+Tab", rows);
		addShortcut("Remove Tabs", "Ctrl+Shift+Tab", rows);
		addShortcut("Autocomplete", "Ctrl+Space", rows);
		addShortcut("Show Suggestions", "Ctrl+Space", rows);
		addShortcut("Show Saved Queries", "Ctrl+Shift+Space", rows);
		addShortcut("Search And Replace", "Ctrl+F", rows);
		addShortcut("Run Query", "Ctrl+Enter", rows);
		tableView.setItemsLater(new JSONArray(rows));
		
		Tab tab = new Tab("SqlCodeArea", tableView);
		tab.setClosable(false);
		return tab;
	}
	
	private Tab createSqlTableViewTab() {
		MapTableView tableView = new MapTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Add", "Ctrl+Q", rows);
		addShortcut("Edit", "Ctrl+E", rows);
		addShortcut("Delete", "Ctrl+D", rows);
		addShortcut("Refresh", "Ctrl+R", rows);
		addShortcut("Scroll To Top", "Ctrl+Home", rows);
		addShortcut("Scroll To End", "Ctrl+End", rows);
		addShortcut("Select All Rows", "Ctrl+A", rows);
		addShortcut("Import CSV", "Ctrl+I", rows);
		addShortcut("Search (Column:pattern)", "Ctrl+F", rows);
		addShortcut("Copy Row (Comma Separated)", "Ctrl+C", rows);
		addShortcut("Move To Next Row", "Arrow Down", rows);
		addShortcut("Move To Previous Row", "Arrow Up", rows);
		addShortcut("Vertical Scrolling Right", "Arrow Right", rows);
		addShortcut("Vertical Scrolling Left", "Arrow Left", rows);
		addShortcut("Multiple Selection", "Ctrl(Pressed)+Left Click", rows);
		addShortcut("Multiple Selection", "Shift(Pressed)+Arrow Up", rows);
		addShortcut("Multiple Selection", "Shift(Pressed)+Arrow Down", rows);
		tableView.setItemsLater(new JSONArray(rows));
		
		Tab tab = new Tab("SqlTableView", tableView);
		tab.setClosable(false);
		return tab;
	}

	private Tab createDBTreeViewTab() {
		MapTableView tableView = new MapTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Search", "Ctrl+F", rows);
		addShortcut("Copy text of selected items (comma separated)", "Ctrl+C", rows);
		tableView.setItemsLater(new JSONArray(rows));
		
		Tab tab = new Tab("DBTreeView", tableView);
		tab.setClosable(false);
		return tab;
	}
	
	private void addShortcut(String desc, String shortcut, List<Map<String, Object>> rows) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("Description", desc);
		map.put("Shortcut", shortcut);
		rows.add(map);
	}
}
