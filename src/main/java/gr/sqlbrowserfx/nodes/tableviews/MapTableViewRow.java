package gr.sqlbrowserfx.nodes.tableviews;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableRowEditBox;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;


public class MapTableViewRow implements SimpleObservable<MapTableViewRow> {

	protected LinkedHashMap<String,SimpleObjectProperty<Object>> propertiesMap = new LinkedHashMap<>();
	protected List<String> columns;
	private List<SimpleObserver<MapTableViewRow>> listeners;
	private SimpleBooleanProperty isUpdateByGuiProperty = new SimpleBooleanProperty(true);
	
	public MapTableViewRow() {
		columns = new ArrayList<>();
		listeners = new ArrayList<>();
	}
	
	public MapTableViewRow(Map<String,Object> entry) {
		this();
		this.setMap(entry);
	}

	public void refreshMap(Map<String, Object> entry) {
		for (String key : entry.keySet()) {
			propertiesMap.get(key).set(entry.get(key));
		}
	}
	
	public void refreshMapFromEditBox(SqlTableRowEditBox entry) {
		for (String key : entry.getMap().keySet()) {
			propertiesMap.get(key).set(entry.getMap().get(key).getText());
		}
	}
	
	public void setMap(Map<String, Object> entry) {
		this.columns.addAll(entry.keySet());
//		Collections.reverse(this.columns);
		
		for (String columnLabel : this.columns) {
			propertiesMap.put(columnLabel,
					new SimpleObjectProperty<Object>(entry.get(columnLabel)));
		}
	}
	
	public MapTableViewRow(MapTableViewRow sqlTableRow) {
		this();
		this.propertiesMap = sqlTableRow.getStringPropertiesMap();
		this.columns = sqlTableRow.getColumns();
	}
	
	
	public SimpleObjectProperty<Object> getObjectProperty(String key) {
		return propertiesMap.get(key);
	}
	
	public Object get(String key) {
		return propertiesMap.get(key) != null?propertiesMap.get(key).getValue():null;
	}

	public void set(String key, Object value) {
		propertiesMap.get(key).setValue(value);
	}

	public BooleanProperty isUpdatedByGui() {
		return isUpdateByGuiProperty;
	}
	public List<String> getColumns() {
		return columns;
	}

	public LinkedHashMap<String, SimpleObjectProperty<Object>> getStringPropertiesMap() {
		return propertiesMap;
	}
	
	@Override
	public String toString() {
		String result = new String();
		for (SimpleObjectProperty<Object> property: propertiesMap.values()) {
			Object value = property.get();
			if (value instanceof String)
				value = value.toString().replaceAll("\n", " ").replace(",", " ");
			if (value == null)
				value = "";
			result += value + ",";
		}
		return result.substring(0,result.length()-1);
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservaleChange(this));
	}

	@Override
	public void changed(MapTableViewRow data) {
		
	}

	@Override
	public synchronized void addObserver(SimpleObserver<MapTableViewRow> listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeObserver(SimpleObserver<MapTableViewRow> listener) {
		listeners.remove(listener);
	}


}
