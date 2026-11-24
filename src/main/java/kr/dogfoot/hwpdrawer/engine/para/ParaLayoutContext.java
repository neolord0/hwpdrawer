package kr.dogfoot.hwpdrawer.engine.para;

import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwpdrawer.engine.CharMetricsMeasurer;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.obstacle.ParaObstacles;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * 문단 레이아웃 동안 공유되는 컨텍스트.
 *
 * Layouter들은 이 컨텍스트를 통해 현재 페이지/Y 위치를 읽고 업데이트한다.
 * 페이지가 넘치면 새 페이지를 만들고 currentPage/currentY를 갱신한다.
 */
public class ParaLayoutContext {
    private final HWPFileReader reader;
    private final CharMetricsMeasurer measurer;
    private final Document document;

    private Area paperArea;
    private Area bodyArea;
    /** 페이지 본문 전체 영역(고정). 다단에서 bodyArea는 단 영역으로 변경되므로 별도 보관. */
    private Area pageBodyArea;
    private Page currentPage;
    private long currentY;
    /** WithLineSegLayouter가 paragraph 사이에서도 페이지 경계를 감지하도록 직전 라인의 vertPos를 보관. */
    private long lastLineSegVertPos = -1;
    /** 현재 섹션의 기본 탭 간격(HWPUNIT). 문단 탭 정의를 넘어선 탭은 이 간격 배수로 진행. */
    private long defaultTabGap = 4000;
    /** 문서 순서로 미리 확정한 문단 번호. 레이아웃 중에는 조회만 한다. */
    private kr.dogfoot.hwpdrawer.engine.para.common.ParagraphNumberer paragraphNumberer;

    public ParaLayoutContext(HWPFileReader reader, CharMetricsMeasurer measurer, Document document) {
        this.reader = reader;
        this.measurer = measurer;
        this.document = document;
    }

    public HWPFileReader reader() { return reader; }

    public kr.dogfoot.hwpdrawer.engine.para.common.ParagraphNumberer paragraphNumberer() {
        return paragraphNumberer;
    }

    public ParaLayoutContext paragraphNumberer(
            kr.dogfoot.hwpdrawer.engine.para.common.ParagraphNumberer numberer) {
        this.paragraphNumberer = numberer;
        return this;
    }
    public CharMetricsMeasurer measurer() { return measurer; }
    public Document document() { return document; }

    public Area paperArea() { return paperArea; }
    public Area bodyArea() { return bodyArea; }
    public Area pageBodyArea() { return pageBodyArea != null ? pageBodyArea : bodyArea; }
    public Page currentPage() { return currentPage; }
    public long currentY() { return currentY; }

    public void paperArea(Area area) { this.paperArea = area; }
    public void bodyArea(Area area) { this.bodyArea = area; }
    public void pageBodyArea(Area area) { this.pageBodyArea = area; }
    public void currentPage(Page page) { this.currentPage = page; }
    public void currentY(long y) { this.currentY = y; }

    public long lastLineSegVertPos() { return lastLineSegVertPos; }
    public void lastLineSegVertPos(long v) { this.lastLineSegVertPos = v; }

    public long defaultTabGap() { return defaultTabGap; }
    public void defaultTabGap(long gap) { this.defaultTabGap = gap; }

    /**
     * 자리차지(takePlace) 분할 표 배치 직후, 후속 단락 줄이 표 영역과 겹치지 않도록
     * 페이지 변경을 강제하는 데 쓰는 표 영역(현재 페이지 좌표). 사용 후 null로 해제.
     */
    private Area takePlaceTableArea;
    public Area takePlaceTableArea() { return takePlaceTableArea; }
    public void takePlaceTableArea(Area a) { this.takePlaceTableArea = a; }

    /** 최근 자리차지 분할 표의 paragraph index. 표보다 뒤 paragraph 컨트롤 라우팅에 사용. */
    private int lastDivideTableParaIdx = -1;
    public int lastDivideTableParaIdx() { return lastDivideTableParaIdx; }
    public void lastDivideTableParaIdx(int idx) { this.lastDivideTableParaIdx = idx; }

    /** 최근 자리차지 분할 표의 마지막 조각 페이지 — 후속 paragraph의 플로팅 컨트롤이 여기로 라우팅. */
    private Page lastDivideTableLastSegPage;
    public Page lastDivideTableLastSegPage() { return lastDivideTableLastSegPage; }
    public void lastDivideTableLastSegPage(Page p) { this.lastDivideTableLastSegPage = p; }

    /** 최근 자리차지 분할 표의 첫 조각 페이지 — 후처리 라인 이동의 source. */
    private Page lastDivideTableFirstSegPage;
    public Page lastDivideTableFirstSegPage() { return lastDivideTableFirstSegPage; }
    public void lastDivideTableFirstSegPage(Page p) { this.lastDivideTableFirstSegPage = p; }

    /** 최근 자리차지 분할 표의 firstSegPage 좌표상 표 영역. */
    private Area lastDivideTableFirstSegArea;
    public Area lastDivideTableFirstSegArea() { return lastDivideTableFirstSegArea; }
    public void lastDivideTableFirstSegArea(Area a) { this.lastDivideTableFirstSegArea = a; }

    /** 최근 자리차지 분할 표의 lastSegPage 좌표상 마지막 조각 바닥. */
    private long lastDivideTableLastSegBottom;
    public long lastDivideTableLastSegBottom() { return lastDivideTableLastSegBottom; }
    public void lastDivideTableLastSegBottom(long y) { this.lastDivideTableLastSegBottom = y; }

    /**
     * 최근 자리차지 분할 표가 firstSegPage 표 위 공간에 본문이 들어가는 모드인지
     * (= 빈셀 케이스처럼 표 단락 본문 "1" 등이 표 위에 자리잡았는지). true 이면
     * moveLinesOverlappingTakePlaceTable 후처리에서 표 위 공간에 들어간 후속 줄은
     * 보존하고 표 영역과 겹친 줄(과 그 아래)만 lastSegPage 로 이동한다.
     */
    private boolean lastDivideTableBodyAboveTable = false;
    public boolean lastDivideTableBodyAboveTable() { return lastDivideTableBodyAboveTable; }
    public void lastDivideTableBodyAboveTable(boolean v) { this.lastDivideTableBodyAboveTable = v; }

    /** 내부 doc(셀/글상자) 레이아웃 중인지 표시. 컨트롤 위치 계산을 분기하는 데 사용. */
    private boolean innerDocLayout = false;
    public boolean innerDocLayout() { return innerDocLayout; }
    public void innerDocLayout(boolean v) { this.innerDocLayout = v; }

    /**
     * 페이지에 걸쳐 분할된 어울림(FitWithText) DivideByCell 표의 anchor paraIdx.
     * 그 직후 첫 텍스트 본문 단락은 표 둘째 조각과 함께 다음 페이지에서 시작해야 한다
     * (LineSeg는 vp=0으로 page break가 인코딩되지만 NoLineSeg 합성기는 이를 모름).
     * 본문 루프가 그 단락을 새 페이지로 보낸 뒤 -1로 리셋한다. 없으면 -1.
     */
    private int pendingFitWrapTableParaIdx = -1;
    public int pendingFitWrapTableParaIdx() { return pendingFitWrapTableParaIdx; }
    public void pendingFitWrapTableParaIdx(int idx) { this.pendingFitWrapTableParaIdx = idx; }

    /** 현재 문단의 최상단 Y (layoutParagraph 진입 시 저장) — vRel=Para 컨트롤 위치 계산에 사용. */
    private long currentParaTopY;
    public long currentParaTopY() { return currentParaTopY; }
    public void currentParaTopY(long y) { this.currentParaTopY = y; }

    /** 텍스트 overflow로 다음 페이지로 밀린 컨트롤들 (어울림/자리차지, 75% 규칙). */
    private final List<DeferredControl> deferredControls = new ArrayList<>();
    public List<DeferredControl> deferredControls() { return deferredControls; }

    /**
     * 새 페이지가 만들어질 때, 그 페이지에 미리 배치되어야 할 지연 컨트롤들을 처리하는 콜백.
     * (NoDivide 표 등이 다음 페이지 상단에 자리잡고, 후속 단락이 그 아래에서 시작하도록.)
     *
     * 콜백은 (직전 anchorPage, 새 page) 를 받아 deferredControls 에서 매칭되는
     * 컨트롤을 새 page 에 배치하고 max bottom Y(여백 포함)를 반환한다. 새 페이지의
     * currentY 를 그 값 위로 끌어올린다.
     */
    @FunctionalInterface
    public interface DeferredPlacer {
        long placeOnNewPage(Page anchorPage, Page newPage);
    }
    private DeferredPlacer deferredPlacer;
    public void deferredPlacer(DeferredPlacer p) { this.deferredPlacer = p; }

    /** 섹션/컨테이너 내에 배치된 FitWithText 컨트롤들 — NoLineSeg 라인 래핑에 참조된다. */
    private final ParaObstacles obstacles = new ParaObstacles();
    public ParaObstacles obstacles() { return obstacles; }

    /**
     * 현재 다단 블록의 단 영역 목록과 현재 단 인덱스.
     * LineSegSynthesizer가 NoLineSeg 문단을 단 폭에 맞게 합성할 때 참조한다.
     * 1단이면 null 또는 크기 1.
     */
    private List<Area> multiColumnAreas;
    private int multiColumnIndex;
    public List<Area> multiColumnAreas() { return multiColumnAreas; }
    public void multiColumnAreas(List<Area> areas) { this.multiColumnAreas = areas; }
    public int multiColumnIndex() { return multiColumnIndex; }
    public void multiColumnIndex(int idx) { this.multiColumnIndex = idx; }

    /**
     * 페이지를 넘는 배분 다단 블록의 후속 페이지별 단 하단(bottom) Y.
     * index 0 = 블록의 다음 페이지, 1 = 그 다음 페이지, …
     * 각 long[]는 단별 bottom (마지막 단은 페이지 하단). NoLineSeg 합성 시
     * 후속 페이지의 단 높이를 배분 결과대로 맞추는 데 쓰인다.
     */
    private List<long[]> multiColumnPageBottoms;
    public List<long[]> multiColumnPageBottoms() { return multiColumnPageBottoms; }
    public void multiColumnPageBottoms(List<long[]> v) { this.multiColumnPageBottoms = v; }

    /**
     * 다단 레이아웃에서 "다음 단으로 이동" 동작을 수행하는 콜백.
     * null이면 1단(다단 없음). 콜백이 {@code true}를 반환하면 다음 단으로 이동 성공,
     * {@code false}이면 모든 단이 소진되어 새 페이지가 필요함을 의미한다.
     */
    @FunctionalInterface
    public interface ColumnAdvancer {
        /** 다음 단으로 이동 시도. 성공 시 true, 마지막 단이면 false. */
        boolean advance();
    }

    private ColumnAdvancer columnAdvancer;

    public void columnAdvancer(ColumnAdvancer advancer) { this.columnAdvancer = advancer; }

    /**
     * 다음 단으로 이동을 시도한다.
     * @return 다음 단 이동 성공 시 true (페이지 변경 불필요), 모든 단 소진 시 false (페이지 변경 필요)
     */
    public boolean tryNextColumn() {
        if (columnAdvancer == null) return false;
        return columnAdvancer.advance();
    }

    /**
     * 새 페이지를 만들고 currentPage/currentY를 초기화한다.
     * 페이지 bodyArea는 다단 단 영역이 아닌 원본 페이지 본문 영역을 사용한다.
     */
    public Page createNewPage() {
        Page previousPage = currentPage;
        Page page = document.addPage();
        page.paperArea().copyFrom(paperArea);
        page.bodyArea().copyFrom(pageBodyArea());
        this.currentPage = page;
        // 새 페이지의 기본 본문 영역은 페이지 전체 본문이다. 다단 호출자는 직후 단 영역으로 덮어쓴다.
        // (다단 ColumnDefine 블록이 페이지를 넘칠 때 bodyArea.top이 이전 페이지 값으로 남는 것을 방지)
        this.bodyArea = pageBodyArea().clone();
        this.currentY = bodyArea.top();
        // 새 페이지에서는 vertPos 누적이 리셋된다 — 다음 라인의 vertPos가 작아졌다고 페이지 경계로
        // 잘못 인식하지 않도록 -1로 초기화한다.
        this.lastLineSegVertPos = -1;
        // 어울림 장애물은 페이지 내 Y 좌표 기반이므로 새 페이지에서는 재사용 불가.
        this.obstacles.clear();
        // 어울림 표 분할의 후속 조각이 예약돼 있으면 이 페이지에 배치한다.
        if (!pendingTableSegments.isEmpty()) {
            page.addControl(pendingTableSegments.remove(0));
        }
        // 직전 페이지를 anchorPage 로 가진 지연 컨트롤들(75%-규칙으로 밀린 NoDivide 표 등)을
        // 이 새 페이지 상단에 미리 배치한다. 그 뒤에 currentY 를 컨트롤 아래로 끌어내려
        // 후속 단락이 컨트롤과 겹치지 않게 한다.
        if (deferredPlacer != null && previousPage != null) {
            long reservedBottom = deferredPlacer.placeOnNewPage(previousPage, page);
            if (reservedBottom > this.currentY) this.currentY = reservedBottom;
        }
        return page;
    }

    /**
     * 현재 페이지의 다음 페이지로 진행한다. 이미 생성된 다음 페이지가 있으면 그것을
     * 재사용하고, 없으면 새 페이지를 만든다.
     *
     * <p>자리차지 표가 셀나누기로 후속 페이지를 미리 만든 뒤, 표 다음 본문이 페이지를
     * 넘어갈 때 그 미리 만든 페이지를 재사용하기 위한 것이다. {@link #createNewPage()}는
     * 무조건 새 페이지를 만들어 빈 페이지가 끼어드는 문제가 있다.</p>
     */
    public Page advanceToNextPageOrCreate() {
        int idx = document.pages().indexOf(currentPage);
        if (idx >= 0 && idx + 1 < document.pages().size()) {
            Page next = document.pages().get(idx + 1);
            this.currentPage = next;
            this.bodyArea = pageBodyArea().clone();
            this.currentY = bodyArea.top();
            this.lastLineSegVertPos = -1;
            this.obstacles.clear();
            return next;
        }
        return createNewPage();
    }

    /**
     * 어울림(FitWithText) 표가 행 단위로 분할될 때, 후속 페이지에 배치할 표 조각 큐.
     * {@link #createNewPage()}가 새 페이지를 만들 때마다 맨 앞 조각을 꺼내 그 페이지에 배치한다.
     */
    private final List<kr.dogfoot.hwpdrawer.document.control.ControlItem> pendingTableSegments
            = new ArrayList<>();

    /** 어울림 표 분할의 후속 조각을 다음 페이지 배치 큐에 등록한다. */
    public void enqueuePendingTableSegment(kr.dogfoot.hwpdrawer.document.control.ControlItem item) {
        pendingTableSegments.add(item);
    }

    /**
     * 섹션 끝에서 아직 배치되지 못한 예약 표 조각을 새 페이지에 강제 배치한다.
     * NoLineSeg 는 어울림 분할 표 다음에 page 를 만들 본문 vertPos 가 없어 예약 조각이
     * 남을 수 있다(예: 표가 사실상 마지막 콘텐츠인 셀나누기/기본/1). 남은 조각마다
     * 새 페이지를 만들어 배치한다. (createNewPage 가 맨 앞 조각을 그 페이지에 배치.)
     */
    public void flushPendingTableSegments() {
        int guard = 0;
        while (!pendingTableSegments.isEmpty() && guard++ < 100) {
            createNewPage();
        }
    }

    /**
     * 현재 페이지에 기본 Row/Column 구조가 없으면 생성한다.
     */
    public void ensureRowAndColumn() {
        if (currentPage.content().rows().isEmpty()) {
            TextRow row = currentPage.content().addRow();
            row.addColumn();
        }
    }

    /**
     * 현재 페이지의 마지막 Row의 마지막 Column을 반환한다.
     */
    public TextColumn currentColumn() {
        TextRow lastRow = currentPage.content().rows().get(currentPage.content().rows().size() - 1);
        return lastRow.columns().get(lastRow.columns().size() - 1);
    }
}
