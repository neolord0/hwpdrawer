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
        Cell;

        public boolean isGso() {
            return this == Gso;
        }

        public boolean isTable() {
            return this == Table;
        }

        public boolean isCell() {
            return this == Cell;
        }

        public boolean isFooter() {
            return this == Footer;
        }

    }
}
