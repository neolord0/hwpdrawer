package kr.dogfoot.hwplib.drawer;

public class DrawingOption {
    private String directoryToSave;
    private int zoomRate;
    private int offsetX;
    private int offsetY;

    public DrawingOption() {
        zoomRate = 100;
        offsetX = 0;
        offsetY = 0;
    }

    public String directoryToSave() {
        return directoryToSave;
    }

    public DrawingOption directoryToSave(String directoryToSave) {
        this.directoryToSave = directoryToSave;
        return this;
    }

    public int zoomRate() {
        return zoomRate;
    }

    public DrawingOption zoomRate(int zoomRate) {
        this.zoomRate = zoomRate;
        return this;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public DrawingOption offset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        return this;
    }

}
