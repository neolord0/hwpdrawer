package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Painter {
    private Graphics2D graphics2D;

    public Painter() {
    }

    public void graphics2D(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
    }

    public void setDrawingFont(CharShape charShape) {
        graphics2D.setFont(FontManager.object().drawingFont(charShape));
        graphics2D.setColor(Convertor.color(charShape.getCharColor()));
    }

    public double setStretch(short ratios) {
        double rate = (double) (ratios) / 100d;

        AffineTransform stretch = new AffineTransform();
        stretch.concatenate(
                AffineTransform.getScaleInstance(rate, 1d));

        graphics2D.setTransform(stretch);
        return rate;
    }

    public void drawString(String s, long x, long y) {
        graphics2D.drawString(s, Convertor.fromHWPUnit(x), Convertor.fromHWPUnit(y));
    }

    public double getCharWidth(String ch, CharShape charShape) {
        graphics2D.setFont(FontManager.object().calculatingFont(charShape));
        return graphics2D.getFontMetrics().stringWidth(ch);
    }

    public void setLineStyle(BorderType type, BorderThickness thickness, Color4Byte color) {
        graphics2D.setStroke(Convertor.stroke(type, thickness));
        graphics2D.setColor(Convertor.color(color));
    }

    public void drawLine(long x1, long y1, long x2, long y2) {
        graphics2D.drawLine(
                Convertor.fromHWPUnit(x1),
                Convertor.fromHWPUnit(y1),
                Convertor.fromHWPUnit(x2),
                Convertor.fromHWPUnit(y2));
    }
}
