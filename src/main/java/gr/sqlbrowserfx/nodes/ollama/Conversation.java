package gr.sqlbrowserfx.nodes.ollama;

import gr.sqlbrowserfx.utils.mapper.Column;
import gr.sqlbrowserfx.utils.mapper.DTO;

@DTO
public class Conversation {
	@Column("id")
	private String id;
	@Column("title")
	private String title;
	@Column("model")
	private String model;
	@Column("created_at")
	private String createdAt;
	@Column("updated_at")
	private String updatedAt;

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String value) {
		this.model = value;
	}

	public String getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(String value) {
		this.createdAt = value;
	}

	public String getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(String value) {
		this.updatedAt = value;
	}

	@Override
	public String toString() {
		return this.title + " " + createdAt;
	}
}
