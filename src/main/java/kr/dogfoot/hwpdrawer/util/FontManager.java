package kr.dogfoot.hwpdrawer.util;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.DocInfo;
import kr.dogfoot.hwplib.object.docinfo.FaceName;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private final static FontManager singleObject = new FontManager();

    public static FontManager object() {
        return singleObject;
    }

    private DocInfo docInfo;

    private Font defaultFont;
    private final Map<String, Font> originalFontMap;
    private final Map<CharShape, Font> calculatingFontMap;
    private final Map<CharShape, Font> drawingFontMap;

    private FontManager() {
        defaultFont = null;
        originalFontMap = new HashMap<>();
        calculatingFontMap = new HashMap<>();
        drawingFontMap = new HashMap<>();
    }

    public void hwpFile(HWPFile hwpFile) {
        docInfo = hwpFile.getDocInfo();
    }

    public Font calculatingFont(CharShape charShape) {
        Font font = calculatingFontMap.get(charShape);
        if (font == null) {
            font = createCalculatingFont(charShape);
            calculatingFontMap.put(charShape, font);
        }
        return font;
    }

    private Font createCalculatingFont(CharShape charShape) {
        Font font = findOriginalFont(charShape);

        return font.deriveFont(Font.TRUETYPE_FONT, charShape.getBaseSize());
    }

    public Font drawingFont(CharShape charShape) {
        Font font = drawingFontMap.get(charShape);
        if (font == null) {
            font = createDrawingFont(charShape);
            drawingFontMap.put(charShape, font);
        }
        return font;
    }

    private Font createDrawingFont(CharShape charShape) {
        Font font = findOriginalFont(charShape);
        int style = Font.TRUETYPE_FONT;
        if (charShape.getProperty().isBold()) {
            style = style | Font.BOLD;
        }
        if (charShape.getProperty().isItalic()) {
            style = style | Font.ITALIC;
        }
        return font.deriveFont(style, Convertor.fontSize(fontSize(charShape)));
    }

    private int fontSize(CharShape charShape) {
        return charShape.getBaseSize() * charShape.getRelativeSizes().getHangul() / 100;
    }

    private Font findOriginalFont(CharShape charShape) {
        String faceName = faceName(charShape);
        Font font = originalFontMap.get(faceName);
        if (font == null) {
            font = FontLoader.singleObject().load(faceName);
            if (font == null) {
                if (defaultFont == null) {
                    defaultFont = FontLoader.singleObject().loadDefault();
                }
                font = defaultFont;
            }
            originalFontMap.put(faceName, font);
        }
        return font;
    }

    private String faceName(CharShape charShape) {
        FaceName faceName = docInfo.getHangulFaceNameList().get(charShape.getFaceNameIds().getHangul());
        return faceName.getName();
    }

    public void clear() {
        originalFontMap.clear();
        calculatingFontMap.clear();
        drawingFontMap.clear();
    }
}
