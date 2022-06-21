package gr.sqlbrowserfx.nodes.codeareas.sql;

import java.util.function.IntFunction;

import org.fxmisc.richtext.CodeArea;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class SimpleLineNumberFactory implements IntFunction<Node>{
	
	private CodeArea codeArea;

	public SimpleLineNumberFactory(CodeArea codeArea) {
		this.codeArea = codeArea;
	}
	
    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        IntFunction<String>  format = (dgts) -> "%1$" + dgts + "s";
        return String.format(format.apply(digits), x);
    }
    
	 @Override
	    public Node apply(int idx) {
		 	Val<Integer> nParagraphs = LiveList.sizeOf(codeArea.getParagraphs());
	        Val<String> formatted = nParagraphs.map(n -> format(idx+1, n));

	        Label lineNo = new Label();
	        lineNo.setPadding(new Insets(0, 15, 0, 0));
	        lineNo.setAlignment(Pos.TOP_RIGHT);
	        lineNo.getStyleClass().add("lineno");

	        // bind label's text to a Val that stops observing codeArea's paragraphs
	        // when lineNo is removed from scene
	        lineNo.textProperty().bind(formatted.conditionOnShowing(lineNo));

	        return lineNo;
	    }

}
