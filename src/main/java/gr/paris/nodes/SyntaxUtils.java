package gr.paris.nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SyntaxUtils {

    static {
    	initialize();
    }
    
	public static String[] FUNCTIONS;
	
	public static String[] TYPES;
	
    public static String[] KEYWORDS;

    public static final List<String> KEYWORDS_lIST = new ArrayList<>();
    public static final HashMap<String,String> CLASSES_LIST = new HashMap<>();

    private static String KEYWORD_PATTERN;
    private static String FUNCTIONS_PATTERN;
    private static final String PAREN_PATTERN = "\\(|\\)";
	private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String CAST_PATTERN = "<[a-zA-Z0-9,<>]+>";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\'([^\"\\\\]|\\\\.)*\'";
    private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
    public static final String TODO_SINGLE_COMMENT_PATTERN = "//TODO[^\n]*";
    public static final String WARN_SINGLE_COMMENT_PATTERN = "//WARN[^\n]*";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String ANNOTATION_PATTERN = "@.[a-zA-Z0-9]+";
    private static final String OPERATION_PATTERN = ":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*";
    private static final String HEX_PATTERN = "#[a-fA-F0-9]+";
    private static final String NUMBERS_PATTERN = "[0-9]+";
    private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";

    
    public static Pattern PATTERN;
    
    public static void initialize() {
    	List<String> list = new ArrayList<>();
    	try (Stream<String> stream = Files.lines(Paths.get("./sql-words"))) {
    		stream.forEach(line -> {
    			list.add(line.toUpperCase());
    			list.add(line.toLowerCase());
			});
    		KEYWORDS = new String[list.size()];
    		KEYWORDS = list.toArray(KEYWORDS);
    		KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
		} catch (IOException e) {
			e.printStackTrace();
		}
    	try (Stream<String> stream = Files.lines(Paths.get("./sqlite-funcs"))) {
    		list.clear();
			stream.forEach(line -> {
				list.add(line);
			});
			FUNCTIONS = new String[list.size()];
			FUNCTIONS = list.toArray(FUNCTIONS);
			FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
		} catch (IOException e) {
			e.printStackTrace();
		}
    	try (Stream<String> stream = Files.lines(Paths.get("./sqlite-types"))) {
    		list.clear();
			stream.forEach(line -> {
				list.add(line.toUpperCase());
				list.add(line.toLowerCase());
			});
			TYPES = new String[list.size()];
			TYPES = list.toArray(TYPES);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	PATTERN = Pattern.compile(
                "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
              + "|(?<PAREN>" + PAREN_PATTERN + ")"
//              + "|(?<BRACE>" + BRACE_PATTERN + ")"
//              + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
              + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
              + "|(?<STRING>" + STRING_PATTERN + ")"
              + "|(?<STRING2>" + STRING_PATTERN_2 + ")"
//              + "|(?<TODO>" + TODO_SINGLE_COMMENT_PATTERN + ")"
//              + "|(?<WARN>" + WARN_SINGLE_COMMENT_PATTERN + ")"
//              + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
//              + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
//              + "|(?<CAST>" + CAST_PATTERN + ")"
//              + "|(?<OPERATION>" + OPERATION_PATTERN + ")"
//              + "|(?<HEX>" + HEX_PATTERN + ")"
//              + "|(?<NUMBER>" + NUMBERS_PATTERN + ")"
              + "|(?<METHOD>" + METHOD_PATTERN + ")"
              + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")"
);
    }
}
