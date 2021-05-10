package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

public abstract class Output {
    public abstract Content content();
    public abstract Type type();
    public abstract String test(int tabCount);

    public enum Type {
        Page,
        Gso,
        Table,
        Cell
    }
}
