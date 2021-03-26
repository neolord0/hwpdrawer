package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Painter {
    private Graphics2D graphics2D;
    private DrawingOption option;

    public Painter() {
    }

    public Painter option(DrawingOption option) {
        this.option = option;
        return this;
    }

    public Painter graphics2D(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
        return this;
    }

    public Painter setDrawingFont(CharShape charShape) {
        graphics2D.setFont(FontManager.object().drawingFont(charShape));
        graphics2D.setColor(Convertor.color(charShape.getCharColor()));
        return this;
    }

    public double setStretch(short ratios) {
        double rate = (double) (ratios) / 100d;

        AffineTransform stretch = new AffineTransform();
        stretch.concatenate(
                AffineTransform.getScaleInstance(rate, 1d));

        graphics2D.setTransform(stretch);
        return rate;
    }

    public Painter string(String s, long x, long y) {
        graphics2D.drawString(s, Convertor.fromHWPUnit(x) + option.offsetX(), Convertor.fromHWPUnit(y) + option.offsetY());
        return this;
    }

    public double getCharWidth(String ch, CharShape charShape) {
        graphics2D.setFont(FontManager.object().calculatingFont(charShape));
        return graphics2D.getFontMetrics().stringWidth(ch);
    }

    public Painter setLineStyle(BorderType type, BorderThickness thickness, Color4Byte color) {
        graphics2D.setStroke(Convertor.stroke(type, thickness));
        graphics2D.setColor(Convertor.color(color));
        return this;
    }

    public Painter line(long x1, long y1, long x2, long y2) {
        graphics2D.drawLine(
                Convertor.fromHWPUnit(x1) + option.offsetX(),
                Convertor.fromHWPUnit(y1) + option.offsetY(),
                Convertor.fromHWPUnit(x2) + option.offsetX(),
                Convertor.fromHWPUnit(y2) + option.offsetY());
        return this;
    }

    public Painter rectangle(Area area, boolean fill) {
        Rectangle rect = area.toConvertedRectangle();
        rect.x += option.offsetX();
        rect.y += option.offsetY();

        if (fill == false) {
            graphics2D.drawRect(rect.x, rect.y, rect.width, rect.height);
        } else {
            graphics2D.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        return this;
    }

    public Painter testLineStyle() {
        setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(255, 0, 0));
        return this;
    }

    public Painter testBackStyle() {
        graphics2D.setColor(Color.WHITE);
        return this;
    }
}
