package kr.dogfoot.hwpdrawer.engine;

import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlFooter;
import kr.dogfoot.hwplib.object.bodytext.control.ControlFootnote;
import kr.dogfoot.hwplib.object.bodytext.control.ControlHeader;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.header.HeaderFooterApplyPage;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.sectiondefine.SectionDefineHeaderProperty;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.FootEndNoteShape;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.ParaCharShape;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.ParaLineSeg;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.ParaText;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.engine.control.ControlAreaCalculator;
import kr.dogfoot.hwpdrawer.engine.control.ControlLayouter;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.obstacle.ParaObstacles;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.common.ParagraphNumberer;
import kr.dogfoot.hwpdrawer.engine.para.ParagraphLayouter;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.NoLineSegLayouter;
import kr.dogfoot.hwpdrawer.engine.para.synth.LineSegSynthesizer;
import kr.dogfoot.hwpdrawer.engine.para.withlineseg.WithLineSegLayouter;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import kr.dogfoot.hwpdrawer.document.page.FootnoteArea;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.Area;

/**
 * HWP 문서의 레이아웃을 계산하여 {@link Document}(중간 결과물)를 생성하는 엔진.
 *
 * <p>섹션/문단 단위로 순회하며, 문단의 LineSeg 유무에 따라 두 Layouter 중 하나에 위임한다.
 * 글자 측정은 {@link CharMetricsMeasurer}, 문단 속성은 {@link ParaShapeHelper}에 위임한다.</p>
 *
 * <p>Clean Code: 작은 메서드로 쪼개고 각 메서드는 한 가지 의도만 드러낸다.</p>
 */
public class LayoutEngine {
    private final HWPFileReader reader;
    private final CharMetricsMeasurer measurer;
    private final WithLineSegLayouter withLineSegLayouter;
    private final LineSegSynthesizer lineSegSynthesizer;
    /** 균형 배분 시뮬레이션 전용 — 단별 폭으로 직접 레이아웃해 줄 수를 측정한다. */
    private final NoLineSegLayouter columnSimLayouter = new NoLineSegLayouter();

    private Document document;
    private LineSegObserver lineSegObserver;
    private final ControlLayouter controlLayouter;

    // 현재 섹션 처리 중 사용하는 paperArea — 내부 레이아웃(InnerLayouter)에서 참조
    private Area currentPaperArea;
    // 현재 섹션의 기본 탭 간격 — 표 셀/글상자 등 내부 레이아웃에 전파하기 위해 보관
    private long currentDefaultTabGap = 4000;

    public LayoutEngine(HWPFileReader reader) {
        this.reader = reader;
        this.measurer = new CharMetricsMeasurer();
        this.withLineSegLayouter = new WithLineSegLayouter();
        this.lineSegSynthesizer = new LineSegSynthesizer();
        this.controlLayouter = new ControlLayouter(this::layoutParagraphList);
    }

    /** 문서 전체를 레이아웃하여 Document를 생성한다. */
    public Document layout() {
        document = new Document();
        for (Section section : reader.sections()) {
            layoutSection(section);
        }
        return document;
    }

    public void dispose() {
        measurer.dispose();
    }

    /**
     * LineSeg 합성 결과를 관찰하는 옵저버를 등록한다(테스트용).
     * 설정되어 있으면 lineseg가 있는 문단에 대해서도 합성을 수행해 원본과 함께 전달한다.
     */
    public LayoutEngine lineSegObserver(LineSegObserver observer) {
        this.lineSegObserver = observer;
        return this;
    }

    // ───────────────────────── 섹션 / 문단 순회 ─────────────────────────

    private void layoutSection(Section section) {
        PageDef pageDef = reader.getPageDef(section);
        if (pageDef == null) return;

        ParaLayoutContext ctx = createLayoutContext(pageDef);
        currentDefaultTabGap = reader.getDefaultTabGap(section);
        ctx.defaultTabGap(currentDefaultTabGap);
        currentPaperArea = ctx.paperArea();
        int sectionFirstPageIdx = document.pages().size();
        // 지연 컨트롤(75%-규칙 NoDivide 표 등)을 새 페이지 생성 시점에 그 페이지 상단에
        // 즉시 배치하도록 콜백 연결. 그래야 후속 단락 줄이 표 아래에서 시작한다.
        ctx.deferredPlacer((anchorPage, newPage) ->
                controlLayouter.placeDeferredOnNewPage(ctx, anchorPage, newPage));
        ctx.createNewPage();

        SectionDefineHeaderProperty secProp = reader.getSectionProperty(section);
        boolean hideEmptyLine = secProp != null && secProp.isHideEmptyLine();
        boolean hideHeader = secProp != null && secProp.isHideHeader();
        boolean hideFooter = secProp != null && secProp.isHideFooter();

        // 머리말/꼬리말 컨트롤 수집 (첫 문단의 control list 에서).
        List<ControlHeader> headers = collectHeaders(section);
        List<ControlFooter> footers = collectFooters(section);

        ColumnState colState = new ColumnState(ctx.bodyArea());
        setupColumnAdvancer(ctx, colState);

        // 현재 다단 블록이 시작된 페이지와 해당 페이지 컬럼의 시작 라인 인덱스를 추적.
        // ControlColumnDefine 전환 시 직전 블록의 최소 글자 기준선 크기(gap)를 계산하는 데 사용.
        Page sectionPage = ctx.currentPage();
        int sectionStartLineIdx = 0;
        // 마지막 ColumnDefine 적용 시점의 라인 카운트 — divideColumn 처리 시 해당 블록에
        // 이미 content 가 들어갔는지 판단해 spurious restart 를 방지.
        int lastColumnDefineLineIdx = 0;

        int count = section.getParagraphCount();
        for (int paraIdx = 0; paraIdx < count; paraIdx++) {
            Paragraph para = section.getParagraph(paraIdx);

            // ControlColumnDefine 감지: 다단 설정이 변경되면 단 영역을 재계산한다.
            ControlColumnDefine colDef = findColumnDefine(para);
            if (colDef != null && isParallelDefine(colDef)) {
                // 평행 다단 블록 — 전용 레이아웃 경로로 위임한다.
                registerColumnSeps(ctx.currentPage(), colState,
                        colState.columnSectionBaseline(lastLineBaseline(ctx)));
                int nextIdx = layoutParallelBlock(section, paraIdx, colDef, ctx, colState);
                setupColumnAdvancer(ctx, colState);
                sectionPage = ctx.currentPage();
                sectionStartLineIdx = currentColumnLineCount(ctx);
                lastColumnDefineLineIdx = sectionStartLineIdx;
                paraIdx = nextIdx - 1;
                continue;
            }
            if (colDef != null) {
                registerColumnSeps(ctx.currentPage(), colState,
                        colState.columnSectionBaseline(lastLineBaseline(ctx)));
                long prevMinFont = sectionMinFont(ctx, sectionPage, sectionStartLineIdx);
                long gap = gapFromMinFont(prevMinFont);
                colState.applyColumnDefine(colDef, ctx.pageBodyArea(), ctx.currentY(), ctx.currentPage().pageNo(), gap, prevMinFont);
                sectionPage = ctx.currentPage();
                ctx.bodyArea(colState.currentArea().clone());
                // maxColumnBottom 보정으로 bodyArea.top()이 currentY보다 높아진 경우,
                // currentY도 함께 올려야 한다. 그렇지 않으면 ControlColumnDefine 문단(빈 줄)이
                // 이전 블록 영역 안에 겹쳐 배치된다.
                if (ctx.bodyArea().top() > ctx.currentY()) {
                    ctx.currentY(ctx.bodyArea().top());
                }
                // 새 다단 설정 후 이전 단의 vertPos 비교값을 초기화한다.
                // 그렇지 않으면 새 단의 첫 라인(vp=0)이 이전 단의 마지막 vp보다 작아
                // handlePageBreakIfNeeded에서 오인하여 다음 단으로 건너뛴다.
                ctx.lastLineSegVertPos(-1);
                // 이전 블록의 overflow 보정으로 인해 새 블록의 top이 페이지 bottom에 도달하면
                // 즉시 새 페이지로 전환한다 (다단 블록 간 겹침 방지).
                if (ctx.bodyArea().top() >= ctx.pageBodyArea().bottom()) {
                    ctx.createNewPage();
                    colState.refreshForNewPage(ctx.pageBodyArea(), ctx.currentPage().pageNo());
                    ctx.bodyArea(colState.currentArea().clone());
                    ctx.currentY(ctx.bodyArea().top());
                    ctx.lastLineSegVertPos(-1);
                }
                // 빈 ColumnDefine 앵커 단락(Normal 글자 없음)은 자신의 lineHeight+lineSpace 만큼
                // 모든 컬럼의 top을 아래로 내리고 자체 layout은 skip — HWP는 이 앵커를 실제로 그리지 않고
                // 다음 단락이 새 컬럼 top 에서 시작하게 한다.
                // sectionStartLineIdx는 갱신하지 않음 — 다음 블록의 sectionMinFont가
                // 직전 콘텐츠 블록의 라인들을 계속 참조해 올바른 gap을 계산하게 한다.
                if (isEmptyAnchor(para) && colState.isMultiColumn()) {
                    // shift = 앵커의 lineHeight+lineSpace + (필요시) 추가 gap
                    // 다음 단락이 자체 ColumnDefine 을 가지면 그 applyColumnDefine 이
                    // gap 을 다시 추가하므로 여기서는 빼서 중복을 막는다.
                    long anchorHeight = anchorLineHeight(para);
                    if (anchorHeight > 0) {
                        boolean nextHasOwnColumnDefine = paraIdx + 1 < count
                                && findColumnDefine(section.getParagraph(paraIdx + 1)) != null;
                        long shift = anchorHeight + (nextHasOwnColumnDefine ? 0 : gap);
                        colState.shiftColumnTops(shift);
                        ctx.bodyArea(colState.currentArea().clone());
                        ctx.currentY(ctx.bodyArea().top());
                    }
                    // ColumnDefine 적용 시점 기록 (빈 앵커여도 새 블록 시작점이므로 갱신).
                    lastColumnDefineLineIdx = currentColumnLineCount(ctx);
                    // 빈 앵커 다음의 실제 콘텐츠 블록도 균형 배분한다 (배분 다단).
                    // 다음 단락이 자체 ColumnDefine 을 가지면 그 시점에 배분되므로 건너뛴다.
                    boolean nextHasOwnColumnDefine = paraIdx + 1 < count
                            && findColumnDefine(section.getParagraph(paraIdx + 1)) != null;
                    if (!nextHasOwnColumnDefine && paraIdx + 1 < count) {
                        applyColumnBalanceIfNeeded(section, paraIdx + 1, count, ctx, colState, false);
                    }
                    continue;
                }
                // 실제 콘텐츠가 있는 단락만 sectionStartLineIdx를 갱신한다.
                sectionStartLineIdx = currentColumnLineCount(ctx);
                lastColumnDefineLineIdx = sectionStartLineIdx;
                // NoLineSeg 일반 다단 블록 균형 배분 — 단 높이를 고르게 맞춘다.
                applyColumnBalanceIfNeeded(section, paraIdx, count, ctx, colState, false);
            }

            if (shouldSkipFirstEmptyPara(paraIdx, para)) continue;

            applyPageBreakIfRequested(para, paraIdx, ctx, colState);
            // 단나누기(divideColumn) 플래그: 같은 ColumnDefine 설정으로 새 다단 블록 시작.
            // ColumnDefine이 별도로 없는 단락이지만, 직전 다단 블록을 종료하고 같은 컬럼 구성을
            // 재적용한다 (HWP의 단나누기 동작).
            if (paraIdx > 0 && colDef == null && hasDivideColumn(para)
                    && colState.canRestartForDivideColumn()
                    && currentColumnLineCount(ctx) > lastColumnDefineLineIdx) {
                long prevMinFont = sectionMinFont(ctx, sectionPage, sectionStartLineIdx);
                long gap = gapFromMinFont(prevMinFont);
                registerColumnSeps(ctx.currentPage(), colState,
                        colState.columnSectionBaseline(lastLineBaseline(ctx)));
                colState.restartWithSameDefine(ctx.pageBodyArea(), ctx.currentY(),
                        ctx.currentPage().pageNo(), gap, prevMinFont);
                sectionPage = ctx.currentPage();
                sectionStartLineIdx = currentColumnLineCount(ctx);
                ctx.bodyArea(colState.currentArea().clone());
                if (ctx.bodyArea().top() > ctx.currentY()) {
                    ctx.currentY(ctx.bodyArea().top());
                }
                ctx.lastLineSegVertPos(-1);
                lastColumnDefineLineIdx = sectionStartLineIdx;
                // 단나누기로 재시작한 새 다단 블록도 균형 배분한다.
                applyColumnBalanceIfNeeded(section, paraIdx, count, ctx, colState, true);
            }
            // 일반 다단(신문식)의 단나누기: 다음 단으로 전진(마지막 단이면 새 페이지).
            // LineSeg가 없는 HWP는 vertPos 리셋으로 단 전환을 인코딩하지 못하므로
            // 단나누기 플래그를 보고 명시적으로 전진해야 한다.
            if (paraIdx > 0 && colDef == null && hasDivideColumn(para)
                    && colState.isMultiColumn() && !colState.canRestartForDivideColumn()) {
                ctx.tryNextColumn();
            }
            // 빈 줄 감추기: SectionDefine.hideEmptyLine 옵션이 켜져 있고 현재 빈 문단이
            // 페이지 하단을 넘어선 위치에 와있으면 그 문단을 통째로 건너뛴다.
            // (HWP의 "빈 줄 감추기" 옵션: 페이지 끝 trailing 빈 문단으로 새 페이지를 만들지 않는다.)
            if (hideEmptyLine && isEmptyParagraph(para)
                    && wouldOverflowAsEmptyLine(para, ctx)) {
                continue;
            }
            // 페이지에 걸쳐 분할된 어울림 DivideByCell 표 직후의 첫 텍스트 본문 단락은
            // 표 둘째 조각과 함께 다음 페이지에서 시작한다 (LineSeg vp=0 page break 재현).
            // NoLineSeg 단락에만 적용 — LineSeg는 vp로 이미 올바른 페이지에 배치된다.
            if (ctx.pendingFitWrapTableParaIdx() >= 0 && paraIdx > ctx.pendingFitWrapTableParaIdx()) {
                if (!isEmptyParagraph(para) && !hasLineSeg(para)) {
                    ctx.createNewPage();
                    colState.refreshForNewPage(ctx.pageBodyArea(), ctx.currentPage().pageNo());
                    ctx.bodyArea(colState.currentArea().clone());
                    ctx.currentY(ctx.bodyArea().top());
                    ctx.lastLineSegVertPos(-1);
                    ctx.pendingFitWrapTableParaIdx(-1);
                    // 새 페이지에 배치된 표 둘째 조각(어울림)을 obstacle로 등록해
                    // 이 단락이 표 주위로 래핑되게 한다.
                    controlLayouter.registerFitWithTextTableObstacles(ctx.currentPage(), ctx);
                }
            }
            advanceToNextColumnOrNewPageIfOverflow(ctx, colState);

            ParaShape paraShape = reader.getParaShape(para);
            ctx.currentY(ctx.currentY() + ParaShapeHelper.topSpace(paraShape));
            ctx.currentParaTopY(ctx.currentY());
            // 자신의 FitWithText(어울림) 컨트롤이 없는 문단에 대해서만 섹션 전체의
            // Paper/Page-relative FitWithText 컨트롤을 선제 등록한다.
            // 자신의 어울림 컨트롤이 있으면 preRegisterObstacles로 충분하며,
            // 다른 문단의 obstacle을 추가하면 잘못된 래핑이 발생한다.
            if (!ControlLayouter.hasFitWithTextControl(para)) {
                controlLayouter.preRegisterPaperAbsoluteObstacles(section, ctx);
            }
            // FitWithText(어울림) 컨트롤의 영역을 text 레이아웃 이전에 등록해 본문 래핑에 반영.
            controlLayouter.preRegisterObstacles(para, ctx);
            // NoLineSeg 문단을 단 폭에 맞게 합성할 수 있도록 현재 다단 영역을 전달한다.
            ctx.multiColumnAreas(colState.columnAreas);
            ctx.multiColumnIndex(colState.currentIndex);
            // 페이지를 넘는 배분 다단 블록: 후속 페이지의 단 배분을 합성기에 전달.
            ctx.multiColumnPageBottoms(colState.pendingBalances);
            layoutParagraph(para, paraIdx, paraShape, ctx);
            ctx.currentY(ctx.currentY() + ParaShapeHelper.bottomSpace(paraShape));

            // 문단 내 컨트롤(이미지/표/글상자) 처리 — layoutParagraph 후 TextLine 확정 상태에서 호출.
            // isNoLineSeg=true이면 anchor 이전 문단이 obstacle 없이 배치되어 shouldDefer 판단에 영향을 줌.
            controlLayouter.layoutControls(para, paraIdx, ctx, !hasLineSeg(para));
        }
        // 섹션 내 75%-규칙으로 지연된 컨트롤들을 일괄 배치(필요 시 빈 페이지 추가)
        controlLayouter.placeDeferredControls(ctx);
        // 자리차지 분할 표 뒤의 본문 라인이 firstSegPage의 표 영역과 겹치면 lastSegPage로
        // 이동 — 표가 placed 된 시점에는 후속 paragraph가 아직 layout 안 됐기 때문에
        // 후처리로 정리한다.
        controlLayouter.moveLinesOverlappingTakePlaceTable(ctx);
        // 어울림 분할 표의 예약 조각이 본문 page 흐름으로 배치되지 못하고 남았으면
        // (NoLineSeg 에서 표 다음 page 트리거 부재) 새 페이지에 강제 배치한다.
        ctx.flushPendingTableSegments();
        // 마지막 다단 섹션의 분리선 등록 (마지막 TextLine baseline 기준)
        registerColumnSeps(ctx.currentPage(), colState,
                colState.columnSectionBaseline(lastLineBaseline(ctx)));

        // 머리말/꼬리말을 섹션이 차지한 모든 페이지에 적용한다.
        populateHeadersAndFooters(pageDef, sectionFirstPageIdx,
                headers, footers, hideHeader, hideFooter);
        // 각주를 앵커가 놓인 페이지 하단에 배치한다. 본문 줄이 모두 확정된 뒤여야
        // 앵커가 어느 페이지에 갔는지 알 수 있으므로 머리말/꼬리말과 같은 시점에 한다.
        populateFootnotes(pageDef, section, collectFootnotes(section));
        // 자리차지 분할 표 다음 trailing 빈 문단이 만든 완전히 빈 마지막 페이지를 제거한다.
        // (NoLineSeg 는 표 둘째 조각 bottom 에서 빈 문단이 시작해 빈 페이지를 만들지만
        //  한글은 그 빈 줄을 표 영역에 흡수해 페이지를 만들지 않는다.)
        // 머리말/꼬리말 적용 후에 호출 — 헤더/풋터가 들어간 빈 본문 페이지(헤더풋터
        // 케이스)는 isVisuallyEmptyPage 가 false 가 되어 보존된다.
        // 단, 마지막 문단이 LineSeg 를 가지면 trailing 빈 페이지도 한글이 vertPos 로
        // 명시 배치한 것(실문서 R06: 빈 문단 6개가 vp=0~8000으로 페이지 3에 캡처)이므로
        // 보존한다 — 제거는 NoLineSeg 합성 잔여물에만 적용.
        boolean lastParaLineSeg = section.getParagraphCount() > 0
                && hasLineSeg(section.getParagraph(section.getParagraphCount() - 1));
        if (!lastParaLineSeg) {
            removeTrailingEmptyPages(sectionFirstPageIdx);
        }

        assignPageNumbers(section, sectionFirstPageIdx, pageDef);
    }

    /**
     * 쪽 번호 매기기(PageNumberPosition) 컨트롤이 있으면, 섹션의 각 페이지에 표시할 쪽
     * 번호 문자열/위치를 계산해 저장한다. 렌더링은 PagePainter가 담당한다.
     *
     * <p>번호 계산: 시작 1에서 페이지마다 증가하되, NewNumber(sort=Page) 컨트롤이 있는
     * 페이지에서 그 값으로 리셋한다. PageHide 컨트롤이 있는 페이지는 표시하지 않는다.
     * (실문서 R03: 표지·목차 2쪽은 PageHide로 숨기고, 본문 시작 쪽에서 NewNumber로 1 리셋.)</p>
     */
    private void assignPageNumbers(Section section, int firstPageIdx, PageDef pageDef) {
        kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderPageNumberPosition pnp = null;
        java.util.Map<Integer, Integer> newNumberByPara = new java.util.HashMap<>();
        java.util.Set<Integer> pageHidePara = new java.util.HashSet<>();
        for (int pi = 0; pi < section.getParagraphCount(); pi++) {
            Paragraph p = section.getParagraph(pi);
            if (p.getControlList() == null) continue;
            for (Control c : p.getControlList()) {
                if (c instanceof kr.dogfoot.hwplib.object.bodytext.control.ControlPageNumberPosition) {
                    pnp = ((kr.dogfoot.hwplib.object.bodytext.control.ControlPageNumberPosition) c).getHeader();
                } else if (c instanceof kr.dogfoot.hwplib.object.bodytext.control.ControlNewNumber) {
                    kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderNewNumber nh =
                            ((kr.dogfoot.hwplib.object.bodytext.control.ControlNewNumber) c).getHeader();
                    if (nh.getProperty().getNumberSort()
                            == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.autonumber.NumberSort.Page) {
                        newNumberByPara.put(pi, nh.getNumber());
                    }
                } else if (c instanceof kr.dogfoot.hwplib.object.bodytext.control.ControlPageHide) {
                    pageHidePara.add(pi);
                }
            }
        }
        if (pnp == null) return;
        kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.pagenumberposition.NumberPosition pos =
                pnp.getProperty().getNumberPosition();
        if (pos == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.pagenumberposition.NumberPosition.None) return;

        String deco = decorationString(pnp.getAfterDecorationLetter());
        if (deco.isEmpty()) deco = decorationString(pnp.getBeforeDecorationLetter());

        // paraIndex → pageIndex (첫 등장)
        List<Page> pages = document.pages();
        java.util.Map<Integer, Integer> paraToPage = new java.util.HashMap<>();
        for (int pg = firstPageIdx; pg < pages.size(); pg++) {
            for (TextRow row : pages.get(pg).content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine ln : col.textLines()) {
                        paraToPage.putIfAbsent(ln.paraIndex(), pg);
                    }
                }
            }
        }
        java.util.Map<Integer, Integer> newNumberByPage = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, Integer> e : newNumberByPara.entrySet()) {
            Integer pg = paraToPage.get(e.getKey());
            if (pg != null) newNumberByPage.put(pg, e.getValue());
        }
        java.util.Set<Integer> hidePages = new java.util.HashSet<>();
        for (int para : pageHidePara) {
            Integer pg = paraToPage.get(para);
            if (pg != null) hidePages.add(pg);
        }

        Area paper = reader.getPaperArea(pageDef);
        Area footer = reader.getFooterArea(pageDef);
        long centerX = (paper.left() + paper.right()) / 2;
        long baseSize = 1000;                       // 쪽 번호 기본 10pt
        // 쪽 번호는 꼬리말 영역 바닥에 정렬된다(baseline ≈ footer.bottom − 작은 디센트).
        long baselineY = footer.bottom() - baseSize / 5;
        kr.dogfoot.hwplib.object.docinfo.CharShape cs =
                reader.hwpFile().getDocInfo().getCharShapeList().isEmpty()
                        ? null : reader.hwpFile().getDocInfo().getCharShapeList().get(0);

        int counter = 1;
        for (int pg = firstPageIdx; pg < pages.size(); pg++) {
            if (newNumberByPage.containsKey(pg)) counter = newNumberByPage.get(pg);
            Page page = pages.get(pg);
            if (!hidePages.contains(pg)) {
                String num = String.valueOf(counter);   // 형식 Number(아라비아). 로마자 등은 미지원.
                String text = deco.isEmpty() ? num : deco + " " + num + " " + deco;
                page.pageNumberText(text)
                        .pageNumberCharShape(cs)
                        .pageNumberCenterX(centerX)
                        .pageNumberBaselineY(baselineY);
            }
            counter++;
        }
    }

    /** HWPString(UTF-16LE)을 String으로. null 문자(0)는 건너뛴다. */
    private static String decorationString(kr.dogfoot.hwplib.object.etc.HWPString s) {
        if (s == null) return "";
        byte[] b = s.getBytes();
        if (b == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < b.length; i += 2) {
            int ch = (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8);
            if (ch != 0) sb.append((char) ch);
        }
        return sb.toString();
    }

    /**
     * 섹션이 만든 페이지 중 완전히 빈(텍스트·컨트롤·단분리선 없는) trailing 페이지를
     * 제거한다. 섹션 첫 페이지는 유지(빈 섹션 방지).
     */
    private void removeTrailingEmptyPages(int sectionFirstPageIdx) {
        List<Page> pages = document.pages();
        while (pages.size() > sectionFirstPageIdx + 1) {
            Page last = pages.get(pages.size() - 1);
            if (!isVisuallyEmptyPage(last)) break;
            pages.remove(pages.size() - 1);
        }
    }

    /** 페이지에 그려질 텍스트(글자)·컨트롤·단분리선이 하나도 없으면 true. */
    private static boolean isVisuallyEmptyPage(Page page) {
        if (!page.controls().isEmpty()) return false;
        if (!page.columnSeps().isEmpty()) return false;
        if (page.header() != null || page.footer() != null) return false;
        if (page.footnotes() != null) return false;
        for (TextRow row : page.content().rows()) {
            for (TextColumn col : row.columns()) {
                for (TextLine line : col.textLines()) {
                    for (kr.dogfoot.hwpdrawer.document.text.TextPart part : line.parts()) {
                        if (!part.charInfos().isEmpty()) return false;
                    }
                }
            }
        }
        return true;
    }

    /** 각주 하나와 그 앵커(본문 안 ControlExtend code 17)의 위치. */
    private static final class FootnoteAnchor {
        final ControlFootnote ctrl;
        final int paraIdx;
        final int charIdx;
        FootnoteAnchor(ControlFootnote ctrl, int paraIdx, int charIdx) {
            this.ctrl = ctrl; this.paraIdx = paraIdx; this.charIdx = charIdx;
        }
    }

    /**
     * 섹션 전체 문단을 훑어 각주를 수집한다.
     *
     * <p>머리말/꼬리말({@link #collectHeaders})과 달리 각주는 <b>첫 문단이 아니라 본문 곳곳</b>에
     * 흩어져 있다. 각주 컨트롤이 문단의 몇 번째 ControlExtend 인지로 본문 속 앵커 위치를 찾는다 —
     * ControlExtend 글자와 controlList 항목이 같은 순서로 대응하기 때문
     * ({@code WithLineSegLayouter.fillCharInfos} 의 {@code ctrlIdx} 와 같은 규칙).</p>
     */
    private static List<FootnoteAnchor> collectFootnotes(Section section) {
        List<FootnoteAnchor> list = new ArrayList<>();
        for (int p = 0; p < section.getParagraphCount(); p++) {
            Paragraph para = section.getParagraph(p);
            if (para.getControlList() == null || para.getText() == null) continue;

            int ctrlIdx = 0;
            int charIdx = 0;
            for (kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar ch : para.getText().getCharList()) {
                if (ch.getType() == kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType.ControlExtend) {
                    if (ctrlIdx < para.getControlList().size()) {
                        Control c = para.getControlList().get(ctrlIdx);
                        if (c instanceof ControlFootnote) {
                            list.add(new FootnoteAnchor((ControlFootnote) c, p, charIdx));
                        }
                    }
                    ctrlIdx++;
                }
                charIdx++;
            }
        }
        return list;
    }

    /**
     * 각주를 앵커가 놓인 페이지의 하단에 배치한다.
     *
     * <p><b>배치 규칙(하단 정렬)</b> — 각주 블록의 <b>맨 아래</b>가 본문 영역 하단에 붙는다.
     * 본문 바로 아래가 아니다. 실문서 R21 실측: 각주 줄 수가 2/14/6 으로 다른데도
     * 마지막 각주 줄의 잉크 하단이 세 페이지 모두 같은 y 다.</p>
     *
     * <pre>
     *   각주높이(k) = 마지막 LineSeg.vp + lineHeight      // 마지막 줄의 lineSpace 는 빼고
     *   블록높이    = Σ 각주높이 + (각주수 - 1) × BetweenNotesMargin
     *   블록상단    = bodyArea.bottom() - 블록높이
     *   각주상단(k) = 각주상단(k-1) + 각주높이(k-1) + BetweenNotesMargin
     *   구분선 y    = 블록상단 - DivideLineBottomMargin
     * </pre>
     *
     * <p><b>BetweenNotesMargin 은 각주 사이 lineSpace 를 "대체"한다</b>(더하는 것이 아니다).
     * 더하면 p2 각주 14줄이 전부 어긋난다. 3페이지 구분선 y(1217/982/1139)와 22개 줄 위치가
     * 이 공식으로 전부 재현된다.</p>
     *
     * <p>{@code DivideLineTopMargin}(본문과 구분선 사이 최소 여백)은 구현하지 않았다 —
     * 본문이 어디서 끊기는지는 한글이 이미 파일 LineSeg 에 반영해 뒀고, 이 문서에서는
     * 그 여백이 한 번도 구속하지 않아(실측 간격이 항상 훨씬 크다) 검증할 표본이 없다.</p>
     */
    private void populateFootnotes(PageDef pageDef, Section section, List<FootnoteAnchor> footnotes) {
        if (footnotes.isEmpty()) return;
        FootEndNoteShape shape = reader.getFootNoteShape(section);
        if (shape == null) return;

        Area bodyArea = reader.getBodyArea(pageDef);
        Area paperArea = reader.getPaperArea(pageDef);
        long betweenMargin = shape.getBetweenNotesMargin();

        // 앵커가 놓인 페이지별로 각주를 모은다 (문서 순서 유지).
        Map<Page, List<ControlFootnote>> byPage = new LinkedHashMap<>();
        for (FootnoteAnchor a : footnotes) {
            Page page = findAnchorPage(a.paraIdx, a.charIdx);
            if (page == null) continue;
            byPage.computeIfAbsent(page, k -> new ArrayList<>()).add(a.ctrl);
        }

        for (Map.Entry<Page, List<ControlFootnote>> e : byPage.entrySet()) {
            Page page = e.getKey();
            List<ControlFootnote> notes = e.getValue();

            long blockHeight = (notes.size() - 1) * betweenMargin;
            for (ControlFootnote fn : notes) blockHeight += noteHeight(fn);
            long blockTop = bodyArea.bottom() - blockHeight;

            FootnoteArea area = new FootnoteArea(
                    blockTop - shape.getDivideLineBottomMargin(),
                    bodyArea.left(),
                    bodyArea.left() + divideLineLength(shape, bodyArea),
                    shape.getDivideLine());

            long top = blockTop;
            for (ControlFootnote fn : notes) {
                area.notes().add(buildFootnote(fn, bodyArea, top, paperArea));
                top += noteHeight(fn) + betweenMargin;
            }
            page.footnotes(area);
        }
    }

    /**
     * 각주 하나의 높이. 각주 안에서는 문단이 여러 개여도 LineSeg 의 vp 가 이어지므로
     * 마지막 문단의 마지막 줄만 보면 된다. 마지막 줄에는 줄 간격(lineSpace)을 더하지 않는다.
     */
    private static long noteHeight(ControlFootnote fn) {
        ParagraphList paragraphs = fn.getParagraphList();
        long height = 0;
        for (int i = 0; i < paragraphs.getParagraphCount(); i++) {
            ParaLineSeg seg = paragraphs.getParagraph(i).getLineSeg();
            if (seg == null) continue;
            for (LineSegItem item : seg.getLineSegItemList()) {
                long bottom = item.getLineVerticalPosition() + item.getLineHeight();
                if (bottom > height) height = bottom;
            }
        }
        return height;
    }

    /**
     * 구분선 길이. {@code DivideLineLength} 가 0xFFFFFFFF 면 "자동"이고, 실측 237px 는
     * 5cm(14173)와 본문폭÷3(14173.33)이 <b>구분되지 않는다</b> — 이 문서의 본문폭이 정확히
     * 15cm 라 두 해석이 같은 값이 된다. 본문폭 기준을 택했다(다단·다른 용지 문서를 확보하면 재검증).
     */
    private static long divideLineLength(FootEndNoteShape shape, Area bodyArea) {
        long length = shape.getDivideLineLength();
        boolean auto = (length <= 0 || length == 0xFFFFFFFFL);
        return auto ? bodyArea.width() / 3 : length;
    }

    /** 각주 앵커(문단 paraIdx 의 charIdx 글자)가 렌더된 페이지. */
    private Page findAnchorPage(int paraIdx, int charIdx) {
        Page found = null;
        for (Page page : document.pages()) {
            for (TextRow row : page.content().rows()) {
                for (TextColumn col : row.columns()) {
                    for (TextLine line : col.textLines()) {
                        if (line.paraIndex() == paraIdx && line.startCharIdx() <= charIdx) {
                            found = page;
                        }
                    }
                }
            }
        }
        return found;
    }

    /** 각주 하나를 top 부터 레이아웃한다. 각주마다 LineSeg 의 vp 가 0 에서 다시 시작한다. */
    private kr.dogfoot.hwpdrawer.document.page.Footnote buildFootnote(
            ControlFootnote ctrl, Area bodyArea, long top, Area paperArea) {
        kr.dogfoot.hwpdrawer.document.page.Footnote fn =
                new kr.dogfoot.hwpdrawer.document.page.Footnote();
        // bottom 을 용지 하단으로 두는 이유: 본문 영역 하단으로 두면 마지막 각주의 마지막 줄
        // 하단이 경계와 정확히 같아져 페이지가 하나 더 생길 수 있다.
        Area area = new Area(bodyArea.left(), top, bodyArea.right(), paperArea.bottom());
        fn.area().copyFrom(area);
        Document innerDoc = layoutParagraphList(ctrl.getParagraphList(), area, area, paperArea, true);
        if (!innerDoc.pages().isEmpty()) {
            fn.content().rows().addAll(innerDoc.pages().get(0).content().rows());
        }
        return fn;
    }

    /** 첫 문단 control list 에서 ControlHeader 들을 수집한다. */
    private static List<ControlHeader> collectHeaders(Section section) {
        List<ControlHeader> list = new ArrayList<>();
        if (section.getParagraphCount() == 0) return list;
        Paragraph first = section.getParagraph(0);
        if (first.getControlList() == null) return list;
        for (Control c : first.getControlList()) {
            if (c instanceof ControlHeader) list.add((ControlHeader) c);
        }
        return list;
    }

    /** 첫 문단 control list 에서 ControlFooter 들을 수집한다. */
    private static List<ControlFooter> collectFooters(Section section) {
        List<ControlFooter> list = new ArrayList<>();
        if (section.getParagraphCount() == 0) return list;
        Paragraph first = section.getParagraph(0);
        if (first.getControlList() == null) return list;
        for (Control c : first.getControlList()) {
            if (c instanceof ControlFooter) list.add((ControlFooter) c);
        }
        return list;
    }

    /**
     * 섹션이 만든 페이지들을 순회하며, 페이지 홀/짝수에 맞는 머리말/꼬리말 컨트롤을
     * 골라 페이지의 header/footer 슬롯을 채운다.
     */
    private void populateHeadersAndFooters(PageDef pageDef, int firstPageIdx,
                                            List<ControlHeader> headers,
                                            List<ControlFooter> footers,
                                            boolean hideHeader, boolean hideFooter) {
        if ((headers.isEmpty() || hideHeader) && (footers.isEmpty() || hideFooter)) return;
        Area headerArea = reader.getHeaderArea(pageDef);
        Area footerArea = reader.getFooterArea(pageDef);
        Area paperArea = reader.getPaperArea(pageDef);
        for (int i = firstPageIdx; i < document.pages().size(); i++) {
            Page page = document.pages().get(i);
            boolean isOdd = (page.pageNo() % 2 == 1);

            if (!hideHeader) {
                ControlHeader chosen = chooseHeaderFooter(headers, isOdd, ControlHeader::getHeader);
                if (chosen != null) {
                    page.header(buildHeader(chosen, headerArea, paperArea));
                }
            }
            if (!hideFooter) {
                ControlFooter chosen = chooseHeaderFooter(footers, isOdd, ControlFooter::getHeader);
                if (chosen != null) {
                    page.footer(buildFooter(chosen, footerArea, paperArea));
                }
            }
        }
    }

    /**
     * applyPage 가 OddPage/EvenPage 인 것을 우선 선택하고, 없으면 BothPage 폴백.
     */
    private static <T> T chooseHeaderFooter(List<T> list, boolean isOdd,
                                             java.util.function.Function<T, ? extends kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeader> headerGetter) {
        T specific = null;
        T both = null;
        for (T t : list) {
            kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeader h = headerGetter.apply(t);
            HeaderFooterApplyPage applyPage = null;
            if (h instanceof kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderHeader) {
                applyPage = ((kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderHeader) h).getApplyPage();
            } else if (h instanceof kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderFooter) {
                applyPage = ((kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderFooter) h).getApplyPage();
            }
            if (applyPage == HeaderFooterApplyPage.BothPage) both = t;
            else if (isOdd && applyPage == HeaderFooterApplyPage.OddPage) specific = t;
            else if (!isOdd && applyPage == HeaderFooterApplyPage.EvenPage) specific = t;
        }
        return specific != null ? specific : both;
    }

    private kr.dogfoot.hwpdrawer.document.page.Header buildHeader(ControlHeader ctrl,
                                                                    Area area, Area paperArea) {
        kr.dogfoot.hwpdrawer.document.page.Header h = new kr.dogfoot.hwpdrawer.document.page.Header();
        h.area().copyFrom(area);
        layoutHeaderFooterContent(ctrl.getParagraphList(), area, paperArea,
                h.content(), TextVerticalAlignment.Top);
        return h;
    }

    private kr.dogfoot.hwpdrawer.document.page.Footer buildFooter(ControlFooter ctrl,
                                                                    Area area, Area paperArea) {
        kr.dogfoot.hwpdrawer.document.page.Footer f = new kr.dogfoot.hwpdrawer.document.page.Footer();
        f.area().copyFrom(area);
        layoutHeaderFooterContent(ctrl.getParagraphList(), area, paperArea,
                f.content(), TextVerticalAlignment.Bottom);
        return f;
    }

    /**
     * 머리말/꼬리말 파라리스트를 주어진 영역에 레이아웃하고, 결과 TextRow 들을
     * content 에 옮긴다. 세로 정렬에 따라 y 좌표를 이동한다.
     */
    private void layoutHeaderFooterContent(ParagraphList paragraphs, Area area, Area paperArea,
                                            kr.dogfoot.hwpdrawer.document.Content content,
                                            TextVerticalAlignment va) {
        Document innerDoc = layoutParagraphList(paragraphs, area, area, paperArea, true);
        if (innerDoc.pages().isEmpty()) return;
        Page innerPage = innerDoc.pages().get(0);
        List<TextRow> rows = innerPage.content().rows();
        if (rows.isEmpty()) return;

        // 콘텐츠 전체 높이 계산 — line.area().bottom() 은 lineGap 까지 포함하므로
        // 시각적 텍스트 하단(descent 까지)을 위해 lineGap 을 뺀다.
        long contentBottom = area.top();
        for (TextRow r : rows) {
            for (TextColumn col : r.columns()) {
                for (TextLine ln : col.textLines()) {
                    long visibleBottom = ln.area().bottom() - ln.lineGap();
                    if (visibleBottom > contentBottom) contentBottom = visibleBottom;
                }
            }
        }
        long contentHeight = contentBottom - area.top();
        long shift = 0;
        if (va == TextVerticalAlignment.Bottom) {
            shift = area.bottom() - contentBottom;
        } else if (va == TextVerticalAlignment.Center) {
            shift = (area.height() - contentHeight) / 2;
        }
        if (shift != 0) shiftRowsY(rows, shift);
        content.rows().addAll(rows);
    }

    private static void shiftRowsY(List<TextRow> rows, long dy) {
        for (TextRow r : rows) {
            for (TextColumn col : r.columns()) {
                for (TextLine ln : col.textLines()) {
                    ln.area().set(ln.area().left(), ln.area().top() + dy,
                            ln.area().right(), ln.area().bottom() + dy);
                }
            }
        }
    }

    /**
     * 다단 전진 콜백을 ctx에 설정한다.
     *
     * <p>콜백은 다음과 같이 동작한다:
     * <ul>
     *   <li>다단 모드에서 다음 단이 남아있으면 → 단 변경 후 {@code true}</li>
     *   <li>다단 모드에서 마지막 단이면 → 새 페이지 + 첫 단으로 이동 후 {@code true}</li>
     *   <li>1단 모드이면 → {@code false} (호출자가 {@link ParaLayoutContext#createNewPage()} 호출)</li>
     * </ul>
     * </p>
     */
    private static void setupColumnAdvancer(ParaLayoutContext ctx, ColumnState colState) {
        ctx.columnAdvancer(() -> {
            if (!colState.isMultiColumn()) return false;

            if (!colState.isLastColumn()) {
                // recordColumnBottom: 위치 계산용 (applyColumnDefine에서 사용)
                colState.recordColumnBottom(Math.min(ctx.currentY(), ctx.bodyArea().bottom()));
                // recordColumnBaseline: 분리선 end_y 계산용
                colState.recordColumnBaseline(lastLineBaseline(ctx));
                colState.nextColumn();
            } else {
                // 마지막 단 → 새 페이지 + 첫 단 (현재 페이지의 단 분리선을 먼저 등록)
                registerColumnSeps(ctx.currentPage(), colState,
                        colState.columnSectionBaseline(lastLineBaseline(ctx)));
                ctx.createNewPage();
                colState.refreshForNewPage(ctx.pageBodyArea(), ctx.currentPage().pageNo());
            }
            ctx.bodyArea(colState.currentArea().clone());
            ctx.currentY(ctx.bodyArea().top());
            // 단/페이지 전진 후 단 영역을 갱신 — refreshForNewPage·setColumnBottoms가
            // columnAreas 리스트를 새 인스턴스로 교체하므로 ctx의 참조를 다시 연결한다.
            // (배분 다단의 마지막 페이지 예약 배분이 NoLineSeg 레이아웃에 반영되게 함)
            ctx.multiColumnAreas(colState.columnAreas);
            ctx.multiColumnIndex(colState.currentIndex);
            // lastLineSegVertPos를 -1로 리셋 — 단 전진 후 lineY<currentY 오판 방지.
            // (handlePageBreakIfNeeded에서도 리셋하지만, advanceToNextColumnOrNewPageIfOverflow
            //  경유 시에도 일관되게 적용되도록 여기서도 초기화한다.)
            ctx.lastLineSegVertPos(-1);
            ctx.obstacles().clear();
            return true;
        });
    }

    /**
     * 현재 Y가 단 영역을 넘으면 다음 단/페이지로 이동한다.
     * tryNextColumn()이 모든 이동 처리를 담당한다.
     */
    private static void advanceToNextColumnOrNewPageIfOverflow(ParaLayoutContext ctx,
                                                                ColumnState colState) {
        if (ctx.currentY() <= ctx.bodyArea().bottom()) return;
        if (!ctx.tryNextColumn()) {
            ctx.createNewPage();
        }
    }

    /** 문단에서 ControlColumnDefine을 찾는다. 없으면 null. */
    private static ControlColumnDefine findColumnDefine(Paragraph para) {
        if (para.getControlList() == null) return null;
        for (Control ctrl : para.getControlList()) {
            if (ctrl instanceof ControlColumnDefine) return (ControlColumnDefine) ctrl;
        }
        return null;
    }

    // ──────────────────── 일반 다단 균형 배분 (NoLineSeg) ────────────────────

    /**
     * NoLineSeg 일반 다단 블록을 균형 배분한다.
     *
     * <p>한 다단 블록(이 ColumnDefine 문단부터 다음 ColumnDefine 직전 또는 섹션 끝까지)의
     * 콘텐츠를 한 단 폭으로 측정해 총 높이 H를 얻고, 각 단의 하단을
     * {@code columnTop + H/단수} 로 줄여 단 높이를 고르게 만든다.
     * HWP는 다단 구역이 종료될 때 단을 이렇게 균형 배분한다.</p>
     *
     * <p>균형 높이가 페이지를 넘으면(블록이 한 페이지를 초과) 신문식 그대로 둔다.
     * 블록에 LineSeg를 가진 문단이 있으면 LineSeg 경로가 배치를 결정하므로 건너뛴다.</p>
     */
    private void applyColumnBalanceIfNeeded(ParagraphListInterface section, int blockStartIdx,
                                            int count, ParaLayoutContext ctx, ColumnState colState,
                                            boolean balanceLastBlock) {
        if (!colState.isMultiColumn()) return;
        if (!colState.isNormalSort() && !colState.isDistributionSort()) return;
        // 배분(Distribution) 다단은 모든 단을 균등 높이로 나눈다 — 마지막 블록도 배분한다.
        if (colState.isDistributionSort()) balanceLastBlock = true;

        int endIdx = blockStartIdx + 1;
        while (endIdx < count) {
            Paragraph p = section.getParagraph(endIdx);
            if (findColumnDefine(p) != null) break;
            // 배분 다단의 단나누기는 블록을 종료하고 새 블록을 재시작한다.
            if (colState.isDistributionSort() && hasDivideColumn(p)) break;
            endIdx++;
        }
        // 본문 신문식: 다음 ColumnDefine으로 종료되는 블록만 균형 배분(마지막 블록은 신문식).
        // 컨트롤 안·배분 다단: 마지막 블록도 균형 배분한다(balanceLastBlock=true).
        if (!balanceLastBlock && endIdx >= count) return;
        boolean blockHasGso = false;
        boolean blockHasTakePlace = false;
        for (int i = blockStartIdx; i < endIdx; i++) {
            Paragraph p = section.getParagraph(i);
            if (hasLineSeg(p)) return;       // LineSeg 경로가 처리
            if (hasTableControl(p)) return;  // 표 높이는 측정에 반영 못함
            if (hasFlowAffectingGso(p)) blockHasGso = true;
            if (hasTakePlaceGso(p)) blockHasTakePlace = true;
        }

        int n = colState.columnCount();
        long pageBottom = ctx.pageBodyArea().bottom();
        // 블록이 시작하는 첫(현재) 페이지의 단 하단. 페이지 분할 셀에서 첫 페이지
        // 가용높이(firstAvail)는 후속 페이지(restAvail=pageBottom)보다 작다. 첫 페이지
        // 초과 판정·단일페이지 배분은 이 값을 기준으로 한다. 비분할에서는 pageBottom과 동일.
        long firstPageBottom = colState.currentArea().bottom();
        long[] bottoms;
        if (colState.isUniformColumnWidth()) {
            long blockTop = colState.columnTop();
            long pageTop = ctx.pageBodyArea().top();
            // 자리차지(TakePlace) 컨트롤 블록: 컨트롤은 단 안에서 본문을 아래로 밀어낸다.
            // 각 단의 (고정 위치) 컨트롤을 고려해 TextRow 높이가 최소가 되는 줄 분배를 찾는다.
            if (blockHasTakePlace) {
                long[] b = balanceTakePlaceControlBlock(section, blockStartIdx, endIdx, ctx, colState);
                if (b != null) {
                    colState.setColumnBottoms(b);
                    ctx.bodyArea(colState.currentArea().clone());
                    return;
                }
                // 실패 시 아래 문단경계 배분으로 폴백.
            }
            // 어울림(FitWithText) 컨트롤 블록: 본문이 컨트롤 주위로 래핑되므로 문단을
            // 단 사이로 쪼개면 컨트롤 배치가 어긋난다. 단 경계를 문단 경계로 잡는다.
            if (blockHasGso) {
                List<Long> paraTops = measureBlockParaTops(section, blockStartIdx, endIdx,
                        colState.columnWidth(), ctx);
                if (paraTops.size() < 2) return;
                long[] b = simulateBalance(paraTops, n, blockTop, endIdx - blockStartIdx,
                        colState.isDistributionSort());
                for (int i = 0; i < n - 1; i++) {
                    if (b[i] >= firstPageBottom) return;  // 첫 페이지 초과 → 신문식 유지
                }
                colState.setColumnBottoms(b);
                ctx.bodyArea(colState.currentArea().clone());
                return;
            }
            // 배분 다단 블록 안에 강제 페이지나누기가 있으면 페이지별 구획으로 나눠
            // 각 구획을 독립적으로 단 배분한다.
            if (colState.isDistributionSort()) {
                List<int[]> segs = splitSegmentsByPageBreak(section, blockStartIdx, endIdx);
                if (segs.size() > 1) {
                    for (int s = 0; s < segs.size(); s++) {
                        int[] seg = segs.get(s);
                        List<Long> segTops = measureBlockLineTops(section, seg[0], seg[1],
                                colState.columnWidth(), ctx);
                        if (segTops.size() < 2) continue;
                        long segTop = (s == 0) ? blockTop : pageTop;
                        long[] segBottoms = simulateBalance(segTops, n, segTop,
                                seg[1] - seg[0], true);
                        if (s == 0) {
                            colState.setColumnBottoms(segBottoms);
                        } else {
                            colState.enqueuePendingBalance(s, segBottoms);
                        }
                    }
                    ctx.bodyArea(colState.currentArea().clone());
                    return;
                }
            }
            List<Long> tops = measureBlockLineTops(section, blockStartIdx, endIdx,
                    colState.columnWidth(), ctx);
            if (tops.size() < 2) return;  // 줄 없음
            long totalH = tops.get(tops.size() - 1) - tops.get(0);
            long firstPageCap = (long) n * (firstPageBottom - blockTop);
            // 균형 배분 대상 블록(배분 다단의 모든 블록, 신문식의 비-마지막 블록)이
            // 한 페이지를 초과하면 페이지별로 나누고 마지막 페이지의 잔여 콘텐츠만 단
            // 배분한다(앞 페이지들은 신문식으로 꽉 참). 여기까지 왔다는 것은 이미
            // 게이트를 통과해 이 블록을 균형 배분한다는 의미다.
            if (totalH > firstPageCap) {
                applyDistributionPageSplit(tops, n, blockTop, pageTop, firstPageBottom, pageBottom,
                        endIdx - blockStartIdx, colState, colState.isDistributionSort());
                ctx.bodyArea(colState.currentArea().clone());
                return;
            }
            bottoms = simulateBalance(tops, n, blockTop, endIdx - blockStartIdx,
                    colState.isDistributionSort());
        } else {
            // 폭이 다른 단 — 실제 단별 폭 시뮬레이션으로 균형 배분.
            bottoms = balanceDifferentWidths(section, blockStartIdx, endIdx, ctx, colState);
            if (bottoms == null) return;  // 균형 배분 불가 → 신문식 유지
        }
        for (int i = 0; i < n - 1; i++) {
            if (bottoms[i] >= firstPageBottom) return;  // 첫 페이지 초과 → 신문식 유지
        }
        colState.setColumnBottoms(bottoms);
        ctx.bodyArea(colState.currentArea().clone());
    }

    /**
     * 한 페이지를 초과하는 배분(Distribution) 다단 블록을 페이지별로 나눈다.
     *
     * <p>줄 top 목록을 신문식으로 페이지·단에 채워 마지막 페이지의 시작 줄을 찾는다.
     * 앞 페이지들은 단이 꽉 차므로 신문식 그대로 두고, 마지막 페이지의 잔여 콘텐츠만
     * 단 배분한다. 마지막 페이지 배분은 그 페이지에 도달했을 때(refreshForNewPage)
     * 적용되도록 ColumnState에 예약(pending)한다.</p>
     */
    private void applyDistributionPageSplit(List<Long> tops, int n, long blockTop,
                                            long pageTop, long firstPageBottom, long pageBottom,
                                            int paraCount, ColumnState colState, boolean distribute) {
        int lineCount = tops.size() - 1;
        // 첫 페이지 단 높이는 firstPageBottom(분할 셀의 firstAvail), 후속 페이지는 pageBottom(rest).
        long h0 = firstPageBottom - blockTop;
        long hp = pageBottom - pageTop;
        int lineIdx = 0;
        int pageIndex = 0;
        int lastPageStartLine = 0;
        int lastPageIndex = 0;
        while (lineIdx < lineCount) {
            long hc = (pageIndex == 0) ? h0 : hp;
            int pageStartLine = lineIdx;
            for (int c = 0; c < n && lineIdx < lineCount; c++) {
                int start = lineIdx;
                long colTop = tops.get(lineIdx);
                // 줄은 "글자 중점"이 단 하단을 넘지 않으면 단에 들어간다 — 실제
                // 레이아웃이 줄 bottom이 페이지를 약간 넘쳐도 배치하는 동작과 일치.
                while (lineIdx < lineCount) {
                    long lineTopOff = tops.get(lineIdx) - colTop;
                    long lineH = tops.get(lineIdx + 1) - tops.get(lineIdx);
                    if (lineTopOff + lineH / 2 > hc) break;
                    lineIdx++;
                }
                if (lineIdx == start) lineIdx++;  // 최소 한 줄은 넣어 무한루프 방지
            }
            lastPageStartLine = pageStartLine;
            lastPageIndex = pageIndex;
            pageIndex++;
        }
        if (lastPageIndex == 0) {
            // 한 페이지에 다 들어감 — 일반 배분.
            colState.setColumnBottoms(simulateBalance(tops, n, blockTop, paraCount, distribute));
            return;
        }
        // 마지막 페이지의 잔여 줄들만 배분 — 그 페이지 도달 시 적용되도록 예약.
        List<Long> lastTops = new ArrayList<>(tops.subList(lastPageStartLine, tops.size()));
        long[] bottoms = simulateBalance(lastTops, n, pageTop, paraCount, distribute);
        colState.enqueuePendingBalance(lastPageIndex, bottoms);
    }

    /**
     * 다단 블록[startIdx, endIdx)을 강제 페이지나누기 문단 경계로 구획화한다.
     * 각 구획은 {@code {start, end}} 쌍이며 한 페이지에 대응한다.
     */
    private static List<int[]> splitSegmentsByPageBreak(ParagraphListInterface section,
                                                        int startIdx, int endIdx) {
        List<int[]> segments = new ArrayList<>();
        int segStart = startIdx;
        for (int i = startIdx + 1; i < endIdx; i++) {
            Paragraph p = section.getParagraph(i);
            if (p.getHeader() != null && p.getHeader().getDivideSort() != null
                    && p.getHeader().getDivideSort().isDividePage()) {
                segments.add(new int[]{segStart, i});
                segStart = i;
            }
        }
        segments.add(new int[]{segStart, endIdx});
        return segments;
    }

    /**
     * 폭이 다른 단의 균형 배분.
     *
     * <p>다단 방향이 왼쪽부터일 때, 앞 단들(col0..col(n-2))을 <b>모두 같은 높이 H</b>로 채우고
     * 마지막 단이 나머지를 받는다. 단 높이가 <b>비증가</b>(col0 ≥ col1 ≥ … ≥ col(n-1))가 되는
     * — 즉 왼쪽 단이 오른쪽 단보다 작지 않은 — <b>가장 작은 H</b>를 찾아 TextRow 높이를
     * 최소화한다. 줄 수가 아닌 실제 렌더링 높이 기준이다.</p>
     *
     * @return 단별 하단 Y (마지막 단은 페이지 하단). 균형 배분 불가 시 {@code null}.
     */
    private long[] balanceDifferentWidths(ParagraphListInterface section, int startIdx, int endIdx,
                                          ParaLayoutContext ctx, ColumnState colState) {
        int n = colState.columnCount();
        long blockTop = colState.columnTop();
        long pageBottom = ctx.pageBodyArea().bottom();
        List<Area> base = new ArrayList<>();
        for (Area a : colState.columnAreas) {
            base.add(new Area(a.left(), blockTop, a.right(), pageBottom));
        }
        // col0를 무한 높이로 흘려 줄 top 들을 얻는다 — H 후보(앞 단들의 공통 높이).
        List<List<TextLine>> full = simulateMultiColumn(section, startIdx, endIdx,
                withBottoms(base, fill(n, pageBottom)), ctx);
        if (full == null || full.get(0).size() < 2) return null;
        List<TextLine> col0Lines = full.get(0);

        for (int k = 1; k < col0Lines.size(); k++) {
            long h = col0Lines.get(k).area().top() - blockTop;  // col0가 k줄일 때의 높이
            long[] cand = fill(n, pageBottom);
            for (int c = 0; c < n - 1; c++) cand[c] = blockTop + h;  // 앞 단들을 높이 H로
            List<List<TextLine>> r = simulateMultiColumn(section, startIdx, endIdx,
                    withBottoms(base, cand), ctx);
            if (r == null) continue;
            // 단 높이가 비증가면(왼쪽 ≥ 오른쪽) 채택 — 가장 작은 H.
            boolean nonIncreasing = true;
            long prev = Long.MAX_VALUE;
            for (int c = 0; c < n; c++) {
                long ch = columnHeight(r.get(c), base.get(c).top());
                if (ch > prev) { nonIncreasing = false; break; }
                prev = ch;
            }
            if (nonIncreasing) return cand;
        }
        return null;
    }

    /** 단 안의 TextLine 들로 단 높이(top 부터 가장 아래 줄 bottom 까지)를 잰다. */
    private static long columnHeight(List<TextLine> lines, long top) {
        long bottom = top;
        for (TextLine line : lines) bottom = Math.max(bottom, line.area().bottom());
        return bottom - top;
    }

    /** 길이 n, 모든 원소가 {@code v}인 배열. */
    private static long[] fill(int n, long v) {
        long[] a = new long[n];
        java.util.Arrays.fill(a, v);
        return a;
    }

    /** base 단 영역의 bottom 을 후보 값으로 교체한 새 영역 목록을 만든다. */
    private static List<Area> withBottoms(List<Area> base, long[] bottoms) {
        List<Area> cols = new ArrayList<>(base.size());
        for (int i = 0; i < base.size(); i++) {
            Area a = base.get(i);
            cols.add(new Area(a.left(), a.top(), a.right(), bottoms[i]));
        }
        return cols;
    }

    /**
     * 다단 블록을 주어진 단 영역(단별 bottom 포함)으로 직접 레이아웃하고
     * 단별 TextLine 목록(top 오름차순)을 반환한다. 균형 배분 후보 평가 전용.
     * 블록이 단 안에 다 들어가지 못해 다음 페이지로 넘치면 {@code null}을 반환한다.
     */
    private List<List<TextLine>> simulateMultiColumn(ParagraphListInterface section, int startIdx, int endIdx,
                                                     List<Area> columns, ParaLayoutContext refCtx) {
        Document measureDoc = new Document();
        ParaLayoutContext mc = newLayoutContext(measureDoc);
        mc.defaultTabGap(currentDefaultTabGap);
        mc.paperArea(refCtx.paperArea());
        mc.pageBodyArea(refCtx.pageBodyArea().clone());
        Page page = measureDoc.addPage();
        page.paperArea().copyFrom(refCtx.paperArea());
        page.bodyArea().copyFrom(refCtx.pageBodyArea());
        mc.currentPage(page);
        mc.bodyArea(columns.get(0).clone());
        mc.currentY(columns.get(0).top());

        int[] idx = {0};
        mc.columnAdvancer(() -> {
            if (idx[0] + 1 < columns.size()) {
                idx[0]++;
            } else {
                mc.createNewPage();   // 단 소진 → 새 페이지 (호출자가 페이지 수로 감지)
                idx[0] = 0;
                return true;
            }
            Area c = columns.get(idx[0]);
            mc.bodyArea(c.clone());
            mc.currentY(c.top());
            mc.lastLineSegVertPos(-1);
            return true;
        });

        for (int i = startIdx; i < endIdx; i++) {
            Paragraph para = section.getParagraph(i);
            ParaShape ps = reader.getParaShape(para);
            if (mc.currentY() > mc.bodyArea().bottom()) mc.tryNextColumn();
            mc.currentY(mc.currentY() + ParaShapeHelper.topSpace(ps));
            mc.currentParaTopY(mc.currentY());
            mc.multiColumnAreas(columns);
            mc.multiColumnIndex(idx[0]);
            long leftMargin = mc.bodyArea().left() + ParaShapeHelper.leftMargin(ps);
            long rightMargin = mc.bodyArea().right() - ParaShapeHelper.rightMargin(ps);
            columnSimLayouter.layout(para, i, ps, leftMargin, rightMargin,
                    ParaShapeHelper.indent(ps), ParaShapeHelper.alignment(ps), mc);
            mc.currentY(mc.currentY() + ParaShapeHelper.bottomSpace(ps));
            if (measureDoc.pageCount() > 1) return null;
        }

        List<List<TextLine>> byColumn = new ArrayList<>();
        for (int c = 0; c < columns.size(); c++) byColumn.add(new ArrayList<>());
        for (TextRow row : page.content().rows()) {
            for (TextColumn tc : row.columns()) {
                for (TextLine line : tc.textLines()) {
                    int c = columnIndexOf(line.area().left(), columns);
                    if (c >= 0) byColumn.get(c).add(line);
                }
            }
        }
        for (List<TextLine> lines : byColumn) {
            lines.sort((a, b) -> Long.compare(a.area().top(), b.area().top()));
        }
        return byColumn;
    }

    /** x 좌표가 속한 단 인덱스. 범위 밖이면 가장 가까운 단. */
    private static int columnIndexOf(long x, List<Area> columns) {
        for (int c = 0; c < columns.size(); c++) {
            if (x >= columns.get(c).left() - 1 && x < columns.get(c).right()) return c;
        }
        int best = 0;
        long bestD = Long.MAX_VALUE;
        for (int c = 0; c < columns.size(); c++) {
            long d = Math.abs(x - columns.get(c).left());
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    /**
     * 측정된 줄 top 목록으로 각 단의 하단 Y를 시뮬레이션한다.
     *
     * <p>여러 문단으로 이루어지고 줄 높이가 모두 같은(균일) 블록은 개수 기준 균등 배분 —
     * 앞 단들은 ⌈줄수/단수⌉줄, 마지막 단은 나머지 (문단 경계에서만 단이 나뉘므로).
     * 그 외(단일 문단이거나 높이가 제각각)는 높이 기준 — 각 단 목표 = 남은 높이/남은 단수,
     * "글자 중점"이 목표 이상이면 다음 단으로.</p>
     *
     * <p>배분(Distribution) 다단은 목표를 단별로 재계산하지 않고 <b>총 높이/단수</b>로 고정한다 —
     * 앞 단이 목표를 초과해도 뒤 단 목표가 줄지 않아 모든 단이 균등 높이에 수렴한다.</p>
     */
    private static long[] simulateBalance(List<Long> tops, int n, long blockTop, int paraCount,
                                          boolean distribute) {
        int lineCount = tops.size() - 1;
        long[] bottoms = new long[n];
        int i = 0;

        if (isUniformLineHeights(tops)) {
            int perColumn = (lineCount + n - 1) / n;  // ⌈줄수/단수⌉
            for (int c = 0; c < n; c++) {
                int start = i;
                i = (c == n - 1) ? lineCount : Math.min(lineCount, start + perColumn);
                bottoms[c] = blockTop + (tops.get(i) - tops.get(start));
            }
            return bottoms;
        }

        long totalH = tops.get(lineCount) - tops.get(0);
        long remaining = totalH;
        for (int c = 0; c < n; c++) {
            int start = i;
            if (c == n - 1) {
                i = lineCount;
            } else {
                long target = distribute ? totalH / n : remaining / (n - c);
                while (i < lineCount) {
                    long colSoFar = tops.get(i) - tops.get(start);
                    long lineHeight = tops.get(i + 1) - tops.get(i);
                    if (i > start && colSoFar + lineHeight / 2 >= target) break;
                    i++;
                }
            }
            long colHeight = tops.get(i) - tops.get(start);
            bottoms[c] = blockTop + colHeight;
            remaining -= colHeight;
        }
        return bottoms;
    }

    /** 블록의 줄 advance(top 간 간격)가 모두 같은지 검사한다 (마지막 줄 제외). */
    private static boolean isUniformLineHeights(List<Long> tops) {
        int lineCount = tops.size() - 1;
        if (lineCount < 3) return true;
        long h0 = tops.get(1) - tops.get(0);
        for (int k = 1; k < lineCount - 1; k++) {
            if (Math.abs((tops.get(k + 1) - tops.get(k)) - h0) > 50) return false;
        }
        return true;
    }

    /**
     * 블록 문단들을 한 단 폭의 긴 단에 레이아웃해 각 줄의 top Y 목록을 반환한다.
     * 마지막 원소는 총 높이(sentinel)다.
     */
    private List<Long> measureBlockLineTops(ParagraphListInterface section, int startIdx, int endIdx,
                                             long columnWidth, ParaLayoutContext refCtx) {
        return measureBlockLineTops(section, startIdx, endIdx, columnWidth, refCtx, true);
    }

    /**
     * @param applyControls true면 어울림/자리차지 컨트롤의 본문 밀어냄을 측정에 반영한다.
     *        자리차지 컨트롤 블록의 최소높이 배분은 순수 텍스트 줄이 필요하므로 false로 호출한다.
     */
    private List<Long> measureBlockLineTops(ParagraphListInterface section, int startIdx, int endIdx,
                                             long columnWidth, ParaLayoutContext refCtx,
                                             boolean applyControls) {
        Document measureDoc = new Document();
        ParaLayoutContext mc = newLayoutContext(measureDoc);
        mc.defaultTabGap(currentDefaultTabGap);
        Area col = new Area(0, 0, columnWidth, 1_000_000_000L);
        mc.paperArea(refCtx.paperArea());
        mc.bodyArea(col);
        mc.pageBodyArea(col.clone());
        Page page = measureDoc.addPage();
        page.paperArea().copyFrom(refCtx.paperArea());
        page.bodyArea().copyFrom(col);
        mc.currentPage(page);
        mc.currentY(0);

        for (int i = startIdx; i < endIdx; i++) {
            Paragraph para = section.getParagraph(i);
            ParaShape ps = reader.getParaShape(para);
            mc.currentY(mc.currentY() + ParaShapeHelper.topSpace(ps));
            mc.currentParaTopY(mc.currentY());
            // 어울림/자리차지 컨트롤이 본문을 밀어내는 만큼 측정에 반영한다.
            if (applyControls) controlLayouter.preRegisterObstacles(para, mc);
            layoutParagraph(para, i, ps, mc);
            if (applyControls) controlLayouter.layoutControls(para, i, mc, !hasLineSeg(para));
            mc.currentY(mc.currentY() + ParaShapeHelper.bottomSpace(ps));
        }

        List<Long> tops = new ArrayList<>();
        for (Page p : measureDoc.pages()) {
            for (TextRow row : p.content().rows()) {
                for (TextColumn c : row.columns()) {
                    for (TextLine line : c.textLines()) {
                        tops.add(line.area().top());
                    }
                }
            }
        }
        tops.add(mc.currentY());  // sentinel = 총 높이
        return tops;
    }

    /**
     * 블록 문단들을 한 단 폭의 긴 단에 레이아웃해 <b>문단별</b> 첫 줄 top Y 목록을
     * 반환한다. 마지막 원소는 총 높이(sentinel)다.
     *
     * <p>컨트롤이 든 블록의 균형 배분에 쓰인다 — 단 경계를 문단 경계로만 잡아
     * 컨트롤이 든 문단이 단 사이로 쪼개지지 않게 한다.</p>
     */
    private List<Long> measureBlockParaTops(ParagraphListInterface section, int startIdx, int endIdx,
                                             long columnWidth, ParaLayoutContext refCtx) {
        Document measureDoc = new Document();
        ParaLayoutContext mc = newLayoutContext(measureDoc);
        mc.defaultTabGap(currentDefaultTabGap);
        Area col = new Area(0, 0, columnWidth, 1_000_000_000L);
        mc.paperArea(refCtx.paperArea());
        mc.bodyArea(col);
        mc.pageBodyArea(col.clone());
        Page page = measureDoc.addPage();
        page.paperArea().copyFrom(refCtx.paperArea());
        page.bodyArea().copyFrom(col);
        mc.currentPage(page);
        mc.currentY(0);

        for (int i = startIdx; i < endIdx; i++) {
            Paragraph para = section.getParagraph(i);
            ParaShape ps = reader.getParaShape(para);
            mc.currentY(mc.currentY() + ParaShapeHelper.topSpace(ps));
            mc.currentParaTopY(mc.currentY());
            controlLayouter.preRegisterObstacles(para, mc);
            layoutParagraph(para, i, ps, mc);
            controlLayouter.layoutControls(para, i, mc, !hasLineSeg(para));
            mc.currentY(mc.currentY() + ParaShapeHelper.bottomSpace(ps));
        }

        // 문단별 첫 줄 top 수집.
        long[] firstTop = new long[endIdx - startIdx];
        java.util.Arrays.fill(firstTop, Long.MAX_VALUE);
        for (Page p : measureDoc.pages()) {
            for (TextRow row : p.content().rows()) {
                for (TextColumn c : row.columns()) {
                    for (TextLine line : c.textLines()) {
                        int pi = line.paraIndex() - startIdx;
                        if (pi >= 0 && pi < firstTop.length) {
                            firstTop[pi] = Math.min(firstTop[pi], line.area().top());
                        }
                    }
                }
            }
        }
        List<Long> paraTops = new ArrayList<>();
        for (long t : firstTop) {
            if (t != Long.MAX_VALUE) paraTops.add(t);
        }
        paraTops.add(mc.currentY());  // sentinel = 총 높이
        return paraTops;
    }

    /**
     * 자리차지(TakePlace) 컨트롤이 든 다단 블록을 균형 배분한다.
     *
     * <p>각 단의 컨트롤은 Paper 기준 고정 위치에서 본문을 아래로 밀어낸다. 순수 텍스트
     * 줄(컨트롤 미반영)을 측정한 뒤, 줄을 단별로 연속 분배하는 모든 경우 중 단별 콘텐츠
     * 하단의 최대값(= TextRow 높이)이 가장 작아지는 분배를 찾는다 — 사용자 규칙:
     * "컨트롤과의 관계를 고려해 TextRow가 최소 높이인 배치".</p>
     *
     * @return setColumnBottoms용 단별 bottom. 측정 불가/페이지 초과 시 {@code null}.
     */
    private long[] balanceTakePlaceControlBlock(ParagraphListInterface section, int blockStart,
                                                int endIdx, ParaLayoutContext ctx,
                                                ColumnState colState) {
        int n = colState.columnCount();
        long columnTop = colState.columnTop();
        long pageBottom = ctx.pageBodyArea().bottom();

        // 순수 텍스트 줄 — 자리차지는 줄 수를 바꾸지 않고 위치만 밀어낸다.
        List<Long> tops = measureBlockLineTops(section, blockStart, endIdx,
                colState.columnWidth(), ctx, false);
        int lineCount = tops.size() - 1;
        if (lineCount < 1) return null;
        long lineH = tops.get(1) - tops.get(0);
        if (lineH <= 0) return null;
        // 문단 시작 줄 인덱스 — 단 내부에서 문단 경계는 컨트롤 위쪽에서만 허용한다
        // (다른 문단의 줄이 이 단 컨트롤의 자리차지 이동을 받지 못해 겹치는 것을 막음).
        int[] paraStarts = pureTextParaStartLineIndices(section, blockStart, endIdx,
                colState.columnWidth(), ctx);

        // 단별 자리차지 컨트롤 밴드 [top, bottom] (바깥 여백 포함).
        long[] ctrlTop = new long[n];
        long[] ctrlBot = new long[n];
        java.util.Arrays.fill(ctrlTop, Long.MAX_VALUE);
        for (int i = blockStart; i < endIdx; i++) {
            Paragraph p = section.getParagraph(i);
            if (p.getControlList() == null) continue;
            for (Control c : p.getControlList()) {
                if (!(c instanceof GsoControl)) continue;
                CtrlHeaderGso h = ((GsoControl) c).getHeader();
                if (h.getProperty().getTextFlowMethod() != TextFlowMethod.TakePlace) continue;
                Area area = ControlAreaCalculator.calculate(h, ctx.paperArea(),
                        ctx.pageBodyArea(), ctx.pageBodyArea().clone(), columnTop);
                int col = columnIndexOf(area.left(), colState.columnAreas);
                if (col < 0 || col >= n) continue;
                ctrlTop[col] = Math.min(ctrlTop[col], area.top() - h.getOutterMarginTop());
                ctrlBot[col] = Math.max(ctrlBot[col], area.bottom() + h.getOutterMarginBottom());
            }
        }
        // 단별 "컨트롤 위에 들어가는 줄 수" — 줄 중점이 컨트롤 상단을 넘지 않으면 위.
        int[] linesAbove = new int[n];
        for (int c = 0; c < n; c++) {
            if (ctrlTop[c] == Long.MAX_VALUE) {
                linesAbove[c] = Integer.MAX_VALUE;
            } else {
                long avail = ctrlTop[c] - columnTop - lineH / 2;
                linesAbove[c] = (int) Math.max(0, avail / lineH + 1);
            }
        }

        long[] bestMax = {Long.MAX_VALUE};
        int[] best = new int[n];
        searchTakePlace(0, 0, 0L, n, lineCount, columnTop, lineH,
                ctrlTop, ctrlBot, linesAbove, paraStarts, new int[n], best, bestMax);
        if (bestMax[0] == Long.MAX_VALUE) return null;

        long[] bottoms = new long[n];
        for (int c = 0; c < n; c++) {
            bottoms[c] = takePlaceTextBottom(best[c], columnTop, lineH);
        }
        for (int c = 0; c < n - 1; c++) {
            if (bottoms[c] >= pageBottom) return null;  // 페이지 초과 → 신문식 유지
        }
        return bottoms;
    }

    /**
     * 자리차지 단의 텍스트 오버플로 하단 — setColumnBottoms용.
     *
     * <p>NoLineSeg 레이아웃은 줄을 <b>자리차지 이동 전</b> 위치로 흘리며 단 넘침을
     * 판정하고, 컨트롤 이동은 그 뒤에 적용된다. 따라서 단이 L줄을 담게 하려면 단
     * 영역 하단을 이동 전 L번째 줄 하단({@code columnTop + L*lineH})으로 설정해야 한다.</p>
     */
    private static long takePlaceTextBottom(int lines, long columnTop, long lineH) {
        return columnTop + (long) lines * lineH;
    }

    /** 자리차지 단의 시각적 콘텐츠 하단(컨트롤이 본문보다 아래로 튀어나오면 컨트롤 하단). */
    private static long takePlaceVisualBottom(int c, int lines, long columnTop, long lineH,
                                              long[] ctrlTop, long[] ctrlBot, int[] linesAbove) {
        if (ctrlTop[c] == Long.MAX_VALUE) {
            return columnTop + (long) lines * lineH;
        }
        if (lines <= linesAbove[c]) {
            return Math.max(columnTop + (long) lines * lineH, ctrlBot[c]);
        }
        return ctrlBot[c] + (long) (lines - linesAbove[c]) * lineH;
    }

    /** 줄을 단별로 연속 분배하며 TextRow 높이(단별 시각 하단의 최대)를 최소화한다. */
    private void searchTakePlace(int col, int linesUsed, long curMax, int n, int lineCount,
                                 long columnTop, long lineH, long[] ctrlTop, long[] ctrlBot,
                                 int[] linesAbove, int[] paraStarts, int[] cur,
                                 int[] best, long[] bestMax) {
        if (col == n - 1) {
            int cnt = lineCount - linesUsed;
            if (cnt < 0 || !takePlaceColumnValid(linesUsed, cnt, linesAbove[col], paraStarts)) {
                return;
            }
            cur[col] = cnt;
            long m = Math.max(curMax, takePlaceVisualBottom(col, cnt, columnTop, lineH,
                    ctrlTop, ctrlBot, linesAbove));
            if (m < bestMax[0]) {
                bestMax[0] = m;
                System.arraycopy(cur, 0, best, 0, n);
            }
            return;
        }
        for (int cnt = 0; linesUsed + cnt <= lineCount; cnt++) {
            if (!takePlaceColumnValid(linesUsed, cnt, linesAbove[col], paraStarts)) continue;
            cur[col] = cnt;
            long m = Math.max(curMax, takePlaceVisualBottom(col, cnt, columnTop, lineH,
                    ctrlTop, ctrlBot, linesAbove));
            if (m >= bestMax[0]) continue;  // 이 단이 이미 최선 이상 — 더 늘려도 개선 없음
            searchTakePlace(col + 1, linesUsed + cnt, m, n, lineCount, columnTop, lineH,
                    ctrlTop, ctrlBot, linesAbove, paraStarts, cur, best, bestMax);
        }
    }

    /**
     * 단[start, start+cnt) 배치가 유효한지 — 단 내부의 문단 경계는 컨트롤 위쪽
     * (linesAbove 줄 이내)에서만 허용한다. 컨트롤 아래에 다른 문단의 줄이 오면
     * 그 줄은 이 단 컨트롤의 자리차지 이동을 받지 못해 겹쳐 그려진다.
     */
    private static boolean takePlaceColumnValid(int start, int cnt, int linesAbove, int[] paraStarts) {
        if (linesAbove == Integer.MAX_VALUE) return true;   // 컨트롤 없는 단
        for (int b : paraStarts) {
            if (b > start && b < start + cnt && b - start > linesAbove) return false;
        }
        return true;
    }

    /** 블록을 한 단 폭에 순수 텍스트로 흘려 각 문단이 시작하는 줄 인덱스를 구한다. */
    private int[] pureTextParaStartLineIndices(ParagraphListInterface section, int startIdx,
                                               int endIdx, long columnWidth,
                                               ParaLayoutContext refCtx) {
        Document measureDoc = new Document();
        ParaLayoutContext mc = newLayoutContext(measureDoc);
        mc.defaultTabGap(currentDefaultTabGap);
        Area col = new Area(0, 0, columnWidth, 1_000_000_000L);
        mc.paperArea(refCtx.paperArea());
        mc.bodyArea(col);
        mc.pageBodyArea(col.clone());
        Page page = measureDoc.addPage();
        page.paperArea().copyFrom(refCtx.paperArea());
        page.bodyArea().copyFrom(col);
        mc.currentPage(page);
        mc.currentY(0);
        for (int i = startIdx; i < endIdx; i++) {
            Paragraph para = section.getParagraph(i);
            ParaShape ps = reader.getParaShape(para);
            mc.currentY(mc.currentY() + ParaShapeHelper.topSpace(ps));
            mc.currentParaTopY(mc.currentY());
            layoutParagraph(para, i, ps, mc);
            mc.currentY(mc.currentY() + ParaShapeHelper.bottomSpace(ps));
        }
        List<Integer> starts = new ArrayList<>();
        int lineIdx = 0;
        int prevPara = Integer.MIN_VALUE;
        for (Page p : measureDoc.pages()) {
            for (TextRow row : p.content().rows()) {
                for (TextColumn c : row.columns()) {
                    for (TextLine line : c.textLines()) {
                        if (line.paraIndex() != prevPara) {
                            starts.add(lineIdx);
                            prevPara = line.paraIndex();
                        }
                        lineIdx++;
                    }
                }
            }
        }
        int[] r = new int[starts.size()];
        for (int i = 0; i < r.length; i++) r[i] = starts.get(i);
        return r;
    }

    /** 문단이 GSO(글상자/이미지)나 표 컨트롤을 가지는지 검사. */
    private static boolean hasGsoOrTableControl(Paragraph para) {
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (c instanceof GsoControl || c instanceof ControlTable) return true;
        }
        return false;
    }

    /**
     * 문단이 표 컨트롤을 가지는지 검사한다.
     *
     * <p>표는 높이를 {@code measureBlockLineTops}에서 측정에 반영하지 못하므로
     * 표가 있는 블록은 균형 배분을 건너뛴다. Gso(글상자/이미지) 컨트롤은
     * 어울림·자리차지여도 measureBlockLineTops가 obstacle/자리차지 이동을
     * 반영해 측정하므로 균형 배분을 진행한다.</p>
     */
    private static boolean hasTableControl(Paragraph para) {
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (c instanceof ControlTable) return true;
        }
        return false;
    }

    /**
     * 문단이 본문 흐름을 밀어내는 Gso 컨트롤(어울림·자리차지)을 가지는지 검사한다.
     * 글앞으로·뒤로는 본문과 무관하게 겹쳐 그려지므로 제외한다.
     */
    private static boolean hasFlowAffectingGso(Paragraph para) {
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (c instanceof GsoControl) {
                TextFlowMethod tfm = ((GsoControl) c).getHeader().getProperty().getTextFlowMethod();
                if (tfm == TextFlowMethod.FitWithText || tfm == TextFlowMethod.TakePlace) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 문단이 자리차지(TakePlace) Gso 컨트롤을 가지는지 검사한다. */
    private static boolean hasTakePlaceGso(Paragraph para) {
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (c instanceof GsoControl
                    && ((GsoControl) c).getHeader().getProperty().getTextFlowMethod()
                       == TextFlowMethod.TakePlace) {
                return true;
            }
        }
        return false;
    }

    // ───────────────────────── 평행 다단 ─────────────────────────

    /** ControlColumnDefine이 평행(Parallel) 다단인지 검사. */
    private static boolean isParallelDefine(ControlColumnDefine cd) {
        return cd.getHeader().getProperty().getColumnSort()
                == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort.Parallel;
    }

    /**
     * 평행 다단 그룹 간 여백 (HWPUNIT). 새 그룹의 첫 줄 top = 직전 그룹 마지막 줄의
     * (lineVerticalPosition + lineHeight) + 이 값. 페이지넘어/2(30pt) 측정으로 확정.
     */
    private static final long PARALLEL_GROUP_GAP = 1080;

    /**
     * 평행 다단 블록의 진행 상태.
     *
     * <p>평행 다단은 각 단(column)이 서로 독립적으로 위→아래로 흐른다. 단나누기(divideColumn)로
     * 다음 단으로 넘어가며, 마지막 단 이후의 단나누기는 같은 페이지 아래에 새 "그룹"을 시작한다.
     * 한 단의 내용이 페이지를 넘치면 같은 단 위치를 유지한 채 다음 페이지로 이어진다.</p>
     */
    private static final class ParallelGroup {
        /** 각 단의 x 범위 (top/bottom 값은 사용하지 않음). */
        private final List<Area> baseAreas;
        private final Area pageBodyArea;
        private final int columnCount;
        /** 현재 그룹이 차지한 페이지들 (index 0 = 그룹 첫 페이지). */
        private final List<Page> groupPages = new ArrayList<>();
        /** 그룹 첫 페이지에서 단들이 시작하는 Y. */
        private long groupTop;
        private int currentColumn;
        /** 각 단이 끝난 그룹 페이지 인덱스. */
        private final int[] colEndPageIdx;
        /** 각 단이 끝난 Y (마지막 줄의 advance 포함 = currentY). */
        private final long[] colEndY;
        /** 각 단 마지막 줄의 lineSpace — 그룹 콘텐츠 bottom 계산에 사용. */
        private final long[] colEndGap;

        ParallelGroup(List<Area> baseAreas, Area pageBodyArea, int columnCount) {
            this.baseAreas = baseAreas;
            this.pageBodyArea = pageBodyArea;
            this.columnCount = columnCount;
            this.colEndPageIdx = new int[columnCount];
            this.colEndY = new long[columnCount];
            this.colEndGap = new long[columnCount];
        }

        /** 단 col을 top에서 시작하는 영역으로 만든다 (높이는 페이지 본문 하단까지). */
        Area columnArea(int col, long top) {
            Area b = baseAreas.get(col);
            return new Area(b.left(), top, b.right(), pageBodyArea.bottom());
        }
    }

    /**
     * 평행 다단 블록을 레이아웃한다. 블록은 startIdx의 ColumnDefine 문단부터 다음 ColumnDefine
     * 직전(또는 섹션 끝)까지이다.
     *
     * @return 블록 다음에 이어서 처리할 문단 인덱스
     */
    private int layoutParallelBlock(ParagraphListInterface paragraphs, int startIdx, ControlColumnDefine cd,
                                     ParaLayoutContext ctx, ColumnState outerColState) {
        int count = paragraphs.getParagraphCount();
        int columnCount = Math.max(1, cd.getHeader().getProperty().getColumnCount());
        Area pageBody = ctx.pageBodyArea();
        List<Area> baseAreas = ColumnState.calculateAreas(
                cd, pageBody, pageBody.top(), ctx.currentPage().pageNo());
        ParallelGroup g = new ParallelGroup(baseAreas, pageBody, columnCount);

        // 그룹 시작 위치 — 직전 블록의 bottom 아래. 직전 콘텐츠가 있으면 블록 간 여백을 둔다.
        // 블록 시작 여백은 직전 블록의 글자 크기 기준으로 계산한다 (applyColumnDefine과 동일).
        long startY = outerColState.columnSectionBottom(ctx.currentY());
        if (startY > pageBody.top()) {
            startY += gapFromMinFont(priorBlockMinFont(paragraphs, startIdx));
        }
        if (startY >= pageBody.bottom()) {
            ctx.createNewPage();
            startY = pageBody.top();
        }
        g.groupPages.add(ctx.currentPage());
        g.groupTop = startY;
        g.currentColumn = 0;
        setupParallelAdvancer(ctx, g);
        targetParallelColumn(ctx, g, 0, ctx.currentPage(), startY);

        int idx = startIdx;
        for (; idx < count; idx++) {
            Paragraph para = paragraphs.getParagraph(idx);
            if (idx > startIdx && findColumnDefine(para) != null) break;

            if (idx > startIdx && hasDivideColumn(para)) {
                finishParallelColumn(ctx, g);
                if (g.currentColumn < columnCount - 1) {
                    g.currentColumn++;
                    targetParallelColumn(ctx, g, g.currentColumn,
                            g.groupPages.get(0), g.groupTop);
                } else {
                    startNewParallelGroup(ctx, g);
                }
            }
            if (ctx.currentY() > ctx.bodyArea().bottom()) {
                ctx.tryNextColumn();
            }
            ParaShape paraShape = reader.getParaShape(para);
            // 평행 다단: 현재 단의 폭만 가진 단일 리스트로 multiColumnAreas를 설정해
            // LineSegSynthesizer의 ShadowColumnFlow가 페이지 넘침 시 같은 단 폭으로
            // 새 페이지의 같은 단 위치에 이어가도록 한다. null로 두면 shadow가 페이지 전환 시
            // 전체 페이지 본문으로 reset되어 텍스트가 col 0 위치로 잘못 이동한다.
            ctx.multiColumnAreas(java.util.Collections.singletonList(ctx.bodyArea().clone()));
            ctx.multiColumnIndex(0);
            ctx.multiColumnPageBottoms(null);
            layoutParagraph(para, idx, paraShape, ctx);
        }
        finishParallelColumn(ctx, g);
        moveToParallelGroupBottom(ctx, g);
        return idx;
    }

    /** 평행 다단용 columnAdvancer — 단이 페이지를 넘치면 같은 단 위치로 다음 페이지에 이어간다. */
    private void setupParallelAdvancer(ParaLayoutContext ctx, ParallelGroup g) {
        ctx.columnAdvancer(() -> {
            int gi = g.groupPages.indexOf(ctx.currentPage());
            Page next;
            if (gi >= 0 && gi + 1 < g.groupPages.size()) {
                next = g.groupPages.get(gi + 1);
            } else {
                next = ctx.createNewPage();
                g.groupPages.add(next);
            }
            targetParallelColumn(ctx, g, g.currentColumn, next, g.pageBodyArea.top());
            return true;
        });
    }

    /** ctx를 그룹 page의 단 col(top에서 시작)로 향하게 한다. */
    private static void targetParallelColumn(ParaLayoutContext ctx, ParallelGroup g,
                                              int col, Page page, long top) {
        ctx.currentPage(page);
        ctx.bodyArea(g.columnArea(col, top));
        ctx.currentY(top);
        ctx.lastLineSegVertPos(-1);
        ctx.obstacles().clear();
        // 평행 다단: 이 단(col)을 위한 column이 페이지에 없으면 새로 추가한다.
        // 단별로 독립된 column에 텍스트를 그려, 두 번째/세 번째 단의 첫 줄이 첫 번째
        // 단의 column에 겹쳐 들어가는 것을 막는다(예: page3에서 a10/a40/a60 겹침).
        TextRow row;
        if (page.content().rows().isEmpty()) {
            row = page.content().addRow();
        } else {
            row = page.content().rows().get(page.content().rows().size() - 1);
        }
        while (row.columns().size() <= col) {
            row.addColumn();
        }
    }

    /** 현재 단의 종료 위치(페이지 인덱스 + Y + 마지막 줄 lineSpace)를 기록한다. */
    private static void finishParallelColumn(ParaLayoutContext ctx, ParallelGroup g) {
        int gi = g.groupPages.indexOf(ctx.currentPage());
        g.colEndPageIdx[g.currentColumn] = Math.max(0, gi);
        g.colEndY[g.currentColumn] = ctx.currentY();
        g.colEndGap[g.currentColumn] = lastParallelLineGap(ctx);
    }

    /** 현재 페이지 마지막 TextLine의 lineSpace(lineGap). 줄이 없으면 0. */
    private static long lastParallelLineGap(ParaLayoutContext ctx) {
        if (ctx.currentPage().content().rows().isEmpty()) return 0L;
        List<TextLine> lines = ctx.currentColumn().textLines();
        return lines.isEmpty() ? 0L : lines.get(lines.size() - 1).lineGap();
    }

    /**
     * 현재 그룹의 가장 먼 페이지와, 그 페이지에서 가장 깊은 단을 찾아
     * {pageIdx, fullY(=advance 포함), contentY(=lineSpace 제외)} 로 반환한다.
     */
    private static long[] parallelGroupBottom(ParallelGroup g) {
        int maxPi = 0;
        for (int i = 0; i < g.columnCount; i++) {
            maxPi = Math.max(maxPi, g.colEndPageIdx[i]);
        }
        long fullY = g.pageBodyArea.top();
        long contentY = g.pageBodyArea.top();
        for (int i = 0; i < g.columnCount; i++) {
            if (g.colEndPageIdx[i] == maxPi && g.colEndY[i] > fullY) {
                fullY = g.colEndY[i];
                contentY = g.colEndY[i] - g.colEndGap[i];
            }
        }
        return new long[]{maxPi, fullY, contentY};
    }

    /** 평행 블록 직전 블록(이전 ColumnDefine까지)에 속한 문단들의 최소 글자 높이를 구한다. */
    private long priorBlockMinFont(ParagraphListInterface paragraphs, int parallelStartIdx) {
        long min = Long.MAX_VALUE;
        for (int i = parallelStartIdx - 1; i >= 0; i--) {
            Paragraph para = paragraphs.getParagraph(i);
            long base = firstCharShape(para).getBaseSize();
            if (base > 0 && base < min) min = base;
            if (findColumnDefine(para) != null) break;
        }
        return min == Long.MAX_VALUE ? 0L : min;
    }

    /**
     * 마지막 단 이후의 단나누기 — 현재 그룹 아래에 새 그룹을 시작한다.
     * 새 그룹 top = 직전 그룹 가장 깊은 단의 contentY(lineSpace 제외) + 그룹 여백.
     */
    private void startNewParallelGroup(ParaLayoutContext ctx, ParallelGroup g) {
        long[] bottom = parallelGroupBottom(g);
        Page newFirst = g.groupPages.get((int) bottom[0]);
        long newTop = bottom[2] + PARALLEL_GROUP_GAP;
        if (newTop >= g.pageBodyArea.bottom()) {
            newFirst = ctx.createNewPage();
            newTop = g.pageBodyArea.top();
        }

        g.groupPages.clear();
        g.groupPages.add(newFirst);
        g.groupTop = newTop;
        g.currentColumn = 0;
        Arrays.fill(g.colEndPageIdx, 0);
        Arrays.fill(g.colEndY, 0L);
        Arrays.fill(g.colEndGap, 0L);
        targetParallelColumn(ctx, g, 0, newFirst, newTop);
    }

    /** 블록 종료 후 ctx를 그룹의 bottom으로 이동해 후속 블록이 이어지게 한다. */
    private void moveToParallelGroupBottom(ParaLayoutContext ctx, ParallelGroup g) {
        long[] bottom = parallelGroupBottom(g);
        ctx.currentPage(g.groupPages.get((int) bottom[0]));
        ctx.bodyArea(g.pageBodyArea.clone());
        ctx.currentY(bottom[1]);
        ctx.lastLineSegVertPos(-1);
    }

    // ───────────────────────── 다단 상태 ─────────────────────────

    /**
     * 섹션 내 다단(Multi-column) 레이아웃 상태.
     * ControlColumnDefine 컨트롤로 단 구성이 변경될 때마다 업데이트된다.
     */
    private static final class ColumnState {
        /** ColumnDefine 없을 때 기본 1단 영역 */
        private List<Area> columnAreas;
        private int currentIndex;
        /** 맞쪽(Both) 방향 재계산을 위해 마지막으로 적용된 ColumnDefine 보관. */
        private ControlColumnDefine savedCd;
        /**
         * 현재 블록에서 overflow로 소진된 단들의 최대 bottom Y.
         * 다단 블록 전환 시 새 블록의 startY가 이 값보다 작아지지 않도록 보정한다.
         * 초기값 Long.MIN_VALUE는 "아직 소진된 단 없음"을 의미한다.
         */
        private long maxColumnBottom = Long.MIN_VALUE;
        /** 분리선 end_y 계산용: 소진된 단들의 마지막 줄 baseline 최대값. */
        private long maxColumnBaseline = Long.MIN_VALUE;
        /** 현재 섹션(페이지)에서 콘텐츠가 실제로 도달한 최대 단 인덱스. */
        private int maxUsedColumnIndex = 0;
        /**
         * 페이지를 넘는 배분 다단 블록의 후속 페이지별 단 배분.
         * index 0 = 다음 페이지에 적용, index 1 = 그 다음 페이지, …
         * 새 페이지 전환(refreshForNewPage)마다 맨 앞 항목을 꺼내 적용한다.
         */
        private final List<long[]> pendingBalances = new ArrayList<>();

        ColumnState(Area pageBodyArea) {
            this.columnAreas = singleColumn(pageBodyArea);
            this.currentIndex = 0;
        }

        /** overflow로 소진된 단의 bottom Y를 기록한다 (위치 계산용). */
        void recordColumnBottom(long y) {
            if (y > maxColumnBottom) maxColumnBottom = y;
        }

        /** 소진된 단의 마지막 줄 baseline을 기록한다 (분리선 end_y 계산용). */
        void recordColumnBaseline(long baseline) {
            if (baseline > maxColumnBaseline) maxColumnBaseline = baseline;
        }

        /** ControlColumnDefine에 따라 단 영역을 재계산한다. */
        void applyColumnDefine(ControlColumnDefine cd, Area pageBodyArea, long startY, int pageNo,
                               long gap, long prevMinFont) {
            // 이전 블록에서 overflow된 단이 있으면, 그 단의 bottom Y가 새 블록의 최소 startY다.
            // (예: 2단 블록에서 1단이 pageBottom까지 채워진 후 2단 중간에서 설정 변경 →
            //  새 블록은 pageBottom 이후, 즉 다음 페이지에서 시작해야 함)
            long effectiveStartY = maxColumnBottom > Long.MIN_VALUE
                    ? Math.max(startY, maxColumnBottom) : startY;
            effectiveStartY += gap;
            // 남은 공간이 직전 블록의 최소 글자 높이보다 작으면 다음 페이지에서 시작한다.
            // (이 경우 layoutSection의 bodyArea.top >= pageBodyArea.bottom 체크가 페이지 전환을 처리함)
            if (prevMinFont > 0 && effectiveStartY + prevMinFont > pageBodyArea.bottom()) {
                effectiveStartY = pageBodyArea.bottom();
            }
            this.maxColumnBottom = Long.MIN_VALUE;
            this.maxColumnBaseline = Long.MIN_VALUE;
            this.maxUsedColumnIndex = 0;
            this.savedCd = cd;
            this.columnAreas = calculateAreas(cd, pageBodyArea, effectiveStartY, pageNo);
            this.currentIndex = 0;
        }

        /** 새 페이지 시작 시 단 영역을 pageBodyArea.top()에서 재계산한다. */
        void refreshForNewPage(Area pageBodyArea, int pageNo) {
            this.maxColumnBottom = Long.MIN_VALUE;
            this.maxColumnBaseline = Long.MIN_VALUE;
            this.maxUsedColumnIndex = 0;
            if (columnAreas.size() <= 1) {
                this.columnAreas = singleColumn(pageBodyArea);
            } else if (savedCd != null && isBothDirection(savedCd)) {
                // 맞쪽(Both): 페이지 번호에 따라 단 순서를 재계산한다.
                this.columnAreas = calculateAreas(savedCd, pageBodyArea, pageBodyArea.top(), pageNo);
            } else {
                // 이전 ColumnDefine 설정을 재사용하되 top을 pageBodyArea.top()으로 리셋
                // (새 페이지에서는 단 높이가 pageBodyArea 전체)
                List<Area> updated = new ArrayList<>();
                for (Area a : columnAreas) {
                    updated.add(new Area(a.left(), pageBodyArea.top(), a.right(), pageBodyArea.bottom()));
                }
                this.columnAreas = updated;
            }
            this.currentIndex = 0;
            // 페이지 초과 배분 다단: 이 페이지에 적용할 예약 배분을 꺼내 적용.
            if (!pendingBalances.isEmpty()) {
                long[] b = pendingBalances.remove(0);
                if (b != null) setColumnBottoms(b);
            }
        }

        /**
         * 페이지 초과 배분 다단의 후속 페이지 단 배분을 예약한다.
         * @param pageOffset 1 = 다음 페이지, 2 = 그 다음 페이지, …
         */
        void enqueuePendingBalance(int pageOffset, long[] bottoms) {
            int idx = pageOffset - 1;
            while (pendingBalances.size() <= idx) pendingBalances.add(null);
            pendingBalances.set(idx, bottoms);
        }

        private static boolean isBothDirection(ControlColumnDefine cd) {
            return cd.getHeader().getProperty().getColumnDirection() ==
                    kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnDirection.Both;
        }

        Area currentArea() { return columnAreas.get(currentIndex); }
        boolean isMultiColumn() { return columnAreas.size() > 1; }
        /** Distribution 모드(배분 다단) 에서만 단나누기 시 같은 ColumnDefine 재시작이 적합. */
        boolean canRestartForDivideColumn() {
            if (savedCd == null || !isMultiColumn()) return false;
            return savedCd.getHeader().getProperty().getColumnSort()
                    == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort.Distribution;
        }

        /** 직전 ColumnDefine 설정으로 새 다단 블록을 재시작 (단나누기 처리용). */
        void restartWithSameDefine(Area pageBodyArea, long startY, int pageNo,
                                    long gap, long prevMinFont) {
            if (savedCd == null) return;
            applyColumnDefine(savedCd, pageBodyArea, startY, pageNo, gap, prevMinFont);
        }
        /** 모든 컬럼의 top을 dy 만큼 아래로 내린다 (빈 앵커 단락의 lineHeight 보정용). */
        void shiftColumnTops(long dy) {
            List<Area> updated = new ArrayList<>();
            for (Area a : columnAreas) {
                updated.add(new Area(a.left(), a.top() + dy, a.right(), a.bottom()));
            }
            this.columnAreas = updated;
        }
        /**
         * 균형 배분: 각 컬럼의 bottom을 컬럼별 지정 값으로 줄인다.
         * 마지막 컬럼은 나머지 콘텐츠를 흡수하도록 원래 하단(페이지 하단)을 유지한다.
         */
        void setColumnBottoms(long[] bottoms) {
            List<Area> updated = new ArrayList<>();
            for (int i = 0; i < columnAreas.size(); i++) {
                Area a = columnAreas.get(i);
                long b = (i < columnAreas.size() - 1 && i < bottoms.length) ? bottoms[i] : a.bottom();
                updated.add(new Area(a.left(), a.top(), a.right(), b));
            }
            this.columnAreas = updated;
        }
        long columnTop() { return columnAreas.get(0).top(); }
        long columnWidth() { return columnAreas.get(0).width(); }
        int columnCount() { return columnAreas.size(); }
        /** 모든 단의 폭이 같은지 검사. */
        boolean isUniformColumnWidth() {
            long w0 = columnAreas.get(0).width();
            for (Area a : columnAreas) {
                if (Math.abs(a.width() - w0) > 50) return false;
            }
            return true;
        }
        boolean isNormalSort() {
            return savedCd != null && savedCd.getHeader().getProperty().getColumnSort()
                    == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort.Normal;
        }
        boolean isDistributionSort() {
            return savedCd != null && savedCd.getHeader().getProperty().getColumnSort()
                    == kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort.Distribution;
        }
        boolean isLastColumn() { return currentIndex == columnAreas.size() - 1; }
        void nextColumn() { currentIndex++; maxUsedColumnIndex = currentIndex; }

        /** 현재 섹션의 실제 콘텐츠 bottom Y. 소진된 단의 최대값과 currentY 중 큰 값. */
        long columnSectionBottom(long currentY) {
            return maxColumnBottom > Long.MIN_VALUE ? Math.max(maxColumnBottom, currentY) : currentY;
        }

        /** 분리선 end_y: 각 단 마지막 줄 baseline의 최대값과 현재 단 baseline 중 큰 값. */
        long columnSectionBaseline(long currentBaseline) {
            return maxColumnBaseline > Long.MIN_VALUE
                    ? Math.max(maxColumnBaseline, currentBaseline) : currentBaseline;
        }

        private static List<Area> singleColumn(Area bodyArea) {
            List<Area> list = new ArrayList<>();
            list.add(bodyArea.clone());
            return list;
        }

        /** ControlColumnDefine에서 각 단의 영역(Area) 목록을 계산한다. */
        static List<Area> calculateAreas(ControlColumnDefine cd, Area base, long startY, int pageNo) {
            CtrlHeaderColumnDefine h = cd.getHeader();
            int count = h.getProperty().getColumnCount();
            long bottom = base.bottom();

            List<Area> areas = new ArrayList<>();
            if (count <= 1) {
                areas.add(new Area(base.left(), startY, base.right(), bottom));
                return areas;
            }

            if (h.getProperty().isSameWidth()) {
                long gap = h.getGapBetweenColumn();
                long colWidth = (base.width() - gap * (count - 1)) / count;
                long x = columnStartX(base, h, count, colWidth, gap, pageNo);
                int dir = columnDirection(h, pageNo);
                for (int i = 0; i < count; i++) {
                    areas.add(new Area(x, startY, x + colWidth, bottom));
                    x += dir * (colWidth + gap);
                }
            } else {
                List<ColumnInfo> infos = h.getColumnInfoList();
                long sumWidthGap = 0;
                for (ColumnInfo ci : infos) sumWidthGap += ci.getWidth() + ci.getGap();
                float rate = sumWidthGap > 0 ? (float) base.width() / sumWidthGap : 1f;
                // FromRight / Both(짝수 페이지) 방향은 오른쪽에서 역방향으로 배치
                boolean fromRight = isEffectivelyFromRight(h, pageNo);
                long x = fromRight ? base.right() : base.left();
                for (ColumnInfo ci : infos) {
                    long colWidth = (long) (ci.getWidth() * rate);
                    long colGap  = (long) (ci.getGap()   * rate);
                    if (fromRight) {
                        x -= colWidth;
                        areas.add(new Area(x, startY, x + colWidth, bottom));
                        x -= colGap;
                    } else {
                        areas.add(new Area(x, startY, x + colWidth, bottom));
                        x += colWidth + colGap;
                    }
                }
            }
            return areas;
        }

        /** sameWidth 단의 시작 X. FromRight / Both(짝수)이면 오른쪽에서 역방향으로 시작. */
        private static long columnStartX(Area base, CtrlHeaderColumnDefine h,
                                          int count, long colWidth, long gap, int pageNo) {
            return isEffectivelyFromRight(h, pageNo) ? base.right() - colWidth : base.left();
        }

        /** sameWidth 단 배치 방향. 오른쪽부터=−1, 왼쪽부터=+1. */
        private static int columnDirection(CtrlHeaderColumnDefine h, int pageNo) {
            return isEffectivelyFromRight(h, pageNo) ? -1 : +1;
        }

        /** Both(맞쪽)는 짝수 페이지에서 오른쪽부터, FromRight는 항상 오른쪽부터. */
        private static boolean isEffectivelyFromRight(CtrlHeaderColumnDefine h, int pageNo) {
            switch (h.getProperty().getColumnDirection()) {
                case FromRight: return true;
                case Both:      return pageNo % 2 == 0;  // 짝수 페이지 = 역방향
                default:        return false;
            }
        }
    }

    /**
     * 텍스트박스 / 표 셀의 내부 ParagraphList를 레이아웃한다.
     * ControlLayouter.InnerLayouter 콜백으로 사용.
     */
    Document layoutParagraphList(ParagraphList paragraphs, Area innerArea, Area pageBodyArea,
                                 Area paperArea, boolean balanceLastBlock) {
        Document innerDoc = new Document();
        ParaLayoutContext innerCtx = newLayoutContext(innerDoc);
        innerCtx.innerDocLayout(true);
        innerCtx.defaultTabGap(currentDefaultTabGap);
        innerCtx.paperArea(paperArea != null ? paperArea : currentPaperArea);
        innerCtx.bodyArea(innerArea);
        innerCtx.pageBodyArea(pageBodyArea.clone());
        innerCtx.createNewPage();
        innerCtx.currentPage().paperArea().copyFrom(innerCtx.paperArea());
        innerCtx.currentPage().bodyArea().copyFrom(innerArea);
        innerCtx.currentY(innerArea.top());

        ColumnState innerColState = new ColumnState(innerArea);
        setupColumnAdvancer(innerCtx, innerColState);

        Page innerSectionPage = innerCtx.currentPage();
        int innerSectionStartLineIdx = 0;

        int count = paragraphs.getParagraphCount();
        for (int paraIdx = 0; paraIdx < count; paraIdx++) {
            Paragraph para = paragraphs.getParagraph(paraIdx);

            ControlColumnDefine colDef = findColumnDefine(para);
            if (colDef != null && isParallelDefine(colDef)) {
                // 셀/글상자 안의 평행 다단 — 메인 섹션과 같은 전용 경로로 위임한다.
                int nextIdx = layoutParallelBlock(paragraphs, paraIdx, colDef, innerCtx, innerColState);
                setupColumnAdvancer(innerCtx, innerColState);
                innerSectionPage = innerCtx.currentPage();
                innerSectionStartLineIdx = currentColumnLineCount(innerCtx);
                paraIdx = nextIdx - 1;
                continue;
            }
            if (colDef != null) {
                long prevMinFont = sectionMinFont(innerCtx, innerSectionPage, innerSectionStartLineIdx);
                long gap = gapFromMinFont(prevMinFont);
                // 단 영역의 하단은 "블록이 시작하는 현재 페이지"의 본문 하단을 쓴다.
                // 페이지 분할 셀에서 첫 페이지(first)의 가용높이는 후속 페이지(rest=pageBodyArea)
                // 보다 작으므로, pageBodyArea(rest)를 쓰면 첫 페이지 단이 과도하게 길어져
                // 신문식 흐름이 단별로 페이지를 가로지른 뒤 다음 단으로 가는(평행다단처럼)
                // 잘못된 결과가 된다. 비분할(글상자·메인섹션)에서는 둘이 동일.
                Area blockPageBody = innerCtx.currentPage().bodyArea();
                innerColState.applyColumnDefine(colDef, blockPageBody, innerCtx.currentY(), 1, gap, prevMinFont);
                innerSectionPage = innerCtx.currentPage();
                innerSectionStartLineIdx = currentColumnLineCount(innerCtx);
                innerCtx.bodyArea(innerColState.currentArea().clone());
                if (innerCtx.bodyArea().top() > innerCtx.currentY()) {
                    innerCtx.currentY(innerCtx.bodyArea().top());
                }
                innerCtx.lastLineSegVertPos(-1);
                // 컨트롤(글상자 등) 안의 다단 균형 배분.
                // balanceLastBlock=true면 마지막 블록도 최소화(컨트롤이 내용에 맞춰 줄어듦),
                // false면 마지막 블록은 신문식으로 컨트롤(페이지처럼) 영역을 채운다.
                applyColumnBalanceIfNeeded(paragraphs, paraIdx, count, innerCtx,
                        innerColState, balanceLastBlock);
            }

            if (shouldSkipFirstEmptyParaInInnerDoc(paraIdx, para, paragraphs)) continue;
            advanceToNextColumnOrNewPageIfOverflow(innerCtx, innerColState);

            ParaShape paraShape = reader.getParaShape(para);
            innerCtx.currentY(innerCtx.currentY() + ParaShapeHelper.topSpace(paraShape));
            innerCtx.currentParaTopY(innerCtx.currentY());
            // 셀/글상자 내부 단락에 mid-paragraph 로 앵커된 어울림(FitWithText) 컨트롤이 있으면
            // throwaway 레이아웃으로 컨트롤 anchor 줄을 찾아 obstacle 을 그 줄 위치에 등록한다.
            // (단락 최상단이 아니라 컨트롤이 실제로 그려지는 줄에 맞춰 본문이 래핑되도록.)
            // 앞쪽 박스의 래핑이 뒤쪽 박스 anchor 줄을 아래로 밀므로, obstacle 을 적용한 채로
            // 재측정하기를 반복해 수렴시킨다(1회 추정이면 뒤쪽 박스가 어긋남).
            if (ControlLayouter.hasMidParagraphFitWithText(para)) {
                // 반복 추정으로 obstacle 위치를 수렴시킨다. 앞쪽 박스의 래핑이 줄을 늘려
                // 뒤쪽 박스 anchor 줄을 아래로 밀므로, obstacle 을 적용한 채 재측정하기를
                // 반복한다. 등록할 obstacle 과 실제 레이아웃 래핑이 일치(fixed point)하도록
                // 마지막 probe(수렴된 est 적용)에서 obstacle 을 등록한다.
                Document probe = probeParagraphLayout(para, paraIdx, paraShape, innerCtx, innerColState, null);
                for (int iter = 0; iter < 2; iter++) {
                    ParaObstacles est = new ParaObstacles();
                    controlLayouter.collectInnerFitWithTextObstacles(para, paraIdx, probe, innerCtx, est);
                    probe = probeParagraphLayout(para, paraIdx, paraShape, innerCtx, innerColState, est);
                }
                controlLayouter.registerInnerFitWithTextObstacles(para, paraIdx, probe, innerCtx);
            } else {
                controlLayouter.preRegisterObstacles(para, innerCtx);
            }
            innerCtx.multiColumnAreas(innerColState.columnAreas);
            innerCtx.multiColumnIndex(innerColState.currentIndex);
            // 페이지를 넘는 다단 블록(분할 셀의 신문식·배분): 후속 페이지의 단 배분을
            // NoLineSeg 합성기(ShadowColumnFlow)에 전달해 page2 단을 균형 배분하게 한다.
            innerCtx.multiColumnPageBottoms(innerColState.pendingBalances);
            layoutParagraph(para, paraIdx, paraShape, innerCtx);
            innerCtx.currentY(innerCtx.currentY() + ParaShapeHelper.bottomSpace(paraShape));

            // 내부 컨트롤(예: 셀 안에 컨트롤) 처리
            controlLayouter.layoutControls(para, paraIdx, innerCtx, !hasLineSeg(para));
        }
        controlLayouter.placeDeferredControls(innerCtx);
        return innerDoc;
    }

    /**
     * 단락을 obstacle 없이 throwaway 문서에 레이아웃해 반환한다 (anchor 줄 조회용).
     * 셀 내부 mid-paragraph 어울림 컨트롤의 obstacle Y 를 anchor 줄에 맞추는 2-pass 의 1차 패스.
     * innerCtx 의 현재 영역·Y·다단 상태를 그대로 미러링하되, 별도 Document 라 innerCtx 에
     * 영향을 주지 않는다.
     */
    private Document probeParagraphLayout(Paragraph para, int paraIdx, ParaShape paraShape,
                                          ParaLayoutContext innerCtx, ColumnState innerColState,
                                          ParaObstacles applyObstacles) {
        Document probeDoc = new Document();
        ParaLayoutContext probe = newLayoutContext(probeDoc);
        probe.innerDocLayout(true);
        probe.paperArea(innerCtx.paperArea());
        probe.bodyArea(innerCtx.bodyArea().clone());
        probe.pageBodyArea(innerCtx.pageBodyArea().clone());
        Page page = probeDoc.addPage();
        page.paperArea().copyFrom(innerCtx.paperArea());
        page.bodyArea().copyFrom(innerCtx.bodyArea());
        probe.currentPage(page);
        probe.currentY(innerCtx.currentY());
        probe.currentParaTopY(innerCtx.currentParaTopY());
        probe.multiColumnAreas(innerColState.columnAreas);
        probe.multiColumnIndex(innerColState.currentIndex);
        // 반복 추정: 이전 추정 obstacle 을 적용해 래핑된 줄 위치를 다시 측정한다.
        if (applyObstacles != null) probe.obstacles().copyFrom(applyObstacles);
        layoutParagraph(para, paraIdx, paraShape, probe);
        return probeDoc;
    }

    /**
     * 문서 순서로 미리 확정한 문단 번호. 문서당 한 번만 계산한다.
     * (번호는 앞 문단에 의존하는 누적 상태라 레이아웃 중에 세면 재배치 경로에서 중복 증가한다.)
     */
    private ParagraphNumberer paragraphNumberer;

    private ParagraphNumberer paragraphNumberer() {
        if (paragraphNumberer == null) {
            paragraphNumberer = ParagraphNumberer.of(
                    reader.hwpFile().getDocInfo(), reader.hwpFile().getBodyText().getSectionList());
        }
        return paragraphNumberer;
    }

    /**
     * ParaLayoutContext를 만든다. 표 셀·글상자 내부나 높이 측정용 컨텍스트도 반드시 이걸로
     * 만들어야 한다 — 직접 생성하면 문단 번호가 전달되지 않아 그 안의 번호 문단이 머리를 잃는다.
     */
    private ParaLayoutContext newLayoutContext(Document doc) {
        return new ParaLayoutContext(reader, measurer, doc).paragraphNumberer(paragraphNumberer());
    }

    private ParaLayoutContext createLayoutContext(PageDef pageDef) {
        Area paperArea = reader.getPaperArea(pageDef);
        Area bodyArea = reader.getBodyArea(pageDef);

        ParaLayoutContext ctx = newLayoutContext(document);
        ctx.paperArea(paperArea);
        ctx.bodyArea(bodyArea);
        ctx.pageBodyArea(bodyArea.clone());  // 다단에서 bodyArea가 변경되므로 원본 보관
        return ctx;
    }

    /** SectionDefine 컨트롤만 있는 첫 문단은 건너뛴다 (시각적으로 빈 내용). */
    private static boolean shouldSkipFirstEmptyPara(int paraIdx, Paragraph para) {
        if (paraIdx != 0) return false;
        return (para.getText() == null || para.getText().getCharList().isEmpty());
    }

    /**
     * 셀·글상자 등 내부 문서에서 첫 빈 문단을 건너뛸지 판단한다.
     *
     * <p>본문 섹션과 달리 내부 문서의 첫 문단은 SectionDefine 앵커가 아니라 사용자가
     * 실제로 넣은 빈 줄일 수 있다. 그 경우 한글은 그 빈 줄에도 한 줄 높이를 준다.
     * 어느 쪽인지는 <b>다음 문단의 LineSeg 세로 위치</b>가 알려준다 — 0 보다 크면
     * 한글이 첫 문단 아래에 다음 문단을 놓았다는 뜻이므로 건너뛰면 안 된다.
     * (실문서 R08 별첨1 셀: 첫 빈 문단 h=900 sp=720 → 다음 문단 y=1620.
     * 이 27px 만큼 셀 내용 전체가 위로 밀려 있었다.)</p>
     *
     * <p>반대로 빈 문단 하나뿐인 셀(표나누기/셀나누기/병합된셀·빈셀)은 비교할 다음
     * 문단이 없으므로 종전대로 건너뛴다 — 그 셀은 내용이 0줄이어도
     * {@code probeCellContentHeight} 가 {@code cellFontBaseSize}(글자 한 줄)로
     * 하한을 잡아 준다.</p>
     */
    private static boolean shouldSkipFirstEmptyParaInInnerDoc(int paraIdx, Paragraph para,
                                                              ParagraphList paragraphs) {
        if (!shouldSkipFirstEmptyPara(paraIdx, para)) return false;
        return !occupiesLineBeforeNextPara(para, paragraphs);
    }

    /**
     * 첫 문단이 한 줄을 차지하는지 판정한다.
     *
     * <p>LineSeg 가 있으면 <b>다음 문단의 세로 위치</b>가 답을 준다 — 0 보다 크면 한글이
     * 첫 문단 아래에 다음 문단을 놓았다는 뜻이다.</p>
     *
     * <p><b>LineSeg 가 없는 파일(_nolineseg)</b>은 그 판정을 할 수 없다. 이때는
     * <b>문단이 2개 이상이면 차지하는 것으로 본다</b> — 한글은 빈 줄에도 한 줄 높이를 주고,
     * 같은 문서의 LineSeg 판이 실제로 그렇게 되어 있다.
     * ({@code 컨트롤_nolineseg/표/일반} p2 위쪽 표: 빈 문단 2개짜리 셀의 내용 높이가
     * 2882 여야 하는데 1282 로 나와 행이 21/21/48 대신 30/30/30 으로 균등 분배됐다.)</p>
     *
     * <p>문단이 하나뿐인 셀(표나누기/병합된셀·빈셀)은 어느 경우든 비교 대상이 없어
     * 종전대로 건너뛴다 — 그 셀은 {@code probeCellContentHeight} 의
     * {@code cellFontBaseSize} 하한이 한 줄 높이를 보장한다.</p>
     *
     * <p>⚠ 이 판정을 넓히면 {@code ControlLayouter} 의 옛 {@code emptyCellFirstLinePadding}
     * 보정과 <b>이중 계상</b>이 된다. 그래서 그 보정은 2026-08-14 에 제거했다 —
     * 남겨 둔 채로 재면 {@code 표나누기_nolineseg/셀단위로나누기/페이지_보다_큰_셀} p2·p4 와
     * {@code 나누지않음/1} p3 가 100.0% → 95.7~97.8% 로 무너진다.</p>
     */
    private static boolean occupiesLineBeforeNextPara(Paragraph first, ParagraphList paragraphs) {
        if (paragraphs == null || paragraphs.getParagraphCount() < 2) return false;
        Paragraph next = paragraphs.getParagraph(1);
        if (!hasLineSeg(next)) {
            // LineSeg 가 아예 없는 파일이면 문단 수로 판정. 한쪽만 없는 비정상 상태는
            // 종전(건너뜀)을 유지해 LineSeg 파일의 동작을 바꾸지 않는다.
            return !hasLineSeg(first);
        }
        return next.getLineSeg().getLineSegItemList().get(0).getLineVerticalPosition() > 0;
    }

    /**
     * ControlColumnDefine 앵커 단락이 ColumnDefine 외의 다른 컨트롤이나 텍스트 없이
     * 순수 앵커 역할만 하는 빈 단락인지 검사.
     * — Normal 글자가 없어야 하고
     * — controlList에 ColumnDefine만 있어야 한다 (이미지/표 등 다른 컨트롤이 있으면 skip 불가).
     */
    private static boolean isEmptyAnchor(Paragraph para) {
        if (para.getText() == null) return false;
        for (HWPChar ch : para.getText().getCharList()) {
            if (ch.getType() == HWPCharType.Normal) return false;
        }
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (!(c instanceof ControlColumnDefine)) return false;
        }
        return true;
    }

    /**
     * 빈 앵커 단락이 차지할 라인의 lineHeight + lineSpace 합.
     * LineSeg가 있으면 그 값을, 없으면(NoLineSeg HWP) ParaShape 기준으로 계산한다
     * — addEmptyLine이 빈 줄을 그릴 때 사용하는 높이와 일치시킨다.
     */
    private long anchorLineHeight(Paragraph para) {
        ParaLineSeg ls = para.getLineSeg();
        if (ls != null && ls.getLineSegItemList() != null && !ls.getLineSegItemList().isEmpty()) {
            LineSegItem lsi = ls.getLineSegItemList().get(0);
            return lsi.getLineHeight() + lsi.getLineSpace();
        }
        ParaShape paraShape = reader.getParaShape(para);
        if (paraShape == null) return 0;
        long baseSize = firstCharShape(para).getBaseSize();
        return ParaShapeHelper.lineHeight(paraShape, 0, 0, false, baseSize);
    }

    /** 단락에 단나누기(divideColumn) 플래그가 설정되어 있는지 검사. */
    private static boolean hasDivideColumn(Paragraph para) {
        if (para.getHeader() == null || para.getHeader().getDivideSort() == null) return false;
        return para.getHeader().getDivideSort().isDivideColumn();
    }

    /**
     * 문단 헤더의 dividePage 플래그가 설정된 경우 (첫 문단 제외) 새 페이지 시작.
     * 다단 상태(colState/bodyArea)도 새 페이지의 col 1 으로 리셋한다.
     */
    private static void applyPageBreakIfRequested(Paragraph para, int paraIdx, ParaLayoutContext ctx,
                                                   ColumnState colState) {
        if (paraIdx == 0) return;
        if (para.getHeader() == null || para.getHeader().getDivideSort() == null) return;
        if (!para.getHeader().getDivideSort().isDividePage()) return;
        ctx.createNewPage();
        // 다단 상태도 새 페이지의 col 1 부터 시작하도록 리셋.
        colState.refreshForNewPage(ctx.pageBodyArea(), ctx.currentPage().pageNo());
        ctx.bodyArea(colState.currentArea().clone());
        ctx.currentY(ctx.bodyArea().top());
        ctx.lastLineSegVertPos(-1);
    }

    /** 다음 문단 시작 전 페이지 오버플로우 체크 (마지막 문단 이후의 빈 페이지 방지). */
    private static void advanceToNewPageIfOverflow(ParaLayoutContext ctx) {
        if (ctx.currentY() > ctx.bodyArea().bottom()) {
            ctx.createNewPage();
        }
    }

    /**
     * 빈 문단이 현 위치에서 한 줄을 추가할 경우 페이지 하단을 넘는지 검사한다.
     * (빈 줄 감추기 옵션 적용 시 사용 — 넘으면 그 문단을 건너뛴다.)
     */
    private boolean wouldOverflowAsEmptyLine(Paragraph para, ParaLayoutContext ctx) {
        ParaShape ps = reader.getParaShape(para);
        if (ps == null) return false;
        long topSpace = ParaShapeHelper.topSpace(ps);
        long baseSize = firstCharShape(para).getBaseSize();
        long lineHeight = ParaShapeHelper.lineHeight(ps, 0, 0, false, baseSize);
        return ctx.currentY() + topSpace + lineHeight > ctx.bodyArea().bottom();
    }

    // ───────────────────────── 문단 레이아웃 디스패치 ─────────────────────────

    /**
     * 한 문단을 레이아웃한다. 빈 문단과 내용 문단을 분기해 적절한 layouter에 위임한다.
     *
     * <p>"빈 문단"의 정의: Normal char가 하나도 없음 (빈 charList 또는 Control char만 있음).</p>
     */
    private void layoutParagraph(Paragraph para, int paraIdx, ParaShape paraShape,
                                  ParaLayoutContext ctx) {
        long leftMargin = ctx.bodyArea().left() + ParaShapeHelper.leftMargin(paraShape);
        long rightMargin = ctx.bodyArea().right() - ParaShapeHelper.rightMargin(paraShape);
        long indent = ParaShapeHelper.indent(paraShape);
        TextLine.Alignment alignment = ParaShapeHelper.alignment(paraShape);

        boolean isEmpty = isEmptyParagraph(para);
        boolean hasLineSeg = hasLineSeg(para);

        if (isEmpty) {
            layoutEmptyParagraph(para, paraIdx, paraShape, leftMargin, rightMargin,
                    indent, alignment, ctx, hasLineSeg);
            return;
        }

        // 셀단위로나누기(DivideByCell) 표를 가진 문단은 표 분할로 페이지 흐름이
        // 원본 LineSeg와 어긋나므로, LineSeg vertPos 기반 페이지 전환을 끈다.
        boolean suppressPageBreak = hasDivideByCellTable(para);

        if (hasLineSeg) {
            if (lineSegObserver != null) {
                List<LineSegItem> origItems = para.getLineSeg().getLineSegItemList();
                long shadowStartY = ctx.bodyArea().top() + origItems.get(0).getLineVerticalPosition();
                List<LineSegItem> synthesized = lineSegSynthesizer.synthesize(
                        para, paraIdx, paraShape, leftMargin, rightMargin, indent, alignment,
                        ctx, shadowStartY);
                lineSegObserver.onPair(paraIdx, para, origItems, synthesized);
            }
            withLineSegLayouter.layoutWithItems(para, paraIdx, paraShape,
                    leftMargin, rightMargin, indent, alignment, ctx,
                    para.getLineSeg().getLineSegItemList(), suppressPageBreak);
        } else {
            List<LineSegItem> synthesized = lineSegSynthesizer.synthesize(
                    para, paraIdx, paraShape, leftMargin, rightMargin, indent, alignment, ctx);
            if (lineSegObserver != null) {
                lineSegObserver.onPair(paraIdx, para, null, synthesized);
            }
            withLineSegLayouter.layoutWithItems(para, paraIdx, paraShape,
                    leftMargin, rightMargin, indent, alignment, ctx, synthesized, suppressPageBreak);
        }
    }

    /** 문단이 셀단위로나누기(DivideByCell) 속성의 표(글자처럼 아님)를 가지는지 검사. */
    private static boolean hasDivideByCellTable(Paragraph para) {
        if (para.getControlList() == null) return false;
        for (Control c : para.getControlList()) {
            if (!(c instanceof ControlTable)) continue;
            ControlTable t = (ControlTable) c;
            CtrlHeaderGso h = t.getHeader();
            if (h != null && !h.getProperty().isLikeWord()
                    && t.getTable().getProperty().getDivideAtPageBoundary()
                        == kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary.DivideByCell) {
                return true;
            }
        }
        return false;
    }

    private void layoutEmptyParagraph(Paragraph para, int paraIdx, ParaShape paraShape,
                                       long leftMargin, long rightMargin, long indent,
                                       TextLine.Alignment alignment, ParaLayoutContext ctx,
                                       boolean hasLineSeg) {
        // 빈 문단도 문단 테두리를 그리려면 TextLine이 하나 필요하다.
        //  - LineSeg가 있으면 WithLineSegLayouter에 위임 (정확한 Y/높이)
        //  - 없으면 한 줄짜리 빈 TextLine을 직접 생성
        if (hasLineSeg) {
            withLineSegLayouter.layout(para, paraIdx, paraShape,
                    leftMargin, rightMargin, indent, alignment, ctx);
        } else {
            addEmptyLine(para, paraIdx, paraShape, leftMargin, rightMargin, ctx);
        }
    }

    private static boolean isEmptyParagraph(Paragraph para) {
        ParaText paraText = para.getText();
        if (paraText == null || paraText.getCharList().isEmpty()) return true;
        if (hasAnyNormalChar(paraText)) return false;
        return !hasAnyLikeWordControl(para);
    }

    private static boolean hasAnyNormalChar(ParaText paraText) {
        if (paraText == null || paraText.getCharList() == null) return false;
        for (HWPChar c : paraText.getCharList()) {
            if (c.getType() == HWPCharType.Normal) return true;
        }
        return false;
    }

    private static boolean hasAnyLikeWordControl(Paragraph para) {
        ParaText paraText = para.getText();
        List<Control> controls = para.getControlList();
        if (paraText == null || controls == null || controls.isEmpty()) return false;
        int ctrlIdx = 0;
        for (HWPChar c : paraText.getCharList()) {
            if (c.getType() == HWPCharType.ControlExtend) {
                HWPCharControlExtend ce = (HWPCharControlExtend) c;
                if (ce.getCode() == 11 && ctrlIdx < controls.size()) {
                    CtrlHeaderGso header = gsoHeaderOf(controls.get(ctrlIdx));
                    if (header != null && header.getProperty().isLikeWord()) return true;
                }
                ctrlIdx++;
            }
        }
        return false;
    }

    private static CtrlHeaderGso gsoHeaderOf(Control ctrl) {
        switch (ctrl.getType()) {
            case Table: return ((ControlTable) ctrl).getHeader();
            case Gso:   return ((GsoControl) ctrl).getHeader();
            default:    return null;
        }
    }

    private static boolean hasLineSeg(Paragraph para) {
        ParaLineSeg lineSeg = para.getLineSeg();
        return lineSeg != null
                && lineSeg.getLineSegItemList() != null
                && !lineSeg.getLineSegItemList().isEmpty();
    }

    // ───────────────────────── 빈 문단 라인 생성 ─────────────────────────

    /**
     * 빈 문단에 한 줄짜리 빈 TextLine을 추가한다.
     *
     * <p>area top/bottom과 maxCharHeight를 채워서 다음 문단이 올바른 위치에 시작하도록 하고
     * 라인 카운트도 일치시킨다. 문단 테두리 정보도 세팅해서 빈 문단의 테두리가 그려지도록 한다.</p>
     */
    private void addEmptyLine(Paragraph para, int paraIdx, ParaShape paraShape,
                               long leftMargin, long rightMargin, ParaLayoutContext ctx) {
        long baseSize = firstCharShape(para).getBaseSize();
        long lineHeight = ParaShapeHelper.lineHeight(paraShape, 0, 0, false, baseSize);

        // 빈 줄이 현재 단/페이지를 넘칠 때만 다음 단(없으면 새 페이지)으로 이동한다.
        // (무조건 tryNextColumn하면 빈 문단마다 단이 강제 전진되는 버그.)
        // 시각적 텍스트(baseSize)가 페이지에 들어가면 fit 으로 판정 — 줄 간격(line gap)은
        // 페이지 밖으로 약간 나가도 보이지 않으므로 OK. 라인 전체(lineHeight=baseSize+gap)
        // 기준으로 검사하면 LineSeg 와 1줄 차이로 어긋남.
        if (ctx.currentY() + baseSize > ctx.bodyArea().bottom()) {
            if (!ctx.tryNextColumn()) {
                ctx.createNewPage();
            }
        }

        ctx.ensureRowAndColumn();
        TextLine emptyLine = ctx.currentColumn().addTextLine();
        emptyLine.paraIndex(paraIdx);
        emptyLine.alignment(ParaShapeHelper.alignment(paraShape));
        emptyLine.firstInPara(true);
        emptyLine.lastInPara(true);
        emptyLine.borderFillId(paraShape.getBorderFillId());
        emptyLine.paraLeft(leftMargin);
        emptyLine.paraRight(rightMargin);
        emptyLine.maxCharHeight(baseSize);
        emptyLine.maxCharBaseSize(baseSize * 17 / 20);
        emptyLine.maxCharDescent(baseSize - baseSize * 17 / 20);
        emptyLine.area().set(leftMargin, ctx.currentY(), rightMargin, ctx.currentY() + lineHeight);
        emptyLine.lineGap(Math.max(0, lineHeight - baseSize));
        emptyLine.addPart();

        ctx.currentY(ctx.currentY() + lineHeight);
    }

    private CharShape firstCharShape(Paragraph para) {
        int shapeId = 0;
        ParaCharShape pcs = para.getCharShape();
        if (pcs != null && pcs.getPositonShapeIdPairList() != null
                && !pcs.getPositonShapeIdPairList().isEmpty()) {
            shapeId = (int) pcs.getPositonShapeIdPairList().get(0).getShapeId();
        }
        return reader.hwpFile().getDocInfo().getCharShapeList().get(shapeId);
    }

    /** 직전 다단 블록의 최소 글자 높이(HWPUNIT)를 반환한다. 페이지가 바뀐 경우 0을 반환한다. */
    private static long sectionMinFont(ParaLayoutContext ctx, Page sectionPage, int fromIdx) {
        if (ctx.currentPage() != sectionPage) return 0L;
        if (ctx.currentPage().content().rows().isEmpty()) return 0L;
        List<TextLine> lines = ctx.currentColumn().textLines();
        long min = Long.MAX_VALUE;
        for (int i = fromIdx; i < lines.size(); i++) {
            long cs = lines.get(i).maxCharHeight();
            if (cs > 0 && cs < min) min = cs;
        }
        return min == Long.MAX_VALUE ? 0L : min;
    }

    /** 최소 글자 높이로부터 다단 블록 간 여백(HWPUNIT)을 계산한다. */
    private static long gapFromMinFont(long minFont) {
        if (minFont <= 0 || minFont >= 1900) return 0L;
        // 반비례: gap = K / min_font. 10pt 기준 1.90mm(539 HWPUNIT) → K = 539000
        return 539000L / minFont;
    }

    /**
     * 현재 다단 섹션의 단 분리선을 page에 등록한다.
     * 분리선 x = 인접한 두 단 사이 간격의 중심점.
     */
    private static void registerColumnSeps(Page page, ColumnState colState, long sectionBottom) {
        if (!colState.isMultiColumn() || colState.savedCd == null) return;
        EachBorder divideLine = colState.savedCd.getHeader().getDivideLine();
        if (divideLine == null || divideLine.getType() == null
                || divideLine.getType() == BorderType.None) return;
        List<Area> areas = colState.columnAreas;
        // 오른쪽부터(fromRight) 배치일 때 areas[i]는 areas[i+1]보다 오른쪽에 위치하므로
        // 단 사이 간격은 areas[i+1].right ~ areas[i].left 구간이다.
        boolean fromRight = areas.size() >= 2 && areas.get(0).left() > areas.get(1).left();
        for (int i = 0; i < areas.size() - 1; i++) {
            if (colState.maxUsedColumnIndex < i) break;
            // gap 양쪽 내측 경계: 픽셀 변환 후 평균해야 1픽셀 오차를 방지한다.
            long innerA = fromRight ? areas.get(i).left() : areas.get(i).right();
            long innerB = fromRight ? areas.get(i + 1).right() : areas.get(i + 1).left();
            long top = areas.get(i).top();
            page.addColumnSep(new Page.ColumnSep(innerA, innerB, top, sectionBottom, divideLine));
        }
    }

    private static int currentColumnLineCount(ParaLayoutContext ctx) {
        if (ctx.currentPage().content().rows().isEmpty()) return 0;
        return ctx.currentColumn().textLines().size();
    }

    /**
     * 분리선 end_y 계산용: 현재 단의 마지막 TextLine 문자 하단 Y (= top + textPartHeight).
     * 줄이 없으면 bodyArea.top()을 반환한다.
     */
    private static long lastLineBaseline(ParaLayoutContext ctx) {
        if (ctx.currentPage().content().rows().isEmpty()) return ctx.bodyArea().top();
        List<TextLine> lines = ctx.currentColumn().textLines();
        if (lines.isEmpty()) return ctx.bodyArea().top();
        TextLine last = lines.get(lines.size() - 1);
        return last.area().top() + last.maxCharHeight();
    }
}
