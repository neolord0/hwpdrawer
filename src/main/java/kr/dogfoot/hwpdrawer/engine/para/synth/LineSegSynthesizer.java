package kr.dogfoot.hwpdrawer.engine.para.synth;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.NoLineSegLayouter;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * LineSeg 정보가 없는 문단에 대해 {@link LineSegItem} 목록을 합성한다.
 *
 * <p>구현 전략(섀도 레이아웃):
 * <ol>
 *   <li>실제 컨텍스트와 분리된 임시 Document/Context를 만든다.</li>
 *   <li>{@link NoLineSegLayouter}를 임시 컨텍스트 위에서 실행해 TextLine을 얻는다.</li>
 *   <li>생성된 TextLine에서 각 라인의 시작 글자 index, 높이, 위치 등을 읽어
 *       {@link LineSegItem}로 변환한다.</li>
 * </ol>
 * 이렇게 합성된 LineSegItem 목록은 WithLineSegLayouter가 소비하며, 결과적으로
 * 렌더링 경로가 하나로 통합된다.</p>
 *
 * <p>페이지 경계 인코딩: WithLineSegLayouter는 {@code vertPos < lastVertPos}일 때
 * 새 페이지/단을 시작한다. 섀도 Document에서 페이지/단이 바뀌면
 * {@code lineVerticalPosition} 값이 자연스럽게 작아지므로 별도 표시가 필요 없다.</p>
 *
 * <p>다단(신문식) 지원: 실제 ctx가 폭이 다른 여러 단을 가질 때, 섀도 컨텍스트에
 * 단을 순회하는 columnAdvancer를 설정한다. {@link NoLineSegLayouter}가 한 단을
 * 넘치면 다음 단(올바른 폭)으로 전진하므로 단마다 정확한 폭으로 줄바꿈된다.</p>
 */
public final class LineSegSynthesizer {
    private final NoLineSegLayouter delegate = new NoLineSegLayouter();

    /**
     * 주어진 문단에 대해 LineSegItem 목록을 합성한다.
     * 실제 ctx(real)의 currentPage/pages/currentY는 전혀 건드리지 않는다.
     */
    public List<LineSegItem> synthesize(Paragraph para, int paraIdx, ParaShape paraShape,
                                         long leftMargin, long rightMargin, long indent,
                                         TextLine.Alignment alignment, ParaLayoutContext realCtx) {
        return synthesize(para, paraIdx, paraShape, leftMargin, rightMargin, indent, alignment,
                realCtx, realCtx.currentY());
    }

    /**
     * 섀도 시작 Y를 명시적으로 지정해 합성한다.
     * (parity 테스트처럼 원본 lineseg의 첫 라인 vertPos에 맞춰 시작점을 정렬하고 싶을 때 사용.)
     */
    public List<LineSegItem> synthesize(Paragraph para, int paraIdx, ParaShape paraShape,
                                         long leftMargin, long rightMargin, long indent,
                                         TextLine.Alignment alignment, ParaLayoutContext realCtx,
                                         long shadowStartY) {
        ParaLayoutContext shadowCtx = newShadowContext(realCtx, shadowStartY);
        delegate.layout(para, paraIdx, paraShape, leftMargin, rightMargin, indent, alignment, shadowCtx);
        return extractItems(shadowCtx.document(), paraIdx, para, realCtx.bodyArea(),
                leftMargin, rightMargin, columnAreasOf(realCtx),
                realCtx.pageBodyArea().top());
    }

    // ──────────────────────────────────────────────────────────────────

    /**
     * 실제 ctx의 다단 영역 — 비어있지 않으면 반환, 아니면 null.
     *
     * <p>size=1도 포함한다: 평행 다단은 단별로 layoutParagraph를 호출할 때 현재 단 영역만
     * 단일 리스트로 전달해, ShadowColumnFlow가 페이지 넘침 시 같은 단 폭으로 다음 페이지의
     * 같은 단 위치에 이어가게 한다(전체 페이지 본문으로 reset되지 않도록).</p>
     */
    private static List<Area> columnAreasOf(ParaLayoutContext realCtx) {
        List<Area> cols = realCtx.multiColumnAreas();
        return (cols != null && !cols.isEmpty()) ? cols : null;
    }

    private static ParaLayoutContext newShadowContext(ParaLayoutContext realCtx, long startY) {
        Document shadowDoc = new Document();
        ParaLayoutContext shadowCtx = new ParaLayoutContext(
                realCtx.reader(), realCtx.measurer(), shadowDoc);
        shadowCtx.paperArea(realCtx.paperArea());
        shadowCtx.bodyArea(realCtx.bodyArea());
        shadowCtx.pageBodyArea(realCtx.pageBodyArea().clone());
        // 탭 스톱 계산에 섹션 기본 탭 간격이 필요하다(줄바꿈 지점이 탭 위치에 좌우됨).
        shadowCtx.defaultTabGap(realCtx.defaultTabGap());
        // 문단 번호는 미리 확정된 읽기 전용 값이라 그대로 공유한다.
        shadowCtx.paragraphNumberer(realCtx.paragraphNumberer());

        Page page = shadowDoc.addPage();
        page.paperArea().copyFrom(realCtx.paperArea());
        page.bodyArea().copyFrom(realCtx.bodyArea());
        shadowCtx.currentPage(page);
        shadowCtx.currentY(startY);
        shadowCtx.currentParaTopY(realCtx.currentParaTopY());
        // 어울림(FitWithText) 컨트롤 래핑을 위해 실제 ctx의 obstacles를 공유.
        shadowCtx.obstacles().copyFrom(realCtx.obstacles());

        // 다단(신문식): 단을 순회하는 columnAdvancer를 설치한다.
        List<Area> cols = columnAreasOf(realCtx);
        if (cols != null) {
            ShadowColumnFlow flow = new ShadowColumnFlow(
                    shadowCtx, cols, realCtx.multiColumnIndex(), realCtx.pageBodyArea(),
                    realCtx.multiColumnPageBottoms());
            shadowCtx.columnAdvancer(flow::advance);
        }
        return shadowCtx;
    }

    /**
     * 섀도 다단 흐름 상태. NoLineSegLayouter가 단을 넘칠 때 호출되어
     * 다음 단(또는 마지막 단 이후엔 새 페이지의 첫 단)으로 ctx를 이동시킨다.
     */
    private static final class ShadowColumnFlow {
        private final ParaLayoutContext ctx;
        /** 페이지 상단 기준 단 영역(2페이지 이후 재사용). */
        private final List<Area> pageTopColumns;
        /** 현재 페이지의 단 영역. */
        private List<Area> columns;
        private int index;
        /** 후속 페이지별 단 하단(배분 다단의 페이지 분리 배분). null이면 없음. */
        private final List<long[]> pageBottoms;
        /** 지금까지 생성한 새 페이지 수(후속 페이지 인덱스). */
        private int pageCreateCount;

        ShadowColumnFlow(ParaLayoutContext ctx, List<Area> blockColumns,
                         int startIndex, Area pageBodyArea, List<long[]> pageBottoms) {
            this.ctx = ctx;
            this.columns = blockColumns;
            this.index = Math.max(0, Math.min(startIndex, blockColumns.size() - 1));
            this.pageBottoms = pageBottoms;
            this.pageTopColumns = new ArrayList<>();
            for (Area c : blockColumns) {
                pageTopColumns.add(new Area(c.left(), pageBodyArea.top(),
                        c.right(), pageBodyArea.bottom()));
            }
        }

        boolean advance() {
            if (index + 1 < columns.size()) {
                index++;
            } else {
                ctx.createNewPage();
                // 배분 다단의 페이지 분리 배분: 이 후속 페이지의 단 하단을 적용한다.
                columns = columnsForNewPage(pageCreateCount);
                pageCreateCount++;
                index = 0;
            }
            Area c = columns.get(index);
            ctx.bodyArea(c.clone());
            ctx.currentY(c.top());
            return true;
        }

        /** 후속 페이지({@code pageOffset}: 0=첫 새 페이지)의 단 영역. */
        private List<Area> columnsForNewPage(int pageOffset) {
            if (pageBottoms == null || pageOffset >= pageBottoms.size()
                    || pageBottoms.get(pageOffset) == null) {
                return pageTopColumns;
            }
            long[] bottoms = pageBottoms.get(pageOffset);
            List<Area> cols = new ArrayList<>(pageTopColumns.size());
            for (int i = 0; i < pageTopColumns.size(); i++) {
                Area a = pageTopColumns.get(i);
                long b = (i < pageTopColumns.size() - 1 && i < bottoms.length)
                        ? bottoms[i] : a.bottom();
                cols.add(new Area(a.left(), a.top(), a.right(), b));
            }
            return cols;
        }
    }

    /**
     * 섀도 Document에서 paraIdx에 해당하는 TextLine을 모두 찾아
     * LineSegItem으로 변환한다. 페이지/단 경계는 vertPos가 줄어드는 것으로 인코딩된다.
     */
    private static List<LineSegItem> extractItems(Document shadowDoc, int paraIdx, Paragraph para,
                                                   Area bodyArea, long leftMargin, long rightMargin,
                                                   List<Area> columnAreas, long pageBodyTop) {
        List<LineSegItem> items = new ArrayList<>();
        long defaultSegWidth = rightMargin - leftMargin;

        boolean isFirstLine = true;
        int pageIdx = 0;
        for (Page page : shadowDoc.pages()) {
            // vertPos 기준: 첫 섀도 페이지는 블록 단 top, 이후 페이지는 페이지 본문 top.
            // (WithLineSegLayouter가 단/페이지 전진 후 그 페이지 top에 vertPos를 더하기 때문)
            long vertBase = (pageIdx == 0) ? bodyArea.top() : pageBodyTop;
            for (TextRow row : page.content().rows()) {
                for (int c = 0; c < row.columns().size(); c++) {
                    for (TextLine line : row.columns().get(c).textLines()) {
                        if (line.paraIndex() != paraIdx) continue;
                        long colLeft = columnLeftOf(line.area().left(), columnAreas, bodyArea.left());
                        long defaultStartPos = leftMargin - columnLeftOf(leftMargin, columnAreas,
                                bodyArea.left());
                        // 폭이 다른 다단: 줄이 속한 단의 폭으로 세그먼트 폭을 보정한다.
                        // (문단이 다음 단으로 넘칠 때 시작 단 폭이 아닌 그 단 폭을 써야 함)
                        long lineSegWidth = columnSegWidth(line.area().left(), columnAreas,
                                bodyArea, defaultSegWidth);
                        addLineItems(items, line, para, vertBase, colLeft,
                                defaultStartPos, lineSegWidth, isFirstLine);
                        isFirstLine = false;
                    }
                }
            }
            pageIdx++;
        }
        return items;
    }

    /**
     * x가 속한 단의 세그먼트 폭을 반환한다. {@code defaultSegWidth}는 시작 단(bodyArea) 기준
     * 폭이며, 다른 단의 폭은 단 폭 차이만큼 보정한다(문단 좌우 여백은 단마다 동일).
     */
    private static long columnSegWidth(long x, List<Area> columnAreas, Area bodyArea,
                                        long defaultSegWidth) {
        if (columnAreas == null) return defaultSegWidth;
        long tol = 200;
        for (Area c : columnAreas) {
            if (x >= c.left() - tol && x <= c.right() + tol) {
                return defaultSegWidth + (c.width() - bodyArea.width());
            }
        }
        return defaultSegWidth;
    }

    /** x가 속한 단의 left를 반환한다. 단 정보가 없으면 fallback. */
    private static long columnLeftOf(long x, List<Area> columnAreas, long fallback) {
        if (columnAreas == null) return fallback;
        long tol = 200;
        for (Area c : columnAreas) {
            if (x >= c.left() - tol && x <= c.right() + tol) return c.left();
        }
        // 어느 단에도 안 들어가면 가장 가까운 단의 left.
        long best = fallback;
        long bestDist = Long.MAX_VALUE;
        for (Area c : columnAreas) {
            long d = Math.abs(x - c.left());
            if (d < bestDist) { bestDist = d; best = c.left(); }
        }
        return best;
    }

    private static void addLineItems(List<LineSegItem> out, TextLine line, Paragraph para,
                                      long vertBase, long colLeft,
                                      long defaultStartPos, long defaultSegWidth,
                                      boolean isFirstLine) {
        List<TextPart> parts = line.parts();
        if (parts == null || parts.isEmpty() || parts.size() == 1) {
            TextPart part = (parts != null && !parts.isEmpty()) ? parts.get(0) : null;
            if (part != null && part.segmentRight() > part.segmentLeft()) {
                long startPos = Math.round(part.segmentLeft()) - colLeft;
                long segW = Math.round(part.segmentRight() - part.segmentLeft());
                out.add(toLineSegItem(line, para, vertBase, startPos, segW, isFirstLine, 0, false));
            } else {
                out.add(toLineSegItem(line, para, vertBase, defaultStartPos, defaultSegWidth,
                        isFirstLine, 0, true));
            }
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            TextPart part = parts.get(i);
            long startPos = Math.round(part.startX()) - colLeft;
            long segW = (part.width() > 0) ? Math.round(part.width()) : defaultSegWidth;
            boolean firstItem = isFirstLine && i == 0;
            out.add(toLineSegItem(line, para, vertBase, startPos, segW, firstItem,
                    i, /*useDefault*/ false));
        }
    }

    private static LineSegItem toLineSegItem(TextLine line, Paragraph para, long vertBase,
                                              long startPosFromColumn, long segmentWidth,
                                              boolean isFirstLine, int partIdx, boolean useDefault) {
        LineSegItem item = new LineSegItem();
        int startCharIdx;
        if (useDefault || partIdx == 0) {
            startCharIdx = line.startCharIdx();
        } else {
            TextPart part = line.parts().get(partIdx);
            startCharIdx = (part.charInfos().isEmpty())
                    ? line.startCharIdx()
                    : part.charInfos().get(0).charIdx();
        }
        int textStart = isFirstLine
                ? 0
                : (int) HWPFileReader.calcTextStreamPosition(para, startCharIdx);
        item.setTextStartPosition(textStart);
        item.setLineVerticalPosition((int) (line.area().top() - vertBase));
        long fullBlock = line.area().bottom() - line.area().top();
        long gap = line.lineGap();
        long textH = fullBlock - gap;
        item.setLineHeight((int) textH);
        item.setTextPartHeight((int) line.maxCharHeight());
        item.setDistanceBaseLineToLineVerticalPosition((int) line.maxCharBaseSize());
        item.setLineSpace((int) gap);
        item.setStartPositionFromColumn((int) startPosFromColumn);
        item.setSegmentWidth((int) segmentWidth);
        return item;
    }
}
