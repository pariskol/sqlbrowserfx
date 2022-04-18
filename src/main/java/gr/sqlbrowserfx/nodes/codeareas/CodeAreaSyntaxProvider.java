package gr.sqlbrowserfx.nodes.codeareas;

import java.util.Set;
import java.util.regex.Matcher;

public interface CodeAreaSyntaxProvider<T> {
	
	public Set<Keyword> getKeywords();
	public Set<Keyword> getKeywords(KeywordType type, T data);
	public 	Matcher getPatternMatcher(String text);
	public String format(String text);
	public String format(String text, FormatterMode mode);
}
