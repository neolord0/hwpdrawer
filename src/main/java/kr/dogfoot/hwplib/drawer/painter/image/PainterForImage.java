package kr.dogfoot.hwplib.drawer.painter.image;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoNormal;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.Content;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.painter.image.background.BackgroundPainter;
import kr.dogfoot.hwplib.drawer.painter.image.control.ControlPainter;
import kr.dogfoot.hwplib.drawer.painter.image.text.TextPainter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.PatternType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

public class PainterForImage {
    private Graphics2D graphics2D;
    private DrawingOption option;

    private final ControlPainter controlPainter;
    private final TextPainter textPainter;
    private final BackgroundPainter backgroundPainter;

    public PainterForImage(DrawingInput input) {
        controlPainter = new ControlPainter(input, this);

        textPainter = new TextPainter(this);
        backgroundPainter = new BackgroundPainter(input, this);
    }


    public void option(DrawingOption option) {
        this.option = option;
    }

    public DrawingOption option() {
        return option;
    }

    public PainterForImage graphics2D(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        this.graphics2D = graphics2D;

        return this;
    }

    public ControlPainter controlPainter() {
        return controlPainter;
    }

    public TextPainter textPainter() {
        return textPainter;
    }

    public BackgroundPainter backgroundPainter() {
        return backgroundPainter;
    }

    public PainterForImage setDrawingFont(CharShape charShape) {
        graphics2D.setFont(FontManager.object().drawingFont(charShape));
        graphics2D.setColor(Convertor.color(charShape.getCharColor()));
        return this;
    }

    public double textOffsetY(CharInfoNormal charInfo) {
        try {
            LineMetrics lm = graphics2D.getFontMetrics().getLineMetrics(charInfo.normalCharacter().getCh(), graphics2D);
            return (lm.getDescent()) * charInfo.height() / lm.getHeight();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double setStretch(short ratios) {
        double rate = (double) (ratios) / 100d;

        AffineTransform stretch = new AffineTransform();
        stretch.concatenate(
                AffineTransform.getScaleInstance(rate, 1d));

        graphics2D.setTransform(stretch);
        return rate;
    }

    public PainterForImage string(String s, long x, long y) {
        graphics2D.drawString(s, Convertor.fromHWPUnit(x) + option.offsetX(), Convertor.fromHWPUnit(y) + option.offsetY());
        return this;
    }

    public PainterForImage setLineStyle(BorderType type, BorderThickness thickness, Color4Byte color) {
        graphics2D.setStroke(Convertor.stroke(type, thickness));
        graphics2D.setColor(Convertor.color(color));
        return this;
    }

    public PainterForImage setLineStyle(LineType type, int thickness, Color4Byte color) {
        graphics2D.setStroke(Convertor.stroke(type, thickness));

        graphics2D.setColor(Convertor.color(color));
        return this;
    }

    public PainterForImage line(long x1, long y1, long x2, long y2) {
        graphics2D.drawLine(
                Convertor.fromHWPUnit(x1) + option.offsetX(),
                Convertor.fromHWPUnit(y1) + option.offsetY(),
                Convertor.fromHWPUnit(x2) + option.offsetX(),
                Convertor.fromHWPUnit(y2) + option.offsetY());
        return this;
    }

    public PainterForImage rectangle(Area area, boolean fill) {
        Rectangle rect = area.toConvertedRectangle();
        rect.x += option.offsetX();
        rect.y += option.offsetY();

        if (!fill) {
            graphics2D.drawRect(rect.x, rect.y, rect.width, rect.height);
        } else {
            graphics2D.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        return this;
    }

    public PainterForImage testLineStyle() {
        setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(255, 0, 0));
        return this;
    }

    public PainterForImage testBackStyle() {
        graphics2D.setColor(Color.WHITE);
        return this;
    }

    public PainterForImage setPatternFill(PatternType type, Color4Byte backColor, Color4Byte patternColor) {
        graphics2D.setColor(Convertor.color(backColor));
        return this;
    }

    public PainterForImage image(Area area, BufferedImage image) {
        Rectangle rect = area.toConvertedRectangle();
        rect.x += option.offsetX();
        rect.y += option.offsetY();

        graphics2D.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);
        return this;
    }

    public void paintContent(Content content) throws Exception {
        for (TextRow row : content.rows()) {
            for (TextColumn column : row.columns()) {
                controlPainter.paintControls(column.behindChildOutputs());
                textPainter.paintTextLines(column.textLines());
                controlPainter.paintControls(column.nonBehindChildOutputs());
            }
        }
    }

    public PainterForImage cellBorder(Area cellArea, BorderFill borderFill) {
        EachBorder leftBorder = borderFill.getLeftBorder();
        if (leftBorder.getType() != BorderType.None) {
            setLineStyle(leftBorder.getType(),
                    leftBorder.getThickness(),
                    leftBorder.getColor());
            line(cellArea.left(), cellArea.top(), cellArea.left(), cellArea.bottom());
        }

        EachBorder topBorder = borderFill.getTopBorder();
        if (topBorder.getType() != BorderType.None) {
            setLineStyle(topBorder.getType(),
                    topBorder.getThickness(),
                    topBorder.getColor());
            line(cellArea.left(), cellArea.top(), cellArea.right(), cellArea.top());
        }

        EachBorder rightBorder = borderFill.getRightBorder();
        if (rightBorder.getType() != BorderType.None) {
            setLineStyle(rightBorder.getType(),
                    rightBorder.getThickness(),
                    rightBorder.getColor());
            line(cellArea.right(), cellArea.top(), cellArea.right(), cellArea.bottom());
        }

        EachBorder bottomBorder = borderFill.getBottomBorder();
        if (bottomBorder.getType() != BorderType.None) {
            setLineStyle(bottomBorder.getType(),
                    bottomBorder.getThickness(),
                    bottomBorder.getColor());
            line(cellArea.left(), cellArea.bottom(), cellArea.right(), cellArea.bottom());
        }
        return this;
    }
}
