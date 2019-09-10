package gr.paris.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.fxmisc.richtext.CodeArea;

import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

public class AutoComplete {

    private static final int LIST_ITEM_HEIGHT = 30;
    private static final int LIST_MAX_HEIGHT = 120;
    private static final int WORD_LENGTH_LIMIT = 45;

    public static String getQuery(CodeArea codeArea, int position) {
        int limit = (position > WORD_LENGTH_LIMIT) ? WORD_LENGTH_LIMIT : position;
        String keywords = codeArea.getText().substring(position - limit, position);
        keywords = keywords.replaceAll("\\p{Punct}", " ").trim();
        keywords = keywords.replaceAll("\\n", " ").trim();
        int last = keywords.lastIndexOf(" ");
        return keywords.substring(last + 1);
    }

    public static ListView<String> getSuggestionsList(String query){
        List<String> suggestions = getQuerySuggestions(query);
        ListView<String> suggestionsList = new ListView<>();
        suggestionsList.getItems().clear();
        suggestionsList.getItems().addAll(FXCollections.observableList(new ArrayList<>(new HashSet<>(suggestions))));
        int suggestionsNum = suggestions.size();
        int listViewLength = ((suggestionsNum * LIST_ITEM_HEIGHT) > LIST_MAX_HEIGHT) ? LIST_MAX_HEIGHT : suggestionsNum * LIST_ITEM_HEIGHT;
        suggestionsList.setPrefHeight(listViewLength);
        return suggestionsList;
    }

    private static List<String> getQuerySuggestions(String query) {
        List<String> suggestions = SyntaxUtils.KEYWORDS_lIST.parallelStream()
        							.filter(keyword -> keyword.startsWith(query)).collect(Collectors.toList());
//        suggestions.sort(Comparator.comparing(String::length).thenComparing(String::compareToIgnoreCase));
        return suggestions;
    }
}
