package kr.dogfoot.hwplib.drawer.painter.html;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.Content;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.painter.html.control.ControlPainter;
import kr.dogfoot.hwplib.drawer.painter.html.text.TextPainter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.FaceName;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.PatternFill;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class PainterForHTML {
    private DrawingInput input;
    private StringBuilder sb;
    private Graphics2D graphics2D;

    private StateOfTextPart stateOfTextPart;
    private boolean openedFontSpan;
    private Stack<Area> parentAreas;

    private CharShape charShape;

    private BorderType lineType;
    private BorderThickness lineThickness;
    private Color4Byte lineColor;

    private final ControlPainter controlPainter;
    private final TextPainter textPainter;


    public PainterForHTML(DrawingInput input) {
        this.input = input;
        sb = new StringBuilder();
        graphics2D = createGraphics2D();

        stateOfTextPart = StateOfTextPart.Closed;
        openedFontSpan = false;
        parentAreas = new Stack<>();

        controlPainter = new ControlPainter(input, this);
        textPainter = new TextPainter(this);
    }

    private Graphics2D createGraphics2D() {
        BufferedImage tempImage = new BufferedImage(
                1000,
                1000,
                BufferedImage.TYPE_INT_RGB);
        return tempImage.createGraphics();
    }

    public String toString() {
        return sb.toString();
    }

    public void addDocType() {
        sb.append("<!DOCTYPE html>\n");
    }

    public void startHTML() {
        sb.append("<html lang=\"kr\">\n");
    }

    public void endHTML() {
        sb.append("</html>");
    }

    public void defaultHead() {
        sb
                .append("<head>\n")
                .append("\t<meta charset=\"UTF-8\">\n")
                .append("\t<title>HWP Viewer</title>\n")
                .append("</head>\n");
    }

    public void startBodyAndPagesDiv() {
        sb
                .append("<body>\n")
                .append("<div>\n");
    }

    public void startPage(PageOutput pageOutput) {
        sb.append("<div style=\"position:relative; border:1px solid black; ");
        addWidth(convertSizeAndPosition(pageOutput.paperArea().width()));
        addHeight(convertSizeAndPosition(pageOutput.paperArea().height()));
        sb.append("\">\n");

        parentAreas.push(new Area(0, 0, pageOutput.paperArea().width(), pageOutput.paperArea().height()));
    }

    public long convertSizeAndPosition(long size) {
        return size / 40;
    }

    private void addWidth(long width) {
        addStyle("width", px(width));
    }

    private void addStyle(String name, String value) {
        sb.append(name).append(":").append(value).append("; ");
    }

    private void addHeight(long height) {
        addStyle("height", px(height));
    }

    public void endPage() {
        sb.append("</div>\n");

        parentAreas.pop();
    }

    public void endBodyAndPagesDiv() {
        sb
                .append("</div>\n")
                .append("</body>");
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

    public ControlPainter controlPainter() {
        return controlPainter;
    }

    public void startTextPart() {
        stateOfTextPart = StateOfTextPart.Opening;
    }

    public void endTextPart() {
        if (openedFontSpan == true) {
            closeFontSpan();
        }

        if (stateOfTextPart == StateOfTextPart.Opened) {
            sb.append("</div>\n");
        }

        stateOfTextPart = StateOfTextPart.Closed;
    }

    public void setDrawingFont(CharShape charShape) {
        graphics2D.setFont(FontManager.object().drawingFont(charShape));

        this.charShape = charShape;
        if (stateOfTextPart == StateOfTextPart.Opened) {
            if (openedFontSpan == true) {
                closeFontSpan();
            }
            openFontSpan();
        }
    }

    private void openFontSpan() {
        sb.append("<span style=\"");
        addFontStyle();
        sb.append("\">");

        openedFontSpan = true;
    }

    private void addFontStyle() {
        addStyle("font-family", faceName(charShape));
        addStyle("font-size", pt(convertFontSize(charShape.getBaseSize())));
        if (charShape.getProperty().isBold()) {
            addStyle("font-weight", "bold");
        } else {
            addStyle("font-weight", "normal");
        }
        if (charShape.getProperty().isItalic()) {
            addStyle("font-style", "italic");
        } else {
            addStyle("font-style", "normal");
        }
        addStyle("color", color(charShape.getCharColor()));

        addUnderLineAndStrikeLineStyle();
    }


    private String faceName(CharShape charShape) {
        FaceName faceName = input.hwpFile().getDocInfo().getHangulFaceNameList().get(charShape.getFaceNameIds().getHangul());
        return faceName.getName();
    }

    private long convertFontSize(int fontSize) {
        return (fontSize / 51);
    }

    private String color(Color4Byte charColor) {
        return String.format("#%02X%02X%02X", charColor.getR(), charColor.getG(), charColor.getB());
    }

    private void addUnderLineAndStrikeLineStyle() {
        addStyle("text-decoration-line", textDecorationLine());
        if (charShape.getProperty().getUnderLineSort() != UnderLineSort.None || charShape.getProperty().isStrikeLine()) {
            addStyle("text-decoration-style", textDecorationStyle());
            addStyle("text-decoration-color", textDecorationColor());
        }
    }

    private String textDecorationLine() {
        if (charShape.getProperty().getUnderLineSort() == UnderLineSort.None && !charShape.getProperty().isStrikeLine()) {
            return "none";
        } else {
            StringBuilder sb = new StringBuilder();
            if (charShape.getProperty().getUnderLineSort() == UnderLineSort.Bottom) {
                sb.append("underline");
            } else if (charShape.getProperty().getUnderLineSort() == UnderLineSort.Top){
                sb.append("overline");
            }
            if (charShape.getProperty().isStrikeLine()) {
                if (charShape.getProperty().getUnderLineSort() != UnderLineSort.None) {
                    sb.append(" ");
                }
                sb.append("line-through");
            }
            return sb.toString();
        }
    }

    private String textDecorationStyle() {
        BorderType2 borderType;
        if (charShape.getProperty().getUnderLineSort() != UnderLineSort.None) {
            borderType = charShape.getProperty().getUnderLineShape();
        } else {
            borderType = charShape.getProperty().getStrikeLineShape();
        }
        switch (borderType) {
            case Solid:
                return "solid";
            case Dash:
                return "dashed";
            case Dot:
                return "dotted";
            default:
                return "solid";
        }
    }

    private String textDecorationColor() {
        if (charShape.getProperty().getUnderLineSort() != UnderLineSort.None) {
            return color(charShape.getUnderLineColor());
        } else {
            return color(charShape.getStrikeLineColor());
        }
    }

    private void closeFontSpan() {
        sb.append("</span>\n");
        openedFontSpan = false;
    }

    public double setStretch(short ratios) {
        double rate = (double) (ratios) / 100d;

        AffineTransform stretch = new AffineTransform();
        stretch.concatenate(AffineTransform.getScaleInstance(rate, 1d));

        graphics2D.setTransform(stretch);
        return rate;
    }

    private String px(long value) {
        return Long.toString(value) + "px";
    }

    private String pt(long value) {
        return Long.toString(value) + "pt";
    }

    public void string(String ch, long x, long y) {
        if (stateOfTextPart == StateOfTextPart.Opening) {
            openTextPartDiv(x, y);
            openFontSpan();
            sb.append(ch);

            stateOfTextPart = StateOfTextPart.Opened;
        } else if (stateOfTextPart == StateOfTextPart.Opened){
            sb.append(ch);
        }
    }

    private void openTextPartDiv(long x, long y) {
        Area parentArea = parentAreas.peek();

        sb.append("<div style=\"");
        addStyle("position", "absolute" );
        addStyle("left", px(convertSizeAndPosition(x - parentArea.left())));
        addStyle("top", px(convertSizeAndPosition(y - parentArea.top())));
        sb.append("\">\n");
    }

    public void setLineStyle(BorderType lineType, BorderThickness lineThickness, Color4Byte lineColor) {
        this.lineType = lineType;
        this.lineThickness = lineThickness;
        this.lineColor = lineColor;
    }

    public void line(long startX, long startY, long endX, long endY) {
        if (startY == endY) {
            sb.append("<hr style=\"");
            addStyle("position", "absolute");
            addStyle("left", px(convertSizeAndPosition(startX)));
            addStyle("top", px(convertSizeAndPosition(startY)));
            addStyle("width", px(convertSizeAndPosition(endX - startX)));
            addStyle("border", currentBorderType());
            sb.append("\"/>");
        }
    }

    private String currentBorderType() {
        return "solid 1px black";
    }

    public void startCellDiv(Area cellArea, BorderFill borderFill, boolean paintBorder) {
        Area parentArea;
        if (parentAreas.isEmpty()) {
            parentArea = new Area(0, 0, 0,0);
        } else {
            parentArea = parentAreas.peek();
        }

        sb.append("<div style=\"");
        addStyle("position", "absolute" );
        addStyle("left", px(convertSizeAndPosition(cellArea.left() - parentArea.left())));
        addStyle("top", px(convertSizeAndPosition(cellArea.top() - parentArea.top())));
        addStyle("width", px(convertSizeAndPosition(cellArea.width())));
        addStyle("height", px(convertSizeAndPosition(cellArea.height())));
        if (paintBorder == true) {
            addStyle("border", "solid 1px black");
        }
        if (borderFill.getFillInfo().getType().hasPatternFill()) {
            addPatternFill(borderFill.getFillInfo().getPatternFill());
        }
        addStyle("padding", "0");
        sb.append("\">\n");
        parentAreas.push(cellArea);
    }

    public void endCellDiv(Area cellArea, BorderFill borderFill) {
        sb.append("</div>");

        parentAreas.pop();
    }

    private void addPatternFill(PatternFill fillInfo) {
        addStyle("background-color", color(fillInfo.getBackColor()));
    }

    enum StateOfTextPart {
        Closed,
        Opening,
        Opened
    }
}
