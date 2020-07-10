package gr.bashfx;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BashSyntaxUtils {

	private static Logger logger = LoggerFactory.getLogger("SQLBROWSER");
	public static final List<String> KEYWORDS_lIST = new ArrayList<>();

	public static final String[] FUNCTIONS = getAutocomplteWords(Arrays.asList("ls /bin"), false);
	public static final String[] TEMPLATES = getAutocomplteWords(Arrays.asList("grep -oi '.*=' /home/paris/eclipse-workspace/bashfx/templates.properties | sed 's/=//g' | sed 's/^/template->/g'"), true);
	public static final String[] KEYWORDS = getAutocomplteWords(Arrays.asList("compgen -k"), false);


	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
//    private static final String CAST_PATTERN = "<[a-zA-Z0-9,<>]+>";
	private static final String SEMICOLON_PATTERN = "\\;";
	private static final String STRING_PATTERN = "\'([^\'\\\\]|\\\\.)*\'";
	private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
	public static final String TODO_SINGLE_COMMENT_PATTERN = "//TODO[^\n]*";
	public static final String WARN_SINGLE_COMMENT_PATTERN = "//WARN[^\n]*";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
	private static final String COMMENT_PATTERN = "#[^\n]*";
	private static final String DOLLAR_PATTERN = "\\$[^ |\n|\"|'|(|//]*";
	private static final String MINUS_PATTERN = "-. | -.. ";
	private static final String VAR_PATTERN = "\b$[^\n]*\b";
//    private static final String ANNOTATION_PATTERN = "@.[a-zA-Z0-9]+";
//    private static final String OPERATION_PATTERN = ":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*";
//    private static final String HEX_PATTERN = "#[a-fA-F0-9]+";
//    private static final String NUMBERS_PATTERN = "[0-9]+";
	private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";
	private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";

	public static final Pattern PATTERN = Pattern
			.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" + "|(?<STRING>" + STRING_PATTERN + ")" + "|(?<STRING2>"
					+ STRING_PATTERN_2 + ")"
//                    + "|(?<TODO>" + TODO_SINGLE_COMMENT_PATTERN + ")"
//                    + "|(?<WARN>" + WARN_SINGLE_COMMENT_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<DOLLAR>" + DOLLAR_PATTERN + ")"
                    + "|(?<MINUS>" + MINUS_PATTERN + ")"
                    + "|(?<VAR>" + VAR_PATTERN + ")"
//                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
//                    + "|(?<CAST>" + CAST_PATTERN + ")"
//                    + "|(?<OPERATION>" + OPERATION_PATTERN + ")"
//                    + "|(?<HEX>" + HEX_PATTERN + ")"
//                    + "|(?<NUMBER>" + NUMBERS_PATTERN + ")"
					+ "|(?<METHOD>" + METHOD_PATTERN + ")" + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");
	
	public static HashMap<String, String> TEMPLATES_MAP;
	
	static {
		KEYWORDS_lIST.addAll(Arrays.asList(KEYWORDS));
		KEYWORDS_lIST.addAll(Arrays.asList(FUNCTIONS));
		KEYWORDS_lIST.addAll(Arrays.asList(TEMPLATES));
		
		initTemplatesMap();
	}
	
	private static void initTemplatesMap()
	{
		TEMPLATES_MAP = new HashMap<>();
		Properties props = new Properties();

		try (InputStream inputStream = new FileInputStream("./templates.properties")) {
			props.load(inputStream);
			for (Object key : props.keySet()) {
				TEMPLATES_MAP.put("template->" + (String) key, (String)props.get(key));
			}

		} catch (IOException e) {

		}
		// print everything
//		props.forEach((k, v) -> System.out.println("Key : " + k + ", Value : " + v));
    }
	private static String[] getAutocomplteWords(List<String> commands, boolean matchAll) {
		List<String> list = new ArrayList<>();

		for (String command : commands) {
			try {
				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command("bash", "-c", command);
				Process process = processBuilder.start();
	
				StringBuilder output = new StringBuilder();
	
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
	
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line + "\n");
					if (line.chars().allMatch(Character::isLetter) || matchAll)
						list.add(line);
				}
	
				int exitVal = process.waitFor();
				if (exitVal == 0) {
					
				} else {
					//abnormal...
				}
	
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String[] array = new String[KEYWORDS_lIST.size()];
		return list.toArray(array);
	}
}
