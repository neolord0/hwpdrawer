package kr.dogfoot.hwpdrawer.engine.control;

import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlContainer;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlRectangle;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponent;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.ListHeaderForTextBox;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.ParaText;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.engine.ParaShapeHelper;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPictureItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPolygonItem;
import kr.dogfoot.hwpdrawer.document.control.GsoShapeItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.obstacle.ParaObstacles;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문단 내 컨트롤(이미지, 글상자, 표 등)을 감지하고 ControlItem으로 변환해 Page에 등록한다.
 *
 * <p>글자처럼(isLikeWord) 컨트롤은 건너뛴다 — 텍스트 흐름에 인라인이므로 별도 처리.
 * 내부 문단 레이아웃(텍스트박스, 표 셀)은 InnerLayouter 콜백으로 위임한다.</p>
 */
public class ControlLayouter {

    /** 텍스트박스 / 표 셀의 내부 문단 목록을 레이아웃하는 콜백.
     *  balanceLastBlock=true면 마지막 다단 블록도 최소화(균형 배분)한다.
     *  innerArea는 첫 페이지 본문 영역, pageBodyArea는 페이지를 넘긴 후속 페이지 본문
     *  영역이다. 표 셀나누기처럼 첫 페이지와 후속 페이지의 가용 높이가 다를 때 분리한다. */
    @FunctionalInterface
    public interface InnerLayouter {
        Document layout(kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList paragraphs,
                        Area innerArea, Area pageBodyArea, Area paperArea, boolean balanceLastBlock);

        /** 첫 페이지와 후속 페이지의 본문 영역이 같은 경우의 단축 호출. */
        default Document layout(kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList paragraphs,
                                Area innerArea, Area paperArea, boolean balanceLastBlock) {
            return layout(paragraphs, innerArea, innerArea, paperArea, balanceLastBlock);
        }
    }

    private final InnerLayouter innerLayouter;

    /**
     * 행 박스가 페이지를 넘어 분할될 때 HWP 가 page1 페이지 끝에 남기는 빈 간격(≈1pt).
     * 한글은 분할되는 행 박스를 페이지 끝까지 꽉 채우지 않고 약 1pt 를 비워둔다 — LineSeg
     * 원본의 표 뒤 본문 vertPos 와 대조해 확정한 값(뒷페이지·병합된셀/1 두 표 모두 page2
     * 조각이 정확히 이만큼 짧아야 LineSeg 정답과 일치). 셀 여백·표 종류와 무관한 상수.
     */
    private static final long SPLIT_ROW_PAGE_GAP = 101;

    /**
     * 분할 표 셀 레이아웃 중 어울림(FitWithText) obstacle 의 page2 클립 경계(셀 로컬 y).
     * 박스 center 가 이 값 이상이면(= 표 분할에서 박스가 page2 로 가면) 그 obstacle 의
     * top 을 이 값으로 끌어내려, page1(경계 위) 본문 줄이 page2 박스에 래핑되지 않게 한다.
     * {@code Long.MAX_VALUE}이면 클립 없음(일반 레이아웃). 셀 레이아웃 호출 직전에 설정한다.
     */
    private long cellFitClipLocal = Long.MAX_VALUE;
    /** 같은 문단에 앵커된 다중 TakePlace 표 스태킹: 직전 표의 paraIdx와 하단(여백 포함). */
    private int stackedTableParaIdx = -1;
    private long stackedTableBottom;

    public ControlLayouter(InnerLayouter innerLayouter) {
        this.innerLayouter = innerLayouter;
    }

    /**
     * 문단 텍스트 레이아웃 전에 이 문단에 속한 FitWithText(어울림) 컨트롤들의 영역을
     * 미리 계산하여 obstacles에 등록한다. NoLineSeg 문단이 자신의 anchor 컨트롤 주변으로
     * 래핑할 수 있게 한다.
     */
    /**
     * vertRelTo=Para 컨트롤의 효과적 paraTopY 를 anchor line 기준으로 계산한다 (내부 doc 한정).
     *
     * <p>한 paragraph 가 여러 라인으로 wrap 된 경우, 컨트롤은 anchor 된 LINE 기준으로 위치해야
     * 한다. 단, anchor 라인이 어울림 wrap segment(같은 y 에 여러 라인이 분할된 그룹)에 속하면,
     * 그 wrap 그룹 바로 위의 distinct y 를 기준으로 한다 — wrap 된 segment 는 시각적으로 한
     * 행이지만, 한글 OK 동작상 컨트롤의 vRelTo=Para 기준은 그 행이 시작되기 전(직전 row top)이다.
     * yOff 가 lineHeight 이상이면 명시적으로 다음 줄로 push 하려는 의도이므로 anchor 라인 top 사용.</p>
     */
    private static long innerDocEffectiveParaTopY(CtrlHeaderGso header, TextLine anchorLine,
                                                   long paraTopY, Document doc) {
        if (anchorLine == null
                || header.getProperty().getVertRelTo() != VertRelTo.Para
                || anchorLine.area().top() <= paraTopY) {
            return paraTopY;
        }
        long anchorTop = anchorLine.area().top();
        long lineHeight = anchorLine.area().bottom() - anchorTop;
        // cellDoc 이 여러 페이지면 각 페이지가 자기 좌표 0부터 시작하므로 다른 페이지의 line 이
        // 같은 y 를 가질 수 있다. anchor 가 속한 페이지로 한정해 wrap segment 를 감지한다.
        Page anchorPage = null;
        outer:
        for (Page p : doc.pages()) {
            for (TextRow row : p.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine ln : col.textLines()) {
                        if (ln == anchorLine) { anchorPage = p; break outer; }
                    }
                }
            }
        }
        int sameYCount = 0;
        long prevDistinctY = paraTopY;
        if (anchorPage != null) {
            for (TextRow row : anchorPage.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine ln : col.textLines()) {
                        long t = ln.area().top();
                        if (t == anchorTop) sameYCount++;
                        else if (t < anchorTop && t > prevDistinctY) prevDistinctY = t;
                    }
                }
            }
        }
        boolean useWrapPrev = (sameYCount > 1) && (header.getyOffset() < lineHeight);
        return useWrapPrev ? prevDistinctY : anchorTop;
    }

    /**
     * 셀/글상자 내부 단락에 mid-paragraph 로 앵커된 어울림(FitWithText) 컨트롤의 obstacle 을
     * anchor 줄 위치 기준으로 등록한다. (paraTopY 단락 최상단이 아니라 컨트롤이 실제로
     * 그려지는 줄에 맞춤.) 2-pass 레이아웃의 1차 결과(srcDoc)에서 각 컨트롤의 anchor 줄을 찾아
     * obstacle 영역을 계산한다 — {@link #processControl} 의 박스 draw 위치와 동일한 계산.
     *
     * @param para     컨트롤이 속한 단락
     * @param paraIdx  단락 인덱스 (srcDoc 내 line.paraIndex 매칭용)
     * @param srcDoc   1차 레이아웃 결과 문서 (anchor 줄 조회)
     * @param ctx      좌표 계산용 ctx (paperArea/bodyArea/currentParaTopY)
     * @param out      계산된 obstacle 을 담을 대상 (보통 ctx.obstacles(), 반복 추정 시 임시 인스턴스)
     */
    public void collectInnerFitWithTextObstacles(Paragraph para, int paraIdx,
                                                  Document srcDoc, ParaLayoutContext ctx,
                                                  ParaObstacles out) {
        ParaText paraText = para.getText();
        List<Control> controlList = para.getControlList();
        if (paraText == null || controlList == null || controlList.isEmpty()) return;
        // ControlExtend 문자 등장 순서대로 (charIdx, controlIdx) 수집.
        int controlIdx = 0, charIdx = 0;
        for (HWPChar ch : paraText.getCharList()) {
            if (ch.getType() == HWPCharType.ControlExtend) {
                HWPCharControlExtend ce = (HWPCharControlExtend) ch;
                if (ce.getCode() == 11 && controlIdx < controlList.size()) {
                    Control ctrl = controlList.get(controlIdx);
                    CtrlHeaderGso header = gsoHeaderOf(ctrl);
                    if (header != null
                            && !header.getProperty().isLikeWord()
                            && header.getProperty().getTextFlowMethod() == TextFlowMethod.FitWithText) {
                        LineLocation loc = findLineLocation(srcDoc, paraIdx, charIdx);
                        TextLine anchorLine = (loc != null) ? loc.line : null;
                        long eptY = innerDocEffectiveParaTopY(header, anchorLine,
                                ctx.currentParaTopY(), srcDoc);
                        Area paraArea = (anchorLine != null)
                                ? anchorLine.area().clone() : ctx.bodyArea().clone();
                        Area area = ControlAreaCalculator.calculate(
                                header, ctx.paperArea(), ctx.bodyArea(), paraArea, eptY);
                        Area areaWithMargin = withMargin(area, header);
                        // page2 박스 shift: 박스가 표 분할에서 page2 로 가는데(center 가 클립
                        // 경계 이상) top 이 경계보다 위면(경계를 걸침), obstacle 을 경계 아래로
                        // 통째로 내린다(top=경계, 높이 유지). 그래야 (1) 경계 위 page1 줄이
                        // 이 박스에 래핑되지 않고, (2) page2 에서 박스가 그려지는 full-height
                        // 만큼(분할 시 segment top 에 정렬) obstacle 도 같은 줄 수를 덮어
                        // 박스 draw 와 래핑이 일치한다(걸처진컨트롤/2 page2 3번째 줄).
                        long center = (areaWithMargin.top() + areaWithMargin.bottom()) / 2;
                        if (center >= cellFitClipLocal && areaWithMargin.top() < cellFitClipLocal) {
                            long h = areaWithMargin.bottom() - areaWithMargin.top();
                            areaWithMargin.top(cellFitClipLocal);
                            areaWithMargin.bottom(cellFitClipLocal + h);
                        }
                        out.add(areaWithMargin.clone(),
                                header.getProperty().getTextHorzArrange(), header.getzOrder());
                    }
                }
                controlIdx++;
            }
            charIdx++;
        }
    }

    /** {@link #collectInnerFitWithTextObstacles} 결과를 ctx.obstacles() 에 바로 등록하는 편의 메서드. */
    public void registerInnerFitWithTextObstacles(Paragraph para, int paraIdx,
                                                   Document srcDoc, ParaLayoutContext ctx) {
        collectInnerFitWithTextObstacles(para, paraIdx, srcDoc, ctx, ctx.obstacles());
    }

    /** 단락에 mid-paragraph(첫 글자가 아닌 위치)로 앵커된 어울림 컨트롤이 있는지. */
    public static boolean hasMidParagraphFitWithText(Paragraph para) {
        ParaText paraText = para.getText();
        List<Control> controlList = para.getControlList();
        if (paraText == null || controlList == null || controlList.isEmpty()) return false;
        int controlIdx = 0, charIdx = 0;
        for (HWPChar ch : paraText.getCharList()) {
            if (ch.getType() == HWPCharType.ControlExtend) {
                HWPCharControlExtend ce = (HWPCharControlExtend) ch;
                if (ce.getCode() == 11 && controlIdx < controlList.size()) {
                    Control ctrl = controlList.get(controlIdx);
                    CtrlHeaderGso header = gsoHeaderOf(ctrl);
                    if (header != null
                            && !header.getProperty().isLikeWord()
                            && header.getProperty().getTextFlowMethod() == TextFlowMethod.FitWithText
                            && header.getProperty().getVertRelTo() == VertRelTo.Para
                            && charIdx > 0) {
                        return true;
                    }
                }
                controlIdx++;
            }
            charIdx++;
        }
        return false;
    }

    /** 셀의 어느 문단이든 mid-paragraph 어울림 컨트롤을 가지는지. */
    private static boolean cellHasMidParagraphFitWithText(Cell cell) {
        kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList pl = cell.getParagraphList();
        for (int i = 0; i < pl.getParagraphCount(); i++) {
            if (hasMidParagraphFitWithText(pl.getParagraph(i))) return true;
        }
        return false;
    }

    public void preRegisterObstacles(Paragraph para, ParaLayoutContext ctx) {
        boolean debug = System.getProperty("debug.preRegister") != null;
        List<Control> controls = para.getControlList();
        if (controls == null || controls.isEmpty()) return;
        for (Control ctrl : controls) {
            CtrlHeaderGso header = gsoHeaderOf(ctrl);
            if (header == null) continue;
            if (header.getProperty().isLikeWord()) continue;
            if (header.getProperty().getTextFlowMethod() != TextFlowMethod.FitWithText) continue;

            Area area = ControlAreaCalculator.calculate(
                    header, ctx.paperArea(), ctx.bodyArea(), ctx.bodyArea().clone(),
                    ctx.currentParaTopY());
            Area areaWithMargin = withMargin(area, header);
            if (debug) {
                System.out.printf("[preRegisterObstacles] paraTopY=%d vrt=%s horzArrange=%s area=(l=%d,t=%d,r=%d,b=%d) withMargin=(l=%d,t=%d,r=%d,b=%d)%n",
                    ctx.currentParaTopY(), header.getProperty().getVertRelTo(),
                    header.getProperty().getTextHorzArrange(),
                    area.left(), area.top(), area.right(), area.bottom(),
                    areaWithMargin.left(), areaWithMargin.top(), areaWithMargin.right(), areaWithMargin.bottom());
            }
            ctx.obstacles().add(areaWithMargin.clone(),
                    header.getProperty().getTextHorzArrange(), header.getzOrder());
        }
    }

    /**
     * 섹션(또는 ParagraphListInterface) 전체를 스캔하여 Paper/Page-relative FitWithText 컨트롤을
     * 현재 시점의 obstacle로 선제 등록한다.
     *
     * <p>NoLineSeg 경로에서 자신의 FitWithText 컨트롤이 없는 문단(예: anchor 이전 문단)은
     * 나중에 등록될 Paper-relative 컨트롤의 영역을 미리 알아야 올바르게 래핑할 수 있다.
     * Paper/Page-relative 컨트롤은 Y 위치가 paraTopY에 무관하게 고정되므로 사전 등록이 가능하다.</p>
     *
     * <p><b>호출 조건</b>: 해당 문단이 자신의 FitWithText 컨트롤을 갖지 않을 때만 호출해야 한다
     * ({@link #hasFitWithTextControl} 참조). 자신의 FitWithText가 있으면 preRegisterObstacles로
     * 충분하며, 다른 문단의 obstacle을 추가하면 잘못된 래핑이 발생한다.</p>
     *
     * <p>동일 obstacle이 per-paragraph preRegisterObstacles에 의해 다시 추가되더라도
     * {@link ParaObstacles#add} 내 중복 검사로 이중 등록이 방지된다.</p>
     *
     * @param paras 섹션 전체 단락 목록
     */
    public void preRegisterPaperAbsoluteObstacles(ParagraphListInterface paras, ParaLayoutContext ctx) {
        int count = paras.getParagraphCount();
        boolean debug = System.getProperty("debug.preRegister") != null;
        for (int i = 0; i < count; i++) {
            Paragraph para = paras.getParagraph(i);
            List<Control> controls = para.getControlList();
            if (controls == null || controls.isEmpty()) continue;
            for (Control ctrl : controls) {
                CtrlHeaderGso header = gsoHeaderOf(ctrl);
                if (header == null) continue;
                if (header.getProperty().isLikeWord()) continue;
                if (header.getProperty().getTextFlowMethod() != TextFlowMethod.FitWithText) continue;
                // Paper/Page-relative 컨트롤만 — 위치가 paraTopY 독립적으로 고정됨.
                VertRelTo vrt = header.getProperty().getVertRelTo();
                if (vrt != VertRelTo.Paper && vrt != VertRelTo.Page) continue;

                Area area = ControlAreaCalculator.calculate(
                        header, ctx.paperArea(), ctx.bodyArea(), ctx.bodyArea().clone(),
                        ctx.bodyArea().top());
                Area areaWithMargin = withMargin(area, header);
                if (debug) {
                    System.out.printf("[preRegisterGlobal] para=%d vrt=%s horzArrange=%s area=(l=%d,t=%d,r=%d,b=%d) withMargin=(l=%d,t=%d,r=%d,b=%d)%n",
                        i, vrt, header.getProperty().getTextHorzArrange(),
                        area.left(), area.top(), area.right(), area.bottom(),
                        areaWithMargin.left(), areaWithMargin.top(), areaWithMargin.right(), areaWithMargin.bottom());
                }
                ctx.obstacles().add(areaWithMargin.clone(),
                        header.getProperty().getTextHorzArrange(), header.getzOrder());
            }
        }
    }

    /**
     * 문단에 자신의 FitWithText(어울림) 컨트롤이 있는지 확인한다.
     * true이면 preRegisterObstacles로 충분하고, 다른 문단의 선제 등록은 불필요(오히려 해로움).
     */
    public static boolean hasFitWithTextControl(Paragraph para) {
        List<Control> controls = para.getControlList();
        if (controls == null || controls.isEmpty()) return false;
        for (Control ctrl : controls) {
            CtrlHeaderGso header = gsoHeaderOf(ctrl);
            if (header == null) continue;
            if (header.getProperty().isLikeWord()) continue;
            if (header.getProperty().getTextFlowMethod() == TextFlowMethod.FitWithText) return true;
        }
        return false;
    }

    /**
     * 문단에서 컨트롤을 찾아, ControlExtend 문자가 위치한 페이지에 추가한다.
     * layoutParagraph() 완료 후 호출 — ctx.document()에 TextLine이 확정된 상태.
     *
     * @param isNoLineSeg true이면 LineSeg 없이 레이아웃된 문단. shouldDefer 판단에 영향을 준다.
     */
    public void layoutControls(Paragraph para, int paraIdx, ParaLayoutContext ctx, boolean isNoLineSeg) {
        // 같은 문단 내 다중 TakePlace 표 스태킹 추적 초기화.
        stackedTableParaIdx = -1;
        stackedTableBottom = 0;
        ParaText paraText = para.getText();
        if (paraText == null) return;
        List<Control> controlList = para.getControlList();
        if (controlList == null || controlList.isEmpty()) return;

        // 컨트롤 순서: ControlExtend 문자 등장 순서대로 (charIdx,controlIdx) 수집.
        // 문단에 분할되는 Divide TakePlace 표가 있으면 표를 먼저 처리해야 한다 — 표 분할
        // 이후 본문 줄이 마지막 조각 페이지로 옮겨지고, 그 다음 같은 문단의 InFrontOfText·
        // BehindText 컨트롤이 옮겨진 본문 줄의 페이지를 anchor로 쓰게 된다.
        java.util.List<int[]> entries = new java.util.ArrayList<>();
        int controlIdx = 0;
        int charIdx = 0;
        for (HWPChar ch : paraText.getCharList()) {
            if (ch.getType() == HWPCharType.ControlExtend) {
                HWPCharControlExtend ce = (HWPCharControlExtend) ch;
                if (ce.getCode() == 11 && controlIdx < controlList.size()) {
                    entries.add(new int[]{charIdx, controlIdx});
                }
                controlIdx++;
            }
            charIdx++;
        }
        // Divide TakePlace 표가 있으면 그것을 entries의 맨 앞으로 이동.
        for (int i = 0; i < entries.size(); i++) {
            Control ctrl = controlList.get(entries.get(i)[1]);
            if (isDivideTakePlaceTable(ctrl)) {
                int[] tableEntry = entries.remove(i);
                entries.add(0, tableEntry);
                break;
            }
        }
        for (int[] entry : entries) {
            int cIdx = entry[0];
            int ctrlIdx = entry[1];
            Control ctrl = controlList.get(ctrlIdx);
            CtrlHeaderGso header = gsoHeaderOf(ctrl);
            LineLocation loc = findLineLocation(ctx.document(), paraIdx, cIdx);
            if (loc != null) {
                if (header != null && header.getProperty().isLikeWord()) {
                    layoutInlineControl(ctrl, header, para, cIdx, loc, ctx);
                } else {
                    processControl(ctrl, ctx, loc.page, ctx.currentParaTopY(), loc.line, paraIdx, para,
                            !hasVisibleText(para), isNoLineSeg);
                }
            }
        }
    }

    /** Divide 속성을 가진 자리차지(TakePlace) 표 컨트롤인지 확인. */
    private static boolean isDivideTakePlaceTable(Control ctrl) {
        if (!(ctrl instanceof ControlTable)) return false;
        CtrlHeaderGso header = gsoHeaderOf(ctrl);
        if (header == null) return false;
        if (header.getProperty().isLikeWord()) return false;
        if (header.getProperty().getTextFlowMethod() != TextFlowMethod.TakePlace) return false;
        return ((ControlTable) ctrl).getTable().getProperty().getDivideAtPageBoundary()
                == DivideAtPageBoundary.Divide;
    }

    /** 지연된 컨트롤들을 섹션 레이아웃 완료 후 배치한다. 각 deferred는 다음 페이지(없으면 생성)에 놓인다. */
    public void placeDeferredControls(ParaLayoutContext ctx) {
        for (kr.dogfoot.hwpdrawer.engine.para.DeferredControl dc : ctx.deferredControls()) {
            Page target = nextPageOrCreate(ctx, dc.anchorPage);
            placeControlOnPage(dc.control, ctx, target);
        }
        ctx.deferredControls().clear();
    }

    /**
     * ctx.createNewPage 콜백: 직전 페이지(anchorPage)에 정박된 지연 컨트롤들을 새 페이지에
     * 즉시 배치한다. 그 컨트롤들이 차지하는 영역의 max bottom Y(여백 포함)을 반환해 ctx 가
     * currentY 를 그 값 위로 끌어올리게 한다.
     *
     * <p>이는 NoDivide 표가 다음 페이지로 밀려난 직후 후속 단락 줄들이 표 위에 겹치지
     * 않도록 한다. 기존 placeDeferredControls 는 섹션 끝에서 한 번에 처리하지만, 그 시점엔
     * 페이지가 이미 텍스트로 채워져 있어 표가 위에 덮어 그려진다.</p>
     */
    public long placeDeferredOnNewPage(ParaLayoutContext ctx, Page anchorPage, Page newPage) {
        long maxBottom = 0;
        java.util.Iterator<kr.dogfoot.hwpdrawer.engine.para.DeferredControl> it =
                ctx.deferredControls().iterator();
        while (it.hasNext()) {
            kr.dogfoot.hwpdrawer.engine.para.DeferredControl dc = it.next();
            if (dc.anchorPage != anchorPage) continue;
            long bottom = placeControlOnPageWithBottom(dc.control, ctx, newPage);
            if (bottom > maxBottom) maxBottom = bottom;
            it.remove();
        }
        return maxBottom;
    }

    /** placeControlOnPage 와 동일하지만, 배치된 areaWithMargin.bottom() 을 반환한다. */
    private long placeControlOnPageWithBottom(Control ctrl, ParaLayoutContext ctx, Page target) {
        CtrlHeaderGso header = gsoHeaderOf(ctrl);
        if (header == null) return 0;
        // 새 페이지 본문 영역 기준으로 area 재계산 — target 의 bodyArea 를 paraArea/paraTopY 로 사용.
        Area pageBody = target.bodyArea();
        Area area = ControlAreaCalculator.calculate(
                header, ctx.paperArea(), pageBody, pageBody.clone(), pageBody.top());
        Area areaWithMargin = withMargin(area, header);
        boolean behindText = (header.getProperty().getTextFlowMethod() == TextFlowMethod.BehindText);
        ControlItem item = createItem(ctrl, area, areaWithMargin, behindText, ctx);
        if (item != null) {
            target.addControl(item);
            return areaWithMargin.bottom();
        }
        return 0;
    }

    private static Page nextPageOrCreate(ParaLayoutContext ctx, Page anchorPage) {
        List<Page> pages = ctx.document().pages();
        int idx = pages.indexOf(anchorPage);
        if (idx >= 0 && idx + 1 < pages.size()) return pages.get(idx + 1);
        Page fresh = ctx.document().addPage();
        fresh.paperArea().copyFrom(ctx.paperArea());
        fresh.bodyArea().copyFrom(ctx.bodyArea());
        return fresh;
    }

    /**
     * 페이지에 이미 배치된 어울림(FitWithText) 표 컨트롤들을 obstacle로 등록한다.
     * 예약 큐(pendingTableSegments)로 배치된 표 둘째 조각은 preRegisterObstacles 경로를
     * 거치지 않으므로, 그 페이지에서 시작하는 본문이 표 주위로 래핑되려면 명시적 등록이 필요하다.
     */
    public void registerFitWithTextTableObstacles(Page page, ParaLayoutContext ctx) {
        for (ControlItem item : page.controls()) {
            if (!(item instanceof TableItem)) continue;
            CtrlHeaderGso header = ((TableItem) item).table().getHeader();
            if (header == null || header.getProperty().isLikeWord()) continue;
            if (header.getProperty().getTextFlowMethod() != TextFlowMethod.FitWithText) continue;
            ctx.obstacles().add(item.areaWithMargin().clone(),
                    header.getProperty().getTextHorzArrange(), header.getzOrder());
        }
    }

    /**
     * 페이지 {@code y} 이하 본문 영역이 비어 있는지 — 본문 텍스트 줄이 {@code y}를
     * 넘어가지 않으면 true. 분할 표가 이전 페이지의 빈 본문 공간에 들어갈 수 있는지
     * 판정한다. 플로팅 컨트롤(어울림/앞으로/뒤로)은 텍스트 흐름과 무관하므로 체크에
     * 포함하지 않는다 — 표와 공존 가능.
     */
    private static boolean pageHasRoomBelow(Page page, long y) {
        for (TextRow row : page.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.area().bottom() > y) return false;
                }
            }
        }
        return true;
    }

    private void placeControlOnPage(Control ctrl, ParaLayoutContext ctx, Page target) {
        CtrlHeaderGso header = gsoHeaderOf(ctrl);
        if (header == null) return;
        Area area = ControlAreaCalculator.calculate(
                header, ctx.paperArea(), ctx.bodyArea(), ctx.bodyArea().clone(), ctx.bodyArea().top());
        Area areaWithMargin = withMargin(area, header);
        boolean behindText = (header.getProperty().getTextFlowMethod() == TextFlowMethod.BehindText);
        ControlItem item = createItem(ctrl, area, areaWithMargin, behindText, ctx);
        if (item != null) target.addControl(item);
    }

    /** ControlExtend 문자(charIdx)가 속한 TextLine의 페이지와 Y 좌표를 반환한다. */
    private static LineLocation findLineLocation(Document doc, int paraIdx, int charIdx) {
        Page foundPage = null;
        TextLine foundLine = null;
        for (Page page : doc.pages()) {
            for (TextRow row : page.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        if (line.paraIndex() == paraIdx && line.startCharIdx() <= charIdx) {
                            foundPage = page;
                            foundLine = line;
                        }
                    }
                }
            }
        }
        return (foundPage != null) ? new LineLocation(foundPage, foundLine.area().top(), foundLine) : null;
    }

    private static class LineLocation {
        final Page page;
        final long lineTopY;
        final TextLine line;
        LineLocation(Page page, long lineTopY, TextLine line) {
            this.page = page; this.lineTopY = lineTopY; this.line = line;
        }
    }

    private void layoutInlineControl(Control ctrl, CtrlHeaderGso header,
                                      Paragraph para, int charIdx,
                                      LineLocation loc, ParaLayoutContext ctx) {
        if (ctrl instanceof ControlTable) normalizeCellMargins((ControlTable) ctrl);
        TextLine line = loc.line;

        // 레이아웃 단계에서 심어둔 인라인 컨트롤 플레이스홀더 CharInfo를 찾아 X 좌표 획득.
        double placeholderX = line.area().left();
        outer:
        for (TextPart part : line.parts()) {
            for (CharInfo ci : part.charInfos()) {
                if (ci.inlineControl() && ci.charIdx() == charIdx) {
                    placeholderX = part.startX() + ci.x();
                    break outer;
                }
            }
        }

        long ctrlWidth = ControlAreaCalculator.resolveWidth(header, ctx.paperArea(), ctx.bodyArea());
        long ctrlHeight = ControlAreaCalculator.resolveHeight(header, ctx.paperArea(), ctx.bodyArea());
        // 글상자가 자동 크기로 커진 경우(shapeComponent.getHeightAtCurrent > 헤더 높이)를
        // 반영한다. createGsoItem 이 박스를 그 높이로 확장하므로, 위치 계산도 같은 높이를
        // 써야 확장된 박스가 라인 아래로 삐져나가 contentHeight(세로 중앙정렬 기준)를
        // 부풀리지 않는다. (실문서 R03 표준서식 예시 셀 편집여백 상자: 11923→15205)
        // 단 회전된 도형은 제외 — getHeightAtCurrent 는 회전 전 높이라서 헤더의
        // 회전 후 외접 박스 높이를 덮어쓰면 안 된다(실문서 R03 도표 ▼ 삼각형 90°).
        if (ctrl instanceof GsoControl) {
            kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponent gsc =
                    ((GsoControl) ctrl).getShapeComponent();
            long curH = gsc.getHeightAtCurrent();
            if (gsc.getRotateAngle() == 0 && curH > ctrlHeight) ctrlHeight = curH;
        }
        long ctrlLeft = Math.round(placeholderX) + header.getOutterMarginLeft();
        // line.area().bottom()은 lineSpace 포함. 컨트롤은 lineSpace 제외한 라인 하단에 맞춤.
        long lineBottom = line.area().bottom() - line.lineGap();
        // 가시 텍스트가 없는 문단에, 라인 baseline(ascent) 안에 들어가는 작은 인라인 객체가
        // 단독으로 있을 때(예: heading 아래 그러데이션 밑줄 바)는, 한글이 라인 하단이 아니라
        // 문단 글자모양이 만든 텍스트 baseline에 객체 하단을 맞춘다. lineBottom은
        // baseline + descent라서 descent(≈4px)만큼 아래로 내려가 밑줄 바가 낮게 그려지던 원인.
        // 라인을 채우는 큰 객체(도표 박스 등)·텍스트와 함께 흐르는 객체는 기존대로 lineBottom.
        long anchorBottom = lineBottom;
        if (line.maxCharBaseSize() > 0
                && !hasVisibleText(para)
                && ctrlHeight <= line.maxCharBaseSize()
                && !(ctrl instanceof ControlTable)
                && !(ctrl instanceof ControlPicture)) {
            // 라인 baseline(ascent) 안에 들어가는 작은 인라인 장식 객체(예: heading 아래
            // 그러데이션 밑줄 바, ControlRectangle h≈557)만 baseline에 하단을 맞춘다.
            // 인라인 표(ControlTable)는 라인을 채우는 블록이라 baseline 장식이 아니라
            // lineBottom에 놓여야 한다 — 표 높이가 문단 글자크기와 거의 같아(ctrlH≈maxBase)
            // 이 분기에 걸리면 표가 baseline 만큼 위로 당겨져 위치가 어긋난다(실문서
            // R22 제목 박스: 9px 위로 올라가던 원인).
            // 그림(ControlPicture)도 제외 — 그림은 baseline "장식"이 아니라 descent 를 가진
            // 글리프라서 아래 전용 분기(0.85/0.15 규칙)로 가야 한다. 여기 걸리면 하단이
            // baseline 에 붙어 descent 만큼 위로 뜬다(실문서 R07 캘리그라피 7px — 그림 높이가
            // 글자 em 보다 작은 그림 전용 줄이 정확히 이 조건을 만족해 버린다).
            anchorBottom = line.area().top() + line.maxCharBaseSize();
        } else if (ctrl instanceof ControlPicture && line.maxCharBaseSize() > 0) {
            // 인라인 그림(글자처럼 취급)의 세로 배치 — LineSeg 실측 4표본으로 확정한 규칙:
            //
            //   H ≥ dist (그림이 줄 ascent 이상 — 줄을 지배): 그림 상단 = 줄 상단.
            //     실문서 R07 본문그림(5448/4631)·OPEN 로고(3245/2758)·실문서 R05 스크린샷(15315/14845).
            //     R05 는 dist/H=0.969 라 "그림 ascent=0.85H 접힘" 가정이 성립하지 않는
            //     반례 — 이 경우 하단을 baseline+0.15H 로 내리면 30px 내려가 다음 줄 캡션과
            //     겹친다(page2 96.5% 회귀 실측). 지배 그림은 무조건 줄 상단에 붙인다.
            //
            //   H < dist (더 큰 글자 em 이 같은 줄 — 그림이 baseline 에 매달림): 그림은
            //     descent=0.15·H 를 가진 글리프처럼 하단 = baseline + 0.15·H.
            //     실문서 R07 캘리그라피(H=2525 < dist=3400, em=4000): 참조 실측과 1px 이내.
            //     기존 max(H, dist) 식은 하단을 baseline 에 붙여 descent 를 빼먹었고
            //     그림이 6.3px 위로 떴다.
            //
            // (dist = line.maxCharBaseSize() = LineSeg 의 distanceBaseLineToLineVerticalPosition)
            long dist = line.maxCharBaseSize();
            anchorBottom = line.area().top()
                    + (ctrlHeight >= dist
                        ? ctrlHeight
                        : dist + Math.round(ctrlHeight * 0.15));
        } else {
            anchorBottom -= shorterInlineRise(line, charIdx);
        }
        long ctrlTop = anchorBottom - ctrlHeight - header.getOutterMarginBottom();
        Area area = new Area(ctrlLeft, ctrlTop, ctrlLeft + ctrlWidth, ctrlTop + ctrlHeight);
        Area areaWithMargin = withMargin(area, header);
        boolean behindText = (header.getProperty().getTextFlowMethod() == TextFlowMethod.BehindText);

        ControlItem item = createItem(ctrl, area, areaWithMargin, behindText, ctx);
        if (item != null) {
            loc.page.addControl(item);
        }
    }

    /**
     * 한 줄에 인라인 컨트롤이 여러 개 있고 높이가 서로 다를 때, 낮은 쪽이 줄 하단에서
     * 올라가는 양. 가장 높은 컨트롤이면 0 이라 <b>인라인 컨트롤이 하나뿐인 줄은 영향이 없다.</b>
     *
     * <p>같은 줄의 인라인 컨트롤들은 <b>baseline 을 공유하고 각자 높이에 비례한 descent</b>
     * ({@code 0.15 × H}, 인라인 그림에서 이미 확인된 비율)를 갖는다. 그래서 가장 높은 것을
     * 기준으로 하면 낮은 것은 {@code 0.15 × (H최대 − H)} 만큼 위로 올라간다.</p>
     *
     * <p>실측 근거 — 실문서 R23 para2 는 결재 표(h=7388)와 접수 표(h=4832)가
     * 한 줄에 있는 코퍼스 유일 사례다. 참조는 두 표의 하단을 다르게 놓는다(접수 316 / 결재 322).
     * 둘 다 줄 하단에 맞추면 접수 표가 6px 아래로 내려간다.</p>
     *
     * <pre>
     *   k       결재 top (참조 199)   접수 top (참조 236)
     *   0.10    199                  237.3
     *   0.15    199                  235.2   ← 채택(인라인 그림과 같은 상수)
     *   0.175   199                  234.1
     * </pre>
     *
     * <p>이 문서만 보면 k=0.12 가 정확히 맞지만 표본이 1건뿐이라 새 상수를 만들지 않고
     * 이미 검증된 0.15 를 재사용했다(잔차 1px). 같은 구조의 문서를 더 확보하면 재측정할 것.</p>
     */
    private static long shorterInlineRise(TextLine line, int charIdx) {
        double maxHeight = 0;
        double ownHeight = -1;
        for (TextPart part : line.parts()) {
            for (CharInfo ci : part.charInfos()) {
                if (!ci.inlineControl()) continue;
                if (ci.height() > maxHeight) maxHeight = ci.height();
                if (ci.charIdx() == charIdx) ownHeight = ci.height();
            }
        }
        if (ownHeight < 0 || maxHeight <= ownHeight) return 0;
        return Math.round((maxHeight - ownHeight) * 0.15);
    }

    private void processControl(Control ctrl, ParaLayoutContext ctx,
                                 Page targetPage, long paraTopY,
                                 TextLine anchorLine, int paraIdx, Paragraph para,
                                 boolean paraHasNoVisibleText,
                                 boolean isNoLineSeg) {
        CtrlHeaderGso header = gsoHeaderOf(ctrl);
        if (header == null) return;

        if (ctrl instanceof ControlTable) normalizeCellMargins((ControlTable) ctrl);

        // 자리차지 분할 표 이후 paragraph의 플로팅 컨트롤(InFrontOfText/BehindText)은
        // 표의 마지막 조각 페이지로 라우팅한다. anchor line은 firstSegPage에 남아 있지만
        // OK 동작은 분할 표 뒤 위치에 컨트롤을 둠.
        TextFlowMethod tfm = header.getProperty().getTextFlowMethod();
        boolean isFloating = (tfm == TextFlowMethod.InFrontOfText || tfm == TextFlowMethod.BehindText);
        if (isFloating && ctx.lastDivideTableParaIdx() >= 0
                && paraIdx > ctx.lastDivideTableParaIdx()
                && ctx.lastDivideTableLastSegPage() != null
                && targetPage == ctx.document().pages().get(
                        ctx.document().pages().indexOf(ctx.lastDivideTableLastSegPage()) - 1)) {
            targetPage = ctx.lastDivideTableLastSegPage();
        }

        // 다단에서 paragraph가 컬럼 경계를 넘으면 ctx.bodyArea()가 이미 다음 컬럼으로
        // advance된 상태일 수 있다. 컨트롤은 anchor line이 위치한 컬럼 기준으로 배치되어야
        // 하므로 line의 area를 paraArea로 사용한다.
        Area paraArea = (anchorLine != null) ? anchorLine.area().clone() : ctx.bodyArea().clone();
        // 표(자리차지)의 가로 기준(horzRelTo=Para)은 문단의 왼쪽 경계여야 한다. anchorLine.area()의
        // left에는 문단 '첫줄 들여쓰기'가 포함돼 있어 그대로 쓰면 표가 들여쓰기만큼 오른쪽으로
        // 밀린다(실문서 R07 머리 표: indent 1331 만큼 우측 이동). 첫줄 들여쓰기만 제거하고
        // 왼쪽 여백(leftMargin)은 유지한다.
        //
        // **양수(들여쓰기)일 때만 뺀다.** 음수 indent 는 '내어쓰기'라서 첫 줄은 문단 왼쪽
        // 경계 그대로이고 둘째 줄부터 안쪽으로 들어간다 — anchorLine.left 에 애초에
        // 포함돼 있지 않다. 그런데도 빼면 |indent| 만큼 표가 오른쪽으로 밀린다
        // (실문서 R10: indent=-1436 → 표가 24px 우측, 97.3%).
        if (ctrl instanceof ControlTable && anchorLine != null && para != null) {
            long firstLineIndent = ParaShapeHelper.indent(ctx.reader().getParaShape(para));
            if (firstLineIndent > 0) {
                paraArea = new Area(paraArea.left() - firstLineIndent, paraArea.top(),
                        paraArea.right(), paraArea.bottom());
            }
        }
        // vertRelTo=Para 컨트롤의 효과적 paraTopY 결정 (내부 doc 셀/글상자 한정).
        long effectiveParaTopY = ctx.innerDocLayout()
                ? innerDocEffectiveParaTopY(header, anchorLine, paraTopY, ctx.document())
                : paraTopY;
        // 분할 표 뒤 플로팅(InFrontOfText/BehindText) + vRelTo=Para 컨트롤의 anchor
        // line이 targetPage(분할 표의 lastSegPage)에 있는데 paraTopY는 표 첫 페이지
        // 흐름 좌표를 가리킬 때, 그대로 쓰면 컨트롤이 page1 좌표로 page2에 그려진다
        // (뒷페이지 케이스). anchor line이 targetPage에 있으면 그 top을 기준으로 쓴다.
        if (isFloating && anchorLine != null
                && header.getProperty().getVertRelTo() == VertRelTo.Para
                && ctx.lastDivideTableParaIdx() >= 0
                && targetPage == ctx.lastDivideTableLastSegPage()) {
            boolean anchorOnTarget = false;
            for (TextRow row : targetPage.content().rows()) {
                for (TextColumn col : row.columns()) {
                    if (col.textLines().contains(anchorLine)) { anchorOnTarget = true; break; }
                }
                if (anchorOnTarget) break;
            }
            if (anchorOnTarget) {
                effectiveParaTopY = anchorLine.area().top();
            }
        }
        // 자리차지 표(vertRelTo=Para)의 세로 기준은 앵커 문단의 '위 여백(topSpace)' 이전
        // 위치여야 한다. currentParaTopY(=paraTopY)는 topSpace를 더한 뒤 값이라 그대로 쓰면
        // 표가 문단 위 여백만큼 아래로 내려간다(서명란 표: 앵커 빈 문단 topSpace 1000≈16px).
        // 표 한정으로 앵커 문단 topSpace를 뺀다(가로 fix와 동일하게 표만 대상).
        if (ctrl instanceof ControlTable && para != null
                && header.getProperty().getVertRelTo() == VertRelTo.Para) {
            effectiveParaTopY -= ParaShapeHelper.topSpace(ctx.reader().getParaShape(para));
        }
        Area area = ControlAreaCalculator.calculate(
                header, ctx.paperArea(), ctx.bodyArea(), paraArea, effectiveParaTopY);
        // 표(ControlTable) 한정: layoutParagraph가 페이지를 advance했지만 paraTopY는 이전
        // 페이지의 누적 currentY로 남아 area.top이 targetPage 본문 영역 밖을 가리키는
        // 경우, area를 targetPage 상단으로 정규화한다. 이 보정이 없으면 표가 페이지 끝에
        // 잘못 그려지거나 분할 로직이 표를 페이지 경계에서 쪼개버린다.
        // 비표 컨트롤(이미지/글상자/도형)은 paraTopY가 페이지 밖이라도 의도된 배치일 수
        // 있어 정규화하지 않는다.
        if (ctrl instanceof ControlTable && area.top() > targetPage.bodyArea().bottom()) {
            long h = area.height();
            // 표의 단락 내 고유 오프셋(area.top - paraTop, 페이지 무관)을 새 페이지 본문
            // 상단에 그대로 재기준한다. 단락 선두 텍스트 줄("1" 등)이 차지하는 높이를
            // 보존해 한글과 같은 위치에 표가 온다. 오프셋이 음수면 outerMarginTop 폴백.
            long intrinsic = area.top() - effectiveParaTopY;
            long offset = (intrinsic >= 0) ? intrinsic : header.getOutterMarginTop();
            long newTop = targetPage.bodyArea().top() + offset;
            area = new Area(area.left(), newTop, area.right(), newTop + h);
        }
        // 같은 문단에 앵커된 자리차지(TakePlace, vRel=Para) 표가 여러 개면 한글은
        // 세로로 쌓는다(실문서 R02 경력기술서: 표 2개가 같은 문단 vOff=0). 직전 표의
        // 하단(여백 포함) 아래로 내린다.
        if (ctrl instanceof ControlTable
                && tfm == TextFlowMethod.TakePlace
                && header.getProperty().getVertRelTo() == VertRelTo.Para
                && stackedTableParaIdx == paraIdx && area.top() < stackedTableBottom) {
            long h = area.height();
            long newTop = stackedTableBottom + header.getOutterMarginTop();
            area = new Area(area.left(), newTop, area.right(), newTop + h);
        }
        Area areaWithMargin = withMargin(area, header);
        boolean behindText = (header.getProperty().getTextFlowMethod() == TextFlowMethod.BehindText);

        // '나누지않음'(NoDivide) 표가 현재 페이지를 넘치면, 표를 분할하지 않고
        // 다음 페이지 상단으로 통째 이동한다. 표 단락의 본문 텍스트는 흐름대로 두고
        // 표 컨트롤만 옮긴다.
        if (relocateNoDivideTable(ctrl, header, area, ctx, targetPage)) {
            long h = area.height();
            Page next = nextPageOrCreate(ctx, targetPage);
            long newTop = next.bodyArea().top() + header.getOutterMarginTop();
            long newBottom = newTop + h;
            area = new Area(area.left(), newTop, area.right(), newBottom);
            areaWithMargin = withMargin(area, header);
            ControlItem moved = createItem(ctrl, area, areaWithMargin, behindText, ctx);
            if (moved != null) next.addControl(moved);
            // 표가 다음 페이지에 들어가는 경우에만 후속 본문이 표 아래에서 시작하도록
            // ctx 를 다음 페이지로 이동. 표가 다음 페이지보다 커서 더 넘치면(나누지않음/1
            // 셋째 표처럼) 본문은 원래 페이지에 계속 두고, 자연 overflow 로 흘러가게 둔다.
            if (newBottom <= next.bodyArea().bottom()) {
                targetPage = next;
                ctx.currentPage(next);
                ctx.bodyArea(next.bodyArea().clone());
                ctx.currentY(areaWithMargin.bottom());
                ctx.lastLineSegVertPos(-1);
            }
            return;
        }

        // '셀단위로나누기'(DivideByCell) 표가 페이지를 넘치면 행 단위로 분할 배치한다.
        if (splitTableByCell(ctrl, header, area, targetPage, ctx)) {
            splitAndPlaceTable((ControlTable) ctrl, header, area, behindText, ctx, targetPage, paraIdx);
            return;
        }

        // '나누기'(Divide) 표가 페이지를 넘치면 셀 내용을 페이지 경계에서 쪼개 배치한다.
        if (splitTableByDivide(ctrl, header, area, targetPage, ctx)) {
            splitAndPlaceDivideTable((ControlTable) ctrl, header, area, behindText, ctx, targetPage, paraIdx);
            return;
        }

        // 어울림/자리차지 + 75% 규칙: 텍스트가 페이지 75% 이상 채워졌고 컨트롤이 기존 라인과 겹치면 다음 페이지로 지연.
        if (shouldDefer(header, areaWithMargin, targetPage, anchorLine, ctx.bodyArea(), isNoLineSeg)) {
            ctx.deferredControls().add(new kr.dogfoot.hwpdrawer.engine.para.DeferredControl(
                    ctrl, targetPage, paraIdx));
            return;
        }

        ControlItem item = createItem(ctrl, area, areaWithMargin, behindText, ctx);
        if (item != null) {
            if (anchorLine != null) item.anchorLineTop(anchorLine.area().top());
            item.yRelativeToAnchor(header.getProperty().getVertRelTo() == VertRelTo.Para);
            targetPage.addControl(item);
            if (!paraHasNoVisibleText) {
                shiftParaLinesBelowTakePlace(header, areaWithMargin, targetPage, paraIdx, ctx);
            }
            advanceForTakePlace(header, areaWithMargin, ctx, paraHasNoVisibleText, anchorLine);
            // 같은 문단의 후속 TakePlace 표 스태킹 기준 갱신.
            if (ctrl instanceof ControlTable
                    && tfm == TextFlowMethod.TakePlace
                    && header.getProperty().getVertRelTo() == VertRelTo.Para) {
                stackedTableParaIdx = paraIdx;
                stackedTableBottom = areaWithMargin.bottom();
            }
        }
    }

    /**
     * 같은 문단 내 TakePlace 컨트롤과 겹치는 TextLine을 컨트롤 하단으로 밀어낸다.
     * 이미 배치된 이후 라인들도 같은 delta만큼 함께 이동한다.
     */
    private static void shiftParaLinesBelowTakePlace(CtrlHeaderGso header, Area ctrlArea,
                                                      Page page, int paraIdx,
                                                      ParaLayoutContext ctx) {
        if (header.getProperty().getTextFlowMethod() != TextFlowMethod.TakePlace) return;
        long ctrlTop = ctrlArea.top();
        long ctrlBot = ctrlArea.bottom();
        if (ctrlBot > ctx.bodyArea().bottom()) return;

        // 컨트롤이 속한 단의 x 범위 — 다른 단의 줄은 자리차지 이동 대상이 아니다.
        // (한 문단이 여러 단에 걸칠 때 다른 단의 줄이 함께 밀려나는 버그 방지)
        long colLeft = Long.MIN_VALUE;
        long colRight = Long.MAX_VALUE;
        List<Area> cols = ctx.multiColumnAreas();
        if (cols != null && cols.size() > 1) {
            for (Area a : cols) {
                if (ctrlArea.left() >= a.left() - 1 && ctrlArea.left() < a.right()) {
                    colLeft = a.left();
                    colRight = a.right();
                    break;
                }
            }
        }

        long shift = 0;
        long lastBot = Long.MIN_VALUE;  // 문단 마지막 줄의 최종 bottom (currentY 보정용)
        for (TextRow row : page.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.paraIndex() != paraIdx) continue;
                    long left = line.area().left();
                    if (left < colLeft || left >= colRight) {
                        // 컨트롤과 다른 단의 줄 — 이동하지 않는다.
                        lastBot = line.area().bottom();
                        continue;
                    }
                    long top = line.area().top() + shift;
                    long bot = line.area().bottom() + shift;
                    // 가시 텍스트 하단(lineGap 제외)이 컨트롤 상단을 넘을 때만 shift 대상.
                    long visibleBot = bot - line.lineGap();
                    if (top < ctrlBot && visibleBot > ctrlTop) {
                        long delta = ctrlBot - top;
                        if (delta > 0) {
                            shift += delta;
                            top += delta;
                            bot += delta;
                        }
                    }
                    if (shift != 0) {
                        line.area().top(top);
                        line.area().bottom(bot);
                    }
                    lastBot = bot;
                }
            }
        }
        // currentY를 문단의 실제 마지막 줄 하단으로 보정한다.
        if (shift > 0 && lastBot > Long.MIN_VALUE && lastBot > ctx.currentY()
                && lastBot <= ctx.bodyArea().bottom()) {
            ctx.currentY(lastBot);
        }
    }

    /**
     * 자리차지(TakePlace): 컨트롤은 문단 흐름에서 자신의 수직 공간을 점유한다.
     *
     * <p>컨트롤-only 문단(빈 문단 + Paper vRelTo): 빈 라인을 컨트롤 하단 아래로 이동시킨 뒤 advance.
     * 그 외: currentY를 max(currentY, ctrlBot)로 진행해 이어지는 문단이 컨트롤 영역을 건너뛰게 한다.
     *
     * <p>WithLineSeg는 LineSegItem.vertPos로 위치가 확정되므로 이 이동의 영향을 받지 않는다.</p>
     */
    private static void advanceForTakePlace(CtrlHeaderGso header, Area areaWithMargin,
                                             ParaLayoutContext ctx, boolean paraHasNoVisibleText,
                                             TextLine blankLine) {
        if (header.getProperty().getTextFlowMethod() != TextFlowMethod.TakePlace) return;

        long ctrlBot = areaWithMargin.bottom();
        boolean paraRel = header.getProperty().getVertRelTo()
                == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo.Para;

        if (paraHasNoVisibleText && !paraRel && blankLine != null
                && blankLine.area().bottom() <= ctrlBot) {
            long lineHeight = blankLine.area().height();
            long newTop = ctrlBot;
            long newBot = newTop + lineHeight;
            if (newBot <= ctx.bodyArea().bottom()) {
                blankLine.area().top(newTop);
                blankLine.area().bottom(newBot);
                ctx.currentY(newBot);
            }
            return;
        }

        // 현재 흐름이 컨트롤 영역에 진입했거나 진입 직전일 때만 컨트롤 하단으로 skip.
        // (Paper vRelTo 컨트롤은 절대 위치에 있어, 현재 currentY보다 훨씬 아래일 수 있다.)
        long ctrlTop = areaWithMargin.top();
        if (ctrlTop <= ctx.currentY() && ctrlBot > ctx.currentY()
                && ctrlBot <= ctx.bodyArea().bottom()) {
            ctx.currentY(ctrlBot);
        }
    }

    /**
     * 표가 '나누지않음'(NoDivide)이고 계산된 영역이 현재 페이지 하단을 넘치는지 검사한다.
     * true면 표를 다음 페이지 상단으로 통째 이동해야 한다.
     *
     * <p>이미 페이지 상단에서 시작하면(area.top ≤ bodyArea.top) 옮겨도 의미가 없으므로
     * false를 반환한다(무한 페이지 생성 방지) — 표가 한 페이지보다 커도 그대로 그린다.</p>
     */
    private static boolean relocateNoDivideTable(Control ctrl, CtrlHeaderGso header,
                                                  Area area, ParaLayoutContext ctx, Page targetPage) {
        if (!(ctrl instanceof ControlTable)) return false;
        if (header.getProperty().isLikeWord()) return false;
        if (((ControlTable) ctrl).getTable().getProperty().getDivideAtPageBoundary()
                != DivideAtPageBoundary.NoDivide) {
            return false;
        }
        Area pageBody = targetPage.bodyArea();
        return area.top() > pageBody.top() && area.bottom() > pageBody.bottom();
    }

    /**
     * 표가 '셀단위로나누기'(DivideByCell)이고 실제 표 높이가 현재 페이지를 넘치는지 검사한다.
     * true면 표를 행(셀) 단위로 페이지에 걸쳐 분할 배치해야 한다.
     *
     * <p>표 컨트롤 헤더의 height는 셀 내용 자동 높이일 때 실제 높이와 다르므로,
     * {@code area.bottom()} 대신 행 높이 합({@link #resolveRowHeights})으로 판정한다.</p>
     */
    private boolean splitTableByCell(Control ctrl, CtrlHeaderGso header,
                                      Area area, Page targetPage, ParaLayoutContext ctx) {
        if (!(ctrl instanceof ControlTable)) return false;
        if (header.getProperty().isLikeWord()) return false;
        // 자리차지(TakePlace)·어울림(FitWithText) 표를 행 단위 분할 대상으로 한다.
        TextFlowMethod tfm = header.getProperty().getTextFlowMethod();
        if (tfm != TextFlowMethod.TakePlace && tfm != TextFlowMethod.FitWithText) return false;
        ControlTable table = (ControlTable) ctrl;
        if (table.getTable().getProperty().getDivideAtPageBoundary()
                != DivideAtPageBoundary.DivideByCell) {
            return false;
        }
        long tableH = totalTableHeight(table, area.left(), ctx);
        return area.top() + tableH > targetPage.bodyArea().bottom();
    }

    /** 표의 실제 높이(행 높이 합)를 반환한다. */
    private long totalTableHeight(ControlTable ctrl, long tableLeft, ParaLayoutContext ctx) {
        int colCount = ctrl.getTable().getColumnCount();
        int rowCount = ctrl.getTable().getRowCount();
        long[] colWidths = resolveColWidths(ctrl, colCount);
        long[] rowHeights = resolveRowHeights(ctrl, rowCount, colWidths, tableLeft, ctx);
        long h = 0;
        for (long rh : rowHeights) h += rh;
        // 셀 간격은 모든 경계에 들어간다 — 행이 n개면 간격은 n+1개.
        h += (long) (rowHeights.length + 1) * cellSpacingOf(ctrl);
        return h;
    }

    /**
     * '나누기'(Divide) 표가 페이지를 넘쳐 셀 내용을 페이지 경계에서 쪼개야 하는지 검사한다.
     *
     * <p>셀단위로나누기(DivideByCell)가 행 전체를 다음 페이지로 보내는 것과 달리,
     * 나누기(Divide)는 한 셀의 내용이 페이지 하단에 닿으면 그 셀 내용을 페이지 경계에서
     * 쪼개 위/아래 페이지에 나눠 그린다.</p>
     */
    private boolean splitTableByDivide(Control ctrl, CtrlHeaderGso header,
                                        Area area, Page targetPage, ParaLayoutContext ctx) {
        if (!(ctrl instanceof ControlTable)) return false;
        if (header.getProperty().isLikeWord()) return false;
        TextFlowMethod tfm = header.getProperty().getTextFlowMethod();
        if (tfm != TextFlowMethod.TakePlace && tfm != TextFlowMethod.FitWithText) return false;
        ControlTable table = (ControlTable) ctrl;
        if (table.getTable().getProperty().getDivideAtPageBoundary()
                != DivideAtPageBoundary.Divide) {
            return false;
        }
        long tableH = totalTableHeight(table, area.left(), ctx);
        if (area.top() + tableH > targetPage.bodyArea().bottom()) return true;
        // 행 높이 합으로는 페이지를 안 넘쳐도, 한 셀의 내용이 페이지를 넘어
        // cellDocument가 여러 페이지로 나뉘면 분할 대상이다. (totalTableHeight는
        // probeCellContentHeight 기반이라 셀 내용이 페이지를 넘는 경우 과소측정된다.)
        long[] colWidths = resolveColWidths(table, table.getTable().getColumnCount());
        long[] colX = cumulative(colWidths, area.left(), cellSpacingOf(table));
        for (Cell cell : collectCells(table)) {
            Area probe = cellProbeArea(cell, colX, colWidths, area.left(), Integer.MAX_VALUE / 2,
                    cellSpacingOf(table));
            Document probeDoc = innerLayouter.layout(cell.getParagraphList(), probe,
                    ctx.paperArea(), true);
            if (probeDoc.pages().size() > 1) return true;
        }
        return false;
    }

    /**
     * '셀단위로나누기'(DivideByCell) 표를 행 단위로 페이지에 걸쳐 분할 배치한다.
     *
     * <p>표의 행을 위→아래로 페이지에 채우다가, 한 행이 현재 페이지 하단을 넘으면
     * 그 행 전체를 다음 페이지로 보낸다(행 내용은 쪼개지 않는다). 페이지별 행 그룹마다
     * 별도 {@link TableItem}을 만들어 해당 페이지에 배치한다.</p>
     */
    private void splitAndPlaceTable(ControlTable ctrl, CtrlHeaderGso header, Area area,
                                     boolean behindText, ParaLayoutContext ctx, Page firstPage,
                                     int paraIdx) {
        int colCount = ctrl.getTable().getColumnCount();
        int rowCount = ctrl.getTable().getRowCount();
        long[] colWidths = resolveColWidths(ctrl, colCount);
        long[] rowHeights = resolveRowHeights(ctrl, rowCount, colWidths, area.left(), ctx);

        long pageTop = ctx.pageBodyArea().top();
        long pageBottom = ctx.pageBodyArea().bottom();

        // 행을 페이지별 그룹 [rowStart, rowEnd)로 분배.
        // 첫 페이지 조각은 표 지정 영역(area.bottom())까지만 둔다 — 한글은 표를
        // 표 박스(지정) 높이 단위로 페이지에 나눈다.
        // 행 추가 판정은 행 지정 높이가 아닌 셀 콘텐츠 높이로 한다: 빈/작은 셀 행은
        // 행 상단이 페이지 안이면 (행 지정 높이가 커 페이지를 넘쳐도) 그 페이지에 두고,
        // 콘텐츠가 큰 셀 행은 콘텐츠가 페이지를 넘으면 다음 페이지로 보낸다.
        long marginB = header.getOutterMarginBottom();
        long marginT = header.getOutterMarginTop();
        boolean takePlace = header.getProperty().getTextFlowMethod() == TextFlowMethod.TakePlace;
        long firstPageLimit = Math.min(area.bottom(), pageBottom);
        long[] rowContent = rowContentHeights(ctrl, rowCount, colWidths, area.left(), ctx);
        // 페이지 대부분(90% 이상)을 차지하는 "큰 셀" 행: 작은 행 뒤 잔여 공간에 억지로
        // 넣으면 NoLineSeg 셀 높이 합성 오차(줄당 미세 차이 누적)로 OK 와 페이지가
        // 어긋난다. 한글은 이런 큰 셀을 자기 페이지에서 시작시키므로, 그룹 첫 행이
        // 아닐 때는 fits 여도 새 그룹(페이지)으로 보낸다.
        long pageBodyHeight = pageBottom - pageTop;
        long bigRowThreshold = pageBodyHeight * 9 / 10;
        List<int[]> groups = new ArrayList<>();
        int r = 0, groupStart = 0;
        long curY = area.top();
        while (r < rowCount) {
            long limit = (groupStart == 0) ? firstPageLimit : pageBottom;
            long margin = (groupStart == 0) ? 0 : marginB;
            // 그룹 첫 행은 한계를 넘어도 강제로 넣어 무한 루프를 막는다.
            boolean fits = curY < limit && curY + rowContent[r] + margin <= limit;
            if (fits && r != groupStart && rowContent[r] > bigRowThreshold) {
                fits = false;  // 큰 셀 행은 자기 페이지에서 시작
            }
            if (r == groupStart || fits) {
                curY += rowHeights[r];
                r++;
            } else {
                groups.add(new int[]{groupStart, r});
                groupStart = r;
                curY = pageTop + marginT;
            }
        }
        groups.add(new int[]{groupStart, rowCount});

        // 각 그룹마다 TableItem 생성.
        //  - 자리차지(TakePlace): 표가 본문 흐름을 점유하므로 후속 그룹마다 페이지를
        //    즉시 만들어 배치하고, 표 다음 본문이 마지막 그룹 아래에서 시작하도록 ctx를 갱신.
        //  - 어울림(FitWithText): 본문이 표 주위로 흐르므로 후속 그룹을 페이지 예약 큐에
        //    등록하고, 본문 레이아웃이 새 페이지를 만들 때 배치되게 한다(ctx는 건드리지 않음).
        Page page = firstPage;
        long lastBottom = area.top();
        for (int gi = 0; gi < groups.size(); gi++) {
            int[] g = groups.get(gi);
            long groupTop = (gi == 0) ? area.top() : pageTop + marginT;
            long groupH = 0;
            for (int rr = g[0]; rr < g[1]; rr++) groupH += rowHeights[rr];
            Area groupArea = new Area(area.left(), groupTop, area.right(), groupTop + groupH);
            TableItem item = new TableItem(ctrl, groupArea, withMargin(groupArea, header), behindText);
            layoutCellRange(ctrl, item, area.left(), groupTop, g[0], g[1],
                    colWidths, rowHeights, ctx);
            item.zOrder(header.getzOrder());
            if (gi == 0) {
                firstPage.addControl(item);
            } else if (takePlace) {
                page = nextPageOrCreate(ctx, page);
                page.addControl(item);
            } else {
                ctx.enqueuePendingTableSegment(item);
            }
            lastBottom = groupArea.bottom();
        }
        // 어울림(FitWithText) 표가 page에 걸쳐 분할되면(2조각 이상), 표 anchor 직후
        // 첫 텍스트 본문 단락은 표 둘째 조각과 함께 다음 페이지에서 시작해야 한다.
        // NoLineSeg 합성기는 이 page break를 모르므로 본문 루프에 신호를 남긴다.
        if (!takePlace && groups.size() > 1) {
            ctx.pendingFitWrapTableParaIdx(paraIdx);
        }
        if (takePlace) {
            Page firstSegPage = firstPage;
            ctx.currentPage(page);
            ctx.bodyArea(ctx.pageBodyArea().clone());
            ctx.currentY(Math.min(lastBottom, pageBottom));
            ctx.lastLineSegVertPos(-1);
            // 표 단락의 텍스트 줄은 layoutParagraph 시점(표 분할 전)에 첫 페이지에
            // 그려진다. 표가 페이지에 걸쳐 분할되면 그 텍스트는 표 마지막 조각 다음에
            // 와야 하므로, 마지막 조각이 놓인 페이지의 currentY 아래로 옮긴다.
            if (page != firstSegPage) {
                moveParaLinesToCurrentPage(paraIdx, firstSegPage, area.top(), ctx);
            }
        }
    }

    /**
     * 단락의 텍스트 줄을 {@code fromPage}에서 떼어내 현재 ctx 페이지의 currentY 아래로
     * 옮긴다. 자리차지 표 분할 후 표 단락 텍스트를 표 마지막 조각 다음으로 보낼 때 쓴다.
     * <p>{@code tableTop} 위에 있는 줄(line.bottom <= tableTop)은 표 앞쪽 본문이므로
     * 그대로 둔다.</p>
     */
    /**
     * 표 단락 이후(paraIdx > X) 줄들을 fromPage에서 떼어내 현재 ctx 페이지의 currentY
     * 아래로 옮긴다. 표가 자리차지(takePlace)로 여러 페이지에 분할될 때, 표 뒤에 와야
     * 하는 본문 단락이 firstSegPage에 미리 배치되어 표와 겹쳐 보이는 것을 막는다.
     */
    private void moveLaterParaLinesToCurrentPage(int tableParaIdx, Page fromPage,
                                                   ParaLayoutContext ctx) {
        List<TextLine> moved = new ArrayList<>();
        for (TextRow row : fromPage.content().rows()) {
            for (TextColumn col : row.columns()) {
                col.textLines().removeIf(line -> {
                    if (line.paraIndex() > tableParaIdx) {
                        moved.add(line);
                        return true;
                    }
                    return false;
                });
            }
        }
        if (moved.isEmpty()) return;
        // 원래 위치 순서대로 옮긴다 (top 기준).
        moved.sort((a, b) -> Long.compare(a.area().top(), b.area().top()));
        ctx.ensureRowAndColumn();
        TextColumn dest = ctx.currentColumn();
        for (TextLine line : moved) {
            long h = line.area().bottom() - line.area().top();
            long newTop = ctx.currentY();
            line.area().top(newTop);
            line.area().bottom(newTop + h);
            dest.textLines().add(line);
            ctx.currentY(newTop + h);
        }
    }

    private void moveParaLinesToCurrentPage(int paraIdx, Page fromPage, long tableTop,
                                             ParaLayoutContext ctx) {
        List<TextLine> moved = new ArrayList<>();
        for (TextRow row : fromPage.content().rows()) {
            for (TextColumn col : row.columns()) {
                col.textLines().removeIf(line -> {
                    if (line.paraIndex() == paraIdx && line.area().bottom() > tableTop) {
                        moved.add(line);
                        return true;
                    }
                    return false;
                });
            }
        }
        if (moved.isEmpty()) return;
        ctx.ensureRowAndColumn();
        TextColumn dest = ctx.currentColumn();
        for (TextLine line : moved) {
            long h = line.area().bottom() - line.area().top();
            long newTop = ctx.currentY();
            line.area().top(newTop);
            line.area().bottom(newTop + h);
            dest.textLines().add(line);
            ctx.currentY(newTop + h);
        }
    }

    // ── '나누기'(Divide) 표 분할 ──────────────────────────────────────────

    /**
     * '나누기'(Divide) 표를 셀 내용 단위로 페이지에 걸쳐 분할 배치한다.
     *
     * <p>각 셀을 높이 제한 없이 레이아웃하면, 셀 내용이 페이지를 넘는 경우 원본 LineSeg에
     * 따라 cellDocument가 여러 페이지로 분할된다. 행마다 (그 행 셀들의 최대) cellDocument
     * 페이지 수를 구해 표가 걸치는 페이지 수를 정하고, 표 페이지마다 별도 {@link TableItem}을
     * 만들어 해당 cellDocument 페이지의 줄만 담는다.</p>
     */
    private void splitAndPlaceDivideTable(ControlTable ctrl, CtrlHeaderGso header, Area area,
                                           boolean behindText, ParaLayoutContext ctx,
                                           Page firstPage, int paraIdx) {
        // 표 단락의 마지막 LineSeg vertPos — 표 다음 본문이 표 첫 페이지에서 이어질지
        // (본문 vertPos가 더 큼) 새 페이지에서 시작할지(더 작음) 판별하는 기준.
        long enteringVertPos = ctx.lastLineSegVertPos();
        int colCount = ctrl.getTable().getColumnCount();
        int rowCount = ctrl.getTable().getRowCount();
        long[] colWidths = resolveColWidths(ctrl, colCount);
        long[] colX = cumulative(colWidths, area.left(), cellSpacingOf(ctrl));

        long pageTop = ctx.pageBodyArea().top();
        long pageBottom = ctx.pageBodyArea().bottom();
        long marginT = header.getOutterMarginTop();
        long marginB = header.getOutterMarginBottom();
        boolean takePlace = header.getProperty().getTextFlowMethod() == TextFlowMethod.TakePlace;

        // 표 단락의 currentPage 흐름이 이전 단락(예: 어울림 앵커 paragraph 누적)으로
        // 한 페이지 앞당겨졌어도 area.top 아래가 비어 있는 이전 페이지가 있다면 거기
        // 부터 표를 배치한다. 그래야 표 분할로 추가 페이지를 만들지 않고 기존 페이지
        // 분포 안에 맞출 수 있다. (병합 셀 표가 page idx > 0에서 시작할 때 회귀 방지.)
        // 단 area.top이 prev 페이지 본문 영역 밖이면 — paragraph 시작 좌표가 페이지
        // advance 전 값으로 남아 area가 잘못된 페이지의 좌표를 가진 경우 — 이동하지 않는다.
        if (takePlace) {
            int curIdx = ctx.document().pages().indexOf(firstPage);
            for (int prevIdx = curIdx - 1; prevIdx >= 0; prevIdx--) {
                Page prev = ctx.document().pages().get(prevIdx);
                if (area.top() > prev.bodyArea().bottom()) break;
                if (!pageHasRoomBelow(prev, area.top())) break;
                firstPage = prev;
            }
        }

        // 1. 각 셀을 레이아웃해 cellDocument를 만든다.
        //    - 단순 셀: 높이 무제한 — 원본 LineSeg가 페이지 분할을 인코딩한다.
        //    - 다단 셀: 평행/배분 다단은 LineSeg만으로 페이지를 못 나누므로, 표의 첫
        //      페이지·후속 페이지의 실제 가용 높이를 줘서 다단이 그 높이에 맞춰 페이지
        //      분할하게 한다. 두 경우 모두 줄 y는 0 기준이라 배치 시 동일하게 옮긴다.
        // page1 가용 높이(표 좌표) — 셀 레이아웃 전에 계산 가능(페이지 기하만 의존).
        // row 0 셀(cy0=0)의 어울림 박스 page2 클립 경계 = page1가용 - mB - mT.
        long maxCellMBpre = 0;
        for (Cell c : collectCells(ctrl)) {
            maxCellMBpre = Math.max(maxCellMBpre, (long) c.getListHeader().getBottomMargin());
        }
        long firstAvailPre = Math.max(1, pageBottom - marginB - area.top() - maxCellMBpre);
        Map<Cell, Document> cellDocs = new HashMap<>();
        for (Cell cell : collectCells(ctrl)) {
            if (hasMultiColumn(cell.getParagraphList())) continue;
            Area innerArea = cellProbeArea(cell, colX, colWidths, area.left(), Integer.MAX_VALUE / 2);
            // row 0 셀에 mid-paragraph 어울림 컨트롤이 있으면 page2 박스 obstacle 클립 활성화.
            // (cy0=0 이라 셀 로컬 경계 = firstAvailPre - mT. 다른 행은 rowY 미상이라 클립 안 함.)
            boolean clip = cell.getListHeader().getRowIndex() == 0
                    && cellHasMidParagraphFitWithText(cell);
            cellFitClipLocal = clip
                    ? firstAvailPre - cell.getListHeader().getTopMargin()
                    : Long.MAX_VALUE;
            cellDocs.put(cell, innerLayouter.layout(cell.getParagraphList(), innerArea,
                    ctx.paperArea(), true));
            cellFitClipLocal = Long.MAX_VALUE;
        }
        for (Cell cell : collectCells(ctrl)) {
            if (!hasMultiColumn(cell.getParagraphList())) continue;
            long cellRowTop = area.top();
            for (int r = 0; r < cell.getListHeader().getRowIndex(); r++) {
                cellRowTop += divideRowSegmentHeight(ctrl, r, 0, cellDocs, ctx);
            }
            int mT = cell.getListHeader().getTopMargin();
            int mB = cell.getListHeader().getBottomMargin();
            long firstAvail = Math.max(1, pageBottom - marginB - cellRowTop - mT - mB);
            long restAvail = Math.max(1, pageBottom - marginB - pageTop - mT - mB);
            Area first = cellProbeArea(cell, colX, colWidths, area.left(), firstAvail);
            Area rest = cellProbeArea(cell, colX, colWidths, area.left(), restAvail);
            cellDocs.put(cell, innerLayouter.layout(cell.getParagraphList(), first, rest,
                    ctx.paperArea(), true));
        }

        // 2. 행 높이(병합 셀 rowSpan span 반영)와 표 좌표 누적. 표를 연속 y좌표로 본다.
        long[] rowH = divideRowHeights(ctrl, rowCount, cellDocs, ctx);
        long[] rowY = new long[rowCount + 1];
        for (int r = 0; r < rowCount; r++) rowY[r + 1] = rowY[r] + rowH[r];
        long tableContentHeight = rowY[rowCount];

        // 표 페이지 경계 (표 좌표). 첫 페이지는 area.top~, 후속은 pageTop+marginT~.
        // 경계가 셀 내부 줄(line) 중간을 자르지 않도록 boundary를 line top까지 끌어올린다.
        // 셀별 bottom margin 최댓값 — 페이지 boundary 계산 시 셀 mB가
        // 페이지 안에 포함되어야 셀 박스 끝이 마지막 라인 본문 + mB가 된다.
        // mB를 차감하지 않으면 마지막 라인 본문이 셀 박스 끝에 닿아 셀이 잘리고,
        // 첫 페이지에 라인이 과도하게 들어간다.
        // mT는 차감하지 않는다: 셀이 페이지를 넘어 이어질 때 후속 segment의 첫 라인은
        // segment 시작에서 mT만큼 떨어진 곳에 배치되지만, lines 좌표가 cellDoc 안에서
        // 표 좌표로 변환될 때 이미 mT를 더해 들어가 있으므로 추가 차감 불필요.
        long maxCellMB = 0;
        for (Cell c : collectCells(ctrl)) {
            maxCellMB = Math.max(maxCellMB, (long) c.getListHeader().getBottomMargin());
        }
        long firstAvail = Math.max(1, pageBottom - marginB - area.top() - maxCellMB);
        long restAvail = Math.max(1, pageBottom - marginB - (pageTop + marginT) - maxCellMB);
        long[][] allLines = collectLinesInTableCoord(ctrl, rowY, rowCount, cellDocs);
        List<long[]> tablePages = new ArrayList<>();
        long curStart = 0;
        do {
            long avail = tablePages.isEmpty() ? firstAvail : restAvail;
            long tentativeEnd = curStart + avail;
            long end;
            long nextStart;
            if (tentativeEnd >= tableContentHeight) {
                end = tentativeEnd;
                nextStart = end;
            } else {
                long snapped = snapBoundaryToLine(tentativeEnd, curStart, allLines);
                if (snapped == tentativeEnd) {
                    // 경계에 걸친 본문 줄이 없음(빈 셀/행 박스 분할). tentativeEnd 는
                    // 셀 bottom margin(maxCellMB)만큼 페이지 경계보다 위에 있는데, 분할되어
                    // 다음 페이지로 이어지는 행 박스는 bottom margin 을 적용하지 않고 페이지
                    // 끝까지 채운다(한글 동작). 단 HWP 는 페이지 끝까지 꽉 채우지 않고
                    // 약 1pt(SPLIT_ROW_PAGE_GAP) 를 남긴다 — LineSeg 원본의 표 뒤 본문
                    // vertPos 와 대조해 확인(뒷페이지·병합된셀/1 모두 page2 조각이 정확히
                    // 이 값만큼 짧아야 LineSeg 정답과 일치). 그래야 표 page2 조각 바닥이
                    // OK 와 같아져 그 위에 겹쳐 그려지는 어울림 박스도 제자리에 온다.
                    end = Math.min(tentativeEnd + maxCellMB - SPLIT_ROW_PAGE_GAP, tableContentHeight);
                    nextStart = end;
                    // 경계가 "본문이 있는 행"의 본문 아래 빈 공간에 떨어진 경우(행의 줄이
                    // 모두 현재 페이지에 들어감)에는 행의 잔여 빈 꼬리를 폐기한다 — 한글은
                    // 잘린 행의 박스를 페이지 한계에서 절단하고(실문서 R02 표4 r5: 47px→42px)
                    // 다음 조각을 다음 행부터 시작한다. 내용이 전혀 없는 행(빈 행 박스
                    // 분할)은 기존대로 박스를 이월한다.
                    long[] rowRange = rowRangeAt(rowY, rowCount, tableContentHeight, tentativeEnd);
                    if (rowRange != null && rowRange[1] > end
                            && anyLineStartsBetween(allLines, rowRange[0], tentativeEnd)
                            && !anyLineStartsBetween(allLines, tentativeEnd, rowRange[1])) {
                        nextStart = rowRange[1];
                    }
                } else {
                    // 줄 경계로 당겨진 cut(snapped)이 행 내부이고 그 행의 나머지 높이에
                    // 더 이상 줄이 없으면, 잔여 빈 꼬리는 다음 페이지로 이월하지 않고
                    // 폐기한다 — 한글은 잘린 행의 박스를 페이지 한계에서 절단하고
                    // (실문서 R02 표4 r5: 47px→42px), 다음 조각은 다음 행부터 시작한다.
                    end = snapped;
                    long rowEnd = rowEndIfInsideRow(rowY, rowCount, tableContentHeight, snapped);
                    nextStart = (rowEnd > snapped
                            && !anyLineStartsBetween(allLines, snapped, rowEnd))
                            ? rowEnd : snapped;
                }
            }
            tablePages.add(new long[]{curStart, end});
            curStart = nextStart;
        } while (curStart < tableContentHeight);
        int tablePageCount = tablePages.size();

        // 3. 셀별로 표 페이지 조각을 만든다. 표를 연속 y좌표로 보고 페이지 경계에서
        //    잘라, 각 셀의 cellArea와 cellDocument를 표 페이지 조각으로 나눈다.
        //    병합 셀(rowSpan>1)도 행 범위 [rowY[ri], rowY[ri+rs]] 그대로 조각낸다.
        Map<Integer, List<TableItem.CellLayout>> pageSegCells = new HashMap<>();
        for (int tpi = 0; tpi < tablePageCount; tpi++) {
            pageSegCells.put(tpi, new ArrayList<>());
        }
        for (Cell cell : collectCells(ctrl)) {
            int ri = cell.getListHeader().getRowIndex();
            int rs = Math.max(1, cell.getListHeader().getRowSpan());
            int ci = cell.getListHeader().getColIndex();
            int cs = cell.getListHeader().getColSpan();
            if (ri < 0 || ri >= rowCount) continue;
            long cy0 = rowY[ri];
            long cy1 = rowY[Math.min(ri + rs, rowCount)];
            long mT = cell.getListHeader().getTopMargin();
            long cx = (ci < colX.length) ? colX[ci] : area.left();
            long cw = sumRange(colWidths, ci, cs);

            List<Integer> pages = new ArrayList<>();
            List<Long> innerBounds = new ArrayList<>();
            List<Long> segTops = new ArrayList<>();
            List<long[]> cellAreas = new ArrayList<>();
            for (int tpi = 0; tpi < tablePageCount; tpi++) {
                long[] tr = tablePages.get(tpi);
                if (cy1 <= tr[0] || cy0 >= tr[1]) continue;
                long segY0 = Math.max(cy0, tr[0]);
                long segY1 = Math.min(cy1, tr[1]);
                long tableTop = (tpi == 0) ? area.top() : pageTop + marginT;
                long cellTop = tableTop + (segY0 - tr[0]);
                long cellBot = tableTop + (segY1 - tr[0]);
                pages.add(tpi);
                if (innerBounds.isEmpty()) innerBounds.add(segY0 - cy0 - mT);
                innerBounds.add(segY1 - cy0 - mT);
                segTops.add(cellTop);
                cellAreas.add(new long[]{cellTop, cellBot});
            }
            if (pages.isEmpty()) continue;
            long[] bounds = new long[innerBounds.size()];
            for (int i = 0; i < bounds.length; i++) bounds[i] = innerBounds.get(i);
            long[] tops = new long[segTops.size()];
            for (int i = 0; i < tops.length; i++) tops[i] = segTops.get(i);
            Document[] segs = splitCellSegments(cellDocs.get(cell), bounds, tops, mT);
            for (int k = 0; k < pages.size(); k++) {
                long[] ca = cellAreas.get(k);
                Area cellArea = new Area(cx, ca[0], cx + cw, ca[1]);
                TableItem.CellLayout cl = new TableItem.CellLayout(cellArea, segs[k], cell);
                // 분할되지 않고 한 페이지에 통째로 놓인 셀은 일반 경로처럼 세로 정렬을
                // 적용한다.
                //
                // 분할된 셀은 조각마다 상단 정렬로 둔다 — 한글도 그렇다. 2026-08-13 에
                // 코퍼스 전수로 확인했다: 표나누기/셀나누기 14개 문서(전부 Center 정렬 셀)의
                // 분할 조각 페이지 30여 장이 **모두 best_dy=0**, 즉 우리 상단 정렬 결과와
                // 한글이 픽셀 단위로 맞는다. 특히 셀나누기/기본/3 seg0 은 실문서 R08 과
                // 같은 signature(Center, slack=-141)인데도 일치한다.
                // 조각에 세로 정렬을 켜면 slack 이 양수인 조각(기본/3 seg1 +318,
                // seg2 +459 → 2.7~3.8px 이동)이 어긋난다. **다시 시도하지 말 것.**
                if (pages.size() == 1) {
                    Area segInner = new Area(
                            cx + cell.getListHeader().getLeftMargin(), ca[0] + mT,
                            cx + cw - cell.getListHeader().getRightMargin(),
                            ca[1] - cell.getListHeader().getBottomMargin());
                    cl.verticalAlignmentOffset(computeCellVerticalOffset(cell, segInner, segs[k]));
                }
                pageSegCells.get(pages.get(k)).add(cl);
            }
        }

        // 3-b. 각 표 페이지마다 셀의 cellArea bottom을 행 안 모든 셀의 마지막 줄
        //      본문 끝(realBot)에 셀 mB를 더한 값으로 클램프한다 — 마지막 줄간격이 셀
        //      박스에 추가되지 않게 + 본문과 박스 사이 mB 여백 유지. 행 안 모든 셀이
        //      같은 끝선을 가지도록 행 단위 최댓값을 쓴다.
        //      병합 셀(rowSpan>1)은 여러 행을 덮어야 하므로 일반 클램프 대상에서 제외하되,
        //      병합 셀의 콘텐츠가 끝나는 페이지(마지막 segment) 셀은 별도로 콘텐츠 끝 +
        //      mB로 클램프한다 — 콘텐츠 아래의 잉여 공간 제거.
        for (int tpi = 0; tpi < tablePageCount; tpi++) {
            List<TableItem.CellLayout> segCells = pageSegCells.get(tpi);
            Map<Integer, Long> rowMaxBot = new HashMap<>();
            for (TableItem.CellLayout cl : segCells) {
                if (cl.cell().getListHeader().getRowSpan() > 1) continue;
                int rri = cl.cell().getListHeader().getRowIndex();
                long maxRealBot = 0;
                for (Page sp : cl.cellDocument().pages()) {
                    for (TextRow srow : sp.content().rows()) {
                        for (TextColumn scol : srow.columns()) {
                            for (TextLine sline : scol.textLines()) {
                                // 세로 정렬 오프셋만큼 내용이 내려가 렌더링되므로 포함한다.
                                long realBot = sline.area().bottom() - sline.lineGap()
                                        + cl.verticalAlignmentOffset();
                                if (realBot > maxRealBot) maxRealBot = realBot;
                            }
                        }
                    }
                }
                if (maxRealBot > 0) {
                    long mB = cl.cell().getListHeader().getBottomMargin();
                    long candidate = maxRealBot + mB;
                    Long cur = rowMaxBot.get(rri);
                    if (cur == null || candidate > cur) rowMaxBot.put(rri, candidate);
                }
            }
            for (TableItem.CellLayout cl : segCells) {
                if (cl.cell().getListHeader().getRowSpan() > 1) continue;
                int rri = cl.cell().getListHeader().getRowIndex();
                Long max = rowMaxBot.get(rri);
                if (max != null) {
                    // 고정 높이 행(지정 높이 > auto 센티넬)은 내용이 짧아도 지정 높이
                    // 박스를 유지한다(실문서 R02 표4 r0: 1줄 내용 + 빈 공간 48px 행).
                    // 축소 클램프는 auto 행 전용(마지막 줄간격 제거 목적)이고,
                    // 내용이 넘치면(max > bottom) 고정 행도 확장한다.
                    if (!isFixedHeightRow(ctrl, rri, rowH) || max > cl.area().bottom()) {
                        cl.area().bottom(max);
                    }
                }
            }
            // 병합 셀의 콘텐츠 끝 페이지(content가 더 이상 나오지 않는 마지막 segment)는
            // 콘텐츠 끝 + mB로 클램프한다. 같은 row span의 끝 행(rowIndex+rowSpan)을 공유하는
            // 셀들은 같은 바닥으로 정렬 — 콘텐츠 있는 셀의 끝에 다른 셀도 맞춘다.
            // (병합 셀과 행 마지막의 단일 셀이 함께 끝나도록.)
            Map<Integer, Long> endRowBot = new HashMap<>();
            for (TableItem.CellLayout cl : segCells) {
                if (cl.cell().getListHeader().getRowSpan() <= 1) continue;
                long maxRealBot = 0;
                for (Page sp : cl.cellDocument().pages()) {
                    for (TextRow srow : sp.content().rows()) {
                        for (TextColumn scol : srow.columns()) {
                            for (TextLine sline : scol.textLines()) {
                                long realBot = sline.area().bottom() - sline.lineGap()
                                        + cl.verticalAlignmentOffset();
                                if (realBot > maxRealBot) maxRealBot = realBot;
                            }
                        }
                    }
                }
                if (maxRealBot > 0) {
                    long mB = cl.cell().getListHeader().getBottomMargin();
                    long candidate = maxRealBot + mB;
                    if (candidate < cl.area().bottom()) {
                        int endRow = cl.cell().getListHeader().getRowIndex()
                                + cl.cell().getListHeader().getRowSpan();
                        Long cur = endRowBot.get(endRow);
                        if (cur == null || candidate > cur) endRowBot.put(endRow, candidate);
                    }
                }
            }
            // 같은 endRow 그룹의 모든 셀(병합 + 그 endRow의 단일 셀)에 동일 바닥 적용.
            for (TableItem.CellLayout cl : segCells) {
                int endRow = cl.cell().getListHeader().getRowIndex()
                        + cl.cell().getListHeader().getRowSpan();
                Long groupBot = endRowBot.get(endRow);
                if (groupBot != null && groupBot < cl.area().bottom()) {
                    cl.area().bottom(groupBot);
                }
            }
            // 위 행의 cellArea bottom이 줄어들었으면 다음 행의 cellArea top도 같이
            // 끌어올려 행 박스가 연결되게 한다 — 두 표로 분리되어 보이지 않게.
            java.util.TreeSet<Integer> risInSeg = new java.util.TreeSet<>();
            for (TableItem.CellLayout cl : segCells) {
                risInSeg.add(cl.cell().getListHeader().getRowIndex());
            }
            Integer prevRi = null;
            for (int rri : risInSeg) {
                if (prevRi != null) {
                    // prevMaxBot 계산: prevRi 행의 셀들 중 병합 셀(rowSpan>1)이 현재
                    // 행 rri까지 덮으면 제외 — 그 셀은 자기 행만 아니라 여러 행을
                    // 차지하므로 그 cellArea bottom은 row 그룹 끝이고, 현재 행의
                    // 시작과 비교하는 게 부적절하다.
                    long prevMaxBot = 0;
                    for (TableItem.CellLayout cl : segCells) {
                        if (cl.cell().getListHeader().getRowIndex() != prevRi) continue;
                        int rs = Math.max(1, cl.cell().getListHeader().getRowSpan());
                        if (rs > 1 && prevRi + rs > rri) continue;
                        if (cl.area().bottom() > prevMaxBot) prevMaxBot = cl.area().bottom();
                    }
                    // prevRi에 유효한 비병합 셀이 없으면 (병합 셀만 있어서 모두 제외된 경우)
                    // 행 간 연결 처리 skip — 잘못 발동하면 현재 행 셀들을 페이지 상단으로
                    // 끌어올린다(병합된셀/2 page2 case).
                    if (prevMaxBot > 0) {
                        for (TableItem.CellLayout cl : segCells) {
                            if (cl.cell().getListHeader().getRowIndex() != rri) continue;
                            long top = cl.area().top();
                            if (top > prevMaxBot) {
                                // 위 행이 줄어들었을 때 — top을 위로 끌어올리고 bottom도
                                // 같이 위로 옮겨 행 height 유지(빈 행 lineGap만큼 늘어남 방지).
                                long delta = top - prevMaxBot;
                                cl.area().top(prevMaxBot);
                                cl.area().bottom(cl.area().bottom() - delta);
                            } else if (top < prevMaxBot) {
                                // 위 행이 늘어났을 때 — top을 아래로 끌어내려 위 행과 겹침
                                // 방지. bottom도 같이 내려 행 height 유지.
                                long delta = prevMaxBot - top;
                                cl.area().top(prevMaxBot);
                                cl.area().bottom(cl.area().bottom() + delta);
                            }
                        }
                    }
                }
                prevRi = rri;
            }
        }

        // 4. 표 페이지마다 TableItem 생성.
        Page page = firstPage;
        long lastBottom = area.top();
        long firstPageBottom = area.top();
        for (int tpi = 0; tpi < tablePageCount; tpi++) {
            long tableTop = (tpi == 0) ? area.top() : pageTop + marginT;
            List<TableItem.CellLayout> segCells = pageSegCells.get(tpi);
            long pageMaxBottom = tableTop;
            for (TableItem.CellLayout cl : segCells) {
                pageMaxBottom = Math.max(pageMaxBottom, cl.area().bottom());
            }
            Area tableArea = new Area(area.left(), tableTop, area.right(), pageMaxBottom);
            TableItem item = new TableItem(ctrl, tableArea, withMargin(tableArea, header), behindText);
            item.cells().addAll(segCells);
            item.zOrder(header.getzOrder());
            if (tpi == 0) {
                firstPage.addControl(item);
            } else if (takePlace) {
                page = nextPageOrCreate(ctx, page);
                page.addControl(item);
            } else {
                ctx.enqueuePendingTableSegment(item);
            }
            lastBottom = pageMaxBottom;
            if (tpi == 0) firstPageBottom = pageMaxBottom;
        }
        // 5. 자리차지 표: 표 다음 본문의 흐름을 위해 ctx를 갱신한다.
        if (takePlace) {
            Page firstSegPage = firstPage;
            // 표 단락(paraIdx) 텍스트가 firstSegPage 의 표 위 공간에 들어가 있는지 본다.
            // 빈셀처럼 표 단락 본문("1")이 firstSegPage 의 area.top 위에 배치되어 있으면
            // 후속 단락도 그 자리(표 위)에서 자연스럽게 이어져야 하므로 currentY 를
            // 표 바닥으로 끌어내리지 않는다. 단, 표 단락 본문이 firstSegPage 에 아예
            // 없거나(LineSeg 가 다른 페이지로 보냄) 표와 겹치면 원래 동작(표 아래로
            // 진행)을 유지해 다른 케이스 회귀를 막는다.
            long preTableCurrentY = ctx.currentY();
            boolean preserveCurrentYAboveTable =
                    tableParaTextFitsAboveTableOnFirstSeg(firstSegPage, paraIdx, area.top());
            // 표 단락의 텍스트 줄은 표 마지막 조각 페이지로 옮긴다(표 컨트롤과 한 단락).
            // 단, 표 area.top보다 위에 있는 줄(표 앞쪽 본문)은 그대로 둔다.
            // 그리고 firstSegPage의 표 단락 이후(paraIdx > X) 줄들도 마지막 조각 페이지로
            // 옮긴다 — 표 뒤에 와야 하는 본문 단락이 firstSegPage에 미리 배치된 경우.
            if (page != firstSegPage) {
                ctx.currentPage(page);
                ctx.currentY(Math.min(lastBottom, pageBottom));
                ctx.lastLineSegVertPos(-1);
                moveParaLinesToCurrentPage(paraIdx, firstSegPage, area.top(), ctx);
                moveLaterParaLinesToCurrentPage(paraIdx, firstSegPage, ctx);
            }
            // 표 다음 본문 단락은 표 첫 페이지부터 흐른다 — 본문의 LineSeg vertPos가
            // 페이지를 결정하므로 시작점을 표 첫 페이지로 둔다(표가 여러 페이지로 분할돼도
            // 본문은 원본 LineSeg대로 표 첫 페이지에서 이어진다).
            ctx.currentPage(firstSegPage);
            ctx.bodyArea(ctx.pageBodyArea().clone());
            // 표 단락 텍스트가 표와 겹치지 않았으면(= 본문이 표 위 공간에 들어감) 후속
            // 단락도 표 위 공간을 이어 채우도록 preTableCurrentY 를 유지. 그렇지 않으면
            // 표 단락 텍스트가 표 마지막 조각 페이지로 옮겨졌으므로 후속 본문은 표 첫
            // 조각 아래에서 시작한다.
            ctx.currentY(preserveCurrentYAboveTable
                    ? preTableCurrentY
                    : Math.min(firstPageBottom, pageBottom));
            // 표 단락 vertPos를 기준으로 남겨, 다음 본문 첫 줄의 vertPos가 더 작으면
            // (표 마지막 페이지 기준) handlePageBreakIfNeeded가 새 페이지로 넘긴다.
            ctx.lastLineSegVertPos(enteringVertPos);
            // 후속 단락의 첫 줄이 firstSegPage의 표 영역과 겹치면 페이지를 넘기게
            // 표 영역을 저장. handlePageBreakIfNeeded가 한 번 처리 후 자동 해제.
            if (page != firstSegPage) {
                Area firstSegArea = new Area(area.left(), area.top(),
                        area.right(), firstPageBottom);
                ctx.takePlaceTableArea(firstSegArea);
                // 후속 paragraph의 플로팅 컨트롤(InFrontOfText/BehindText)을 lastSegPage로
                // 라우팅하기 위해 표의 paraIdx와 lastSegPage를 저장.
                ctx.lastDivideTableParaIdx(paraIdx);
                ctx.lastDivideTableLastSegPage(page);
                ctx.lastDivideTableFirstSegPage(firstSegPage);
                ctx.lastDivideTableFirstSegArea(firstSegArea);
                ctx.lastDivideTableLastSegBottom(Math.min(lastBottom, pageBottom));
                ctx.lastDivideTableBodyAboveTable(preserveCurrentYAboveTable);
            }
        }
    }

    /**
     * firstSegPage 에 paraIdx 단락 텍스트가 있고, 그 줄들이 모두 표 area.top 위에 있는지
     * (= 표 단락 본문이 표 위 공간에 자리잡아 후속 단락도 그 자리에서 이어져야 하는지).
     * 줄이 하나도 없으면(LineSeg 가 다른 페이지로 보냈거나 단락이 비어 있으면) false.
     */
    private static boolean tableParaTextFitsAboveTableOnFirstSeg(Page firstSegPage,
                                                                  int paraIdx, long tableTop) {
        boolean foundLine = false;
        for (TextRow row : firstSegPage.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.paraIndex() != paraIdx) continue;
                    if (line.area().bottom() > tableTop) return false;
                    foundLine = true;
                }
            }
        }
        return foundLine;
    }

    /**
     * 후처리: 자리차지 분할 표가 있던 firstSegPage에서 표 paragraph 이후 paragraph의
     * 본문 라인을 검사한다. 본문 라인 중 어느 하나라도 표 영역과 겹치면(본문이
     * 표 위 공간에 다 안 들어가는 경우), 표 paragraph 이후 모든 본문 라인을 lastSegPage로
     * 이동한다 — 본문을 분리하지 않고 한 페이지에 모은다.
     * <p>표가 layoutControls에서 placed 될 때는 아직 후속 paragraph가 layout 안 됐기
     * 때문에 layoutSection 마지막에 후처리로 정리한다.</p>
     */
    public void moveLinesOverlappingTakePlaceTable(ParaLayoutContext ctx) {
        if (ctx.lastDivideTableParaIdx() < 0) return;
        Page from = ctx.lastDivideTableFirstSegPage();
        Page to = ctx.lastDivideTableLastSegPage();
        Area tableArea = ctx.lastDivideTableFirstSegArea();
        if (from == null || to == null || tableArea == null || from == to) return;
        int tableParaIdx = ctx.lastDivideTableParaIdx();
        boolean bodyAboveTable = ctx.lastDivideTableBodyAboveTable();
        final long tableBottomOnLastSeg = ctx.lastDivideTableLastSegBottom();

        // 1단계: 표 paragraph 뒤 라인 중 표 영역과 겹치는 줄이 있는지 확인.
        // bodyAboveTable 모드(빈셀 등)에서는 처음 겹치는 줄의 top 도 기록 — 그 위 줄은
        // 표 위 공간에 정상 배치된 것이므로 보존한다.
        boolean anyOverlap = false;
        long firstOverlapTop = Long.MAX_VALUE;
        for (TextRow row : from.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.paraIndex() <= tableParaIdx) continue;
                    if (line.area().bottom() > tableArea.top()
                            && line.area().top() < tableArea.bottom()) {
                        anyOverlap = true;
                        if (line.area().top() < firstOverlapTop) {
                            firstOverlapTop = line.area().top();
                        }
                    }
                }
            }
        }
        // lastSegPage(page2)로 넘친 표 뒤 본문도 이동 트리거로 본다. 표가 페이지를 거의
        // 꽉 채워 firstSegPage 에 본문이 안 남고 전부 lastSegPage 상단(표 영역)으로 넘어간
        // 경우(병합된셀/2), from 검사만으로는 anyOverlap 이 false 라 조기 반환되어 본문이
        // 표 위에 방치된다. 표 바닥 위 본문 라인이 있으면 이동을 진행한다.
        for (TextRow row : to.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.paraIndex() > tableParaIdx
                            && line.area().top() < tableBottomOnLastSeg) {
                        anyOverlap = true;
                    }
                }
            }
        }
        if (!anyOverlap) {
            // 표 위 공간에 본문이 다 들어감 — 옮기지 않는다 (예: 빈셀 케이스).
            ctx.lastDivideTableParaIdx(-1);
            ctx.lastDivideTableLastSegPage(null);
            ctx.lastDivideTableFirstSegPage(null);
            ctx.lastDivideTableFirstSegArea(null);
            ctx.takePlaceTableArea(null);
            ctx.lastDivideTableBodyAboveTable(false);
            return;
        }

        // 2단계: 표 paragraph 뒤 본문 라인을 lastSegPage 로 옮긴다.
        //  - bodyAboveTable 모드: 첫 겹침 줄부터(그 위 줄은 보존). 빈셀처럼 표 위 공간
        //    에 일부 줄이 정상 배치된 케이스에서 그 줄들까지 함께 옮기지 않도록.
        //  - 그 외(기본): 모든 줄을 옮긴다 — 표 단락 본문이 LineSeg 로 다른 페이지에
        //    배치되어 firstSegPage 의 표 위 공간 줄들은 잘못 흘러든 것일 수 있다.
        final long threshold = bodyAboveTable ? firstOverlapTop : Long.MIN_VALUE;
        List<TextLine> moved = new ArrayList<>();
        for (TextRow row : from.content().rows()) {
            for (TextColumn col : row.columns()) {
                col.textLines().removeIf(line -> {
                    if (line.paraIndex() > tableParaIdx
                            && line.area().top() >= threshold) {
                        moved.add(line);
                        return true;
                    }
                    return false;
                });
            }
        }
        // lastSegPage(page2)에서 합성 중 표 영역으로 넘쳐 들어온 표 뒤 본문 라인도 수집한다.
        // 본문이 firstSegPage 하단(표 첫 조각 위치)에서 흐르다 페이지를 넘으면, 일부 줄은
        // firstSegPage 에 남고(위에서 수집) 일부는 lastSegPage 상단(표 둘째 조각 위)으로
        // 넘어간다(예: 병합된셀 본문 "7\ncccc"). 표 바닥 위 줄을 그대로 두면 표에 겹쳐
        // 그려지므로 firstSegPage 라인과 함께 표 아래로 재배치한다.
        for (TextRow row : to.content().rows()) {
            for (TextColumn col : row.columns()) {
                col.textLines().removeIf(line -> {
                    if (line.paraIndex() > tableParaIdx
                            && line.area().top() < tableBottomOnLastSeg) {
                        moved.add(line);
                        return true;
                    }
                    return false;
                });
            }
        }
        if (moved.isEmpty()) return;
        // paraIndex 순(같은 단락 내에서는 top 순)으로 정렬한다. 서로 다른 페이지에서 모은
        // 라인을 top 만으로 정렬하면 순서가 뒤섞인다 — lastSegPage 로 넘친 라인의 top 이
        // firstSegPage 라인보다 작아 앞으로 가버린다(7·cccc 가 1~6 앞에 오는 버그).
        moved.sort((a, b) -> a.paraIndex() != b.paraIndex()
                ? Integer.compare(a.paraIndex(), b.paraIndex())
                : Long.compare(a.area().top(), b.area().top()));
        TextColumn dest;
        if (to.content().rows().isEmpty()) {
            dest = to.content().addRow().addColumn();
        } else {
            TextRow lastRow = to.content().rows().get(to.content().rows().size() - 1);
            if (lastRow.columns().isEmpty()) {
                dest = lastRow.addColumn();
            } else {
                dest = lastRow.columns().get(lastRow.columns().size() - 1);
            }
        }
        // lastSegPage에 이미 배치된 본문 라인(예: moveParaLinesToCurrentPage가 옮긴
        // 표 단락의 본문 "1")의 최하단부터 이어 배치 — 표 다음 행 영역과 겹치지 않게.
        long y = ctx.lastDivideTableLastSegBottom();
        for (TextRow row : to.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (line.area().bottom() > y) y = line.area().bottom();
                }
            }
        }
        for (TextLine line : moved) {
            long h = line.area().bottom() - line.area().top();
            long oldTop = line.area().top();
            line.area().top(y);
            line.area().bottom(y + h);
            dest.textLines().add(line);
            // 이 줄에 anchor 된 vertRelTo=Para·Line 컨트롤(예: 분할 표 뒤 문단의 사각형)은
            // 줄과 함께 옮겨야 한다. processControl 이 줄 이동 전 좌표(page1 흐름)로 컨트롤을
            // 배치했으므로, 줄과 같은 delta 로 컨트롤 area 를 옮기고 lastSegPage 로 모은다.
            // (vertRelTo=Paper·Page 컨트롤은 절대좌표라 제외 — yRelativeToAnchor=false)
            shiftAnchoredControlsWithLine(from, to, oldTop, y - oldTop);
            y += h;
        }
        ctx.lastDivideTableParaIdx(-1);
        ctx.lastDivideTableLastSegPage(null);
        ctx.lastDivideTableFirstSegPage(null);
        ctx.lastDivideTableFirstSegArea(null);
        ctx.takePlaceTableArea(null);
        ctx.lastDivideTableBodyAboveTable(false);
    }

    /**
     * {@link #moveLinesOverlappingTakePlaceTable} 가 anchor 줄을 oldTop→oldTop+delta 로 옮길 때,
     * 그 줄에 anchor 된 vertRelTo=Para·Line 컨트롤(yRelativeToAnchor)을 같은 delta 로 옮기고
     * lastSegPage({@code to})로 모은다. processControl 은 줄 이동 전 좌표로 컨트롤을 배치하므로
     * 이 보정이 없으면 컨트롤이 page1 흐름 좌표로 page2 에 그려진다(뒷페이지 케이스).
     */
    private static void shiftAnchoredControlsWithLine(Page from, Page to,
                                                       long oldTop, long delta) {
        for (Page p : new Page[]{from, to}) {
            java.util.Iterator<ControlItem> it = p.controls().iterator();
            List<ControlItem> moveToDest = new ArrayList<>();
            while (it.hasNext()) {
                ControlItem item = it.next();
                if (!item.yRelativeToAnchor()) continue;
                if (item.anchorLineTop() != oldTop) continue;
                item.area().top(item.area().top() + delta);
                item.area().bottom(item.area().bottom() + delta);
                item.areaWithMargin().top(item.areaWithMargin().top() + delta);
                item.areaWithMargin().bottom(item.areaWithMargin().bottom() + delta);
                item.anchorLineTop(oldTop + delta);
                if (p != to) {   // from 페이지에 있던 컨트롤은 lastSegPage 로 이동.
                    it.remove();
                    moveToDest.add(item);
                }
            }
            for (ControlItem item : moveToDest) to.addControl(item);
        }
    }

    /** 셀 내용 레이아웃용 영역 — left/right는 colSpan·좌우 여백 반영, top=0, 주어진 높이. */
    private static Area cellProbeArea(Cell cell, long[] colX, long[] colWidths,
                                       long tableLeft, long height) {
        return cellProbeArea(cell, colX, colWidths, tableLeft, height, 0);
    }

    private static Area cellProbeArea(Cell cell, long[] colX, long[] colWidths,
                                       long tableLeft, long height, long spacing) {
        int ci = cell.getListHeader().getColIndex();
        int cs = cell.getListHeader().getColSpan();
        long cx = (ci < colX.length) ? colX[ci] : tableLeft;
        long cw = sumRange(colWidths, ci, cs, spacing);
        int mL = cell.getListHeader().getLeftMargin();
        int mR = cell.getListHeader().getRightMargin();
        return new Area(cx + mL, 0, cx + cw - mR, height);
    }

    /**
     * 셀 cellDocument의 줄들을 표 페이지 조각별 Document로 나눈다.
     *
     * <p>cellDoc은 (여러 페이지여도) 연속 y좌표 흐름으로 본다. 각 줄의 content y가
     * 어느 조각 {@code [bounds[i], bounds[i+1])}에 속하는지로 분배하고, 그 조각의
     * 절대 y로 옮긴다 — content y {@code bounds[i]}가 {@code tops[i]}에 온다.</p>
     *
     * @param bounds 길이 n+1. 조각 i는 content y {@code [bounds[i], bounds[i+1])}.
     * @param tops   길이 n. 조각 i 줄의 절대 y 기준.
     */
    private static Document[] splitCellSegments(Document cellDoc, long[] bounds, long[] tops, long mT) {
        int n = tops.length;
        Document[] out = new Document[n];
        TextColumn[] dstCols = new TextColumn[n];
        for (int i = 0; i < n; i++) {
            out[i] = new Document();
            Page p = out[i].addPage();
            dstCols[i] = p.content().addRow().addColumn();
        }
        if (cellDoc == null) return out;
        // 1단계: line을 segment별로 분류해 (contTop, line) 쌍으로 모은다.
        @SuppressWarnings("unchecked")
        List<long[]>[] segContTops = new List[n];
        @SuppressWarnings("unchecked")
        List<TextLine>[] segLines = new List[n];
        for (int i = 0; i < n; i++) {
            segContTops[i] = new ArrayList<>();
            segLines[i] = new ArrayList<>();
        }
        long pageOffset = 0;
        for (Page src : cellDoc.pages()) {
            long pageMax = 0;
            for (TextRow row : src.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        pageMax = Math.max(pageMax, line.area().bottom());
                    }
                }
            }
            for (TextRow row : src.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        long contTop = line.area().top() + pageOffset;
                        for (int i = 0; i < n; i++) {
                            if (contTop >= bounds[i]
                                    && (contTop < bounds[i + 1] || i == n - 1)) {
                                segContTops[i].add(new long[]{contTop});
                                segLines[i].add(line);
                                break;
                            }
                        }
                    }
                }
            }
            pageOffset += pageMax;
        }
        // 2단계: 각 segment의 첫 줄을 segment 상단에서 mT 만큼 떨어진 위치(tops[i]+mT)에
        //        두고, 나머지는 첫 줄 기준 상대 위치로 옮긴다. 첫 줄 위의 줄간격 빈공간은
        //        제거되고 셀 상단 마진(mT)만 남는다.
        long[] firstContTops = new long[n];
        for (int i = 0; i < n; i++) {
            if (segLines[i].isEmpty()) {
                firstContTops[i] = bounds[i];
                continue;
            }
            firstContTops[i] = segContTops[i].get(0)[0];
            for (int k = 0; k < segLines[i].size(); k++) {
                TextLine line = segLines[i].get(k);
                long contTop = segContTops[i].get(k)[0];
                long h = line.area().bottom() - line.area().top();
                long nt = tops[i] + mT + (contTop - firstContTops[i]);
                line.area().top(nt);
                line.area().bottom(nt + h);
                dstCols[i].textLines().add(line);
            }
        }
        // 3단계: cellDoc의 컨트롤을 segment별로 분배. 컨트롤이 가장 가까운(거리가 짧은)
        //        line의 segment에 배치 — 컨트롤은 anchor line을 따라가야 한다.
        //        스택 좌표에서 컨트롤 top과 각 segment의 line 범위 거리 비교.
        long pageOffset2 = 0;
        // 각 segment의 line 범위(stacked 좌표) 수집.
        long[] segMinTop = new long[n];
        long[] segMaxBot = new long[n];
        for (int i = 0; i < n; i++) {
            segMinTop[i] = Long.MAX_VALUE;
            segMaxBot[i] = Long.MIN_VALUE;
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < segContTops[i].size(); k++) {
                long ct = segContTops[i].get(k)[0];
                long ch = segLines[i].get(k).area().bottom() - segLines[i].get(k).area().top();
                if (ct < segMinTop[i]) segMinTop[i] = ct;
                if (ct + ch > segMaxBot[i]) segMaxBot[i] = ct + ch;
            }
        }
        for (Page src : cellDoc.pages()) {
            long pageMax = 0;
            for (TextRow row : src.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        pageMax = Math.max(pageMax, line.area().bottom());
                    }
                }
            }
            for (kr.dogfoot.hwpdrawer.document.control.ControlItem ci : src.controls()) {
                long ciTop = ci.area().top() + pageOffset2;
                long ciBot = ci.area().bottom() + pageOffset2;
                // 컨트롤 center가 어느 segment 범위에 가장 가까운지 찾는다.
                long routingY = (ciTop + ciBot) / 2;
                int bestSeg = -1;
                long bestDist = Long.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (segLines[i].isEmpty()) continue;
                    long dist;
                    if (routingY < segMinTop[i]) dist = segMinTop[i] - routingY;
                    else if (routingY > segMaxBot[i]) dist = routingY - segMaxBot[i];
                    else dist = 0;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestSeg = i;
                    }
                }
                if (bestSeg < 0) bestSeg = 0;
                // 컨트롤이 segment 경계를 가로지르는지 확인 — ciTop가 segment 시작
                // 이전이고 ciBot가 다음 segment에 속하면 양쪽 segment에 그려야 한다.
                // 단, 겹침이 아주 작은 segment(컨트롤 높이의 일정 비율 미만)는 제외한다 —
                // 컨트롤이 경계 근처에 있어 인접 segment에 몇 백 HWPUNIT만 걸치는 경우
                // 그 작은 조각까지 그리면 잘못된 작은 사각형이 생긴다(걸처진컨트롤/1).
                long ciHeight = Math.max(1, ciBot - ciTop);
                long minOverlap = Math.max(MIN_CROSS_OVERLAP, ciHeight / 4);
                java.util.List<Integer> crossSegs = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) {
                    long segLo = bounds[i];
                    long segHi = bounds[i + 1];
                    long overlap = Math.min(ciBot, segHi) - Math.max(ciTop, segLo);
                    if (overlap >= minOverlap) {
                        crossSegs.add(i);
                    }
                }
                // 가로지르지 않거나 한 segment에만 의미있게 걸치면 best segment에 배치.
                // 두 개 이상 segment에 의미있게 걸치면 각 segment에 clone 추가.
                if (crossSegs.size() <= 1) {
                    placeControlInSegment(ci, ciTop, ciBot, bestSeg,
                            firstContTops, tops, mT, out);
                } else {
                    for (int si : crossSegs) {
                        kr.dogfoot.hwpdrawer.document.control.ControlItem clone =
                                ci.cloneWithArea(ci.area().clone(),
                                        ci.areaWithMargin() != null
                                                ? ci.areaWithMargin().clone() : ci.area().clone());
                        // clone의 area는 원본 ci.area 그대로(아직 segment 좌표로 변환 전).
                        // 변환을 위해 ciTop/ciBot를 원본 값으로 다시 사용.
                        placeControlInSegment(clone, ciTop, ciBot, si,
                                firstContTops, tops, mT, out);
                    }
                }
            }
            pageOffset2 += pageMax;
        }
        return out;
    }

    /** 컨트롤을 지정 segment 좌표로 변환해 그 segment Document에 추가한다. */
    private static void placeControlInSegment(
            kr.dogfoot.hwpdrawer.document.control.ControlItem ci,
            long ciTop, long ciBot, int segIdx,
            long[] firstContTops, long[] tops, long mT, Document[] out) {
        long relOffset = Math.max(0, ciTop - firstContTops[segIdx]);
        long delta = (tops[segIdx] + mT + relOffset) - ciTop;
        ci.area().top(ciTop + delta);
        ci.area().bottom(ciBot + delta);
        if (ci.areaWithMargin() != null && ci.areaWithMargin() != ci.area()) {
            ci.areaWithMargin().top(ci.areaWithMargin().top() + delta);
            ci.areaWithMargin().bottom(ci.areaWithMargin().bottom() + delta);
        }
        out[segIdx].pages().get(0).addControl(ci);
    }

    /** 표 행 r의 cellDocument {@code pageIdx} 페이지 조각 높이 — 셀별 (내용 높이 + 상하 여백) 최댓값. */
    private long divideRowSegmentHeight(ControlTable ctrl, int r, int pageIdx,
                                         Map<Cell, Document> cellDocs, ParaLayoutContext ctx) {
        long h = 0;
        for (Cell cell : cellsOfRow(ctrl, r)) {
            long content = cellPageContentHeight(cellDocs.get(cell), pageIdx);
            content = Math.max(content, cellFontBaseSize(cell, ctx));
            content += cell.getListHeader().getTopMargin() + cell.getListHeader().getBottomMargin();
            h = Math.max(h, content);
        }
        return h;
    }

    /**
     * 셀 cellDocument의 모든 페이지 내용 높이 합 + 셀 상하 여백 (셀 전체 표시 높이).
     * <p>{@link #collectLinesInTableCoord}와 동일하게 각 페이지의 마지막 줄 bottom(줄간격 포함)을
     * 누적해, 다중 페이지 cellDocument에서도 line의 표 좌표가 행 범위에 포함되도록 한다.
     * 단 마지막 줄의 줄간격만 한 번 빼서 셀 박스 끝에 줄간격이 추가되지 않게 한다.</p>
     */
    private long cellTotalContentHeight(Document cellDoc, Cell cell, ParaLayoutContext ctx) {
        // 모든 줄(line)을 셀 좌표로 모은 뒤, 가장 아래 줄의 realBot(=area.bottom - lineGap)을
        // 콘텐츠 끝으로 본다 — 마지막 줄의 줄간격은 적용하지 않는다(줄간격은 라인과 라인
        // 사이에만 적용).
        long lastLineRealBot = 0;
        long pageOffset = 0;
        for (Page src : cellDoc.pages()) {
            long pageMax = 0;
            long pageLastLineRealBot = 0;
            for (TextRow row : src.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        if (line.area().bottom() > pageMax) {
                            pageMax = line.area().bottom();
                            pageLastLineRealBot = line.area().bottom() - line.lineGap();
                        }
                    }
                }
            }
            if (pageLastLineRealBot > 0) {
                lastLineRealBot = pageOffset + pageLastLineRealBot;
            }
            pageOffset += pageMax;
        }
        long h = lastLineRealBot;
        long mT = cell.getListHeader().getTopMargin();
        long mB = cell.getListHeader().getBottomMargin();
        return Math.max(h, cellFontBaseSize(cell, ctx)) + mT + mB;
    }

    /**
     * 모든 셀 줄을 표 연속 y좌표로 모은다. 반환 배열 원소는 {@code {lineTop, lineRealBot, lineGapBot}}.
     * <ul>
     *   <li>{@code lineRealBot} = {@code area.bottom - lineGap} — 줄 본문(글자) 끝.
     *   <li>{@code lineGapBot} = {@code area.bottom} — 줄간격 포함 끝.
     * </ul>
     * '나누기' 표의 페이지 경계를 줄 경계에 맞추는 데 쓴다.
     */
    private static long[][] collectLinesInTableCoord(ControlTable ctrl, long[] rowY,
                                                      int rowCount,
                                                      Map<Cell, Document> cellDocs) {
        List<long[]> all = new ArrayList<>();
        for (Cell cell : collectCells(ctrl)) {
            int ri = cell.getListHeader().getRowIndex();
            if (ri < 0 || ri >= rowCount) continue;
            Document cd = cellDocs.get(cell);
            if (cd == null) continue;
            long cy0 = rowY[ri];
            long mT = cell.getListHeader().getTopMargin();
            long pageOffset = 0;
            for (Page src : cd.pages()) {
                long pageMax = 0;
                for (TextRow row : src.content().rows()) {
                    for (TextColumn col : row.columns()) {
                        for (TextLine line : col.textLines()) {
                            pageMax = Math.max(pageMax, line.area().bottom());
                        }
                    }
                }
                for (TextRow row : src.content().rows()) {
                    for (TextColumn col : row.columns()) {
                        for (TextLine line : col.textLines()) {
                            long lt = cy0 + mT + line.area().top() + pageOffset;
                            long lgb = cy0 + mT + line.area().bottom() + pageOffset;
                            long lrb = lgb - line.lineGap();
                            all.add(new long[]{lt, lrb, lgb});
                        }
                    }
                }
                pageOffset += pageMax;
            }
        }
        return all.toArray(new long[0][]);
    }

    /**
     * boundary가 행 내부(행 시작과 끝 사이, 경계 제외)에 있으면 그 행의 끝(content 좌표)을
     * 반환하고, 행 경계 위에 정확히 있으면 -1을 반환한다.
     */
    private static long rowEndIfInsideRow(long[] rowY, int rowCount,
                                           long tableContentHeight, long boundary) {
        long[] range = rowRangeAt(rowY, rowCount, tableContentHeight, boundary);
        return range == null ? -1 : range[1];
    }

    /**
     * boundary가 행 내부(경계 제외)에 있으면 그 행의 {시작, 끝}(content 좌표)을 반환하고,
     * 행 경계 위에 정확히 있으면 null을 반환한다.
     */
    private static long[] rowRangeAt(long[] rowY, int rowCount,
                                      long tableContentHeight, long boundary) {
        for (int r = 1; r <= rowCount; r++) {
            long rowBot = (r < rowCount) ? rowY[r] : tableContentHeight;
            if (rowBot > boundary) {
                return (rowY[r - 1] < boundary) ? new long[]{rowY[r - 1], rowBot} : null;
            }
        }
        return null;
    }

    /** [from..to) 구간에서 시작하는 줄이 있는지 확인한다. */
    private static boolean anyLineStartsBetween(long[][] lines, long from, long to) {
        for (long[] l : lines) {
            if (l[0] >= from && l[0] < to) return true;
        }
        return false;
    }

    /**
     * 페이지 경계를 줄에 맞춰 끌어올린다.
     * <ul>
     *   <li>boundary가 줄 본문(top ~ realBot) 안에 있으면 줄 top으로 — 그 줄은 다음 페이지로.
     *   <li>boundary가 줄간격 영역(realBot ~ gapBot) 안에 있으면 realBot으로 — 잘리는 셀에
     *       마지막 줄의 줄간격이 포함되지 않도록.
     * </ul>
     * {@code minBoundary} 이하로는 내리지 않는다.
     */
    private static long snapBoundaryToLine(long boundary, long minBoundary, long[][] lines) {
        long adjusted = boundary;
        // 두 단계: (1) 본문을 자르는 줄이 있으면 top으로, (2) 그 결과가 어느 줄 gapBot
        // 이내(== 포함)이면 그 줄 realBot까지 더 끌어내려 마지막 줄간격을 제거.
        for (long[] l : lines) {
            long top = l[0], realBot = l[1];
            if (top <= minBoundary) continue;
            if (top < adjusted && realBot > adjusted) {
                adjusted = top;
            }
        }
        for (long[] l : lines) {
            long realBot = l[1], gapBot = l[2];
            if (realBot <= minBoundary) continue;
            if (realBot < adjusted && gapBot >= adjusted) {
                adjusted = realBot;
            }
        }
        return adjusted;
    }

    /**
     * '나누기' 표의 행별 높이. {@link #resolveRowHeights}와 같은 rowSpan span 처리이되,
     * 셀 높이를 cellDocument 전체 내용 높이(모든 페이지 합)로 본다 — 셀 내용이 페이지를
     * 넘어 cellDocument가 여러 페이지로 나뉘는 '나누기' 표에서 정확한 행 높이를 얻는다.
     */
    private long[] divideRowHeights(ControlTable ctrl, int rowCount,
                                     Map<Cell, Document> cellDocs, ParaLayoutContext ctx) {
        List<RowInfo> infos = new ArrayList<>();
        for (Cell cell : collectCells(ctrl)) {
            int ri = cell.getListHeader().getRowIndex();
            int rs = cell.getListHeader().getRowSpan();
            long storedH = cell.getListHeader().getHeight();
            long contentH = cellTotalContentHeight(cellDocs.get(cell), cell, ctx);
            addRowInfo(infos, ri, rs, Math.max(contentH, storedH), storedH);
        }
        long[] calc = new long[rowCount];
        long[] cellH = new long[rowCount];
        java.util.Arrays.fill(calc, -1L);
        java.util.Arrays.fill(cellH, -1L);
        int maxSpan = 1;
        for (RowInfo i : infos) if (i.span > maxSpan) maxSpan = i.span;
        for (int span = 1; span <= maxSpan; span++) {
            for (RowInfo info : infos) {
                if (info.span == span) applyRowInfo(info, calc, cellH, rowCount);
            }
        }
        for (RowInfo info : infos) {
            if (isSpanAllUnset(info, calc, rowCount)) {
                for (int i = info.index; i < info.index + info.span && i < rowCount; i++) {
                    calc[i] = (i == info.index) ? (info.height - info.span + 1) : 1;
                }
            }
        }
        for (int i = 0; i < rowCount; i++) if (calc[i] < 0) calc[i] = 0;
        return calc;
    }

    /** rowIndex가 r인 셀들을 모은다. */
    private static List<Cell> cellsOfRow(ControlTable ctrl, int r) {
        List<Cell> out = new ArrayList<>();
        for (Cell cell : collectCells(ctrl)) {
            if (cell.getListHeader().getRowIndex() == r) out.add(cell);
        }
        return out;
    }

    /**
     * 행 r의 지정 높이 — 행에 속한 (rowSpan=1) 셀들의 ListHeader 높이 최댓값.
     * old_version의 {@code lh.getHeight()}에 해당한다. 자동 높이 셀(센티넬 이하)은
     * 지정 높이가 부정확하므로 0으로 본다(셀 내용 높이가 대신 쓰인다).
     */
    private static long rowSpecHeight(ControlTable ctrl, int r) {
        long h = 0;
        for (Cell cell : cellsOfRow(ctrl, r)) {
            if (cell.getListHeader().getRowSpan() != 1) continue;
            long stored = cell.getListHeader().getHeight();
            if (stored > AUTO_HEIGHT_THRESHOLD) {
                h = Math.max(h, stored);
            }
        }
        return h;
    }

    /**
     * 행 r 의 지정 높이가 <b>아직 행 박스 바닥을 정하는가</b>.
     *
     * <p>{@code rowSpecHeight(r) > 0} 만으로 "고정 높이 행"이라고 보면 안 된다.
     * {@code divideRowHeights} 는 행 높이를 {@code max(내용, 지정)} 으로 잡으므로,
     * 내용이 지정 높이를 넘긴 행은 <b>내용이 높이를 정한 auto 행</b>이 되어 있다.
     * 그런 행에서 지정 높이는 이미 소진돼 바닥을 정하지 못하는데도 고정 행으로 취급하면
     * 축소 클램프가 통째로 막힌다.</p>
     *
     * <p>증상: 실문서 R08 〈별첨 1〉 표(1행, 지정 72705, 내용 99562)가
     * 두 페이지로 쪼개졌을 때 <b>마지막 조각의 상자 바닥이 내용 끝 + mB 가 아니라 행 끝까지</b>
     * 내려가 한글보다 16px 아래에 그려졌다(4p: 35932 vs 내용끝+mB 34953 = 한글 582px).</p>
     *
     * <p>반대로 실문서 R02 표4 r0(1줄 내용 + 빈 공간 48px)은 {@code rowH == spec} 이라
     * 여전히 고정 행으로 판정되어 지정 높이 박스를 유지한다.</p>
     */
    private static boolean isFixedHeightRow(ControlTable ctrl, int r, long[] rowH) {
        long spec = rowSpecHeight(ctrl, r);
        return spec > 0 && r >= 0 && r < rowH.length && rowH[r] <= spec;
    }

    /** cellDocument의 {@code pageIdx} 페이지 콘텐츠 높이 (페이지 줄은 y=0 기준). 페이지가 없으면 0. */
    private static long cellPageContentHeight(Document cellDoc, int pageIdx) {
        if (pageIdx < 0 || pageIdx >= cellDoc.pages().size()) return 0;
        long maxBottom = 0;
        boolean any = false;
        for (TextRow row : cellDoc.pages().get(pageIdx).content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    any = true;
                    maxBottom = Math.max(maxBottom, line.area().bottom() - line.lineGap());
                }
            }
        }
        return any ? maxBottom : 0;
    }

    /**
     * cellDocument의 {@code pageIdx} 페이지만 담은 단일 페이지 Document를 만든다.
     * 그 페이지의 줄은 y=0 기준이므로 {@code newTop}만큼 아래로 옮긴다.
     * 페이지가 없으면 빈 Document를 반환한다(셀 테두리만 그려진다).
     */
    private static Document singleCellPage(Document cellDoc, int pageIdx, long newTop) {
        Document out = new Document();
        Page dst = out.addPage();
        if (pageIdx < 0 || pageIdx >= cellDoc.pages().size()) return out;
        for (TextRow row : cellDoc.pages().get(pageIdx).content().rows()) {
            dst.content().rows().add(row);
        }
        for (TextRow row : dst.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    Area a = line.area();
                    long h = a.bottom() - a.top();
                    a.top(a.top() + newTop);
                    a.bottom(a.top() + h);
                }
            }
        }
        return out;
    }

    private static boolean hasVisibleText(Paragraph para) {
        ParaText text = para.getText();
        if (text == null) return false;
        for (HWPChar ch : text.getCharList()) {
            if (ch.getType() == HWPCharType.Normal && ch.getCode() > 32) return true;
        }
        return false;
    }

    /**
     * 어울림/자리차지 컨트롤을 다음 페이지로 미룰지 판단한다.
     *
     * <p>NoLineSeg 모드에서는 anchor 이전 문단들이 obstacle을 알지 못해 컨트롤 영역과 겹칠 수 있다.
     * 이 겹침은 "잘못된 양성(false positive)"이므로 NoLineSeg 경로에서 컨트롤이 현재 페이지 body
     * 내에 있으면 defer하지 않는다. LineSeg 모드에서는 HWP 원본 데이터가 기준이므로 기존 로직을 유지.</p>
     *
     * @param isNoLineSeg true이면 LineSeg 없이 synthesize된 문단 (NoLineSeg 경로)
     */
    private static boolean shouldDefer(CtrlHeaderGso header, Area areaWithMargin,
                                        Page anchorPage, TextLine anchorLine, Area bodyArea,
                                        boolean isNoLineSeg) {
        TextFlowMethod tfm = header.getProperty().getTextFlowMethod();
        if (tfm != TextFlowMethod.FitWithText && tfm != TextFlowMethod.TakePlace) return false;
        if (anchorLine == null) return false;
        // NoLineSeg + FitWithText 표는 defer 하지 않는다. FitWithText 는 본문이 표 주위로
        // 흘러야 하므로 표를 다른 페이지로 옮기면 본문 흐름과 어긋난다. NoLineSeg 합성기는
        // 후속 단락을 표가 있던 페이지에 자연스럽게 배치하므로 defer 가 오히려 부작용.
        if (isNoLineSeg && tfm == TextFlowMethod.FitWithText) return false;
        long threshold = bodyArea.top() + bodyArea.height() * 75 / 100;
        if (anchorLine.area().bottom() < threshold) return false;
        // NoLineSeg 모드에서도 anchor 라인 자체가 컨트롤 영역과 겹치는 것은 표/컨트롤이
        // anchor 위치에 배치되었다는 정상 신호 — defer 판단에 그대로 사용한다.
        // 75%-규칙이 이미 anchor 가 페이지 하단 25% 안에 있어야 한다는 강한 조건이므로
        // 전체 페이지에 적용해도 안전하다.
        return overlapsAnyTextLine(anchorPage, areaWithMargin);
    }

    private static boolean overlapsAnyTextLine(Page page, Area area) {
        for (TextRow row : page.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    if (intersects(area, line.area())) return true;
                }
            }
        }
        return false;
    }

    private static boolean intersects(Area a, Area b) {
        return a.left() < b.right() && a.right() > b.left()
            && a.top()  < b.bottom() && a.bottom() > b.top();
    }

    private static CtrlHeaderGso gsoHeaderOf(Control ctrl) {
        switch (ctrl.getType()) {
            case Table: return ((ControlTable) ctrl).getHeader();
            case Gso:   return ((GsoControl) ctrl).getHeader();
            default:    return null;
        }
    }

    private static Area withMargin(Area area, CtrlHeaderGso h) {
        return new Area(
                area.left()   - h.getOutterMarginLeft(),
                area.top()    - h.getOutterMarginTop(),
                area.right()  + h.getOutterMarginRight(),
                area.bottom() + h.getOutterMarginBottom());
    }

    private ControlItem createItem(Control ctrl, Area area, Area areaWithMargin,
                                    boolean behindText, ParaLayoutContext ctx) {
        ControlItem item;
        switch (ctrl.getType()) {
            case Gso:
                item = createGsoItem((GsoControl) ctrl, area, areaWithMargin, behindText, ctx);
                break;
            case Table:
                item = createTableItem((ControlTable) ctrl, area, areaWithMargin, behindText, ctx);
                break;
            default:
                return null;
        }
        if (item != null) {
            CtrlHeaderGso header = gsoHeaderOf(ctrl);
            if (header != null) item.zOrder(header.getzOrder());
        }
        return item;
    }

    private ControlItem createGsoItem(GsoControl gso, Area area, Area areaWithMargin,
                                       boolean behindText, ParaLayoutContext ctx) {
        switch (gso.getGsoType()) {
            case Container: {
                return createContainerItem((ControlContainer) gso, area, areaWithMargin, behindText, ctx);
            }
            case Picture: {
                return new GsoPictureItem((ControlPicture) gso, area, areaWithMargin, behindText);
            }
            case Polygon: {
                return createPolygonItem((kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPolygon) gso,
                        area, behindText);
            }
            case Rectangle: {
                ControlRectangle rect = (ControlRectangle) gso;
                if (rect.getTextBox() != null) {
                    ListHeaderForTextBox lh = rect.getTextBox().getListHeader();
                    long topM = lh.getTopMargin(), botM = lh.getBottomMargin();
                    Area probeArea = new Area(area.left(), area.top(), area.right(),
                            area.top() + 100_000_000L);
                    long innerTop = area.top() + topM;
                    Document innerDoc;
                    if (hasMultiColumn(rect.getTextBox().getParagraphList())) {
                        // 컨트롤 안 일반 다단:
                        //  다단 최소높이 측정(최소화 레이아웃) 후 컨트롤 컨텐츠 영역과 비교.
                        //   - 다단최소높이 > 컨트롤영역 → 최소화하고 컨트롤 높이를 그만큼 키운다.
                        //   - 다단최소높이 ≤ 컨트롤영역 → 컨트롤을 페이지처럼 두고 신문식 배치.
                        long ctrlContentH = rect.getShapeComponent().getHeightAtCurrent()
                                - topM - botM;
                        Document probeDoc = layoutTextBox(rect, probeArea, ctx, true);
                        long minContentH = contentHeight(probeDoc, innerTop);
                        boolean minimize = minContentH > ctrlContentH;
                        long contentH = minimize ? minContentH : ctrlContentH;
                        long boxH = contentH + topM + botM;
                        area = new Area(area.left(), area.top(), area.right(), area.top() + boxH);
                        areaWithMargin = withMargin(area, gso.getHeader());
                        innerDoc = layoutTextBox(rect, area, ctx, minimize);
                    } else {
                        // 단일 단 글상자: 콘텐츠 측정과 함께, ShapeComponent의 'AtCurrent' 크기가
                        // CtrlHeaderGso 크기보다 크면 그것을 박스 최소 높이로 적용한다 — 자동
                        // 크기 조정으로 박스가 늘어난 경우(예: 빈셀 case의 글상자).
                        Document probeDoc = layoutTextBox(rect, probeArea, ctx, true);
                        long contentH = contentHeight(probeDoc, innerTop);
                        long shapeH = rect.getShapeComponent().getHeightAtCurrent();
                        long minHeight = Math.max(contentH + topM + botM, shapeH);
                        if (minHeight > area.height()) {
                            area = new Area(area.left(), area.top(), area.right(),
                                    area.top() + minHeight);
                            areaWithMargin = withMargin(area, gso.getHeader());
                        }
                        innerDoc = layoutTextBox(rect, area, ctx, true);
                    }
                    GsoTextBoxItem item = new GsoTextBoxItem(rect, area, areaWithMargin, behindText);
                    item.innerDocument(innerDoc);
                    item.verticalAlignmentOffset(computeVerticalAlignmentOffset(rect, area, innerDoc));
                    return item;
                }
                return new GsoTextBoxItem(rect, area, areaWithMargin, behindText);
            }
            default:
                return new GsoShapeItem(gso, area, areaWithMargin, behindText);
        }
    }

    /**
     * 묶음 개체(그룹)를 페이지 좌표로 배치한다.
     *
     * <p>2패스로 배치한다.
     * <ol>
     *   <li>각 자식의 로컬 사각형 [0,0]~[w,h]를 자기 변환({@link #childRenderMatrix})으로
     *       그룹 공간 사각형으로 옮기고, 전체 그룹 바운딩 박스를 구한다. 이 변환은
     *       좌우/상하 대칭(flip)·회전을 포함한다.</li>
     *   <li>그룹 bbox 를 컨테이너 area 에 선형 매핑한다. 컨테이너 area 는 이미
     *       widthAtCurrent(리사이즈 반영) 기준이므로, 그룹의 페이지 배치·리사이즈가
     *       이 매핑으로 처리된다.</li>
     * </ol>
     * 텍스트는 자연 크기로 그려지고 도형의 기하만 이 변환을 따른다(실문서 R03 표지 제목 배너:
     * 그림자가 우하단으로 뒤집혀 배치되고 본체 박스 높이가 area 에 꽉 차는 것으로 확인).</p>
     */
    private GsoContainerItem createContainerItem(ControlContainer container, Area area,
            Area areaWithMargin, boolean behindText, ParaLayoutContext ctx) {
        GsoContainerItem item = new GsoContainerItem(area, areaWithMargin, behindText);
        List<GsoControl> children = container.getChildControlList();
        if (children.isEmpty()) return item;

        // 1차: 자식별 그룹 공간 사각형 + 그룹 bbox.
        double[][] rects = new double[children.size()][];
        double gMinX = Double.MAX_VALUE, gMinY = Double.MAX_VALUE;
        double gMaxX = -Double.MAX_VALUE, gMaxY = -Double.MAX_VALUE;
        for (int i = 0; i < children.size(); i++) {
            ShapeComponent sc = children.get(i).getShapeComponent();
            double[] m = childRenderMatrix(sc);
            long w = sc.getWidthAtCurrent(), h = sc.getHeightAtCurrent();
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (long[] p : new long[][]{{0, 0}, {w, 0}, {0, h}, {w, h}}) {
                double x = m[0] * p[0] + m[1] * p[1] + m[2];
                double y = m[3] * p[0] + m[4] * p[1] + m[5];
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
            rects[i] = new double[]{minX, minY, maxX, maxY};
            gMinX = Math.min(gMinX, minX); gMinY = Math.min(gMinY, minY);
            gMaxX = Math.max(gMaxX, maxX); gMaxY = Math.max(gMaxY, maxY);
        }
        double gW = gMaxX - gMinX, gH = gMaxY - gMinY;
        if (gW <= 0 || gH <= 0) return item;

        // 2차: 그룹 bbox → 컨테이너 area 선형 매핑.
        double sx = (double) area.width() / gW;
        double sy = (double) area.height() / gH;
        for (int i = 0; i < children.size(); i++) {
            double[] r = rects[i];
            long cl = area.left() + Math.round((r[0] - gMinX) * sx);
            long ct = area.top()  + Math.round((r[1] - gMinY) * sy);
            long cr = area.left() + Math.round((r[2] - gMinX) * sx);
            long cb = area.top()  + Math.round((r[3] - gMinY) * sy);
            ControlItem childItem = createContainerChild(
                    children.get(i), new Area(cl, ct, cr, cb), behindText, ctx);
            if (childItem != null) item.addChild(childItem);
        }
        return item;
    }

    /**
     * 그룹 자식의 로컬 좌표를 그룹 공간 좌표로 옮기는 2×3 아핀 변환을 구한다.
     *
     * <p>{@code M = translation · (마지막 scale·rotate 쌍)}. scaleRotate 쌍 리스트의
     * 마지막(가장 안쪽) 쌍이 자식 자신의 변환이고, 앞쪽 쌍들은 조상 그룹의 리사이즈다.
     * 조상 리사이즈는 컨테이너 area(widthAtCurrent 기반)에 이미 반영되어 있으므로 자식에
     * 다시 적용하면 이중 축소된다 — 마지막 쌍만 쓴다.</p>
     *
     * <p>각 행렬 배열은 [a,b,c,d,e,f] = (x' = a·x + b·y + c, y' = d·x + e·y + f).</p>
     */
    private static double[] childRenderMatrix(ShapeComponent sc) {
        kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.renderingnfo.RenderingInfo ri =
                sc.getRenderingInfo();
        double[] m = ri.getTranslationMatrix().getValues().clone();
        List<kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.renderingnfo.ScaleRotateMatrixPair> pairs =
                ri.getScaleRotateMatrixPairList();
        if (!pairs.isEmpty()) {
            kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.renderingnfo.ScaleRotateMatrixPair pair =
                    pairs.get(pairs.size() - 1);
            m = mul2x3(m, pair.getScaleMatrix().getValues());
            m = mul2x3(m, pair.getRotateMatrix().getValues());
        }
        return m;
    }

    /** 2×3 아핀 행렬 A∘B (B를 먼저 적용). 각 배열은 [a,b,c,d,e,f]. */
    private static double[] mul2x3(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] + a[1] * b[3],
                a[0] * b[1] + a[1] * b[4],
                a[0] * b[2] + a[1] * b[5] + a[2],
                a[3] * b[0] + a[4] * b[3],
                a[3] * b[1] + a[4] * b[4],
                a[3] * b[2] + a[4] * b[5] + a[5],
        };
    }

    /** 그룹 자식 하나를 페이지 좌표 childArea 기준의 ControlItem으로 만든다. */
    private ControlItem createContainerChild(GsoControl child, Area childArea,
            boolean behindText, ParaLayoutContext ctx) {
        if (child instanceof kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPolygon) {
            return createPolygonItem((kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPolygon) child,
                    childArea, behindText);
        }
        if (child instanceof ControlRectangle) {
            ControlRectangle rect = (ControlRectangle) child;
            if (rect.getTextBox() != null) {
                GsoTextBoxItem tb = new GsoTextBoxItem(rect, childArea, childArea, behindText);
                Document innerDoc = layoutTextBox(rect, childArea, ctx, true);
                tb.innerDocument(innerDoc);
                tb.verticalAlignmentOffset(computeVerticalAlignmentOffset(rect, childArea, innerDoc));
                return tb;
            }
            return new GsoShapeItem(child, childArea, childArea, behindText);
        }
        return new GsoShapeItem(child, childArea, childArea, behindText);
    }

    /**
     * 다각형(Polygon)을 area 기준의 {@link GsoPolygonItem}으로 만든다. 다각형 꼭짓점 좌표는
     * 도형 자체 좌표계(꼭짓점 바운딩 박스)이므로, 그 바운딩 박스를 area 에 비례 매핑해
     * 페이지 좌표 꼭짓점을 구한다. (실문서 R03 글머리표 마름모.)
     */
    private GsoPolygonItem createPolygonItem(
            kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPolygon poly,
            Area area, boolean behindText) {
        List<kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponenteach.polygon.PositionXY> pts =
                poly.getShapeComponentPolygon().getPositionList();
        if (pts.isEmpty()) return new GsoPolygonItem(poly, new long[0], new long[0], area, area, behindText);
        long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE, maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE;
        for (kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponenteach.polygon.PositionXY p : pts) {
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
        }
        long pw = Math.max(1, maxX - minX), ph = Math.max(1, maxY - minY);

        kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponent sc =
                poly.getShapeComponent();
        int rotate = sc.getRotateAngle();
        boolean flipH = sc.getProperty().isFlipHorizontal();
        boolean flipV = sc.getProperty().isFlipVertical();

        long[] xs = new long[pts.size()], ys = new long[pts.size()];
        if (rotate == 0 && !flipH && !flipV) {
            // 무변환: 점 bbox를 area에 비례 매핑 (기존 경로 그대로 — 픽셀 불변).
            for (int i = 0; i < pts.size(); i++) {
                xs[i] = area.left() + (pts.get(i).getX() - minX) * area.width() / pw;
                ys[i] = area.top()  + (pts.get(i).getY() - minY) * area.height() / ph;
            }
            return new GsoPolygonItem(poly, xs, ys, area, area, behindText);
        }

        // 회전/플립 폴리곤: bbox 정규화 → 플립 → 중심 기준 회전(y-down에서 +각도=시계방향,
        // 실문서 R03 도표 ▶+90°=▼ 로 확인) → 회전 후 bbox를 area(=회전된 외접 박스)에 매핑.
        double[] fx = new double[pts.size()], fy = new double[pts.size()];
        double rad = Math.toRadians(rotate);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double tMinX = Double.MAX_VALUE, tMinY = Double.MAX_VALUE;
        double tMaxX = -Double.MAX_VALUE, tMaxY = -Double.MAX_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            double nx = (pts.get(i).getX() - minX) / (double) pw;   // 0..1
            double ny = (pts.get(i).getY() - minY) / (double) ph;
            if (flipH) nx = 1.0 - nx;
            if (flipV) ny = 1.0 - ny;
            double dx = (nx - 0.5) * pw, dy = (ny - 0.5) * ph;      // 중심 기준
            fx[i] = dx * cos - dy * sin;
            fy[i] = dx * sin + dy * cos;
            tMinX = Math.min(tMinX, fx[i]); tMaxX = Math.max(tMaxX, fx[i]);
            tMinY = Math.min(tMinY, fy[i]); tMaxY = Math.max(tMaxY, fy[i]);
        }
        double tw = Math.max(1e-9, tMaxX - tMinX), th = Math.max(1e-9, tMaxY - tMinY);
        for (int i = 0; i < pts.size(); i++) {
            xs[i] = area.left() + Math.round((fx[i] - tMinX) * area.width() / tw);
            ys[i] = area.top()  + Math.round((fy[i] - tMinY) * area.height() / th);
        }
        return new GsoPolygonItem(poly, xs, ys, area, area, behindText);
    }

    private Document layoutTextBox(ControlRectangle rect, Area area, ParaLayoutContext ctx,
                                   boolean balanceLastBlock) {
        TextBox textBox = rect.getTextBox();
        ListHeaderForTextBox lh = textBox.getListHeader();
        Area innerArea = new Area(
                area.left()   + lh.getLeftMargin(),
                area.top()    + lh.getTopMargin(),
                area.right()  - lh.getRightMargin(),
                area.bottom() - lh.getBottomMargin());
        return innerLayouter.layout(textBox.getParagraphList(), innerArea, ctx.paperArea(),
                balanceLastBlock);
    }

    /** 문단 목록에 단 수 2 이상의 ColumnDefine(일반 다단)이 있는지 검사. */
    private static boolean hasMultiColumn(
            kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList pl) {
        for (int i = 0; i < pl.getParagraphCount(); i++) {
            Paragraph p = pl.getParagraph(i);
            if (p.getControlList() == null) continue;
            for (Control c : p.getControlList()) {
                if (c instanceof kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine) {
                    if (((kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine) c)
                            .getHeader().getProperty().getColumnCount() > 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private TableItem createTableItem(ControlTable ctrl, Area area, Area areaWithMargin,
                                       boolean behindText, ParaLayoutContext ctx) {
        TableItem item = new TableItem(ctrl, area, areaWithMargin, behindText);
        layoutCells(ctrl, item, area, ctx);
        return item;
    }

    // 저장된 높이가 이 값 이하이면 셀 내용의 실제 높이로 대체한다.
    private static final long AUTO_HEIGHT_THRESHOLD = 282;

    // 셀 컨트롤이 페이지 segment 경계에 걸칠 때, 인접 segment에 이 값 미만으로만
    // 겹치면 그 segment에는 그리지 않는다(작은 잘림 조각 방지). 컨트롤 높이의 1/4과
    // 이 값 중 큰 쪽을 임계값으로 쓴다.
    private static final long MIN_CROSS_OVERLAP = 1000;

    private void layoutCells(ControlTable ctrl, TableItem item, Area tableArea,
                               ParaLayoutContext ctx) {
        int colCount = ctrl.getTable().getColumnCount();
        int rowCount = ctrl.getTable().getRowCount();
        long[] colWidths = resolveColWidths(ctrl, colCount);
        long[] rowHeights = resolveRowHeights(ctrl, rowCount, colWidths, tableArea.left(), ctx);
        layoutCellRange(ctrl, item, tableArea.left(), tableArea.top(),
                0, rowCount, colWidths, rowHeights, ctx);
    }

    /**
     * 표의 행 [rowStart, rowEnd) 범위만 {@code groupTop}부터 레이아웃한다.
     * 표 전체 레이아웃은 [0, rowCount)로, 셀단위로나누기(DivideByCell)는 페이지별
     * 행 그룹으로 나눠 각 그룹마다 호출한다.
     */
    private void layoutCellRange(ControlTable ctrl, TableItem item, long tableLeft, long groupTop,
                                  int rowStart, int rowEnd, long[] colWidths, long[] rowHeights,
                                  ParaLayoutContext ctx) {
        long spacing = cellSpacingOf(ctrl);
        long[] colX = cumulative(colWidths, tableLeft, spacing);
        long[] rowY = new long[rowHeights.length];
        long y = groupTop + spacing;
        for (int r = rowStart; r < rowEnd && r < rowHeights.length; r++) {
            rowY[r] = y;
            y += rowHeights[r] + spacing;
        }

        for (Row row : ctrl.getRowList()) {
            for (Cell cell : row.getCellList()) {
                int ri = cell.getListHeader().getRowIndex();
                int ci = cell.getListHeader().getColIndex();
                int rs = cell.getListHeader().getRowSpan();
                int cs = cell.getListHeader().getColSpan();
                if (ri < rowStart || ri >= rowEnd) continue;
                long cx = (ci < colX.length) ? colX[ci] : tableLeft;
                long cy = (ri < rowY.length) ? rowY[ri] : groupTop;
                long cw = sumRange(colWidths, ci, cs, spacing);
                // rowSpan이 그룹 끝을 넘으면 그룹 안의 행까지만 합산한다.
                long ch = sumRange(rowHeights, ri, Math.min(rs, rowEnd - ri), spacing);

                int marginL = cell.getListHeader().getLeftMargin();
                int marginT = cell.getListHeader().getTopMargin();
                int marginR = cell.getListHeader().getRightMargin();
                int marginB = cell.getListHeader().getBottomMargin();

                Area cellArea = new Area(cx, cy, cx + cw, cy + ch);
                Area innerArea = new Area(
                        cx + marginL, cy + marginT,
                        cx + cw - marginR, cy + ch - marginB);

                // 셀 안 일반 다단: 다단최소높이 > 셀 컨텐츠 영역이면 최소화, 아니면
                // 셀을 페이지처럼 두고 신문식 배치 (글상자와 동일 규칙).
                boolean cellBalance = true;
                if (hasMultiColumn(cell.getParagraphList())) {
                    long cellContentH = cell.getListHeader().getHeight() - marginT - marginB;
                    Area probeArea = new Area(innerArea.left(), innerArea.top(),
                            innerArea.right(), innerArea.top() + 100_000_000L);
                    Document probe = innerLayouter.layout(cell.getParagraphList(),
                            probeArea, ctx.paperArea(), true);
                    long minH = contentHeight(probe, innerArea.top());
                    cellBalance = minH > cellContentH;
                }
                Document cellDoc = innerLayouter.layout(
                        cell.getParagraphList(), innerArea, ctx.paperArea(), cellBalance);
                TableItem.CellLayout cl = new TableItem.CellLayout(cellArea, cellDoc, cell);
                cl.verticalAlignmentOffset(computeCellVerticalOffset(cell, innerArea, cellDoc));
                item.cells().add(cl);
            }
        }
    }

    private static long sumRange(long[] arr, int start, int count) {
        return sumRange(arr, start, count, 0);
    }

    /**
     * 병합 셀의 크기 — 걸친 칸들의 크기 합에, 그 사이에 있던 셀 간격도 더한다.
     * span이 n이면 내부 경계는 n-1개다.
     */
    private static long sumRange(long[] arr, int start, int count, long spacing) {
        long s = 0;
        int n = 0;
        for (int i = start; i < start + count && i < arr.length; i++) { s += arr[i]; n++; }
        return s + (n > 1 ? (long) (n - 1) * spacing : 0);
    }

    private static long[] cumulative(long[] sizes, long origin) {
        return cumulative(sizes, origin, 0);
    }

    /**
     * 각 칸의 시작 좌표 — 셀 간격(cellSpacing)이 있으면 <b>모든 경계</b>에 들어간다.
     *
     * <p>즉 칸이 n개면 간격은 n+1개(바깥 양끝 + 사이 n-1개)이고, 표 전체 크기는
     * {@code 칸 크기 합 + (n+1) × spacing} 이 된다. 실문서 R03 의 항목부호 체계 표
     * (cellSpacing=283, 2열)에서 검증했다 — 파일의 셀 폭 합 646.9px, 표 컨트롤 폭 661.0px,
     * 차이 14.1px = 3 × 4.72px = (2+1) × spacing. 참조 출력의 파란 셀 좌단·회색 셀
     * 좌단·우단·표 우단 네 지점이 모두 이 식과 1px 이내로 맞는다.</p>
     *
     * <p>셀 자체의 크기는 파일에 저장된 값 그대로다(간격 때문에 줄지 않는다). 따라서
     * 셀 안의 줄바꿈은 영향받지 않고 위치만 밀린다.</p>
     */
    private static long[] cumulative(long[] sizes, long origin, long spacing) {
        long[] r = new long[sizes.length];
        long v = origin + spacing;
        for (int i = 0; i < sizes.length; i++) {
            r[i] = v;
            v += sizes[i] + spacing;
        }
        return r;
    }

    /** 표의 셀 간격(HWP 단위). 대부분의 표는 0이라 위 계산이 기존과 동일해진다. */
    private static long cellSpacingOf(ControlTable ctrl) {
        return ctrl.getTable().getCellSpacing();
    }

    /**
     * 열 너비 결정 — span 증가 순으로 처리.
     * span=1 셀: 해당 열 너비 확정.
     * span>1 셀: span 범위 내에 미확정 열이 정확히 1개면 그 열로 (셀 너비 - 확정된 열들 합)을 할당.
     */
    private static long[] resolveColWidths(ControlTable ctrl, int colCount) {
        long[] widths = new long[colCount];
        java.util.Arrays.fill(widths, -1L);

        List<Cell> cells = collectCells(ctrl);
        cells.sort((a, b) -> Integer.compare(a.getListHeader().getColSpan(), b.getListHeader().getColSpan()));

        // 병합 셀(colSpan)은 자기 span 안에 미확정 컬럼이 1개일 때만 폭을 확정할 수 있다.
        // 모든 행이 병합 셀인 표(실문서 R02 표8: 단일 컬럼 셀이 c0·c7뿐)는 한 번의 패스로는
        // 일부만 풀리므로, 더 이상 풀리는 컬럼이 없을 때까지 반복 전파한다.
        //
        // 폭이 음수가 되는 제약은 곧바로 쓰지 않고 미룬다(allowNegative). 행마다 열 경계가
        // 어긋난 파일에서는 서로 모순되는 제약이 공존하는데, 먼저 만난 모순 제약이 열을
        // 음수로 선점하면 올바른 제약이 끝내 반영되지 못한다. 실문서 R24
        // page1 표(4열)가 그 예다 — row3의 병합 셀 폭 7758이 col3(10028)보다 작아
        // col2=-2270으로 선점되고, row0이 요구하는 col2=312(전체폭 52158 보존)가 무시되어
        // 표가 312 HWPUNIT(5.2px) 좁아졌다(한글은 312를 채택해 전체폭을 보존한다).
        // 음수 제약을 미뤄 교착됐을 때만 마지막 수단으로 허용해, 그 제약밖에 없는 파일의
        // 기존 동작(음수→0 클램프)은 유지한다.
        boolean allowNegative = false;
        while (true) {
            boolean changed = false;
            for (Cell cell : cells) {
                int ci = cell.getListHeader().getColIndex();
                int cs = cell.getListHeader().getColSpan();
                long cw = cell.getListHeader().getWidth();
                if (cs == 1) {
                    if (ci < colCount && widths[ci] == -1) {
                        widths[ci] = cw;
                        changed = true;
                    }
                } else {
                    int emptyIdx = singleUnset(widths, ci, cs);
                    if (emptyIdx >= 0) {
                        long w = cw - sumSet(widths, ci, cs);
                        if (w < 0 && !allowNegative) continue;   // 모순 제약 — 다른 셀이 풀도록 미룸
                        widths[emptyIdx] = w;
                        changed = true;
                    }
                }
            }
            if (changed) {
                allowNegative = false;   // 풀린 게 있으면 다시 비음수 제약 우선으로
                continue;
            }
            if (!allowNegative && hasUnset(widths)) {
                allowNegative = true;    // 교착 — 음수 제약이라도 허용해 한 번 더
                continue;
            }
            if (hasUnset(widths) && distributeAmongUnset(widths, cells, colCount)) {
                allowNegative = false;   // 합 제약으로 갈라 놓았으니 다시 정상 전파
                continue;
            }
            break;
        }
        for (int i = 0; i < widths.length; i++) if (widths[i] < 0) widths[i] = 0;
        return widths;
    }

    /**
     * 미확정 열이 <b>2개 이상</b> 남은 병합 셀의 폭을 그 열들에 균등 분배한다. 하나라도
     * 갈라 놓으면 true.
     *
     * <p>위 전파는 병합 셀 안에 미확정 열이 <b>정확히 1개</b>일 때만 폭을 확정한다. 그런데
     * 어떤 열들은 <b>항상 함께</b> 병합되어 끝내 개별 제약을 못 받는다. 그런 열은 -1 로 남아
     * 0 으로 클램프되고, 그만큼 표 전체가 좁아지면서 열 하나가 통째로 사라진다.</p>
     *
     * <p>실측 — 실문서 R20 표3(논리 8열): col3·col4 는 row0 의 colSpan=2 셀과
     * row6 의 colSpan=5 셀에만 등장해 둘이 갈린 적이 없다. 그 결과 <b>회사명 열(7890 =
     * 131.5px)이 사라져</b> 표 오른쪽 끝이 896 → 764 로 줄고 이후 열이 전부 왼쪽으로 밀렸다
     * (유사도 97.2%). 합 제약(col3+col4=7890)은 파일에 분명히 있으므로 그걸 쓰면 된다.</p>
     *
     * <p><b>어떻게 나누든 렌더 결과는 같다</b> — 갈린 적이 없다는 것은 그 사이에 그려질
     * 셀 경계가 없다는 뜻이라, 합만 맞으면 된다. 그래서 균등 분배하고 나머지는 마지막 열에
     * 몰아 합을 정확히 보존한다.</p>
     *
     * <p>{@code cells} 는 colSpan 오름차순이라 <b>가장 좁은(=가장 구체적인) 제약</b>부터
     * 쓴다. 폭이 모자란(음수) 제약은 건너뛴다 — 위 전파의 {@code allowNegative} 와 같은 이유다.</p>
     */
    private static boolean distributeAmongUnset(long[] widths, List<Cell> cells, int colCount) {
        for (Cell cell : cells) {
            int ci = cell.getListHeader().getColIndex();
            int cs = cell.getListHeader().getColSpan();
            if (cs < 2 || ci + cs > colCount) continue;

            int unset = 0;
            for (int i = ci; i < ci + cs; i++) if (widths[i] == -1) unset++;
            if (unset < 2) continue;

            long remain = cell.getListHeader().getWidth() - sumSet(widths, ci, cs);
            if (remain < 0) continue;

            long each = remain / unset;
            int seen = 0;
            for (int i = ci; i < ci + cs; i++) {
                if (widths[i] != -1) continue;
                seen++;
                widths[i] = (seen == unset) ? remain - each * (unset - 1) : each;
            }
            return true;
        }
        return false;
    }

    private static boolean hasUnset(long[] widths) {
        for (long w : widths) if (w == -1) return true;
        return false;
    }

    /**
     * 행 높이 결정 — old_version의 RowPosition.calculate()를 따름.
     *
     * <p>각 셀마다 (effectiveH=max(내용+여백, 저장높이), storedH)를 RowInfo로 수집한 뒤
     * span 증가 순으로 {@link #applyRowInfo}를 적용한다. 추가로 '모두 미확정' 폴백을
     * 후처리한다. 표 전체 높이에 미달하면 차액을 균등 분배.</p>
     */
    private long[] resolveRowHeights(ControlTable ctrl, int rowCount, long[] colWidths,
                                       long tableLeft, ParaLayoutContext ctx) {
        long[] colX = cumulative(colWidths, tableLeft, cellSpacingOf(ctrl));

        // 1. (ri, rs)별 RowInfo 수집 — 동일 키는 height 최댓값으로 병합.
        // '크기 보호(protectSize)' 표는 셀 크기가 고정이라 내용이 넘쳐도 한글이 셀을 늘리지
        // 않고 넘친 줄을 잘라낸다(실문서 R19 머리 표: 날짜 칸 끝의 "14"). 이 경우
        // storedH(한글이 저장한 실제 높이)를 그대로 쓰고 넘침은 렌더 클리핑에 맡긴다.
        // 보호 안 된 표(기본)는 기존대로 내용에 맞춰 늘린다.
        CtrlHeaderGso th = gsoHeaderOf(ctrl);
        boolean protectSize = th != null && th.getProperty().isProtectSize();

        List<RowInfo> infos = new ArrayList<>();
        for (Cell cell : collectCells(ctrl)) {
            int ri = cell.getListHeader().getRowIndex();
            int rs = cell.getListHeader().getRowSpan();
            long storedH = cell.getListHeader().getHeight();
            long content = probeCellContentHeight(cell, colX, colWidths, ctx);
            long effectiveH = protectSize ? storedH : Math.max(content, storedH);
            addRowInfo(infos, ri, rs, effectiveH, storedH);
        }

        // 2. span 증가 순 처리.
        long[] calc = new long[rowCount];
        long[] cellH = new long[rowCount];
        java.util.Arrays.fill(calc, -1L);
        java.util.Arrays.fill(cellH, -1L);

        int maxSpan = 1;
        for (RowInfo i : infos) if (i.span > maxSpan) maxSpan = i.span;
        for (int span = 1; span <= maxSpan; span++) {
            for (RowInfo info : infos) {
                if (info.span == span) applyRowInfo(info, calc, cellH, rowCount);
            }
        }

        // 3. 모든 행이 여전히 -1인 span 폴백 (old_version RowPosition.calculate 후단).
        for (RowInfo info : infos) {
            if (isSpanAllUnset(info, calc, rowCount)) {
                for (int i = info.index; i < info.index + info.span && i < rowCount; i++) {
                    calc[i] = (i == info.index) ? (info.height - info.span + 1) : 1;
                }
            }
        }
        for (int i = 0; i < rowCount; i++) if (calc[i] < 0) calc[i] = 0;

        // 4. 표 전체 높이에 맞춰 보정.
        //
        // 기본: 모든 행에 균등 분배 (기존 동작).
        // 특수: 표 높이가 행 내용 합보다 훨씬 크고(잉여 > 내용합), auto-height 행과
        // 고정-height 행이 섞여 있으면 잉여를 auto 행에만 분배. 이는 한컴이 매우 큰
        // 표(예: 나누지않음/1 셋째 표)에서 auto 행 하나가 page 전체를 차지하도록 하는
        // 동작과 일치. 일반 크기 표는 기존 균등 분배가 OK 와 더 잘 맞아 그대로 둔다.
        CtrlHeaderGso tableHeader = gsoHeaderOf(ctrl);
        if (tableHeader != null && rowCount > 0) {
            long totalH = ControlAreaCalculator.resolveHeight(tableHeader, ctx.paperArea(), ctx.bodyArea());
            // 셀 간격이 있으면 그 몫(경계 rowCount+1개)은 행이 아니라 간격이 쓴다.
            // 빼지 않으면 간격만큼이 행 높이로 균등 분배돼 행이 통째로 두꺼워진다.
            totalH -= (long) (rowCount + 1) * cellSpacingOf(ctrl);
            long sumH = 0;
            for (long h : calc) sumH += h;
            if (sumH < totalH) {
                long extra = totalH - sumH;
                long[] rowStoredH = new long[rowCount];
                java.util.Arrays.fill(rowStoredH, -1);
                for (RowInfo info : infos) {
                    if (info.span == 1 && info.index >= 0 && info.index < rowCount) {
                        if (rowStoredH[info.index] < 0
                                || info.originalHeight > rowStoredH[info.index]) {
                            rowStoredH[info.index] = info.originalHeight;
                        }
                    }
                }
                int autoCount = 0, fixedCount = 0;
                for (int i = 0; i < rowCount; i++) {
                    if (rowStoredH[i] < 0) continue;
                    if (rowStoredH[i] <= AUTO_HEIGHT_THRESHOLD) autoCount++;
                    else fixedCount++;
                }
                // 분배 우선순위:
                // 1) auto/fixed 혼합 + 잉여가 내용합보다 크면: auto 행에만 분배
                //    (큰 표에서 auto 행이 잉여를 흡수)
                // 2) 모두 auto + 한 행이 dominant(다른 행 대비 5배 이상)면: 그 행에 잉여 전체
                //    (NoLineSeg 합성기 잔차로 sumH<totalH 인 경우 dominant 행이 흡수해 LineSeg 일치)
                // 3) 그 외: 균등 분배 (기존 동작 유지)
                boolean useAutoMixed = autoCount > 0 && fixedCount > 0 && extra > sumH;
                int dominantAuto = -1;
                if (autoCount == rowCount && rowCount >= 2) {
                    long maxH = 0, minH = Long.MAX_VALUE;
                    int maxIdx = -1;
                    for (int i = 0; i < rowCount; i++) {
                        if (calc[i] > maxH) { maxH = calc[i]; maxIdx = i; }
                        if (calc[i] < minH) minH = calc[i];
                    }
                    if (minH > 0 && maxH >= minH * 5) dominantAuto = maxIdx;
                }
                if (useAutoMixed) {
                    long per = extra / autoCount;
                    for (int i = 0; i < rowCount; i++) {
                        if (rowStoredH[i] >= 0 && rowStoredH[i] <= AUTO_HEIGHT_THRESHOLD) {
                            calc[i] += per;
                        }
                    }
                } else if (dominantAuto >= 0) {
                    calc[dominantAuto] += extra;
                } else {
                    long per = extra / rowCount;
                    for (int i = 0; i < rowCount; i++) calc[i] += per;
                }
            }
        }
        return calc;
    }

    /** old_version RowPosition.addInfo: 동일 (index, span) 키는 height 최댓값 병합. */
    private static void addRowInfo(List<RowInfo> infos, int index, int span, long height, long originalHeight) {
        for (RowInfo info : infos) {
            if (info.index == index && info.span == span) {
                info.height = Math.max(info.height, height);
                info.originalHeight = originalHeight;
                return;
            }
        }
        infos.add(new RowInfo(index, span, height, originalHeight));
    }

    /** old_version RowPosition.set: span=1은 직접 설정, 다중 span은 미확정 슬롯 처리 + 폴백. */
    private static void applyRowInfo(RowInfo info, long[] calc, long[] cellH, int rowCount) {
        if (info.span == 1) {
            setRowHeight(info.index, info.height, info.originalHeight, calc, cellH, rowCount);
            return;
        }
        int empty = singleUnsetInRange(calc, info.index, info.span, rowCount);
        if (empty >= 0) {
            // span 내 미확정 슬롯이 정확히 하나 — 그 슬롯에 차액 할당.
            setRowHeight(empty, info.height - sumRangeClamped(calc, info.index, info.span, rowCount), -1,
                    calc, cellH, rowCount);
        } else if (empty == -1) {
            // 모두 확정됨. cellHeight==0인 셀이 정확히 하나면 거기로 차액 흡수.
            int zeroIdx = zeroCellHeightIndex(cellH, info.index, info.span, rowCount);
            if (zeroIdx >= 0) {
                setRowHeight(zeroIdx,
                        info.height - sumRangeExcludingZero(calc, cellH, info.index, info.span, rowCount),
                        -1, calc, cellH, rowCount);
            }
            // 그래도 모자라면 마지막 행을 확장.
            long currentSum = sumRangeClamped(calc, info.index, info.span, rowCount);
            if (info.height > currentSum) {
                int last = info.index + info.span - 1;
                if (last < rowCount) {
                    setRowHeight(last,
                            info.height - sumRangeClamped(calc, info.index, info.span - 1, rowCount),
                            -1, calc, cellH, rowCount);
                }
            }
        }
    }

    private static void setRowHeight(int idx, long h, long originalH,
                                      long[] calc, long[] cellH, int rowCount) {
        if (idx < 0 || idx >= rowCount) return;
        if (h > calc[idx]) {
            calc[idx] = h;
            cellH[idx] = originalH;
        }
    }

    /** span 내 -1 슬롯이 정확히 하나면 그 인덱스, 0이면 -1(모두 확정), 2 이상이면 -2. */
    private static int singleUnsetInRange(long[] arr, int start, int count, int rowCount) {
        int unset = 0, idx = -1;
        for (int i = start; i < start + count && i < rowCount; i++) {
            if (arr[i] == -1) { unset++; idx = i; }
        }
        if (unset == 0) return -1;
        if (unset > 1) return -2;
        return idx;
    }

    private static long sumRangeClamped(long[] arr, int start, int count, int rowCount) {
        long s = 0;
        for (int i = start; i < start + count && i < rowCount; i++) {
            if (arr[i] != -1) s += arr[i];
        }
        return s;
    }

    private static int zeroCellHeightIndex(long[] cellH, int start, int count, int rowCount) {
        int c = 0, idx = -1;
        for (int i = start; i < start + count && i < rowCount; i++) {
            if (cellH[i] == 0) { c++; idx = i; }
        }
        return (c == 1) ? idx : -1;
    }

    private static long sumRangeExcludingZero(long[] calc, long[] cellH, int start, int count, int rowCount) {
        long s = 0;
        for (int i = start; i < start + count && i < rowCount; i++) {
            if (calc[i] != -1 && cellH[i] != 0) s += calc[i];
        }
        return s;
    }

    private static boolean isSpanAllUnset(RowInfo info, long[] calc, int rowCount) {
        for (int i = info.index; i < info.index + info.span && i < rowCount; i++) {
            if (calc[i] != -1) return false;
        }
        return true;
    }

    private static class RowInfo {
        final int index;
        final int span;
        long height;
        long originalHeight;
        RowInfo(int index, int span, long height, long originalHeight) {
            this.index = index; this.span = span;
            this.height = height; this.originalHeight = originalHeight;
        }
    }

    /** 각 행의 셀 콘텐츠 높이(셀별 최댓값)를 반환한다. rowSpan>1 병합 셀은 제외. */
    private long[] rowContentHeights(ControlTable ctrl, int rowCount, long[] colWidths,
                                      long tableLeft, ParaLayoutContext ctx) {
        long[] colX = cumulative(colWidths, tableLeft, cellSpacingOf(ctrl));
        long[] ch = new long[rowCount];
        for (Cell cell : collectCells(ctrl)) {
            int ri = cell.getListHeader().getRowIndex();
            if (cell.getListHeader().getRowSpan() != 1) continue;
            if (ri < 0 || ri >= rowCount) continue;
            long content = probeCellContentHeight(cell, colX, colWidths, ctx);
            if (content > ch[ri]) ch[ri] = content;
        }
        return ch;
    }

    private long probeCellContentHeight(Cell cell, long[] colX, long[] colWidths, ParaLayoutContext ctx) {
        int ci = cell.getListHeader().getColIndex();
        int cs = cell.getListHeader().getColSpan();
        long cx = (ci < colX.length) ? colX[ci] : 0;
        long cw = sumRange(colWidths, ci, cs);
        int marginL = cell.getListHeader().getLeftMargin();
        int marginT = cell.getListHeader().getTopMargin();
        int marginR = cell.getListHeader().getRightMargin();
        int marginB = cell.getListHeader().getBottomMargin();

        Area probeInner = new Area(cx + marginL, 0, cx + cw - marginR, Integer.MAX_VALUE / 2);
        Document probeDoc = innerLayouter.layout(cell.getParagraphList(), probeInner,
                ctx.paperArea(), true);
        // 내용 높이는 최소한 셀 첫 문자의 폰트 baseSize 이상이어야 한다.
        long contentH = Math.max(contentHeight(probeDoc, 0), cellFontBaseSize(cell, ctx));
        // (2026-08-14) 예전에는 여기서 emptyCellFirstLinePadding 으로 1줄을 보태 줬다 —
        // shouldSkipFirstEmptyPara 가 NoLineSeg 셀의 첫 빈 문단을 건너뛰어 1줄이 누락됐기
        // 때문이다. 이제 LayoutEngine.occupiesLineBeforeNextPara 가 NoLineSeg 파일에서도
        // "문단이 2개 이상이면 첫 빈 문단도 한 줄을 차지한다"고 판정하므로 누락 자체가 없다.
        // 보정을 남겨 두면 이중 계상이 된다(표나누기_nolineseg/셀단위로나누기/페이지_보다_큰_셀
        // p2·p4 와 나누지않음/1 p3 가 100.0% → 95.7~97.8% 로 무너졌다).
        return contentH + marginT + marginB;
    }

    /** 셀의 첫 문단 첫 글자 CharShape의 baseSize. 없으면 0. */
    private static long cellFontBaseSize(Cell cell, ParaLayoutContext ctx) {
        if (cell.getParagraphList() == null
                || cell.getParagraphList().getParagraphs() == null
                || cell.getParagraphList().getParagraphs().length == 0) {
            return 0;
        }
        Paragraph firstPara = cell.getParagraphList().getParagraphs()[0];
        int shapeId = 0;
        kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.ParaCharShape pcs = firstPara.getCharShape();
        if (pcs != null && pcs.getPositonShapeIdPairList() != null
                && !pcs.getPositonShapeIdPairList().isEmpty()) {
            shapeId = (int) pcs.getPositonShapeIdPairList().get(0).getShapeId();
        }
        kr.dogfoot.hwplib.object.docinfo.CharShape cs =
                ctx.reader().hwpFile().getDocInfo().getCharShapeList().get(shapeId);
        return (cs != null) ? cs.getBaseSize() : 0;
    }

    private static List<Cell> collectCells(ControlTable ctrl) {
        List<Cell> all = new ArrayList<>();
        for (Row row : ctrl.getRowList()) all.addAll(row.getCellList());
        return all;
    }

    /**
     * 셀 안쪽 여백(안 여백)을 정규화한다.
     *
     * <p>셀의 {@code applyInnerMargin} 플래그가 꺼져 있으면 셀 자체 여백값은 무의미하고
     * 표 레벨 기본 안 여백(Table.getXxxInnerMargin)을 써야 한다(한글 규칙). 이후의 모든
     * 셀 여백 사용처가 별도 분기 없이 올바른 값을 읽도록, 여기서 셀 여백 필드를 표 기본값으로
     * 덮어쓰고 플래그를 켜서 표시한다(idempotent — 반복 호출·다중 패스에 안전).</p>
     *
     * <p>실문서 R03 표지 취지 상자: 표 기본 여백 L/R=850·T/B=283, 셀 applyInner=false 로
     * 셀 값(141)을 쓰면 본문이 좌·상으로 밀린다.</p>
     */
    private static void normalizeCellMargins(ControlTable ctrl) {
        kr.dogfoot.hwplib.object.bodytext.control.table.Table table = ctrl.getTable();
        for (Cell cell : collectCells(ctrl)) {
            kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell lh = cell.getListHeader();
            if (lh.getProperty().isApplyInnerMagin()) continue;
            lh.setLeftMargin(table.getLeftInnerMargin());
            lh.setRightMargin(table.getRightInnerMargin());
            lh.setTopMargin(table.getTopInnerMargin());
            lh.setBottomMargin(table.getBottomInnerMargin());
            lh.getProperty().setApplyInnerMagin(true);
        }
    }

    /** span 범위 내 미확정(-1) 셀이 정확히 1개면 그 인덱스를, 아니면 -1 반환. */
    private static int singleUnset(long[] arr, int start, int count) {
        int idx = -1;
        int c = 0;
        for (int i = start; i < start + count && i < arr.length; i++) {
            if (arr[i] == -1) { c++; idx = i; }
        }
        return (c == 1) ? idx : -1;
    }

    private static long sumSet(long[] arr, int start, int count) {
        long s = 0;
        for (int i = start; i < start + count && i < arr.length; i++) {
            if (arr[i] != -1) s += arr[i];
        }
        return s;
    }

    private static long computeCellVerticalOffset(Cell cell, Area innerArea, Document cellDoc) {
        TextVerticalAlignment vAlign = cell.getListHeader().getProperty().getTextVerticalAlignment();
        if (vAlign == TextVerticalAlignment.Top) return 0;
        long innerHeight = innerArea.height();
        long contentH = contentHeight(cellDoc, innerArea.top());
        if (contentH >= innerHeight) return 0;
        switch (vAlign) {
            case Center: return (innerHeight - contentH) / 2;
            case Bottom: return innerHeight - contentH;
            default:     return 0;
        }
    }

    // ── 글상자 세로 정렬 ─────────────────────────────────────────────────

    private static long computeVerticalAlignmentOffset(ControlRectangle rect, Area area, Document innerDoc) {
        ListHeaderForTextBox lh = rect.getTextBox().getListHeader();
        TextVerticalAlignment vAlign = lh.getProperty().getTextVerticalAlignment();
        if (vAlign == TextVerticalAlignment.Top) return 0;

        long innerTop = area.top() + lh.getTopMargin();
        long innerHeight = area.height() - lh.getTopMargin() - lh.getBottomMargin();
        long contentHeight = contentHeight(innerDoc, innerTop);

        // 내용이 상자보다 크면(예: 도장 ㊞ 글자가 작은 상자보다 큼) 한글은 위로 밀어
        // 위·아래로 균등하게 넘치도록 가운데/아래 정렬한다(음수 오프셋). 위로 클램프하지 않는다.
        switch (vAlign) {
            case Center: return (innerHeight - contentHeight) / 2;
            case Bottom: return innerHeight - contentHeight;
            default:     return 0;
        }
    }

    private static long contentHeight(Document doc, long innerTop) {
        long maxBottom = innerTop;
        for (kr.dogfoot.hwpdrawer.document.page.Page page : doc.pages()) {
            for (TextRow row : page.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        // line.area().bottom()은 lineSpace(lineGap)를 포함.
                        // 콘텐츠의 실제 하단은 마지막 라인의 gap을 제외한 값.
                        long lineBottom = line.area().bottom() - line.lineGap();
                        maxBottom = Math.max(maxBottom, lineBottom);
                    }
                }
            }
            // 내부 컨트롤(vRelTo=Para)도 콘텐츠 높이에 포함한다.
            for (kr.dogfoot.hwpdrawer.document.control.ControlItem item : page.controls()) {
                maxBottom = Math.max(maxBottom, item.area().bottom());
            }
        }
        return maxBottom - innerTop;
    }

}
