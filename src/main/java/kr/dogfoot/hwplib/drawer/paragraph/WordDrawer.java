package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class WordDrawer {
    private final DrawingInput input;
    private final ParaListDrawer paraListDrawer;
    private final TextLineDrawer textLineDrawer;
    private final WordSplitter wordSplitter;

    private final ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    private boolean checkOverRight;
    private boolean applyMinimumSpace;


    public WordDrawer(DrawingInput input, ParaListDrawer paraListDrawer, TextLineDrawer textLineDrawer) {
        this.input = input;
        this.paraListDrawer = paraListDrawer;
        this.textLineDrawer = textLineDrawer;
        wordSplitter = new WordSplitter(paraListDrawer, textLineDrawer, this);

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
            if (textLineDrawer.noDrawingCharacter() && paraListDrawer.drawingState().isNormal()) {
                paraListDrawer.checkNewColumnAndPage();
                textLineDrawer.firstCharInfo(charInfo);
            }

            if (paraListDrawer.drawingState().isNormal() && charInfo.type() == CharInfo.Type.Control) {
                paraListDrawer.addControlChar((ControlCharInfo) charInfo);
            } else {
                textLineDrawer.addChar(charInfo);
            }
        }

        return hasNewLine;
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        if (isAllLineDivideByWord()) {
            if (!textLineDrawer.noDrawingCharacter()) {
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

    public void stopAddingCharAtSplittingWord() {
        wordSplitter.stopAddingChar();
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
}
