package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
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
import org.apache.poi.ss.formula.functions.T;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class ParagraphDrawer {
    private HWPDrawer drawer;
    private WordSplitter wordSplitter;
    private TextFlowCalculator textFlowCalculator;

    private DrawingState drawingState;
    private DrawingInfo info;

    private Map<Integer, CharInfo> charInfos;

    private Area currentTextLineArea;
    private long lineHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;
    private Area storedTextLineArea;

    private int controlExtendCharIndex;

    private Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(HWPDrawer drawer) {
        this.drawer = drawer;
        wordSplitter = new WordSplitter(drawer);
        textFlowCalculator = new TextFlowCalculator();
        charInfos = new HashMap<>();

        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(Paragraph paragraph, DrawingInfo info) throws Exception {
        info.startParagraph(paragraph);
        initialize(info);

        drawingState = DrawingState.Normal;

        drawer.controlDrawer()
                .controlList(info.paragraph().getControlList(), info)
                .drawControlsForBehind()
                .removeControlsForBehind();

        textFlowCalculator.setControls(drawer.controlDrawer());

        if (info.noText()) {
            drawTextAndNewLine();
        } else {
            processChar();
        }
        drawer.controlDrawer()
                .drawControlsForTopBottom()
                .removeControlsForTopBottom();

        long paragraphHeight =  currentTextLineArea.top() - info.paragraphArea().top();

        boolean newPage = info.endParagraph(paragraphHeight);
        if (newPage) {
            drawer.pageMaker().newPage(info);
        }
    }

    private void initialize(DrawingInfo info) {
        this.info = info;
        charInfos.clear();

        wordSplitter
                .info(info)
                .resetWord();

        currentTextLineArea = new Area(info.paragraphArea()).height(0);
        drawer.textLineDrawer()
                .initialize(info)
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
                .calculateWidth(drawer.painter());
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
            CtrlHeaderGso headerGso = null;
            switch (control.getType()) {
                case Table:
                    headerGso = ((ControlTable) control).getHeader();
                    break;
                case Gso:
                    headerGso = ((GsoControl) control).getHeader();
                    break;
            }
            if (headerGso != null && headerGso.getProperty().isLikeWord()) {
                charInfo.control(control);
            }
        }
        return charInfo;
    }

    private void startRecalculating() {
        wordSplitter.resetWord();

        currentTextLineArea = recalculatingTextAreas.poll();
        drawer.textLineDrawer()
                .reset()
                .addNewTextPart(currentTextLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void endRecalculating() {
        currentTextLineArea = storedTextLineArea.moveY(lineHeight);
        drawer.textLineDrawer()
                .reset()
                .addNewTextPart(currentTextLineArea);
    }

    private void startRedraw() {
        wordSplitter.resetWord();

        drawer.textLineDrawer()
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

        if (!drawer.textLineDrawer().isOverRight(wordSplitter.wordWidth(), false)) {
            addWordAllCharsToLine(wordSplitter.charsOfWord(), false, false);
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            if (!drawer.textLineDrawer().isOverRight(wordSplitter.wordWidth(), true)) {
                addWordAllCharsToLine(wordSplitter.charsOfWord(), false, true);
                wordSplitter.resetWord();

                drawer.textLineDrawer().setBestSpaceRate();
                drawTextAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
    }

    public void addSpaceCharToLine(NormalCharInfo spaceCharInfo) {
        if (drawingState.canAddChar() && spaceCharInfo != null) {
            drawer.textLineDrawer().addChar(spaceCharInfo);
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            addCharToLine(charInfo, checkOverRight, applyMinimumSpace);
        }
    }

    public boolean addCharToLine(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && drawer.textLineDrawer().isOverRight(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                drawer.textLineDrawer().setBestSpaceRate();
            }
            hasNewLine = true;
            drawTextAndNewLine();
        } else {
            hasNewLine = false;
        }
        drawer.textLineDrawer().justNewLine(false);
        addCharToLine(charInfo);
        return hasNewLine;
    }

    private void addCharToLine(CharInfo charInfo) {
        if (drawingState.canAddChar()) {
            if (drawer.textLineDrawer().noNormalChar() && drawingState == DrawingState.Normal) {
                setLineFirst((charInfo.index() - 1),  (charInfo.position() - charInfo.character().getCharSize()));
            }

            drawer.textLineDrawer().addChar(charInfo);
        }
    }

    private void setLineFirst(int index, int position) {
        lineFirstCharIndex = index;
        lineFirstCharPosition = position;
    }

    public void drawTextAndNewLine() throws Exception {
        if (!drawer.textLineDrawer().justNewLine()) {
            lineHeight = drawer.textLineDrawer().lineHeight();
            checkNewPage();
            checkTextFlow();
            drawTextLine();
            nextArea();
        }
    }

    private void checkNewPage() throws IOException {
        if (isOverBottom(drawer.textLineDrawer().maxCharHeight())) {
            if (info.isBodyText() == true) {
                drawer.controlDrawer()
                        .drawControlsForFront()
                        .removeControlsForFront();

                drawer.pageMaker().newPage(info);

                currentTextLineArea.top(info.pageArea().top());
                drawer.textLineDrawer().area(currentTextLineArea);
            }
        }
    }

    private boolean isOverBottom(long height) {
        return currentTextLineArea.top() + height > info.pageArea().bottom();
    }

    private void checkTextFlow() {
        if (drawingState == DrawingState.Normal) {
            currentTextLineArea
                    .height(drawer.textLineDrawer().maxCharHeight());
            TextFlowCalculator.Result result = textFlowCalculator.calculate(currentTextLineArea);
            currentTextLineArea
                    .moveY(result.offsetY());
            drawer.textLineDrawer().area(currentTextLineArea);

            drawingState = result.nextState();

            if (drawingState == DrawingState.StartRecalculating) {
                storedTextLineArea = currentTextLineArea;
                for (Area dividedArea : result.dividedAreas()) {
                    recalculatingTextAreas.offer(dividedArea);
                }
            }
        }
    }

    private void drawTextLine() throws UnsupportedEncodingException {
        switch (drawingState) {
            case Normal:
                drawer.textLineDrawer().draw();
                break;
            case Recalculating:
                if (isLastTextPart()) {
                    drawer.textLineDrawer().draw();
                }
                break;
        }
    }

    private boolean isLastTextPart() {
        return recalculatingTextAreas.size() == 0 || drawer.textLineDrawer().lastLine();
    }

    private void nextArea() {
        switch(drawingState) {
            case Normal:
            case StartRedrawing:
                currentTextLineArea.moveY(lineHeight);
                drawer.textLineDrawer()
                        .reset()
                        .addNewTextPart(currentTextLineArea);
                break;
            case Recalculating:
                if (isLastTextPart()) {
                    drawingState = DrawingState.EndRecalculating;
                } else {
                    currentTextLineArea = recalculatingTextAreas.poll();
                    drawer.textLineDrawer()
                            .resetPart()
                            .addNewTextPart(currentTextLineArea);
                }
                break;
        }
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        ArrayList<CharInfo> charsOfWord = wordSplitter.charsOfWord();

        if (isAllLineDivideByWord(info.paraShape())) {
            if (!drawer.textLineDrawer().noNormalChar()) {
                drawTextAndNewLine();
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

            drawer.textLineDrawer().lastLine(true);
            drawTextAndNewLine();
        }
    }

    private void controlExtend(ControlCharInfo charInfo) {
        wordSplitter.addCharOfWord(charInfo);
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
