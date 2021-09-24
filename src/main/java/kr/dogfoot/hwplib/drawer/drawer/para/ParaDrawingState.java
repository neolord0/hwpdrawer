package kr.dogfoot.hwplib.drawer.drawer.para;

public enum ParaDrawingState {
    Normal,
    StartRecalculating,
    Recalculating,
    EndRecalculating,
    StartRedrawing;

    public boolean canAddChar() {
        return this == Normal || this == Recalculating;
    }

    public boolean isNormal() {
        return this == Normal;
    }

    public boolean isEndRecalculating() {
        return this == EndRecalculating;
    }

    public boolean isStartRecalculating() {
        return this == StartRecalculating;
    }

    public boolean isRecalculating() {
        return this == Recalculating;
    }
}
