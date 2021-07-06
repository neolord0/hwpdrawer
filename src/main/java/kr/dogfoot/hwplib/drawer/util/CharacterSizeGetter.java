package kr.dogfoot.hwplib.drawer.util;

import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.awt.*;
import java.awt.image.BufferedImage;


public class CharacterSizeGetter {
    private final static CharacterSizeGetter singleObject = new CharacterSizeGetter();

    public static CharacterSizeGetter singleObject() {
        return singleObject;
    }

    private final Graphics2D graphics2DForCalculating;

    public CharacterSizeGetter() {
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        graphics2DForCalculating = (Graphics2D) tempImage.getGraphics();
    }

    public double getCharWidth(String ch, CharShape charShape) {
        graphics2DForCalculating.setFont(FontManager.object().calculatingFont(charShape));
        return graphics2DForCalculating.getFontMetrics().stringWidth(ch);
    }
}
