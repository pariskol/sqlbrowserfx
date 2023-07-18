package gr.sqlbrowserfx.nodes.codeareas.java;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.sqlbrowserfx.nodes.codeareas.CodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.codeareas.FormatterMode;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.utils.SqlFormatter;

public class JavaCodeAreaSyntaxProvider implements CodeAreaSyntaxProvider<String> {


	private static String[] FUNCTIONS = new String[] {};
	private static String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "var"
    };
	
	private static final Set<Keyword> KEYWORDS_lIST = new LinkedHashSet<>(
			Arrays.asList(KEYWORDS).stream().map(word -> new Keyword(word, KeywordType.KEYWORD)).toList());

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
    		                          + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)
    private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
	private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<PAREN>" + PAREN_PATTERN + ")"
            + "|(?<BRACE>" + BRACE_PATTERN + ")"
            + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<STRING2>" + STRING_PATTERN_2 + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")"
            + "|(?<METHOD>" + METHOD_PATTERN + ")"
    );
    
	public static void init(String dbType) {
		init();
	}

	private static void init() {
	}

	
	@Override
	public Set<Keyword> getKeywords() {
		return KEYWORDS_lIST;
	}
	
	@Override
	public Set<Keyword> getKeywords(KeywordType type, String tableAlias) {
		return getKeywords();
	}
	
	@Override
	public Matcher getPatternMatcher(String text) {
		return PATTERN.matcher(text);
	}
	
	@Override
	public String format(String text) {
		return SqlFormatter.format(text);
	}
	
	@Override
	public String format(String text, FormatterMode mode) {
		switch (mode) {
		case DEFAULT:
			return SqlFormatter.formatDefault(text);
		case ALTERNATE:
			return SqlFormatter.formatAlternative(text);
		default:
			return SqlFormatter.format(text);
		}
	}
}
