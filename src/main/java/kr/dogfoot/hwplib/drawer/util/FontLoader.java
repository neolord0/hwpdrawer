package kr.dogfoot.hwplib.drawer.util;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class FontLoader {
    private final static FontLoader singleObject = new FontLoader();

    public static FontLoader object() {
        return singleObject;
    }

    private String fontPath;
    private final Map<String, String> fontFilenameMap;

    private FontLoader() {
        fontFilenameMap = new HashMap<>();
        initialize();
    }

    public void fontPath(String fontPath) {
        this.fontPath = fontPath;
    }

    private void initialize() {
        addFontFilename("맑은 고딕", "malgun.ttf");
        addFontFilename("바탕", "batang.ttc");
        addFontFilename("나눔고딕", "NanumGothic.ttf");
        addFontFilename("HY헤드라인M", "hy헤드라인m.ttf");
        addFontFilename("휴먼명조", "휴먼명조.ttf");
        addFontFilename("휴먼고딕", "휴먼고딕.TTF");
        addFontFilename("함초롬바탕", "HANBatang.ttf");
        addFontFilename("함초롬돋움", "HANDotum.ttf");
    }

    public Font load(String fontName) {
        String path = fontPath + File.separator + fontFilenameMap.get(fontName);
        if (path != null) {
            Font font;
            try {
                font = Font.createFont(Font.TRUETYPE_FONT,
                        new FileInputStream(path));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return font;
        }
        return null;
    }

    public FontLoader addFontFilename(String name, String filename) {
        fontFilenameMap.put(name, filename);
        return this;
    }
}
