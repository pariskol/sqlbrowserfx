package gr.sqlbrowserfx.nodes.queriesMenu;

import gr.sqlbrowserfx.utils.mapper.Column;
import gr.sqlbrowserfx.utils.mapper.DTO;

@DTO
public class QueryDTO {

	@Column("QUERY")
	private String sql;
	@Column("DESCRIPTION")
	private String description;
	@Column("CATEGORY")
	private String category;
	
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	
	
}
