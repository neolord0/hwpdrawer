package kr.dogfoot.hwpdrawer.util;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class FontLoader {
    private final static FontLoader singleObject = new FontLoader();

    public static FontLoader singleObject() {
        return singleObject;
    }

    private String fontPath;

    private String defaultFontName;
    private final Map<String, String> fontFilenameMap;

    private FontLoader() {
        fontFilenameMap = new HashMap<>();
        initialize();
    }

    public void fontPath(String fontPath) {
        this.fontPath = fontPath;
    }

    private void initialize() {
        defaultFontName = "새굴림";

        addFontFilename("새굴림", "NGULIM.TTF");
        addFontFilename("바탕", "batang.ttc");
        addFontFilename("맑은 고딕", "malgun.ttf");
        addFontFilename("나눔고딕", "NanumGothic.ttf");
        addFontFilename("HY헤드라인M", "hy헤드라인m.ttf");
        addFontFilename("휴먼명조", "휴먼명조.ttf");
        addFontFilename("휴먼고딕", "휴먼고딕.TTF");
        addFontFilename("함초롬바탕", "HANBatang.ttf");
        addFontFilename("함초롬돋움", "HANDotum.ttf");
    }

    public Font load(String fontName) {
        String path = fontFilenameMap.get(fontName);
        if (path != null) {
            path = fontPath + File.separator + fontFilenameMap.get(fontName);
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

    public Font loadDefault() {
        return load(defaultFontName);
    }

    public FontLoader addFontFilename(String name, String filename) {
        fontFilenameMap.put(name, filename);
        return this;
    }
}
