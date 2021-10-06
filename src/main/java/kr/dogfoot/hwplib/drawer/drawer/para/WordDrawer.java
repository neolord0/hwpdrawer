package kr.dogfoot.hwplib.drawer.drawer.para;

import kr.dogfoot.hwplib.drawer.drawer.RedrawException;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoNormal;
import kr.dogfoot.hwplib.drawer.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
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
    private ArrayList<TableOutput> addedSplitTables;

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
        if (paraDrawer.drawingState().canAddChar() && spaceCharInfo != null && !wordSplitter.stopAddingChar()) {
            textLineDrawer.addChar(spaceCharInfo);
        }
    }

    private void addWordAllChars(boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : charsOfWord) {
            addChar(charInfo, checkOverRight, applyMinimumSpace);

            if (wordSplitter.stopAddingChar()) {
                break;
            }
        }
    }

    public boolean addChar(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && textLineDrawer.isOverWidth(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                textLineDrawer.setBestSpaceRate();
            }
            paraDrawer.saveTextLineAndNewLine();

            if (paraDrawer.drawingState().isNormal()
                    || paraDrawer.drawingState().isEndRecalculating()) {
                hasNewLine = true;
            } else {
                hasNewLine = false;
            }

            if (paraDrawer.drawingState().isStartRecalculating()
                    || paraDrawer.drawingState().isStartRedrawing()) {
                wordSplitter.stopAddingChar(true);
            }
        } else {
            hasNewLine = false;
        }

        textLineDrawer.justNewLine(false);

        if (paraDrawer.drawingState().canAddChar()) {
            if (textLineDrawer.noDrawingChar()) {
                textLineDrawer.firstDrawingCharInfo(charInfo);
                if (textLineDrawer.controlOutputCount() == 0) {
                    textLineDrawer.firstCharInfo(charInfo);
                }
                paraDrawer.checkNewColumnAndPage();
            }

            if (wordSplitter.stopAddingChar() == false) {
                if (charInfo.type() == CharInfo.Type.Control) {
                    addCharInfoControl((CharInfoControl) charInfo);
                } else {
                    if (!textLineDrawer.isOverWidth(charInfo.width(), applyMinimumSpace)) {
                        textLineDrawer.addChar(charInfo);
                    }
                }
            }
        }
        return hasNewLine;
    }

    private void addCharInfoControl(CharInfoControl charInfoControl) throws Exception {
        ControlOutput controlOutput = drawControl(charInfoControl);
        if (controlOutput != null) {
            if (controlOutput.controlCharInfo().isLikeLetter()) {
                textLineDrawer.addChar(controlOutput.controlCharInfo());
            } else {
                addControlOutputToPage(controlOutput);
                if (!controlOutput.isSplitTable()) {
                    textLineDrawer.addControlOutput(controlOutput);
                }
            }

            if (controlOutput.isSplitTable()) {
                textLineDrawer.hasSplitTable(true);
            }
        }
    }

    private ControlOutput drawControl(CharInfoControl controlCharInfo) throws Exception {
        if (controlCharInfo.control().getType() == ControlType.Table
                && alreadyAddedSplitTable(controlCharInfo.control())) {
            return null;
        }
        return controlDrawer.draw(controlCharInfo);
    }

    private boolean alreadyAddedSplitTable(Control control) {
        if (addedSplitTables == null) {
            return false;
        }
        for (TableOutput tableOutput : addedSplitTables) {
            if (tableOutput.controlCharInfo().control() == control) {
                return true;
            }
        }
        return false;
    }

    private void addControlOutputToPage(ControlOutput controlOutput) throws RedrawException {
        CharInfoControl controlCharInfo = controlOutput.controlCharInfo();
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText
                || controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            if (!textFlowCalculator.alreadyAdded(controlCharInfo)) {
                if (output.checkRedrawingTextLine(controlOutput.areaWithOuterMargin())) {
                    if (isOver75PercentOfPageHeight(paraDrawer.currentTextArea().bottom())) {
                        output.addChildControlsCrossingPage(controlOutput);
                    } else {
                        if (output.addChildOutput(controlOutput)) {
                            textFlowCalculator.add(controlCharInfo, controlOutput.areaWithOuterMargin());
                        }
                        TextLine firstRedrawingTextLine = output.deleteRedrawingTextLine(controlCharInfo.areaWithOuterMargin());
                        if (firstRedrawingTextLine.firstChar() != null) {
                            throw new RedrawException(firstRedrawingTextLine.paraIndex(),
                                    firstRedrawingTextLine.firstChar().charIndex(),
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
                    if (output.addChildOutput(controlOutput)) {
                        textFlowCalculator.add(controlCharInfo, controlOutput.areaWithOuterMargin());
                    }
                }
            }
        } else {
            output.addChildOutput(controlOutput);
        }

        if (controlCharInfo.output().isSplitTable()) {
            TableOutput tableOutput = (TableOutput) controlCharInfo.output();
            if (tableOutput.split() && tableOutput.getDivideAtPageBoundary() == DivideAtPageBoundary.DivideByCell) {
               Area areaToPageBottom = new Area(output.currentPage().bodyArea())
                       .top(controlOutput.areaWithOuterMargin().bottom());
               textFlowCalculator.addForTakePlace(controlCharInfo, areaToPageBottom);;
            }
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

    public void adjustControlAreaAtNewPage(Area currentTextArea) {
        for (CharInfo charInfo : charsOfWord) {
            if (charInfo.type() == CharInfo.Type.Control) {
                ((CharInfoControl) charInfo).area(input, currentTextArea);
            }
        }
    }

    public void addSplitTables(ArrayList<TableOutput> splitTables) throws RedrawException {
        for (TableOutput tableOutput : splitTables) {
            if (tableOutput.controlCharInfo().isLikeLetter()) {
                textLineDrawer.addChar(tableOutput.controlCharInfo());
            } else {
                addControlOutputToPage(tableOutput);
                if (!tableOutput.split()) {
                    textLineDrawer.addControlOutput(tableOutput);
                }
            }
        }
        addedSplitTables = splitTables;
    }

    public void addChildControls(ControlOutput[] childControls, boolean inCell) throws RedrawException {
        if (childControls == null) {
            return;
        }

        for (ControlOutput childOutput : childControls) {
            if (inCell) {
                CellOutput cellOutput = (CellOutput) output.currentOutput();
                childOutput.areaWithoutOuterMargin()
                        .moveY(cellOutput.cell().getListHeader().getTopMargin() - childOutput.areaWithoutOuterMargin().top());
            }
            if (childOutput.controlCharInfo().isLikeLetter()) {
                textLineDrawer.addChar(childOutput.controlCharInfo());
            } else {
                addControlOutputToPage(childOutput);
                if (!childOutput.isSplitTable()) {
                    textLineDrawer.addControlOutput(childOutput);
                }
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
                        .append(charInfo.charIndex())
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
                        .append(charInfo.charIndex())
                        .append(")");
            } else {
                sb
                        .append(controlCharInfo.control().getType())
                        .append("(")
                        .append(charInfo.charIndex())
                        .append(")");
            }
        }
        return sb.toString();
    }

}
