package kr.dogfoot.hwplib.drawer.util;

import java.awt.*;
import java.io.File;
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
        addFontPath("맑은 고딕", "font" + File.separator + "malgun.ttf");
        addFontPath("바탕", "font" + File.separator + "batang.ttc");
        addFontPath("나눔고딕", "font" + File.separator + "NanumGothic.ttf");
        addFontPath("HY헤드라인M", "font" + File.separator + "hy헤드라인m.ttf");
        addFontPath("휴먼명조", "font" + File.separator + "휴먼명조.ttf");
        addFontPath("휴먼고딕", "font" + File.separator + "휴먼고딕.TTF");
        addFontPath("함초롬바탕", "font" + File.separator + "HANBatang.ttf");
        addFontPath("함초롬돋움", "font" + File.separator + "HANDotum.ttf");
    }

    public Font load(String fontName) {
        String fontPath = fontPathMap.get(fontName);
        if (fontPath != null) {
            Font font = null;
            try {
                font = Font.createFont(Font.TRUETYPE_FONT,
                        new FileInputStream(fontPath));
            } catch (Exception e) {
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
