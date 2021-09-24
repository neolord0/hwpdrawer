package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.util.TextPosition;

public class BreakDrawingException extends Exception {
    private Type type;
    private TextPosition position;
    private int columnIndex;

    public BreakDrawingException() {
        this(TextPosition.ParaList_Start_Position);
    }

    public BreakDrawingException(TextPosition position) {
        this.position = position;
        columnIndex = -1;
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

    public BreakDrawingException forOverPage() {
        type = Type.ForOverPage;
        return this;
    }

    public Type type() {
        return type;
    }

    public TextPosition position() {
        return position;
    }

    public int columnIndex() {
        return columnIndex;
    }

    public BreakDrawingException columnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
        return this;
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
