package kr.dogfoot.hwplib.drawer.util;

public class MyStringBuffer {
    private StringBuffer sb;

    public MyStringBuffer() {
        sb = new StringBuffer();
    }

    public MyStringBuffer append(String s) {
        sb.append(s);
        return this;
    }

    public MyStringBuffer tab(int count) {
        for (int index = 0; index < count; index++) {
            sb.append('\t');
        }
        return this;
    }

    public MyStringBuffer append(Area area) {
        sb.append(area.toString());
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
