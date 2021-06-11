package kr.dogfoot.hwplib.drawer.paragraph;

public class BreakingDrawException extends Exception {
    private Type type;
    private int paraIndex;
    private int charIndex;
    private int charPosition;
    private long startY;

    public BreakingDrawException(int paraIndex, int charIndex, int charPosition, long startY) {
        this.paraIndex = paraIndex;
        this.charIndex = charIndex;
        this.charPosition = charPosition;
        this.startY = startY;
    }

    public BreakingDrawException forRedrawing() {
        type = Type.ForRedrawing;
        return this;
    }

    public BreakingDrawException forDistributionColumn() {
        type = Type.ForDistributionColumn;
        return this;
    }

    public BreakingDrawException forEndingPara() {
        type = Type.ForEndingPara;;
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
    
    public long startY() {
        return startY;
    }

    public enum Type {
        ForRedrawing,
        ForDistributionColumn,
        ForEndingPara;

        public boolean isForRedrawing() {
            return this == ForRedrawing;
        }

        public boolean isForDistributionColumn() {
            return this == ForDistributionColumn;
        }

        public boolean isForEndingPara() {
            return this == ForEndingPara;
        }
    }
}
