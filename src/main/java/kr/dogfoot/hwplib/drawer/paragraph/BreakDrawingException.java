package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.TextPosition;

public class BreakDrawingException extends Exception {
    private Type type;
    private TextPosition position;

    public BreakDrawingException() {
        this(TextPosition.ParaList_Start_Position);
    }

    public BreakDrawingException(int paraIndex, int charIndex, int charPosition) {
        position = new TextPosition(paraIndex, charIndex, charPosition);
    }

    public BreakDrawingException(TextPosition position) {
        this.position = position;
    }


    public BreakDrawingException forNewPage() {
        type = Type.ForNewPage;
        return this;
    }

    public BreakDrawingException forEndingPara() {
        type = Type.ForEndingPara;
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

    public BreakDrawingException forOverTextBoxArea() {
        type = Type.ForOverTextBoxArea;
        return this;
    }

    public BreakDrawingException forOverPage(CharInfo firstCharForNextPage) {
        type = Type.ForOverPage;
        return this;
    }

    public Type type() {
        return type;
    }

    public TextPosition position() {
        return position;
    }

    public enum Type {
        ForNewPage,
        ForEndingPara,
        ForEndingTest,
        ForDividingColumn,
        ForOverTextBoxArea,
        ForOverPage;

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

        public boolean isForOverTextBoxArea() {
            return this == ForOverTextBoxArea;
        }

        public boolean isForOverPage() {
            return this == ForOverPage;
        }
    }
}
