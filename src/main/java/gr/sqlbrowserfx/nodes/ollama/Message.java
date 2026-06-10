package gr.sqlbrowserfx.nodes.ollama;

import org.json.JSONObject;

import gr.sqlbrowserfx.utils.mapper.Column;
import gr.sqlbrowserfx.utils.mapper.DTO;

@DTO
public class Message {
	@Column("id")
	private Integer id;
	@Column("conversation_id")
	private String conversationId;
	@Column("role")
	private String role;
	@Column("content")
	private String content;
	@Column("meta_data")
	private String metaData;
	@Column("created_at")
	private String createdAt;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer value) {
		this.id = value;
	}

	public String getConversationId() {
		return this.conversationId;
	}

	public void setConversationId(String value) {
		this.conversationId = value;
	}

	public String getRole() {
		return this.role;
	}

	public void setRole(String value) {
		this.role = value;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String value) {
		this.content = value;
	}

	public String getMetaData() {
		return metaData;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public String getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(String value) {
		this.createdAt = value;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
}
