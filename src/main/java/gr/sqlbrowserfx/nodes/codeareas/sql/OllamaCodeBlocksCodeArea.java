package gr.sqlbrowserfx.nodes.codeareas.sql;

import gr.sqlbrowserfx.nodes.ContextMenuOwner;
import gr.sqlbrowserfx.nodes.SearchAndReplacePopOver;
import gr.sqlbrowserfx.nodes.codeareas.HighLighter;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import gr.sqlbrowserfx.utils.RichTextFXUtils;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 *
 * @author paris
 */
public class OllamaCodeBlocksCodeArea extends CodeArea implements HighLighter, ContextMenuOwner {

    private static final Pattern SQL_BLOCK = Pattern.compile("(?s)```sql\\R(.*?)\\R```");
    private static final String CHAT_PATTERN = "(?m)^\\s*(user:|assistant:)";
    private static final Pattern CHAT_HIGHLIGHT_PATTERN = Pattern.compile("(?<CHAT>" + CHAT_PATTERN + ")");
    private final SqlCodeAreaSyntaxProvider syntaxProvider = new SqlCodeAreaSyntaxProvider();
    private final SimpleBooleanProperty isTextSelectedProperty = new SimpleBooleanProperty(false);
    protected SearchAndReplacePopOver searchAndReplacePopOver;

    public OllamaCodeBlocksCodeArea() {
        this.enableHighlighting();
        this.setContextMenu(this.createContextMenu());
        this.selectedTextProperty().addListener((ob, ov, nv) -> this.isTextSelectedProperty.set(!nv.isEmpty()));
    }

    public SimpleBooleanProperty isTextSelectedProperty() {
        return isTextSelectedProperty;
    }

    @Override
    public void enableHighlighting() {
        this.multiPlainChanges().successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
    }

    private StyleSpans<Collection<String>> computeChatHighlighting(String text) {
        var matcher = CHAT_HIGHLIGHT_PATTERN.matcher(text);
        int lastEnd = 0;
        var spansBuilder = new StyleSpansBuilder<Collection<String>>();

        while (matcher.find()) {
            var styleClass = hasGroup(matcher, "CHAT") ? "method" : null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }

    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {

        var sqlSpans = computeSqlBlockHighlighting(text);
        var chatSpans = computeChatHighlighting(text);

        return RichTextFXUtils.mergeTwoSpans(sqlSpans, chatSpans);
    }

    public StyleSpans<Collection<String>> computeSqlBlockHighlighting(String text) {
        var sqlMatcher = SQL_BLOCK.matcher(text);
        var spansBuilder = new StyleSpansBuilder<Collection<String>>();
        var lastEnd = 0;

        while (sqlMatcher.find()) {
            spansBuilder.add(Collections.emptyList(), sqlMatcher.start() - lastEnd);
            var block = sqlMatcher.group();
            var sql = sqlMatcher.group(1);
            var sqlStartInBlock = block.indexOf(sql);
            spansBuilder.add(Collections.emptyList(), sqlStartInBlock);

            var sqlSpans = computeSqlHighlighting(sql);
            spansBuilder.addAll(sqlSpans);

            int closingFenceLength = block.length() - sqlStartInBlock - sql.length();
            spansBuilder.add(Collections.emptyList(), closingFenceLength);

            lastEnd = sqlMatcher.end();
        }

        spansBuilder.addAll(computeChatHighlighting(text.substring(lastEnd)));
        return spansBuilder.create();
    }

    private StyleSpans<Collection<String>> computeSqlHighlighting(String sql) {
        var matcher = syntaxProvider.getPatternMatcher(sql);
        var lastKwEnd = 0;
        var spansBuilder = new StyleSpansBuilder<Collection<String>>();

        while (matcher.find()) {
            var styleClass
                    = hasGroup(matcher, "COMMENT") ? "comment"
                    : hasGroup(matcher, "STRING") ? "string"
                    : hasGroup(matcher, "STRING2") ? "string"
                    : hasGroup(matcher, "STRING3") ? "string"
                    : hasGroup(matcher, "DIAMOND") ? "diamond"
                    : hasGroup(matcher, "ANNOTATION") ? "annotation"
                    : hasGroup(matcher, "METHOD") ? "method"
                    : hasGroup(matcher, "FUNCTION") ? "function"
                    : hasGroup(matcher, "KEYWORD") ? "keyword"
                    : hasGroup(matcher, "PAREN") ? "paren"
                    : hasGroup(matcher, "SEMICOLON") ? "semicolon"
                    : null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), sql.length() - lastKwEnd);

        return spansBuilder.create();
    }

    @Override
    public void enableShowLineNumbers(boolean enable) {
        if (enable) {
            this.setParagraphGraphicFactory(new SimpleLineNumberFactory(this));
        } else {
            this.setParagraphGraphicFactory(null);
        }
    }

    @Override
    public ContextMenu createContextMenu() {
        var menu = new ContextMenu();

        var copy = new MenuItem("Copy", JavaFXUtils.createIcon("/icons/copy.png"));
        copy.setOnAction(event -> this.copy());
        copy.disableProperty().bind(this.isTextSelectedProperty().not());
        var searchAndReplace = new MenuItem("Search...", JavaFXUtils.createIcon("/icons/magnify.png"));
        searchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

        menu.getItems().addAll(copy, searchAndReplace);
        return menu;
    }

    protected void showSearchAndReplacePopup() {
        if (searchAndReplacePopOver == null) {
            searchAndReplacePopOver = new SearchAndReplacePopOver(this);
        }
        if (!this.getSelectedText().isEmpty()) {
            searchAndReplacePopOver.getFindField().setText(this.getSelectedText());
            searchAndReplacePopOver.getFindField().selectAll();
        }
        var boundsInScene = this.localToScreen(this.getBoundsInLocal());
        searchAndReplacePopOver.getFindField().requestFocus();
        searchAndReplacePopOver.show(getParent(), boundsInScene.getMaxX() - 400, boundsInScene.getMinY());
    }

    private boolean hasGroup(Matcher matcher, String group) {
        try {
            return matcher.group(group) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
