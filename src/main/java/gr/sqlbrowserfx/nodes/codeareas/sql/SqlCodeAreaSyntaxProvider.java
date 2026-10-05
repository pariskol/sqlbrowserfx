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
import java.util.stream.Stream;

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

    private static final Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

    private static String DB_TYPE = "";
    private static List<String> FUNCTIONS;
    private static List<String> TYPES;
    private static List<String> KEYWORDS = Arrays.asList(new String[]{
        "add", "ADD", "all", "ALL", "alter", "ALTER", "and", "AND", "any", "ANY",
        "as", "AS", "asc", "ASC", "backup", "BACKUP", "between", "BETWEEN", "by", "BY",
        "case", "CASE", "check", "CHECK", "column", "COLUMN", "constraint", "CONSTRAINT", "create", "CREATE",
        "database", "DATABASE", "default", "DEFAULT", "delete", "DELETE", "desc", "DESC", "distinct", "DISTINCT",
        "drop", "DROP", "else", "ELSE", "end", "END", "exists", "EXISTS", "foreign", "FOREIGN",
        "from", "FROM", "full", "FULL", "group", "GROUP", "having", "HAVING", "in", "IN",
        "index", "INDEX", "inner", "INNER", "insert", "INSERT", "into", "INTO", "is", "IS",
        "join", "JOIN", "key", "KEY", "left", "LEFT", "like", "LIKE", "limit", "LIMIT",
        "not", "NOT", "null", "NULL", "on", "ON", "or", "OR", "order", "ORDER",
        "outer", "OUTER", "primary", "PRIMARY", "procedure", "PROCEDURE", "right", "RIGHT", "rownum", "ROWNUM",
        "select", "SELECT", "set", "SET", "table", "TABLE", "top", "TOP", "truncate", "TRUNCATE",
        "union", "UNION", "unique", "UNIQUE", "update", "UPDATE", "values", "VALUES", "view", "VIEW",
        "where", "WHERE", "with", "WITH", "grant", "GRANT", "revoke", "REVOKE", "commit", "COMMIT",
        "rollback", "ROLLBACK", "savepoint", "SAVEPOINT", "transaction", "TRANSACTION", "cascade", "CASCADE", "restrict", "RESTRICT",
        "replace", "REPLACE", "declare", "DECLARE", "cursor", "CURSOR", "fetch", "FETCH", "open", "OPEN",
        "close", "CLOSE", "loop", "LOOP", "while", "WHILE", "if", "IF", "then", "THEN",
        "elsif", "ELSIF", "return", "RETURN", "language", "LANGUAGE", "function", "FUNCTION", "trigger", "TRIGGER"
    });

    private static final Set<Keyword> KEYWORDS_lIST = new LinkedHashSet<>(KEYWORDS.stream().map(word -> new Keyword(word, KeywordType.KEYWORD)).toList());
    private static final Map<String, Set<String>> COLUMNS_MAP = new HashMap<>();

    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\'([^\'\\\\]|\\\\.)*\'";
    private static final String STRING_PATTERN_2 = "\"([^\"\\\\]|\\\\.)*\"";
//    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String COMMENT_PATTERN = "--[^\n]*";
//    private static final String NUMBERS_PATTERN = "[0-9]+";
    private static final String METHOD_PATTERN = "\\.[a-zA-Z0-9_]+";

    private static String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static String FUNCTIONS_PATTERN;
    private static Pattern PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<PAREN>" + PAREN_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<STRING2>" + STRING_PATTERN_2 + ")" + "|(?<COMMENT>" + COMMENT_PATTERN + ")" + "|(?<METHOD>" + METHOD_PATTERN + ")"
    );

    public static void init(String dbType) {
        DB_TYPE = dbType;
        init();
    }

    private static void init() {
        var funcs = getAutocomplteWords("funcs");
        FUNCTIONS = funcs.stream().map(dto -> (String) dto.get("name")).toList();
        TYPES = getAutocomplteWords("types").stream().map(dto -> (String) dto.get("name")).toList();
        KEYWORDS = Stream.concat(
                KEYWORDS.stream(),
                getAutocomplteWords("sql").stream().map(dto -> (String) dto.get("name"))
        )
                .distinct()
                .collect(Collectors.toList());

        KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        FUNCTIONS_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
        PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<PAREN>" + PAREN_PATTERN + ")"
                + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" + "|(?<STRING>" + STRING_PATTERN + ")"
                + "|(?<STRING2>" + STRING_PATTERN_2 + ")" + "|(?<COMMENT>" + COMMENT_PATTERN + ")" + "|(?<METHOD>" + METHOD_PATTERN + ")"
                + "|(?<FUNCTION>" + FUNCTIONS_PATTERN + ")");

        KEYWORDS_lIST.addAll(SqlCodeAreaSyntaxProvider.KEYWORDS.stream().map(kw -> new Keyword(kw, KeywordType.KEYWORD)).toList());
        KEYWORDS_lIST.addAll(SqlCodeAreaSyntaxProvider.TYPES.stream().map(kw -> new Keyword(kw, KeywordType.TYPE)).toList());
        KEYWORDS_lIST.addAll(funcs.stream().map(dto -> new Keyword((String) dto.get("name"), (String) dto.get("description"), KeywordType.FUNCTION)).toList());
    }

    private static List<HashMap<String, Object>> getAutocomplteWords(String category) {
        var list = new ArrayList<HashMap<String, Object>>();
        try {
            SqlBrowserFXAppManager.getConfigSqlConnector().executeQuery(
                    "select name, description from autocomplete where category= ? and type in (?,'sql') order by name",
                    Arrays.asList(new Object[]{category, DB_TYPE}), rset -> {
                try {
                    var dto = DTOMapper.map(rset);
                    list.add(dto);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }

        return list;
    }

    public static void bind(List<Keyword> list) {
        KEYWORDS_lIST.addAll(list);
    }

    public static void bind(String table, List<String> columns) {
        COLUMNS_MAP.put(table, new HashSet<>(columns));
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
