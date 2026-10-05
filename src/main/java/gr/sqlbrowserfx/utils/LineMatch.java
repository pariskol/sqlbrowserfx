package gr.sqlbrowserfx.utils;

public class LineMatch {

    private final int lineNumber;
    private final String content;

    public LineMatch(int lineNumber, String content) {
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return lineNumber + ": " + content;
    }
}
