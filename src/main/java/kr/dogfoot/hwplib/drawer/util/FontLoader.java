package kr.dogfoot.hwplib.drawer.util;

import java.awt.*;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class FontLoader {
    private static FontLoader singleObject = new FontLoader();

    public static FontLoader object() {
        return singleObject;
    }

    private Map<String, String> fontPathMap;

    private FontLoader() {
        fontPathMap = new HashMap<>();
        initialize();
    }

    private void initialize() {
        addFontPath("맑은 고딕", "font/malgun.ttf");
        addFontPath("바탕", "font/batang.ttc");
        addFontPath("나눔고딕", "font/NanumGothic.ttf");
    }

    public Font load(String fontName) {
        String fontPath = fontPathMap.get(fontName);
        if (fontPath != null) {
            Font font = null;
            try {
                font=Font.createFont(Font.TRUETYPE_FONT,
                        new FileInputStream(fontPath));
            } catch(Exception e) {
                System.out.println(e);
                return null;
            }
            return font;
        }
        return null;
    }

    public FontLoader addFontPath(String name, String path) {
        fontPathMap.put(name, path);
        return this;
    }
}
