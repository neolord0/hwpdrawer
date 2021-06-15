package kr.dogfoot.hwplib.drawer.paragraph;

public class BreakDrawingException extends Exception {
    private Type type;
    private int paraIndex;
    private int charIndex;
    private int charPosition;

    public BreakDrawingException() {
        this(0, 0, 0);
    }

    public BreakDrawingException(int paraIndex, int charIndex, int charPosition) {
        this.paraIndex = paraIndex;
        this.charIndex = charIndex;
        this.charPosition = charPosition;
    }

    public BreakDrawingException forNewPage() {
        type = Type.ForNewPage;
        return this;
    }

    public BreakDrawingException forEndingPara() {
        type = Type.ForEndingPara;;
        return this;
    }

    public BreakDrawingException forEndingTest() {
        type = Type.ForEndingTest;
        return this;
    }

    public BreakDrawingException forDividingColumn() {
        type = Type.ForEndingTest;
        return this;
    }

    public Type type() {
        return type;
    }

    public int paraIndex() {
        return paraIndex;
    }

    public int charIndex() {
        return charIndex;
    }

    public int charPosition() {
        return charPosition;
    }

    public enum Type {
        ForNewPage,
        ForEndingPara,
        ForEndingTest,
        ForDividingColumn;

        public boolean isForNewPage() {
            return this == ForNewPage;
        }

        public boolean isForEndingPara() {
            return this == ForEndingPara;
        }

        public boolean isForEndingTest() {
            return this == ForEndingTest;
        }

        public boolean isForDividingColumn() {
            return this == ForDividingColumn;
        }
    }
}
