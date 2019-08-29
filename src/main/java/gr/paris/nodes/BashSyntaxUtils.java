package gr.paris.nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.conn.SqliteConnector;
import gr.sqlfx.utils.DTOMapper;

public class BashSyntaxUtils {

	private static SqlConnector sqlConnector = new SqliteConnector("./sqlbrowser.db");
	private static Logger logger = LoggerFactory.getLogger("SQLBROWSER");
	
	public static final String[] FUNCTIONS = getAutocomplteWords("bash_function");
//	public static final String[] TYPES = getAutocomplteWords("types");
	public static final String[] KEYWORDS = getAutocomplteWords("bash");

	public static final List<String> KEYWORDS_lIST = new ArrayList<>();

	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PAREN_PATTERN = "\\(|\\)";
//    private static final String BRACE_PATTERN = "\\{|\\}";
//    private static final String BRACKET_PATTERN = "\\[|\\]";
//    private static final String CAST_PATTERN = "<[a-zA-Z0-9,<>]+>";
	private static final String SEMICOLON_PATTERN = "\\;";
	private static final String STRING_PATTERN = "\'([^\'\\\\]|\\\\.)*\'";
	private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
	public static final String TODO_SINGLE_COMMENT_PATTERN = "//TODO[^\n]*";
	public static final String WARN_SINGLE_COMMENT_PATTERN = "//WARN[^\n]*";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
	private static final String COMMENT_PATTERN = "#[^\n]*";
	private static final String VAR_PATTERN = "\b$[^\n]*\b";
//    private static final String ANNOTATION_PATTERN = "@.[a-zA-Z0-9]+";
//    private static final String OPERATION_PATTERN = ":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*";
//    private static final String HEX_PATTERN = "#[a-fA-F0-9]+";
//    private static final String NUMBERS_PATTERN = "[0-9]+";
	private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";
	private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";

	public static final Pattern PATTERN = Pattern
			.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<PAREN>" + PAREN_PATTERN + ")"
//                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
//                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" + "|(?<STRING>" + STRING_PATTERN + ")" + "|(?<STRING2>"
					+ STRING_PATTERN_2 + ")"
//                    + "|(?<TODO>" + TODO_SINGLE_COMMENT_PATTERN + ")"
//                    + "|(?<WARN>" + WARN_SINGLE_COMMENT_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<VAR>" + VAR_PATTERN + ")"
//                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
//                    + "|(?<CAST>" + CAST_PATTERN + ")"
//                    + "|(?<OPERATION>" + OPERATION_PATTERN + ")"
//                    + "|(?<HEX>" + HEX_PATTERN + ")"
//                    + "|(?<NUMBER>" + NUMBERS_PATTERN + ")"
					+ "|(?<METHOD>" + METHOD_PATTERN + ")" + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");
	
	private static String[] getAutocomplteWords(String category) {
		List<String> list = new ArrayList<>();
		try {
			sqlConnector.executeQuery("select name from autocomplete where category= ?", Arrays.asList(new String[]{category}), rset -> {
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
}
