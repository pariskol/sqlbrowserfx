package gr.sqlbrowserfx.nodes.codeareas;

import java.util.Collection;

import org.fxmisc.richtext.model.StyleSpans;

public interface HighLighter {
	
	public StyleSpans<Collection<String>> computeHighlighting(String text);
	
	public void enableHighlighting();
	public void enableShowLineNumbers(boolean enable);
}
