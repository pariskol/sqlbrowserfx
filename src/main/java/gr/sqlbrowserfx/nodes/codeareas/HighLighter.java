package gr.sqlbrowserfx.nodes.codeareas;

import java.util.Collection;

import org.fxmisc.richtext.model.StyleSpans;

public interface HighLighter {
	
	StyleSpans<Collection<String>> computeHighlighting(String text);
	
	void enableHighlighting();
	void enableShowLineNumbers(boolean enable);
}
