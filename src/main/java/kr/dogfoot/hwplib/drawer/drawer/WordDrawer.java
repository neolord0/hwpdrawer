package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoNormal;
import kr.dogfoot.hwplib.drawer.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.drawer.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class WordDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawer paraDrawer;
    private final TextLineDrawer textLineDrawer;
    private final TextFlowCalculator textFlowCalculator;

    private final WordSplitter wordSplitter;
    private final ControlDrawer controlDrawer;

    private final ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    private boolean checkOverRight;
    private boolean applyMinimumSpace;

    public WordDrawer(DrawingInput input, InterimOutput output, ParaDrawer paraDrawer, TextLineDrawer textLineDrawer, TextFlowCalculator textFlowCalculator) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
        this.textLineDrawer = textLineDrawer;
        this.textFlowCalculator = textFlowCalculator;

        wordSplitter = new WordSplitter(paraDrawer, textLineDrawer, this);
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

    public void addWordToLine(CharInfoNormal spaceCharInfo) throws Exception {
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
                paraDrawer.saveTextLineAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
        reset();
    }

    private void addSpaceChar(CharInfoNormal spaceCharInfo) {
        if (paraDrawer.drawingState().canAddChar() && spaceCharInfo != null && !wordSplitter.stoppedAddingChar()) {
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
            paraDrawer.saveTextLineAndNewLine();

            if (paraDrawer.drawingState().isNormal() || paraDrawer.drawingState().isEndRecalculating()) {
                hasNewLine = true;
            } else {
                hasNewLine = false;
            }
        } else {
            hasNewLine = false;
        }

        textLineDrawer.justNewLine(false);

        if (paraDrawer.drawingState().canAddChar()) {
            if (textLineDrawer.noDrawingChar() && paraDrawer.drawingState().isNormal()) {
                textLineDrawer.firstCharInfo(charInfo);
                paraDrawer.checkNewColumnAndPage();
            }

            if (wordSplitter.stoppedAddingChar() == false) {
                if (paraDrawer.drawingState().isNormal() && charInfo.type() == CharInfo.Type.Control) {
                    addControlChar((CharInfoControl) charInfo);
                } else {
                    textLineDrawer.addChar(charInfo);
                }
            }
        }
        return hasNewLine;
    }

    private void addControlChar(CharInfoControl controlCharInfo) throws Exception {
        ControlOutput output2 = controlDrawer.draw(controlCharInfo);
        addControlOutput(output2);
    }

    public void addControlOutput(ControlOutput output2) throws RedrawException {
        CharInfoControl controlCharInfo = output2.controlCharInfo();
        if (controlCharInfo.isLikeLetter()) {
            textLineDrawer.addChar(controlCharInfo);
            return;
        }

        if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText
                || controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            if (!textFlowCalculator.alreadyAdded(controlCharInfo)) {
                if (output.checkRedrawingTextLine(controlCharInfo.areaWithOuterMargin())) {
                    if (isOver75PercentOfPageHeight(paraDrawer.currentTextPartArea().bottom())) {
                        output.addControlMovedToNextPage(output2, controlCharInfo);
                    } else {
                        if (output.addChildOutput(output2)) {
                            textFlowCalculator.add(controlCharInfo, output2.areaWithOuterMargin());
                        }

                        TextLine firstRedrawingTextLine = output.deleteRedrawingTextLine(controlCharInfo.areaWithOuterMargin());

                        if (firstRedrawingTextLine.firstChar() != null) {
                            throw new RedrawException(firstRedrawingTextLine.paraIndex(),
                                    firstRedrawingTextLine.firstChar().index(),
                                    firstRedrawingTextLine.firstChar().prePosition(),
                                    firstRedrawingTextLine.area().top());
                        } else {
                            if (firstRedrawingTextLine.firstChar() != null) {
                                throw new RedrawException(firstRedrawingTextLine.paraIndex(), 0, 0,
                                        firstRedrawingTextLine.area().top());
                            }
                        }
                    }
                } else {
                    if (output.addChildOutput(output2)) {
                        textFlowCalculator.add(controlCharInfo, output2.areaWithOuterMargin());
                    }
                }
            }
        } else {
            output.addChildOutput(output2);
        }
        textLineDrawer.addControlCharInfo(controlCharInfo);
    }

    private boolean isOver75PercentOfPageHeight(long y) {
        return y - input.pageInfo().bodyArea().top() >= input.pageInfo().bodyArea().height() * 75 / 100;
    }

    private void spanningWord(CharInfoNormal spaceCharInfo) throws Exception {
        if (isAllLineDivideByWord()) {
            if (!textLineDrawer.noDrawingChar()) {
                paraDrawer.saveTextLineAndNewLine();
            }
            if (paraDrawer.drawingState().isEndRecalculating()) {
                input.previousChar(charsOfWord.size() + 1);
            }
            addWordAllChars(true, false);
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.splitByLineAndAdd(charsOfWord, input.paraShape());

            if (paraDrawer.drawingState().isEndRecalculating()) {
                input.previousChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
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
                ((CharInfoControl) charInfo).area(input, paraDrawer.currentTextPartArea());
            }
        }
    }

    public void stopAddingChar() {
        wordSplitter.stopAddingChar(true);
    }

    public void continueAddingChar() {
        wordSplitter.stopAddingChar(false);
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
            CharInfoNormal normalCharInfo = (CharInfoNormal) charInfo;
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
            CharInfoControl controlCharInfo = (CharInfoControl) charInfo;
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
}
