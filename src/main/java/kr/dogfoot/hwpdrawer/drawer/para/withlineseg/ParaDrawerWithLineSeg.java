package kr.dogfoot.hwpdrawer.drawer.para.withlineseg;

import kr.dogfoot.hwpdrawer.drawer.para.ParaDrawer;
import kr.dogfoot.hwpdrawer.input.paralist.ParagraphListInfo;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.CharPosition;
import kr.dogfoot.hwpdrawer.input.DrawingInput;

public class ParaDrawerWithLineSeg extends ParaDrawer {
    private final CharAdder charAdder;
    private final TextLineDrawer textLineDrawer;

    private final DividingProcessor dividingProcessor;

    private final Area currentTextArea;
    private int paraIndent;

    public ParaDrawerWithLineSeg(DrawingInput input, InterimOutput output) {
        super(input, output);

        textLineDrawer = new TextLineDrawer(input, output);
        charAdder = new CharAdder(input, output, this, textLineDrawer);

        dividingProcessor = new DividingProcessor(input, output, this, charAdder);

        currentTextArea = new Area();
    }

    @Override
    public void draw(boolean redraw, CharPosition startPosition, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (!redraw) {
            start(startPosition);
        }

        checkNextPage();
        resetForNewPara();
        processChars();
        end();
    }

    private void start(CharPosition startPosition) throws Exception {
        input.startPara();
        charAdder.resetAtStartingPara();

        if (startPosition != null && startPosition.charIndex() > 0) {
            input.gotoCharPositionInPara(startPosition);
        } else {
            dividingProcessor.process();
        }
    }

    private void checkNextPage() {
        if (input.sortOfText() == ParagraphListInfo.Sort.ForBody
                && input.currentLineSeg().getLineVerticalPosition() == input.paraShape().getTopParaSpace() / 2) {
            nextPage();
        }
    }

    private void resetForNewPara() {
        resetCurrentTextArea();
        textLineDrawer
                .initialize(currentTextArea)
                .addNewTextPart(input.currentLineSeg().getStartPositionFromColumn(), input.currentLineSeg().getSegmentWidth() - paraIndent);
    }

    private void resetCurrentTextArea() {
        currentTextArea.set(input.paraArea())
                .top(input.paraArea().top() + input.currentLineSeg().getLineVerticalPosition())
                .height(input.currentLineSeg().getLineHeight());

        paraIndent = 0;
        if (input.firstLine()) {
            if (input.paraShape().getIndent() > 0) {
                paraIndent = input.paraShape().getIndent() / 2;
            }
        } else {
            if (input.paraShape().getIndent() < 0) {
                paraIndent = -input.paraShape().getIndent() / 2;
            }
        }

        currentTextArea.left(currentTextArea.left() + paraIndent);
    }

    private void processChars() throws Exception {
        while (input.nextChar()) {
            charAdder.addChar(input.currentChar(), currentTextArea);

            if (input.nextLineSeg() != null && input.nextLineSeg().getTextStartPosition() == input.charPosition()) {
                saveTextLineAndNewLine();
            }
        }
    }

    @Override
    public void gotoStartCharOfCurrentRow() {

    }

    public void saveTextLineAndNewLine() {
        if (input.nextLineSeg() == null || input.nextLineSeg().getStartPositionFromColumn() == 0 || input.currentChar().isParaBreak()) {
            textLineDrawer.saveToOutput();
        } else {
            textLineDrawer.addNewTextPart(input.nextLineSeg().getStartPositionFromColumn() - paraIndent, input.nextLineSeg().getSegmentWidth());
        }

        input.moveNextLineSeg();
        if (input.currentLineSeg() != null && input.currentLineSeg().getStartPositionFromColumn() == 0) {
            checkNextPage();

            resetCurrentTextArea();
            textLineDrawer
                    .reset(currentTextArea)
                    .addNewTextPart(input.currentLineSeg().getStartPositionFromColumn(), input.currentLineSeg().getSegmentWidth() - paraIndent);
        }
    }

    public void nextPage() {
        if (output.currentPage().empty()) {
            return;
        }
        input.nextPage();
        output.nextPage(input);
    }

    private void end() {
        output.setLastLineInPara();
    }
}
