package kr.dogfoot.hwplib.drawer;

public class DrawingOption {
    private String directoryToSave;
    private int zoomRate;

    public DrawingOption() {
        zoomRate = 100;
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
}
