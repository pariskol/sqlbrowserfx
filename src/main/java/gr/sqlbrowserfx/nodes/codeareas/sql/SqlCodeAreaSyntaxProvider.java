package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.nodes.codeareas.CodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.codeareas.FormatterMode;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;
import gr.sqlbrowserfx.utils.SqlFormatter;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;

public class SqlCodeAreaSyntaxProvider implements CodeAreaSyntaxProvider<String> {

	private static Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	private static String DB_TYPE = "";
	private static String[] FUNCTIONS;
	private static String[] TYPES;
	private static String[] KEYWORDS;

	private static final Set<Keyword> KEYWORDS_lIST = new LinkedHashSet<>();
	private static final Map<String, Set<String>> COLUMNS_MAP = new HashMap<>();

	private static final String PAREN_PATTERN = "\\(|\\)";
	private static final String SEMICOLON_PATTERN = "\\;";
	private static final String STRING_PATTERN = "\'([^\'\\\\]|\\\\.)*\'";
	private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
	private static final String COMMENT_PATTERN = "--[^\n]*";
//    private static final String NUMBERS_PATTERN = "[0-9]+";
	private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";

	private static String KEYWORD_PATTERN;
	private static String FUNCTIONS_PATTERN;
	private static Pattern PATTERN;

	public static void init(String dbType) {
		DB_TYPE = dbType;
		init();
	}

	private static void init() {
		FUNCTIONS = getAutocomplteWords("funcs");
		TYPES = getAutocomplteWords("types");
		KEYWORDS = getAutocomplteWords("sql");

		KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
		FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
		PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<PAREN>" + PAREN_PATTERN + ")"
				+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" + "|(?<STRING>" + STRING_PATTERN + ")" + "|(?<STRING2>"
				+ STRING_PATTERN_2 + ")" + "|(?<COMMENT>" + COMMENT_PATTERN + ")" + "|(?<METHOD>" + METHOD_PATTERN + ")"
				+ "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");

		KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntaxProvider.KEYWORDS).stream()
				.map(kw -> new Keyword(kw, KeywordType.KEYWORD)).collect(Collectors.toList()));
		KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntaxProvider.TYPES).stream()
				.map(kw -> new Keyword(kw, KeywordType.TYPE)).collect(Collectors.toList()));
		KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntaxProvider.FUNCTIONS).stream()
				.map(kw -> new Keyword(kw, KeywordType.FUNCTION)).collect(Collectors.toList()));
	}

	private static String[] getAutocomplteWords(String category) {
		List<String> list = new ArrayList<>();
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector().executeQuery(
					"select name from autocomplete where category= ? and type in (?,'sql') order by name",
					Arrays.asList(new Object[] { category, DB_TYPE }), rset -> {
						try {
							HashMap<String, Object> dto = DTOMapper.map(rset);
							list.add((String) dto.get("name"));
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					});
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}

		String[] array = new String[list.size()];
		return list.toArray(array);
	}

	public static void bind(List<Keyword> list) {
		KEYWORDS_lIST.addAll(list);
	}

	public static void bind(String table, List<String> columns) {
		COLUMNS_MAP.put(table, new HashSet<String>(columns));
	}
	
	@Override
	public Set<Keyword> getKeywords() {
		return KEYWORDS_lIST;
	}
	
	@Override
	public Set<Keyword> getKeywords(KeywordType type, String tableAlias) {
		switch (type) {
		case COLUMN:
			return COLUMNS_MAP.get(tableAlias) != null ? COLUMNS_MAP.get(tableAlias).stream().map(col -> new Keyword(col, KeywordType.COLUMN)).collect(Collectors.toSet()) : new HashSet<>();
		case KEYWORD:
		default:
			return getKeywords();
		}
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
