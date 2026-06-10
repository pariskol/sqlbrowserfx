package gr.sqlbrowserfx.utils;

import java.util.Collection;
import java.util.HashSet;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class RichTextFXUtils {

    /**
     * Merges two StyleSpans layers into one.
     *
     * <p>
     * This is useful when you have multiple independent highlighters
     * <p>
     * Rules:
     * <ul>
     * <li>Both spans are assumed to cover the same total document length</li>
     * <li>At each segment, styles are unioned (merged)</li>
     * <li>If one layer has no style for a region, only the other is applied</li>
     * </ul>
     * </p>
     *
     * <p>
     * Note: This performs a linear merge similar to a "zip" over both span streams.</p>
     *
     * @param a first style layer (e.g. SQL highlighting)
     * @param b second style layer (e.g. chat highlighting)
     * @return merged StyleSpans containing styles from both inputs
     */
    public static StyleSpans<Collection<String>> mergeTwoSpans(StyleSpans<Collection<String>> a, StyleSpans<Collection<String>> b) {
        var builder = new StyleSpansBuilder<Collection<String>>();
        var ita = a.iterator();
        var itb = b.iterator();

        var sa = ita.hasNext() ? ita.next() : null;
        var sb = itb.hasNext() ? itb.next() : null;

        var ra = sa != null ? sa.getLength() : 0;
        var rb = sb != null ? sb.getLength() : 0;

        while (sa != null || sb != null) {
            var len = Math.min(ra, rb);

            var styles = new HashSet<String>();
            if (sa != null) {
                styles.addAll(sa.getStyle());
            }
            if (sb != null) {
                styles.addAll(sb.getStyle());
            }

            builder.add(styles, len);

            ra -= len;
            rb -= len;

            if (ra == 0) {
                sa = ita.hasNext() ? ita.next() : null;
                ra = sa != null ? sa.getLength() : 0;
            }

            if (rb == 0) {
                sb = itb.hasNext() ? itb.next() : null;
                rb = sb != null ? sb.getLength() : 0;
            }
        }

        return builder.create();
    }
}
