package gr.sqlbrowserfx.nodes.codeareas;

public class Keyword {

	private String keyword;
	private KeywordType type;

	
	public Keyword(String keyword, KeywordType type) {
		super();
		this.keyword = keyword;
		this.type = type;
	}

	public Keyword() {
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public KeywordType getType() {
		return type;
	}

	public void setType(KeywordType type) {
		this.type = type;
	}

	public boolean isFunction() {
		return this.type == KeywordType.FUNCTION;
	}
	
	public boolean isKeyword() {
		return this.type == KeywordType.KEYWORD;
	}
	
	public boolean isType() {
		return this.type == KeywordType.TYPE;
	}

	public boolean isTable() {
		return this.type == KeywordType.TABLE;
	}

	public boolean isQuery() {
		return this.type == KeywordType.QUERY;
	}

	public boolean isColumn() {
		return this.type == KeywordType.COLUMN;

	}
	
	public boolean isVariable() {
		return this.type == KeywordType.VARIABLE;
	}
	
	public boolean isAlias() {
		return this.type == KeywordType.ALIAS;
	}
	
	@Override
	public int hashCode() {
		// return always 0 to always invoke equals in sets
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Keyword) {
			Keyword keyword = (Keyword) obj;
			return this.getKeyword().equals(keyword.getKeyword()) && this.getType() == keyword.getType();
		}
		else {
			return super.equals(obj);
		}
	}
	

}
