package gr.paris.nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SyntaxUtils {

	public static final String[] FUNCTIONS = getFunctions();
	public static final String[] TYPES = getTypes();
    public static final String[] KEYWORDS = getKeywords();
    
    public static final List<String> KEYWORDS_lIST = new ArrayList<>();

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
//    private static final String BRACE_PATTERN = "\\{|\\}";
//    private static final String BRACKET_PATTERN = "\\[|\\]";
//    private static final String CAST_PATTERN = "<[a-zA-Z0-9,<>]+>";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\'([^\"\\\\]|\\\\.)*\'";
    private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
    public static final String TODO_SINGLE_COMMENT_PATTERN = "//TODO[^\n]*";
    public static final String WARN_SINGLE_COMMENT_PATTERN = "//WARN[^\n]*";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
//    private static final String ANNOTATION_PATTERN = "@.[a-zA-Z0-9]+";
//    private static final String OPERATION_PATTERN = ":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*";
//    private static final String HEX_PATTERN = "#[a-fA-F0-9]+";
//    private static final String NUMBERS_PATTERN = "[0-9]+";
    private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";
    private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";

    public static final Pattern PATTERN = Pattern.compile(
                      "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
//                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
//                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<STRING2>" + STRING_PATTERN_2 + ")"
//                    + "|(?<TODO>" + TODO_SINGLE_COMMENT_PATTERN + ")"
//                    + "|(?<WARN>" + WARN_SINGLE_COMMENT_PATTERN + ")"
//                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
//                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
//                    + "|(?<CAST>" + CAST_PATTERN + ")"
//                    + "|(?<OPERATION>" + OPERATION_PATTERN + ")"
//                    + "|(?<HEX>" + HEX_PATTERN + ")"
//                    + "|(?<NUMBER>" + NUMBERS_PATTERN + ")"
                    + "|(?<METHOD>" + METHOD_PATTERN + ")"
                    + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")"
    );
    
    public static String[] getFunctions() {
    	List<String> list = new ArrayList<>();
    	String[] array = null;
    	try (Stream<String> stream = Files.lines(Paths.get("./sqlite-funcs"))) {
    		list.clear();
			stream.forEach(line -> {
				list.add(line);
			});
			array = new String[list.size()];
			array = list.toArray(array);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return array;
    }
    
    public static String[] getKeywords() {
    	List<String> list = new ArrayList<>();
    	String[] array = null;
    	try (Stream<String> stream = Files.lines(Paths.get("./sql-words"))) {
    		stream.forEach(line -> {
    			list.add(line.toUpperCase());
    			list.add(line.toLowerCase());
			});
			array = new String[list.size()];
			array = list.toArray(array);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return array;
    }
    
    public static String[] getTypes() {
    	List<String> list = new ArrayList<>();
    	String[] array = null;
    	try (Stream<String> stream = Files.lines(Paths.get("./sqlite-types"))) {
			stream.forEach(line -> {
				list.add(line.toUpperCase());
				list.add(line.toLowerCase());
			});
			array = new String[list.size()];
			array = list.toArray(array);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return array;
    }
}
