package gr.sqlbrowserfx.nodes.codeareas.typescript;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.sqlbrowserfx.nodes.codeareas.CodeAreaSyntaxProvider;
import gr.sqlbrowserfx.nodes.codeareas.FormatterMode;
import gr.sqlbrowserfx.nodes.codeareas.Keyword;
import gr.sqlbrowserfx.nodes.codeareas.KeywordType;

public class TypeScriptCodeAreaSyntaxProvider implements CodeAreaSyntaxProvider<String> {


	private static final String[] FUNCTIONS = new String[] {};
	private static final String[] KEYWORDS = new String[] {
		    // JavaScript keywords
		    "break", "case", "catch", "class", "const",
		    "continue", "debugger", "default", "delete", "do",
		    "else", "export", "extends", "finally", "for",
		    "function", "if", "import", "in", "instanceof",
		    "new", "return", "super", "switch", "this",
		    "throw", "try", "typeof", "var", "void",
		    "while", "with", "yield",

		    // Strict mode / future reserved (JS)
		    "enum", "implements", "interface", "let",
		    "package", "private", "protected", "public",
		    "static", "await", "async",

		    // Literals
		    "true", "false", "null",

		    // TypeScript keywords
		    "any", "boolean", "constructor", "declare",
		    "get", "module", "namespace", "number",
		    "readonly", "require", "set", "string",
		    "symbol", "type", "from", "of",

		    // TypeScript type system / modifiers
		    "abstract", "as", "asserts", "bigint",
		    "infer", "is", "keyof", "never",
		    "object", "override", "readonly",
		    "unknown", "unique", "using",

		    // TS access & class modifiers
		    "protected", "private", "public",

		    // TS utility / contextual
		    "global", "implements", "interface",

		    // JSX / TSX (often included)
		    "jsx", "intrinsic"
		};

	
	private static final Set<Keyword> KEYWORDS_lIST = new LinkedHashSet<>(
			Arrays.asList(KEYWORDS).stream().map(word -> new Keyword(word, KeywordType.KEYWORD)).toList());

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String ANNOTATION_PATTERN = "@.[a-zA-Z0-9]+";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String STRING_PATTERN_2 = "'([^'\\\\]|\\\\.)*'";
	private static final String STRING_PATTERN_3 = "(?s)`.*?`";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
    		                          + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)
    private static final String FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
	private static final String METHOD_PATTERN = "\\.[a-zA-Z_][a-zA-Z0-9_]*";
	// Match <tag ...> but only outside quotes
	private static final String DIAMOND_PATTERN = "</?|>";
	
	private static final Pattern PATTERN = Pattern.compile(
	        "(?<COMMENT>" + COMMENT_PATTERN + ")"
	      + "|(?<STRING>" + STRING_PATTERN + ")"
	      + "|(?<STRING2>" + STRING_PATTERN_2 + ")"
	      + "|(?<STRING3>" + STRING_PATTERN_3 + ")"
	      + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
	      + "|(?<METHOD>" + METHOD_PATTERN + ")"
	      + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
	      + "|(?<PAREN>" + PAREN_PATTERN + ")"
	      + "|(?<BRACE>" + BRACE_PATTERN + ")"
	      + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
	      + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
	      + "|(?<DIAMOND>" + DIAMOND_PATTERN + ")"
	      + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")"
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
		return this.format(text, FormatterMode.DEFAULT);
	}
	
	@Override
	public String format(String text, FormatterMode mode) {
		var out = new StringBuilder();
        var indentLevel = 0;

        var inSingleQuote = false;
        var inDoubleQuote = false;
        var inTemplateString = false;
        var inLineComment = false;
        var inBlockComment = false;
        var escape = false;

        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            var next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

            // Handle comments
            if (!inSingleQuote && !inDoubleQuote && !inTemplateString) {
                if (!inBlockComment && !inLineComment && c == '/' && next == '/') {
                    inLineComment = true;
                } else if (!inBlockComment && !inLineComment && c == '/' && next == '*') {
                    inBlockComment = true;
                }
            }

            if (inLineComment) {
                out.append(c);
                if (c == '\n') {
                    inLineComment = false;
                    appendIndent(out, indentLevel);
                }
                continue;
            }

            if (inBlockComment) {
                out.append(c);
                if (c == '*' && next == '/') {
                    out.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // Handle strings
            if (!escape) {
                if (c == '\'' && !inDoubleQuote && !inTemplateString) {
                    inSingleQuote = !inSingleQuote;
                } else if (c == '"' && !inSingleQuote && !inTemplateString) {
                    inDoubleQuote = !inDoubleQuote;
                } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                    inTemplateString = !inTemplateString;
                }
            }

            escape = (c == '\\' && !escape);

            if (inSingleQuote || inDoubleQuote || inTemplateString) {
                out.append(c);
                continue;
            }

            // Formatting rules
            switch (c) {
                case '{':
                    out.append(" {\n");
                    indentLevel++;
                    appendIndent(out, indentLevel);
                    break;

                case '}':
                    indentLevel--;
                    out.append("\n");
                    appendIndent(out, indentLevel);
                    out.append("}");
                    break;

                case ';':
                    out.append(";\n");
                    appendIndent(out, indentLevel);
                    break;

                case '\n':
                case '\r':
                    // ignore original newlines
                    break;

                default:
                    out.append(c);
            }
        }

        return out.toString().trim();
	}
	
	private void appendIndent(StringBuilder out, int level) {
        for (int i = 0; i < level; i++) {
            out.append("    "); // 4 spaces per indent level
        }
    }
}
