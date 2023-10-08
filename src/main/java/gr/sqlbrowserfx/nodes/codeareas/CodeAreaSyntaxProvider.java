package gr.sqlbrowserfx.nodes.codeareas;

import java.util.Set;
import java.util.regex.Matcher;

public interface CodeAreaSyntaxProvider<T> {
	
	Set<Keyword> getKeywords();
	Set<Keyword> getKeywords(KeywordType type, T data);
	Matcher getPatternMatcher(String text);
	String format(String text);
	String format(String text, FormatterMode mode);
}
