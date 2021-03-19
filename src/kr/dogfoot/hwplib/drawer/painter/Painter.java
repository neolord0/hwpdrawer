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

    public void string(String s, long x, long y) {
        graphics2D.drawString(s, Convertor.fromHWPUnit(x) + option.offsetX(), Convertor.fromHWPUnit(y) + option.offsetY());
    }

    public double getCharWidth(String ch, CharShape charShape) {
        graphics2D.setFont(FontManager.object().calculatingFont(charShape));
        return graphics2D.getFontMetrics().stringWidth(ch);
    }

    public void setLineStyle(BorderType type, BorderThickness thickness, Color4Byte color) {
        graphics2D.setStroke(Convertor.stroke(type, thickness));
        graphics2D.setColor(Convertor.color(color));
    }

    public void line(long x1, long y1, long x2, long y2) {
        graphics2D.drawLine(
                Convertor.fromHWPUnit(x1) + option.offsetX(),
                Convertor.fromHWPUnit(y1) + option.offsetY(),
                Convertor.fromHWPUnit(x2) + option.offsetX(),
                Convertor.fromHWPUnit(y2) + option.offsetY());
    }

    public void rectangle(Area area) {
        Rectangle rect = area.toConvertedRectangle();
        rect.x += option.offsetX();
        rect.y += option.offsetY();

        graphics2D.drawRect(rect.x, rect.y, rect.width, rect.height);

    }

    public void option(DrawingOption option) {
        this.option = option;
    }
}
