package kr.dogfoot.hwpdrawer.util;

public class MyStringBuilder {
    private final StringBuilder sb;

    public MyStringBuilder() {
        sb = new StringBuilder();
    }

    public MyStringBuilder append(String s) {
        sb.append(s);
        return this;
    }

    public MyStringBuilder tab(int count) {
        for (int index = 0; index < count; index++) {
            sb.append('\t');
        }
        return this;
    }

    public MyStringBuilder append(Area area) {
        sb.append(area.toString());
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
