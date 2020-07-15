package gr.sqlbrowserfx.nodes.codeareas.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeAreaSyntax;

public class LogCodeAreaSyntax {

	public static final String[] FUNCTIONS = {"INFO", "DEBUG"};
	public static final String[] KEYWORDS = {"ERROR", "FATAL"};
	public static final List<String> KEYWORDS_lIST = new ArrayList<>();
	
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	public static final String TODO_SINGLE_COMMENT_PATTERN = "//TODO[^\n]*";
	public static final String WARN_SINGLE_COMMENT_PATTERN = "//WARN[^\n]*";
	private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";

	public static final Pattern PATTERN = Pattern
			.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + 
					 "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");
	
	static {
        KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntax.KEYWORDS));
        KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntax.FUNCTIONS));
	}
	
}

