package gr.sqlfx.sqlTableView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gr.sqlfx.listeners.SimpleChangeListener;
import gr.sqlfx.listeners.SimpleObservable;
import javafx.beans.property.SimpleObjectProperty;


public class SqlTableRow implements SimpleObservable<SqlTableRow> {

	protected LinkedHashMap<String,SimpleObjectProperty<Object>> propertiesMap = new LinkedHashMap<>();
	protected List<String> columns;
	private List<SimpleChangeListener<SqlTableRow>> listeners;
	
	public SqlTableRow() {
		columns = new ArrayList<>();
		listeners = new ArrayList<>();
	}
	
	public SqlTableRow(Map<String,Object> entry) {
		this();
		this.columns.addAll(entry.keySet());
//		Collections.reverse(this.columns);
		
		for (String columnLabel : this.columns) {
			propertiesMap.put(columnLabel,
					new SimpleObjectProperty<Object>(entry.get(columnLabel)));
		}
	}
	
	public SqlTableRow(SqlTableRow sqlTableRow) {
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
	public synchronized void addListener(SimpleChangeListener<SqlTableRow> listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(SimpleChangeListener<SqlTableRow> listener) {
		listeners.remove(listener);
	}


}
