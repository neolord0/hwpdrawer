package kr.dogfoot.hwplib.drawer.output;

public abstract class Output {
    private Output parent;

    public Output parent() {
        return parent;
    }

    public void parent(Output parent) {
        this.parent = parent;
    }

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
