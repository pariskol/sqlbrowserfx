package gr.sqlbrowserfx.nodes.codeareas;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleFileCodeAreaSyntaxProvider implements CodeAreaSyntaxProvider<String>{

	private final Pattern pattern = Pattern.compile("");
	private final Set<Keyword> emptySet = new HashSet<>();
	
	@Override
	public Set<Keyword> getKeywords() {
		return emptySet;
	}

	@Override
	public Set<Keyword> getKeywords(KeywordType type, String data) {
		return emptySet;
	}

	@Override
	public Matcher getPatternMatcher(String text) {
		return pattern.matcher(text);
	}

	@Override
	public String format(String text) {
		return text;
	}

	@Override
	public String format(String text, FormatterMode mode) {
		return text;
	}

}
