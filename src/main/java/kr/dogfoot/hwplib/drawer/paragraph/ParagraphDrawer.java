package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ParagraphDrawer {
    private PagePainter pagePainter;
    private DrawingInfo info;

    private TextLineDrawer textLineDrawer;
    private WordSplitter wordSplitter;
    private TextFlowCalculator textFlowCalculator;
    private boolean cancelNewLine;

    private DrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;

    private Map<Integer, CharInfo> charInfos;

    private Area currentTextLineArea;
    private long lineHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;
    private Area storedTextLineArea;

    private int controlExtendCharIndex;

    private Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(DrawingInfo info) {
        this.pagePainter = null;
        this.info = info;

        textLineDrawer = new TextLineDrawer(info);
        wordSplitter = new WordSplitter(this, info);

        textFlowCalculator = new TextFlowCalculator();

        charInfos = new HashMap<>();
        recalculatingTextAreas = new LinkedList<>();
    }

    public ParagraphDrawer(PagePainter pagePainter, DrawingInfo info) {
        this.pagePainter = pagePainter;
        this.info = info;

        textLineDrawer = new TextLineDrawer(info);
        wordSplitter = new WordSplitter(this, info);

        textFlowCalculator = new TextFlowCalculator();

        charInfos = new HashMap<>();
        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(Paragraph paragraph) throws Exception {
        info.startParagraph(paragraph);

        initialize();

        drawingState = DrawingState.Normal;
        newLineAtRecalculating = false;
        newLineAtNormal = false;
        if (info.noText()) {
            saveTextLineAndNewLine();
        } else {
            processChar();
        }
        long paragraphHeight = currentTextLineArea.top() - info.paragraphArea().top();
        if (paragraph.getHeader().isLastInList()) {
            paragraphHeight -= (lineHeight - textLineDrawer.maxCharHeight());
        }
        boolean newPage = info.endParagraph(paragraphHeight);
        if (newPage) {
            saveAndNewPage();
        }
    }

    private void initialize() {
        charInfos.clear();

        wordSplitter
                .resetWord();

        currentTextLineArea = new Area(info.paragraphArea()).height(0);
        textLineDrawer
                .initialize()
                .addNewTextPart(currentTextLineArea);

        controlExtendCharIndex = 0;

        recalculatingTextAreas.clear();
    }

    private void processChar() throws Exception {
        while (info.nextChar()) {
            switch (info.character().getType()) {
                case Normal:
                    normalChar(normalCharInfo((HWPCharNormal) info.character()));
                    break;
                case ControlChar:
                    controlChar((HWPCharControlChar) info.character());
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    controlExtend(controlCharInfo((HWPCharControlExtend) info.character()));
                    break;
            }

            switch (drawingState) {
                case StartRecalculating:
                    startRecalculating();
                    drawingState = DrawingState.Recalculating;
                    break;
                case EndRecalculating:
                    endRecalculating();
                    drawingState = DrawingState.Normal;
                    break;
                case StartRedrawing:
                    startRedraw();
                    drawingState = DrawingState.Normal;
                    break;
            }
        }
    }

    public NormalCharInfo normalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        NormalCharInfo charInfo = (NormalCharInfo) charInfos.get(info.charIndex());
        if (charInfo == null) {
            charInfo = newNormalCharInfo(ch);
            charInfos.put(info.charIndex(), charInfo);
        }
        return charInfo;
    }

    private NormalCharInfo newNormalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        return new NormalCharInfo(ch, info.charShape(), info.charIndex(), info.charPosition())
                .calculateWidth();
    }

    private ControlCharInfo controlCharInfo(HWPCharControlExtend ch) {
        ControlCharInfo charInfo = (ControlCharInfo) charInfos.get(info.charIndex());
        if (charInfo == null) {
            charInfo = newControlCharInfo(ch);
            charInfos.put(info.charIndex(), charInfo);
            controlExtendCharIndex++;
        }
        return charInfo;
    }

    private ControlCharInfo newControlCharInfo(HWPCharControlExtend ch) {
        ControlCharInfo charInfo = new ControlCharInfo(ch, info.charShape(), info.charIndex(), info.charPosition());
        if (ch.getCode() == 11) {
            Control control = info.paragraph().getControlList().get(controlExtendCharIndex);
            CtrlHeaderGso gsoHeader = null;
            switch (control.getType()) {
                case Table:
                    gsoHeader = ((ControlTable) control).getHeader();
                    break;
                case Gso:
                    gsoHeader = ((GsoControl) control).getHeader();
                    break;
            }
            if (gsoHeader != null) {
                charInfo
                        .control(control, gsoHeader)
                        .area(info);
            }
        }
        return charInfo;
    }

    private void startRecalculating() {
        wordSplitter.resetWord();

        currentTextLineArea = recalculatingTextAreas.poll();
        textLineDrawer
                .reset()
                .addNewTextPart(currentTextLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void endRecalculating() {
        currentTextLineArea = storedTextLineArea.moveY(lineHeight);
        textLineDrawer
                .reset()
                .addNewTextPart(currentTextLineArea);
    }

    private void startRedraw() {
        wordSplitter.resetWord();

        textLineDrawer
                .reset()
                .addNewTextPart(currentTextLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void normalChar(NormalCharInfo charInfo) throws Exception {
        if (!charInfo.character().isSpace()) {
            wordSplitter.addCharOfWord(charInfo);
        } else {
            addWordToLine(charInfo);
        }
    }

    private void addWordToLine(NormalCharInfo spaceCharInfo) throws Exception {
        if (wordSplitter.noChar()) {
            addSpaceCharToLine(spaceCharInfo);
            return;
        }
        if (!isOverRight(wordSplitter.wordWidth(), false)) {
            addWordAllCharsToLine(wordSplitter.charsOfWord(), false, false);
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            if (!isOverRight(wordSplitter.wordWidth(), true)) {
                addWordAllCharsToLine(wordSplitter.charsOfWord(), false, true);
                wordSplitter.resetWord();

                textLineDrawer.setBestSpaceRate();
                saveTextLineAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
    }

    public void addSpaceCharToLine(NormalCharInfo spaceCharInfo) {
        if (drawingState.canAddChar() && spaceCharInfo != null) {
            textLineDrawer.addChar(spaceCharInfo);
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            addCharToLine(charInfo, checkOverRight, applyMinimumSpace);
        }
    }

    public boolean addCharToLine(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && isOverRight(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                textLineDrawer.setBestSpaceRate();
            }
            saveTextLineAndNewLine();
            if (drawingState == DrawingState.Normal || drawingState == DrawingState.EndRecalculating) {
                hasNewLine = true;
            } else {
                hasNewLine = false;
            }
        } else {
            hasNewLine = false;
        }
        textLineDrawer.justNewLine(false);
        addCharToLine(charInfo);
        return hasNewLine;
    }

    private void addCharToLine(CharInfo charInfo) throws Exception {
        if (drawingState.canAddChar()) {
            if (noNormalCharAtTextLine() && drawingState == DrawingState.Normal) {
                checkNewPage();
                setLineFirst((charInfo.index() - 1),  (charInfo.position() - charInfo.character().getCharSize()));
            }

            if (drawingState == DrawingState.Normal && charInfo.type() == CharInfo.Type.Control) {
                ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;

                if (controlCharInfo.isLikeLetter() == false) {
                    if (controlCharInfo.textFlowMethod() == 0/*어울림*/) {
                        textFlowCalculator.addForSquare(controlCharInfo);
                    } else if (controlCharInfo.textFlowMethod() == 1/*자리차지*/) {
                        textFlowCalculator.addForTopBottom(controlCharInfo);
                    }

                    if (controlCharInfo.textFlowMethod() == 2/*뒤로*/) {
                        info.contentBuffer().addBehindControl(controlCharInfo);
                    } else {
                        info.contentBuffer().addNotBehindControl(controlCharInfo);
                    }
                } else {
                    textLineDrawer.addChar(charInfo);
                }
            } else {
                textLineDrawer.addChar(charInfo);
            }
        }
    }

    private void checkNewPage() throws Exception {
        if (isOverBottom(textLineDrawer.maxCharHeight())) {
            if (info.isBodyText() == true) {
                saveAndNewPage();
            }
        }
    }

    private void saveAndNewPage() throws Exception {
        if (info.isBodyText()) {
            pagePainter.saveCurrentPage();
            info.newPage();
        }

        currentTextLineArea.top(info.pageArea().top());
        textLineDrawer.area(currentTextLineArea);
        textFlowCalculator.reset();
    }

    private void setLineFirst(int index, int position) {
        lineFirstCharIndex = index;
        lineFirstCharPosition = position;
    }

    public void saveTextLineAndNewLine() throws Exception {
        if (!textLineDrawer.justNewLine() && drawingState.canAddChar()) {
            cancelNewLine = false;
            lineHeight = textLineDrawer.lineHeight();
            checkTextFlow();
            saveTextLine();
            nextArea();

            newLineAtRecalculating = false;
            newLineAtNormal = false;
        }
    }

    private boolean isOverBottom(long height) {
        return currentTextLineArea.top() + height > info.pageArea().bottom();
    }

    private void checkTextFlow() {
        if (drawingState == DrawingState.Normal) {
            currentTextLineArea
                    .height(textLineDrawer.maxCharHeight());
            TextFlowCalculator.Result result = textFlowCalculator.calculate(currentTextLineArea);
            currentTextLineArea
                    .moveY(result.offsetY());
            cancelNewLine = result.cancelNewLine();

            textLineDrawer.area(currentTextLineArea);
            drawingState = result.nextState();

            if (drawingState == DrawingState.StartRecalculating) {
                storedTextLineArea = currentTextLineArea;
                for (Area dividedArea : result.dividedAreas()) {
                    recalculatingTextAreas.offer(dividedArea);
                }
            }
        }
    }

    private void saveTextLine() {
        switch (drawingState) {
            case Normal:
                textLineDrawer.saveToContentBuffer();
                if (newLineAtNormal) {
                    info.contentBuffer().setLastTextPartToLastLine();
                }
                break;

            case Recalculating:
                if (recalculatingTextAreas.size() == 0 || newLineAtRecalculating) {
                    textLineDrawer.saveToContentBuffer();
                    if (newLineAtRecalculating) {
                        info.contentBuffer().setLastTextPartToLastLine();
                    }
                    drawingState = DrawingState.EndRecalculating;
                }
                break;
        }
    }

    private void nextArea() {
        switch(drawingState) {
            case Normal:
            case StartRedrawing:
                if (!cancelNewLine) {
                    currentTextLineArea.moveY(lineHeight);
                }
                textLineDrawer
                        .reset()
                        .addNewTextPart(currentTextLineArea);
                break;
            case Recalculating:
                currentTextLineArea = recalculatingTextAreas.poll();
                textLineDrawer
                        .resetPart()
                        .addNewTextPart(currentTextLineArea);
                break;
        }
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        ArrayList<CharInfo> charsOfWord = wordSplitter.charsOfWord();

        if (isAllLineDivideByWord(info.paraShape())) {
            if (!noNormalCharAtTextLine()) {
                saveTextLineAndNewLine();
            }

            if (drawingState == DrawingState.EndRecalculating) {
                info.beforeChar(charsOfWord.size() + 1);
            }
            addWordAllCharsToLine(charsOfWord, true, false);
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split();
            if (drawingState == DrawingState.EndRecalculating) {
                info.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        }
    }

    private boolean isAllLineDivideByWord(ParaShape paraShape) {
        return paraShape.getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                && paraShape.getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord;
    }

    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            addWordToLine(null);
            if (drawingState == DrawingState.Recalculating) {
                newLineAtRecalculating = true;
            } else if (drawingState == DrawingState.Normal) {
                newLineAtNormal = true;
            }
            saveTextLineAndNewLine();
        }
    }

    private void controlExtend(ControlCharInfo charInfo) {
        if (charInfo.character().getCode() == 11) {
            wordSplitter.addCharOfWord(charInfo);
        }
    }

    public boolean isOverRight(double width, boolean applyMinimumSpace) {
        return textLineDrawer.isOverRight(width, applyMinimumSpace);
    }

    public boolean noNormalCharAtTextLine() {
        return textLineDrawer.noNormalChar();
    }

    public enum DrawingState {
        Normal,
        StartRecalculating,
        Recalculating,
        EndRecalculating,
        StartRedrawing;

        public boolean canAddChar() {
            return this == Normal || this == Recalculating;
        }
    }
}
