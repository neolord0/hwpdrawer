package kr.dogfoot.hwplib.drawer.painter.background;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.*;

import java.awt.image.BufferedImage;

public class BackgroundPainter {
    private final DrawingInput input;
    private final Painter painter;

    public BackgroundPainter(DrawingInput input, Painter painter) {
        this.input = input;
        this.painter = painter;
    }

    public void paint(FillInfo fillInfo, Area area) {
        if (fillInfo.getType().hasPatternFill()) {
            patternFill(fillInfo.getPatternFill(), area);
        }
        if (fillInfo.getType().hasGradientFill()) {
            gradientFill(fillInfo.getGradientFill(), area);
        }
        if (fillInfo.getType().hasImageFill()) {
            imageFill(fillInfo.getImageFill(), area);
        }
    }

    private void patternFill(PatternFill patternFill, Area area) {
        painter
                .setPatternFill(patternFill.getPatternType(), patternFill.getBackColor(), patternFill.getPatternColor())
                .rectangle(area, true);
    }

    private void gradientFill(GradientFill gradientFill, Area area) {
        painter
                .setPatternFill(PatternType.None, gradientFill.getColorList().get(0),  gradientFill.getColorList().get(0))
                .rectangle(area, true);
    }

    private void imageFill(ImageFill imageFill, Area area) {
        BufferedImage image = input.image(imageFill.getPictureInfo().getBinItemID());
        if (image != null) {
            painter.image(area, image);
        }
    }
}
