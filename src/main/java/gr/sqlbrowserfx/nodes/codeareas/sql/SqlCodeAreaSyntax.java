package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;

public class SqlCodeAreaSyntax {

	private static Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);
	
	private static String DB_TYPE = "";
	public static  String[] FUNCTIONS;
	public static  String[] TYPES;
	public static  String[] KEYWORDS;

	public static final Set<String> KEYWORDS_lIST = new LinkedHashSet<>();
	public static final Map<String, List<String>> COLUMNS_MAP = new HashMap<>();

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
	public static Pattern PATTERN;

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
		PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" 
								+ "|(?<PAREN>" + PAREN_PATTERN + ")"
								+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" 
								+ "|(?<STRING>" + STRING_PATTERN + ")" 
								+ "|(?<STRING2>" + STRING_PATTERN_2 + ")"
								+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
								+ "|(?<METHOD>" + METHOD_PATTERN + ")" 
								+ "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");
		
		KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntax.KEYWORDS));
		KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntax.TYPES));
        KEYWORDS_lIST.addAll(Arrays.asList(SqlCodeAreaSyntax.FUNCTIONS));
	}
	
	private static String[] getAutocomplteWords(String category) {
		List<String> list = new ArrayList<>();
		try {
			SqlBrowserFXAppManager.getConfigSqlConnector()
								  .executeQuery("select name from autocomplete where category= ? and type in (?,'sql') order by name", 
										  Arrays.asList(new Object[]{category, DB_TYPE}), rset -> {
											try {
												HashMap<String, Object> dto = DTOMapper.map(rset);
												list.add((String)dto.get("name"));
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
	
    public static void bind(List<String> list) {
        KEYWORDS_lIST.addAll(list);
    }
    
    public static void bind(String table, List<String> columns) {
		COLUMNS_MAP.put(table, columns);
	}
    
}
