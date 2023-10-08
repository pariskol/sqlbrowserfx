package gr.sqlbrowserfx.nodes.codeareas.log;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.sqlbrowserfx.nodes.codeareas.CodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.codeareas.FormatterMode;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;

@SuppressWarnings("rawtypes")
public class LogCodeAreaSyntaxProvider implements CodeAreaSyntaxProvider {

	private static final String[] FUNCTIONS = {"INFO", "DEBUG"};
	private static final String[] KEYWORDS = {"ERROR", "FATAL"};
	private static final Set<Keyword> KEYWORDS_lIST = new LinkedHashSet<>();
	
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";

	private static final Pattern PATTERN = Pattern
			.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + 
					 "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");
	
	static {
		KEYWORDS_lIST.addAll(Arrays.stream(KEYWORDS)
				.map(kw -> new Keyword(kw, KeywordType.KEYWORD)).toList());
		KEYWORDS_lIST.addAll(Arrays.stream(FUNCTIONS)
				.map(kw -> new Keyword(kw, KeywordType.FUNCTION)).toList());

	}

	@Override
	public Set<Keyword> getKeywords() {
		return KEYWORDS_lIST;
	}

	@Override
	public Set<Keyword> getKeywords(KeywordType type, Object data) {
		throw new RuntimeException("Method 'getKeywords' not implemented");
	}
	
	@Override
	public Matcher getPatternMatcher(String text) {
		return PATTERN.matcher(text);
	}
	
	@Override
	public String format(String text) {
		throw new RuntimeException("Method 'format' not implemented");
	}
	
	@Override
	public String format(String text, FormatterMode mode) {
		throw new RuntimeException("Method 'format' not implemented");
	}
	
}

