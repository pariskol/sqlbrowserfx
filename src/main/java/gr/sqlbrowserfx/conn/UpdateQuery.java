package gr.sqlbrowserfx.conn;

import java.util.List;

public class UpdateQuery {

	private String query;
	List<Object> params;

	
	public UpdateQuery(String query, List<Object> params) {
		this.query = query;
		this.params = params;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}

}
