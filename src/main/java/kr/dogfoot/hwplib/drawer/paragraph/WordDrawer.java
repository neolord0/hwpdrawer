package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class WordDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;
    private final TextLineDrawer textLineDrawer;
    private final TextFlowCalculator textFlowCalculator;

    private final WordSplitter wordSplitter;
    private final ControlDrawer controlDrawer;

    private final ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    private boolean checkOverRight;
    private boolean applyMinimumSpace;


    public WordDrawer(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer, TextLineDrawer textLineDrawer, TextFlowCalculator textFlowCalculator) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
        this.textLineDrawer = textLineDrawer;
        this.textFlowCalculator = textFlowCalculator;

        wordSplitter = new WordSplitter(paraListDrawer, textLineDrawer, this);
        controlDrawer = new ControlDrawer(input, output);

        charsOfWord = new ArrayList<>();
    }

    public void reset() {
        charsOfWord.clear();
        wordWidth = 0;
    }

    public void addCharOfWord(CharInfo charInfo) {
        charsOfWord.add(charInfo);
        wordWidth += charInfo.width();
    }

    public void addWordToLine(NormalCharInfo spaceCharInfo) throws Exception {
        if (charsOfWord.isEmpty()) {
            addSpaceChar(spaceCharInfo);
            return;
        }

        if (!textLineDrawer.isOverWidth(wordWidth, false)) {
            addWordAllChars(false, false);
            addSpaceChar(spaceCharInfo);
        } else {
            if (!textLineDrawer.isOverWidth(wordWidth, true)) {
                addWordAllChars(false, true);

                textLineDrawer.setBestSpaceRate();
                paraListDrawer.saveTextLineAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
        reset();
    }

    private void addSpaceChar(NormalCharInfo spaceCharInfo) {
        if (paraListDrawer.drawingState().canAddChar() && spaceCharInfo != null) {
            textLineDrawer.addChar(spaceCharInfo);
        }
    }

    private void addWordAllChars(boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        this.checkOverRight = checkOverRight;
        this.applyMinimumSpace = applyMinimumSpace;

        for (CharInfo charInfo : charsOfWord) {
            addChar(charInfo);

            if (wordSplitter.stoppedAddingChar()) {
                break;
            }
        }
    }

    private boolean addChar(CharInfo charInfo) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && textLineDrawer.isOverWidth(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                textLineDrawer.setBestSpaceRate();
            }

            paraListDrawer.saveTextLineAndNewLine();

            if (paraListDrawer.drawingState().isNormal() || paraListDrawer.drawingState().isEndRecalculating()) {
                hasNewLine = true;
            } else {
                hasNewLine = false;
            }
        } else {
            hasNewLine = false;
        }

        textLineDrawer.justNewLine(false);

        if (paraListDrawer.drawingState().canAddChar()) {
            if (textLineDrawer.noDrawingChar() && paraListDrawer.drawingState().isNormal()) {
                paraListDrawer.checkNewColumnAndPage();
                textLineDrawer.firstCharInfo(charInfo);
            }

            if (paraListDrawer.drawingState().isNormal() && charInfo.type() == CharInfo.Type.Control) {
                addControlChar((ControlCharInfo) charInfo);
            } else {
                textLineDrawer.addChar(charInfo);
            }
        }
        return hasNewLine;
    }

    private void addControlChar(ControlCharInfo controlCharInfo) throws Exception {
        ControlOutput output2 = controlDrawer.draw(controlCharInfo);
        controlCharInfo.output(output2);

        if (controlCharInfo.isLikeLetter()) {
            textLineDrawer.addChar(controlCharInfo);
            return;
        }

        if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText
                || controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            if (!textFlowCalculator.alreadyAdded(controlCharInfo)) {
                if (output.checkRedrawingTextLine(controlCharInfo.areaWithOuterMargin())) {
                    if (isOver75PercentOfPageHeight(paraListDrawer.currentTextPartArea().bottom())) {
                        output.addControlMovedToNextPage(output2, controlCharInfo);
                    } else {
                        textFlowCalculator.add(controlCharInfo);
                        output.addChildOutput(output2);

                        TextLine firstRedrawingTextLine = output.deleteRedrawingTextLine(controlCharInfo.areaWithOuterMargin());
                        throw new BreakingDrawException(firstRedrawingTextLine.paraIndex(),
                                firstRedrawingTextLine.firstChar().index(),
                                firstRedrawingTextLine.firstChar().position(),
                                firstRedrawingTextLine.area().top()).forRedrawing();
                    }
                } else {
                    textFlowCalculator.add(controlCharInfo);
                    output.addChildOutput(output2);
                }
            }
        } else {
            output.addChildOutput(output2);
        }
    }

    private boolean isOver75PercentOfPageHeight(long y) {
        return y - input.pageInfo().bodyArea().top() >= input.pageInfo().bodyArea().height() * 75 / 100;
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        if (isAllLineDivideByWord()) {
            if (!textLineDrawer.noDrawingChar()) {
                paraListDrawer.saveTextLineAndNewLine();
            }
            if (paraListDrawer.drawingState().isEndRecalculating()) {
                input.beforeChar(charsOfWord.size() + 1);
            }

            addWordAllChars(true, false);
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.splitByLineAndAdd(charsOfWord, input.paraShape());

            if (paraListDrawer.drawingState().isEndRecalculating()) {
                input.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
        }
        addSpaceChar(spaceCharInfo);
    }

    private boolean isAllLineDivideByWord() {
        return input.paraShape().getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                && input.paraShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord;
    }

    public boolean addChar(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        this.checkOverRight = checkOverRight;
        this.applyMinimumSpace = applyMinimumSpace;
        return addChar(charInfo);
    }

    public void adjustControlAreaAtNewPage() {
        for (CharInfo charInfo : charsOfWord) {
            if (charInfo.type() == CharInfo.Type.Control) {
                ((ControlCharInfo) charInfo).area(input);
            }
        }
    }

    public String test() {
        StringBuilder sb = new StringBuilder();
        for (CharInfo charInfo : charsOfWord) {
            sb.append(testCharInfo(charInfo));
        }
        return sb.toString();
    }

    private String testCharInfo(CharInfo charInfo) {
        StringBuilder sb = new StringBuilder();
        if (charInfo.type() == CharInfo.Type.Normal) {
            NormalCharInfo normalCharInfo = (NormalCharInfo) charInfo;
            try {
                sb
                        .append(normalCharInfo.normalCharacter().getCh())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;
            if (controlCharInfo.control() == null) {
                sb
                        .append(controlCharInfo.character().getCode())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            } else {
                sb
                        .append(controlCharInfo.control().getType())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            }

        }
        return sb.toString();
    }

    public void stopAddingChar() {
        wordSplitter.stopAddingChar(true);
    }

    public void continueAddingChar() {
        wordSplitter.stopAddingChar(false);
    }
}
