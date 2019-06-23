package gr.paris.nodes;

import java.util.Arrays;
import java.util.List;

public class Keywords {

    /**
     * Get All Keywords from classes text file and split them to classes import path and keywords
     */
    public static void onKeywordsBind() {
        SyntaxUtils.KEYWORDS_lIST.addAll(Arrays.asList(SyntaxUtils.KEYWORDS));
        SyntaxUtils.KEYWORDS_lIST.addAll(Arrays.asList(SyntaxUtils.FUNCTIONS));
        SyntaxUtils.KEYWORDS_lIST.addAll(Arrays.asList(SyntaxUtils.TYPES));
    }
    
    public static void bind(List<String> list) {
        SyntaxUtils.KEYWORDS_lIST.addAll(list);
    }
}
