package kr.dogfoot.hwplib.drawer.output;

public abstract class Output {
    public abstract Content content();

    public abstract Type type();

    public abstract String test(int tabCount);

    public enum Type {
        Page,
        Header,
        Footer,
        Gso,
        Table,
        Cell
    }
}
