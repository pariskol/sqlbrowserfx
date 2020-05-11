package gr.sqlbrowserfx.nodes.sqlTableView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import javafx.beans.property.SimpleObjectProperty;


public class MapTableViewRow implements SimpleObservable<MapTableViewRow> {

	protected LinkedHashMap<String,SimpleObjectProperty<Object>> propertiesMap = new LinkedHashMap<>();
	protected List<String> columns;
	private List<SimpleChangeListener<MapTableViewRow>> listeners;
	
	public MapTableViewRow() {
		columns = new ArrayList<>();
		listeners = new ArrayList<>();
	}
	
	public MapTableViewRow(Map<String,Object> entry) {
		this();
		setMap(entry);
	}

	public void refreshMap(Map<String, Object> entry) {
		for (String key : entry.keySet()) {
			propertiesMap.get(key).set(entry.get(key));
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
			if (value != null && value instanceof String)
				value = value.toString().replace(",", "|");
			if (value == null)
				value = "";
			result += value + ",";
		}
		return result.substring(0,result.length()-1);
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onChange(this));
	}

	@Override
	public void changed(MapTableViewRow data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public synchronized void addListener(SimpleChangeListener<MapTableViewRow> listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(SimpleChangeListener<MapTableViewRow> listener) {
		listeners.remove(listener);
	}


}
