package gr.sqlbrowserfx.nodes.sqlCodeArea;

import java.util.List;
import java.util.stream.Collectors;

import org.fxmisc.richtext.CodeArea;

public class CodeAreaAutoComplete {

    private static final int WORD_LENGTH_LIMIT = 45;

    public static String getQuery(CodeArea codeArea, int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = codeArea.getText().substring(position - limit, position);
        keywords = keywords.replaceAll("\\p{Punct}", " ").trim();
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1);
    }

    public static List<String> getQuerySuggestions(String query) {
        List<String> suggestions = CodeAreaSyntax.KEYWORDS_lIST.parallelStream()
        							.filter(keyword -> keyword.startsWith(query)).collect(Collectors.toList());
//        suggestions.sort(Comparator.comparing(String::length).thenComparing(String::compareToIgnoreCase));
        return suggestions;
    }
}
