package kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;


public class GsoContent extends ControlContent {
    private GsoControl control;
    private Area textMargin;
    private long calculatedContentHeight;
    private TextVerticalAlignment verticalAlignment;

    public GsoContent(GsoControl control, Area controlArea) {
        this.control = control;
        this.controlArea = controlArea;
        calculatedContentHeight = 0;
    }

    public GsoControl control() {
        return control;
    }

    public GsoContent textMargin(int left, int top, int right, int bottom) {
        textMargin = new Area(left, top, right, bottom);
        return this;
    }

    public Area textArea() {
        return new Area(controlArea)
                .applyMargin(textMargin.left(),
                        textMargin.top(),
                        textMargin.right(),
                        textMargin.bottom());
    }

    public void verticalAlignment(TextVerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void adjustTextAreaAndVerticalAlignment() {
        Area textArea = textArea();
        long offsetY = offsetY(textArea, verticalAlignment);
        if (offsetY != 0) {
            for (TextPart textPart : textParts) {
                textPart.area()
                        .moveY(offsetY);
            }

            for (ControlContent controlContent : behindChildContents) {
                controlContent.moveY(offsetY);
            }

            for (ControlContent controlContent : nonBehindChildContents) {
                controlContent.moveY(offsetY);
            }
        }
    }


    private long offsetY(Area textArea, TextVerticalAlignment verticalAlignment) {
        if (calculatedContentHeight < textArea.height()) {
            switch (verticalAlignment) {
                case Top:
                    return 0;
                case Center:
                    return (textArea.height() - calculatedContentHeight) / 2;
                case Bottom:
                    return textArea.height() - calculatedContentHeight;
            }
        }
        return 0;
    }

    public long calculatedContentHeight() {
        return calculatedContentHeight;
    }

    public void calculatedContentHeight(long calculatedContentHeight) {
        this.calculatedContentHeight =
                Math.max(this.calculatedContentHeight, calculatedContentHeight);
    }

    @Override
    public void addChildContent(ControlContent childContent) {
        if (childContent.vertRelTo() == VertRelTo.Para) {
            calculatedContentHeight(childContent.controlArea.bottom() - textArea().top());
        }

        super.addChildContent(childContent);
    }


    @Override
    public Type type() {
        return Type.Gso;
    }

    @Override
    public int zOrder() {
        return control.getHeader().getzOrder();
    }

    @Override
    public int textFlowMethod() {
        return control.getHeader().getProperty().getTextFlowMethod();
    }

    @Override
    public Area calculatedControlArea() {
        return null;
    }

    @Override
    public VertRelTo vertRelTo() {
        return control.getHeader().getProperty().getVertRelTo();
    }

}
