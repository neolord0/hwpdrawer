package kr.dogfoot.hwpdrawer;

public class DrawingOption {
    private OutputType outputType;
    private String directoryToSave;
    private String fontPath;

    private int zoomRate;
    private int offsetX;
    private int offsetY;

    private boolean auxiliaryLine;

    public DrawingOption() {
        outputType = OutputType.Image;
        zoomRate = 100;
        offsetX = 0;
        offsetY = 0;
        auxiliaryLine = false;
    }

    public OutputType outputType() {
        return outputType;
    }

    public DrawingOption outputType(OutputType outputType) {
        this.outputType = outputType;
        return this;
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

    public String fontPath() {
        return fontPath;
    }

    public DrawingOption fontPath(String fontPath) {
        this.fontPath = fontPath;
        return this;
    }

    public DrawingOption auxiliaryLine(boolean auxiliaryLine) {
        this.auxiliaryLine = auxiliaryLine;
        return this;
    }

    public boolean auxiliaryLine() {
        return auxiliaryLine;
    }

    public enum OutputType {
        Image,
        HTML,
    }
}
