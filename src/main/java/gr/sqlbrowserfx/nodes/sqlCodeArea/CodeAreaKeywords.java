package gr.sqlbrowserfx.nodes.sqlCodeArea;

import java.util.Arrays;
import java.util.List;

public class CodeAreaKeywords {

    /**
     * Get All Keywords from classes text file and split them to classes import path and keywords
     */
    public static void onKeywordsBind() {
        CodeAreaSyntax.KEYWORDS_lIST.addAll(Arrays.asList(CodeAreaSyntax.KEYWORDS));
        CodeAreaSyntax.KEYWORDS_lIST.addAll(Arrays.asList(CodeAreaSyntax.FUNCTIONS));
        CodeAreaSyntax.KEYWORDS_lIST.addAll(Arrays.asList(CodeAreaSyntax.TYPES));
    }
    
    public static void bind(List<String> list) {
        CodeAreaSyntax.KEYWORDS_lIST.addAll(list);
    }
}
