package kr.dogfoot.hwpdrawer.drawer.para.withlineseg;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.text.TextLine;
import kr.dogfoot.hwpdrawer.output.text.TextPart;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.input.DrawingInput;

public class TextLineDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private TextLine textLine;

    public TextLineDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public TextLineDrawer initialize(Area area) {
        reset(area);
        return this;
    }

    public TextLineDrawer reset(Area area) {
        textLine = new TextLine(input.paraIndex(), new Area(area));
        return this;
    }

    public void addNewTextPart(long startX, long width) {
        textLine.addNewTextPart(startX, width);
    }

    public void addChar(CharInfo charInfo) {
        textLine.currentTextPart().addCharInfo(charInfo);
    }

    public void saveToOutput() {
        setBaseSpaceRate();
        textLine.maxCharHeight(input.currentLineSeg().getLineHeight())
                .alignment(input.paraShape().getProperty1().getAlignment());
        output.addTextLine(textLine);
    }

    private void setBaseSpaceRate() {
        long wordsWidth;
        long spacesWidth;
        for (TextPart textPart : textLine.parts()) {
            if (input.paraShape().getProperty1().getMinimumSpace() == 0) {
                textPart.spaceRate(1.0);
            } else {
                wordsWidth = 0;
                spacesWidth = 0;

                int count = textPart.charInfos().size();
                for (int index = 0; index < count; index++) {
                    CharInfo charInfo = textPart.charInfos().get(index);
                    if (charInfo.character().isSpace()) {
                        spacesWidth += charInfo.widthAddingCharSpace();
                    } else {
                        wordsWidth += charInfo.widthAddingCharSpace();
                    }
                }

                for (int index = count - 1; index >= 0; index--) {
                    CharInfo charInfo = textPart.charInfos().get(index);
                    if (charInfo.character().isSpace()) {
                        spacesWidth -= charInfo.widthAddingCharSpace();
                    } else {
                        break;
                    }
                }
                
                double minSpaceRate = (double) (textLine.currentTextPart().width() - wordsWidth) / (double) spacesWidth;
                if (minSpaceRate > 1.0f) {
                    textPart.spaceRate(1.0f);
                } else {
                    minSpaceRate = Math.max(minSpaceRate, (100.0f - input.paraShape().getProperty1().getMinimumSpace()) / 100f);
                    textPart.spaceRate(minSpaceRate);
                }
            }
        }
    }

}
