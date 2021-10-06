package kr.dogfoot.hwplib.drawer.drawer.para;

public enum ParaDrawingState {
    Normal,
    StartRecalculating,
    Recalculating,
    EndRecalculating,
    StartRedrawing,
    ETC;

    public boolean canAddChar() {
        return this == Normal || this == Recalculating;
    }

    public boolean isNormal() {
        return this == Normal;
    }

    public boolean isStartRecalculating() {
        return this == StartRecalculating;
    }

    public boolean isRecalculating() {
        return this == Recalculating;
    }

    public boolean isEndRecalculating() {
        return this == EndRecalculating;
    }

    public boolean isStartRedrawing() {
        return this == StartRedrawing;
    }

}
