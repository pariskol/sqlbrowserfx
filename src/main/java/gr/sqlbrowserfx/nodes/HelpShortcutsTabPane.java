package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import gr.sqlbrowserfx.nodes.tableviews.JSONTableView;
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
		JSONTableView tableView = new JSONTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Undo", "Ctrl+Z", rows);
		addShortcut("Redo", "Ctrl+Shift+Z", rows);
		addShortcut("Format code (all or selected text)", "Ctrl+Shift+F", rows);
		addShortcut("Save Query (selected text)", "Ctrl+S", rows);
		addShortcut("Open New Tab", "Ctrl+N", rows);
		addShortcut("Switch Tabs", "Ctrl+Page Up", rows);
		addShortcut("Switch Tabs", "Ctrl+Page Down", rows);
		addShortcut("To Upper Case (selected text)", "Ctrl+U", rows);
		addShortcut("To Lower Case (selected text)", "Ctrl+I", rows);
		addShortcut("Comment/Uncomment Selected Text", "Ctrl+/", rows);
		addShortcut("Wrap Selected Text in ()", "Ctrl+9", rows);
		addShortcut("Wrap Selected Text in ''", "Ctrl+'", rows);
		addShortcut("Add Tabs (to selected text)", "Ctrl+Tab", rows);
		addShortcut("Remove Tabs (from selected text)", "Ctrl+Shift+Tab", rows);
		addShortcut("Autocomplete", "Ctrl+Space", rows);
		addShortcut("Show Suggestions", "Ctrl+Space", rows);
		addShortcut("Show Saved Queries", "Ctrl+Shift+Space", rows);
		addShortcut("Search And Replace (optional selected text)", "Ctrl+F", rows);
		addShortcut("Show History", "Ctrl+H", rows);
		addShortcut("File Quick Access", "Ctrl+O", rows);
		addShortcut("Run Query (optional selected text)", "Ctrl+Enter", rows);
		addShortcut("Copy", "Ctrl+C", rows);
		addShortcut("Paste", "Ctrl+P", rows);
		addShortcut("Cut", "Ctrl+X", rows);
		addShortcut("Select All", "Ctrl+A", rows);
		addShortcut("Word Cursor Step", "Ctrl+Arrow Right", rows);
		addShortcut("Word Cursor Step", "Ctrl+Arrow Left", rows);
		addShortcut("Select Word", "Ctrl+Shift+Arrow Right", rows);
		addShortcut("Select Word", "Ctrl+Shift+Arrow Left", rows);
		addShortcut("Select Text", "Shift(Pressed)+Arrows", rows);
		addShortcut("Scroll To Top", "Ctrl+Home", rows);
		addShortcut("Scroll To End", "Ctrl+End", rows);
		addShortcut("Go To Start Of Line", "Home", rows);
		addShortcut("Go To End Of Line", "End", rows);
		addShortcut("Go To Line", "Ctrl+L", rows);
		addShortcut("Skip Rows", "Page Up/Page Down", rows);
		addShortcut("Navigation", "Arrows", rows);
		addShortcut("Context Menu", "Right Click", rows);

		tableView.setItemsLater(new JSONArray(rows));
		
		Tab tab = new Tab("SqlCodeArea", tableView);
		tab.setClosable(false);
		return tab;
	}
	
	private Tab createSqlTableViewTab() {
		JSONTableView tableView = new JSONTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Add", "Ctrl+Q", rows);
		addShortcut("Edit", "Ctrl+E", rows);
		addShortcut("Delete", "Ctrl+D", rows);
		addShortcut("Refresh", "Ctrl+R", rows);
		addShortcut("Import CSV", "Ctrl+I", rows);
		addShortcut("Search (optional column:pattern)", "Ctrl+F", rows);
		addShortcut("Copy Row (comma separated)", "Ctrl+C", rows);
		addShortcut("Vertical Scrolling", "Arrow Right/Arrow Left", rows);
		addShortcut("Scroll To Top", "Home", rows);
		addShortcut("Scroll To End", "End", rows);
		addShortcut("Select All Rows", "Ctrl+A", rows);
		addShortcut("Select Rows", "Shift(Pressed)+Arrows", rows);
		addShortcut("Select Rows", "Ctrl(Pressed)+Left Click", rows);
		addShortcut("Skip rows", "Page Up/Page Down", rows);
		addShortcut("Navigation", "Arrow Up/Arrow Down", rows);
		addShortcut("Context Menu", "Right Click", rows);
		tableView.setItemsLater(new JSONArray(rows));
		
		Tab tab = new Tab("SqlTableView", tableView);
		tab.setClosable(false);
		return tab;
	}

	private Tab createDBTreeViewTab() {
		JSONTableView tableView = new JSONTableView();
		List<Map<String, Object>> rows = new ArrayList<>();
		addShortcut("Search", "Ctrl+F", rows);
		addShortcut("Copy text of selected items (comma separated if many)", "Ctrl+C", rows);
		addShortcut("Navigation", "Arrow Up/Arrow Down", rows);
		addShortcut("Expand Selected", "Arrow Right", rows);
		addShortcut("Collapse Selected", "Arrow Left", rows);
		addShortcut("Scroll To Top", "Home", rows);
		addShortcut("Scroll To End", "End", rows);
		addShortcut("Select Rows", "Shift(Pressed)+Arrows", rows);
		addShortcut("Select Rows", "Ctrl(Pressed)+Left Click", rows);
		addShortcut("Context Menu", "Right Click", rows);
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
