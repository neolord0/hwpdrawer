package kr.dogfoot.hwpdrawer.engine.para.withlineseg;

import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlAutoNumber;
import kr.dogfoot.hwplib.object.bodytext.control.ControlFootnote;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderAutoNumber;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderFootnote;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.autonumber.NumberSort;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.ParaLineSeg;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlInline;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwpdrawer.engine.control.ControlAreaCalculator;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.VerticalAlignment;
import kr.dogfoot.hwplib.object.docinfo.tabdef.TabInfo;
import kr.dogfoot.hwplib.object.docinfo.tabdef.TabSort;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.engine.CharMetrics;
import kr.dogfoot.hwpdrawer.engine.CharMetricsMeasurer;
import kr.dogfoot.hwpdrawer.engine.ParaShapeHelper;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.ParagraphLayouter;
import kr.dogfoot.hwpdrawer.engine.para.common.LineAlignmentProcessor;
import kr.dogfoot.hwpdrawer.engine.para.common.FootnoteNumberFormatter;
import kr.dogfoot.hwpdrawer.engine.para.common.LineMetrics;
import kr.dogfoot.hwpdrawer.engine.para.common.MinimumSpaceProcessor;
import kr.dogfoot.hwpdrawer.engine.para.common.ParagraphHead;
import kr.dogfoot.hwpdrawer.engine.para.common.ParagraphIndent;
import kr.dogfoot.hwpdrawer.engine.para.common.TabStopHelper;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.ScriptStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * LineSeg 정보를 활용하여 문단을 레이아웃한다.
 * 글자 위치/폭은 double로 계산한다.
 */
public class WithLineSegLayouter implements ParagraphLayouter {

    @Override
    public void layout(Paragraph para, int paraIdx, ParaShape paraShape,
                       long leftMargin, long rightMargin, long indent,
                       TextLine.Alignment alignment, ParaLayoutContext ctx) {
        layoutWithItems(para, paraIdx, paraShape, leftMargin, rightMargin, indent,
                alignment, ctx, lineSegItemsOf(para));
    }

    public void layoutWithItems(Paragraph para, int paraIdx, ParaShape paraShape,
                                 long leftMargin, long rightMargin, long indent,
                                 TextLine.Alignment alignment, ParaLayoutContext ctx,
                                 List<LineSegItem> lineSegItems) {
        layoutWithItems(para, paraIdx, paraShape, leftMargin, rightMargin, indent,
                alignment, ctx, lineSegItems, false);
    }

    /**
     * 외부에서 합성된 LineSegItem 목록을 사용해 문단을 레이아웃한다.
     * (LineSeg 정보가 없는 HWP에 대해 LineSegSynthesizer가 만든 목록을 소비할 때 사용.)
     *
     * @param suppressPageBreak true이면 LineSeg vertPos 기반 페이지 전환을 끄고 줄을
     *        currentY에 이어 배치한다. 셀단위로나누기(DivideByCell) 표를 가진 문단에
     *        사용 — 표 분할로 페이지 흐름이 원본 LineSeg와 어긋나기 때문.
     */
    public void layoutWithItems(Paragraph para, int paraIdx, ParaShape paraShape,
                                 long leftMargin, long rightMargin, long indent,
                                 TextLine.Alignment alignment, ParaLayoutContext ctx,
                                 List<LineSegItem> lineSegItems, boolean suppressPageBreak) {
        if (lineSegItems.isEmpty()) return;

        List<HWPChar> charList = charListOf(para);
        boolean paraHasContent = charList.size() > 1;
        ParagraphHead head = ParagraphHead.of(ctx.reader(), para, paraShape, ctx.measurer(), ctx.paragraphNumberer());

        for (int lineIdx = 0; lineIdx < lineSegItems.size(); lineIdx++) {
            LineSegItem lsi = lineSegItems.get(lineIdx);
            boolean isFirstLine = (lineIdx == 0);
            boolean isLastLine = (lineIdx == lineSegItems.size() - 1);
            // 마지막 비주얼 행에 좌/우 세그먼트가 두 개 있을 때(어울림 래핑),
            // 왼쪽(앞) 세그먼트가 마지막 라인 취급으로 Justify가 막힐지 여부.
            // 자연폭이 세그먼트를 거의 꽉 채운 경우(=세그먼트 폭 부족으로 강제 줄바꿈)는
            // 양쪽정렬을 적용해야 하고, 짧은 경우(자연 종료)는 마지막 줄처럼 왼쪽정렬한다.
            // 실제 판정은 fillCharInfos 로 자연폭을 얻은 뒤(아래) 한다.
            boolean leftSegOfLastRow = false;
            if (!isLastLine && lineIdx + 1 == lineSegItems.size() - 1) {
                LineSegItem nextLsi = lineSegItems.get(lineIdx + 1);
                if (lsi.getLineVerticalPosition() == nextLsi.getLineVerticalPosition()) {
                    leftSegOfLastRow = true;
                }
            }

            if (!suppressPageBreak) {
                handlePageBreakIfNeeded(lsi, lineIdx, paraHasContent, paraIdx, ctx);
            }

            int textStart = textStartOf(para, lsi);
            int textEnd = textEndOf(para, lineSegItems, lineIdx, charList.size());

            TextLine line = createTextLine(ctx, paraIdx, paraShape, alignment,
                    leftMargin, rightMargin, lsi, isFirstLine);
            line.startCharIdx(textStart);

            // 글머리표가 있는 문단: 첫 줄은 문단 시작 위치에서 글머리표를 그린 뒤 본문이
            // bodyOffset만큼 밀리고, 둘째 줄부터는 줄 자체가 그만큼 오른쪽에서 시작한다.
            double headOffset = (head == null || isFirstLine) ? 0 : head.bodyOffset();
            double paraIndent = ParagraphIndent.offsetFor(indent, isFirstLine) + headOffset;
            // 내어쓰기가 줄 폭 이상이면 적용하지 않는다. 그대로 더하면 둘째 줄부터 세그먼트
            // 밖으로 밀려나고 segmentWidth 가 음수가 되어 글자가 통째로 사라진다.
            // 실문서 R01의 세로 라벨 셀 "신청인"(폭 26px)이 그 예로,
            // indent=-45640(내어쓰기 380px) ≫ segW=1440(24px)이라 "청"·"인" 두 줄이
            // 사라지고 "신"만 남았다. 한글은 이 경우 내어쓰기를 무시한다 — 파일 LineSeg 도
            // 세 줄 모두 startX=0·segW=1440 으로 같고, 참조 출력도 세 글자가 같은 x 에 있다.
            // 코퍼스 전체 다줄 음수 내어쓰기 문단 1415개 중 이 조건에 걸리는 것은 이 1건뿐이라
            // 다른 문서의 정상 내어쓰기(예: "□가사간병/방문지원" +18px)는 영향받지 않는다.
            if (paraIndent >= lsi.getSegmentWidth()) {
                paraIndent = 0;
            }
            double lineLeft = ctx.bodyArea().left() + lsi.getStartPositionFromColumn() + paraIndent;
            double segmentWidth = lsi.getSegmentWidth() - paraIndent;
            long lineY = suppressPageBreak
                    ? ctx.currentY()
                    : ctx.bodyArea().top() + lsi.getLineVerticalPosition();

            assignLineArea(line, lineLeft, lineY, segmentWidth, lsi, paraShape);

            TextPart part = line.addPart();
            part.startX(lineLeft);

            LineMetrics metrics = new LineMetrics();
            if (head != null && isFirstLine) {
                addParagraphHead(part, head, metrics);
            }
            fillCharInfos(part, metrics, para, charList, textStart, textEnd,
                    ctx.reader(), ctx.measurer(), ctx, paraShape, segmentWidth);

            applyMinimumSpaceIfNeeded(part, paraShape, segmentWidth, metrics);

            // 강제 줄나눔(Shift+Enter, 코드 10)으로 끝나는 줄은 문단 마지막 줄과 똑같이
            // 늘리지 않는다. 자연 줄바꿈이 아니라 사용자가 끊은 줄이라 한글은 양쪽정렬
            // 신축을 적용하지 않는다. 실문서 R01 보육료 신청구분 셀이
            // 그 예로, 문단마다 59번 글자가 코드 10 이고 다음 줄 textStart 가 60 이다.
            // 이 검사가 없으면 그 줄이 세그먼트 우단까지 늘어나 어절이 최대 36px 밀린다
            // (참조 줄끝 856 vs 우리 891).
            boolean forcedBreak = endsWithForcedLineBreak(charList, textEnd);
            line.endsWithLineBreak(forcedBreak);
            // 마지막 비주얼 행의 앞(좌) 세그먼트: 자연폭이 세그먼트를 거의 꽉 채웠으면
            // (강제 줄바꿈) 양쪽정렬, 짧으면 마지막 줄처럼 왼쪽정렬.
            boolean effectiveLastLine = isLastLine || forcedBreak;
            if (leftSegOfLastRow && !forcedBreak) {
                boolean nearlyFull = metrics.visibleCharX() >= segmentWidth * 0.9;
                effectiveLastLine = !nearlyFull;
            }
            applyAlignmentAndStretch(part, alignment, effectiveLastLine, segmentWidth, metrics, lineLeft);

            finalizeLine(line, part, metrics, effectiveLastLine);
            ctx.currentY(lineY + lsi.getLineHeight() + lsi.getLineSpace());
        }
    }

    private static List<LineSegItem> lineSegItemsOf(Paragraph para) {
        ParaLineSeg lineSeg = para.getLineSeg();
        if (lineSeg == null || lineSeg.getLineSegItemList() == null) return new ArrayList<>();
        return lineSeg.getLineSegItemList();
    }

    private static List<HWPChar> charListOf(Paragraph para) {
        return (para.getText() != null) ? para.getText().getCharList() : new ArrayList<>();
    }

    private static int textStartOf(Paragraph para, LineSegItem lsi) {
        return HWPFileReader.charIndexFromTextStreamPosition(para, lsi.getTextStartPosition());
    }

    private static int textEndOf(Paragraph para, List<LineSegItem> items, int lineIdx, int charsSize) {
        if (lineIdx + 1 >= items.size()) return charsSize;
        return HWPFileReader.charIndexFromTextStreamPosition(
                para, items.get(lineIdx + 1).getTextStartPosition());
    }

    private static void handlePageBreakIfNeeded(LineSegItem lsi, int lineIdx,
                                                  boolean paraHasContent, int paraIdx,
                                                  ParaLayoutContext ctx) {
        long vertPos = lsi.getLineVerticalPosition();
        // vertPos가 감소하면 페이지 변경. 추가로 자리차지 분할 표 직후 표 paragraph
        // 뒤 paragraph(paraIdx > lastDivideTableParaIdx) 첫 줄이 자연 페이지 변경이
        // 안 되는 경우에도 강제로 페이지 변경 — 본문이 표 위에 끼어들지 않도록.
        long lineY = ctx.bodyArea().top() + vertPos;
        long lineHeight = lsi.getLineHeight() + lsi.getLineSpace();
        boolean vertPosBackward = ctx.lastLineSegVertPos() >= 0 && vertPos < ctx.lastLineSegVertPos();
        // 각 문단이 페이지를 가득 채우는 전면 인라인 표(글자처럼, lineHeight≈페이지)인 경우
        // 문단마다 vertPos=0이라 '감소'(vertPos<last)로 페이지 변경이 탐지되지 않는다. 대신
        // 새 문단 첫 줄이 현재 흐름(currentY) 위에 놓이면(lineY<currentY) 앞 줄이 페이지를
        // 채웠다는 뜻이므로 페이지를 넘긴다. (일반 흐름은 새 문단 vertPos>last라 걸리지 않음.)
        // 한 문단 안에서도 줄마다 페이지를 채우는 경우가 있다(실문서 R09: 문단 1개에
        // 전면 인라인 표 2개, LineSeg 2줄이 둘 다 vp=0·h≈페이지). 이때는 lineIdx>0 이라
        // 위 조건에 안 걸려 두 페이지가 1페이지에 겹쳐 렌더됐다(pages=1, expected=2).
        //
        // 다만 <b>같은 vertPos 가 곧 페이지 변경은 아니다</b> — 어울림(텍스트 감싸기) 래핑은
        // 한 visual row 를 좌/우 세그먼트로 쪼개면서 둘에 같은 vertPos 를 준다. 그래서
        // "직전 줄이 본문 영역 아래로 넘어갔을 때"(=페이지를 다 채웠을 때)로 좁힌다.
        // 어울림 좌/우 세그먼트는 한 줄 높이만 소비하므로 이 조건에 걸리지 않는다.
        boolean prevLineFilledPage = ctx.currentY() > ctx.bodyArea().bottom();
        boolean overlapNewPage = ctx.lastLineSegVertPos() >= 0
                && vertPos <= ctx.lastLineSegVertPos() && lineY < ctx.currentY()
                && (lineIdx == 0 || prevLineFilledPage);
        Area tableArea = ctx.takePlaceTableArea();
        // 표 paragraph 뒤 본문은 layout 후 후처리에서 정리(moveLinesOverlappingTakePlaceTable)
        // 하므로 여기서는 페이지 변경을 강제하지 않는다.
        boolean afterTablePara = false;
        if (vertPosBackward || afterTablePara || overlapNewPage) {
            // 단 변경 또는 페이지 변경.
            // 다단이 있으면 먼저 다음 단으로 이동을 시도하고, 실패하면 새 페이지.
            if (!ctx.tryNextColumn()) {
                ctx.advanceToNextPageOrCreate();
            } else {
                // 단 변경 성공 — lastLineSegVertPos를 -1로 리셋해 다음 라인과 올바르게 비교한다.
                ctx.lastLineSegVertPos(-1);
            }
            ctx.takePlaceTableArea(null);
        } else if (lineIdx == 0 && vertPos == 0 && paraHasContent && ctx.lastLineSegVertPos() >= 0) {
            // 단에 항목이 하나뿐이어서 vertPos 비교(0 < 0)로 감지 불가능한 단 변경을 보완한다.
            // vertPos==0 조건으로 동일 단 내 중간 단락 오인식을 방지하고,
            // paraHasContent 조건으로 빈 단락의 오인식을 방지한다.
            if (lineY < ctx.currentY() && ctx.tryNextColumn()) {
                ctx.lastLineSegVertPos(-1);
            }
        }
        ctx.lastLineSegVertPos(vertPos);
    }

    private static TextLine createTextLine(ParaLayoutContext ctx, int paraIdx, ParaShape paraShape,
                                            TextLine.Alignment alignment,
                                            long leftMargin, long rightMargin,
                                            LineSegItem lsi, boolean isFirstLine) {
        ctx.ensureRowAndColumn();
        TextColumn column = ctx.currentColumn();

        TextLine line = column.addTextLine();
        line.paraIndex(paraIdx);
        line.alignment(alignment);
        line.firstInPara(isFirstLine);

        line.borderFillId(paraShape.getBorderFillId());
        line.paraLeft(leftMargin);
        line.paraRight(rightMargin);
        line.paraTopSpace(ParaShapeHelper.topSpace(paraShape));
        line.paraBottomSpace(ParaShapeHelper.bottomSpace(paraShape));
        line.lineGap(lsi.getLineSpace());
        return line;
    }

    private static void assignLineArea(TextLine line, double lineLeft, long lineY,
                                        double segmentWidth, LineSegItem lsi,
                                        ParaShape paraShape) {
        long left = Math.round(lineLeft);
        long right = Math.round(lineLeft + segmentWidth);
        line.area().set(left, lineY, right, lineY + lsi.getLineHeight() + lsi.getLineSpace());
        line.maxCharHeight(lsi.getTextPartHeight());
        line.maxCharBaseSize(baseLineDistance(lsi, paraShape));
    }

    /**
     * 줄 위(top)에서 baseline 까지의 거리.
     *
     * <p>보통은 LineSeg 가 저장한 값을 그대로 쓴다(줄 높이의 0.85). 그러나 문단 모양의
     * <b>세로 정렬이 가운데(ParaShape.property1.verticalAlignment = Center)</b>면 저장된
     * 값이 baseline 이 아니라 <b>줄의 세로 중심</b>이다 — 코퍼스 전수 확인 결과
     * {@code vAlign=Center} 문단 43줄이 <b>전부 정확히 {@code baseDist = lineHeight/2}</b> 다.
     * 그대로 baseline 으로 쓰면 글자가 자기 줄 상자 위로 삐져나간다
     * (실문서 R10 제목: 줄 상자 top 181.5px 인데 잉크가 174px 부터 시작).</p>
     *
     * <p>가운데 정렬은 글자 em 상자를 줄 중심에 맞추는 것이므로
     * {@code baseline = 중심 − h/2 + 0.85h = 중심 + 0.35h} 다. 글자 높이 h 가 줄 높이와
     * 같으면 {@code 0.5h + 0.35h = 0.85h} 로 기본 규약과 정확히 일치한다.</p>
     */
    private static int baseLineDistance(LineSegItem lsi, ParaShape paraShape) {
        int stored = lsi.getDistanceBaseLineToLineVerticalPosition();
        if (paraShape == null
                || paraShape.getProperty1().getVerticalAlignment() != VerticalAlignment.Center) {
            return stored;
        }
        return stored + (int) Math.round(lsi.getTextPartHeight() * 0.35);
    }

    /**
     * 문단 머리(글머리표)를 줄 맨 앞에 배치한다. 폭을 {@link ParagraphHead#bodyOffset()}
     * (글머리표 폭 + 본문과의 거리)로 잡아, 뒤따르는 본문이 자연히 그 위치에서 시작하게 한다.
     * 공백이 아니므로 양쪽정렬 분배 대상에서 제외된다 — 한글도 이 간격은 늘리지 않는다.
     */
    private static void addParagraphHead(TextPart part, ParagraphHead head, LineMetrics metrics) {
        CharMetrics cm = head.metrics();
        CharInfo ci = new CharInfo()
                .ch(head.ch())
                .charShape(head.charShape())
                .x(metrics.charX())
                .width(head.bodyOffset())
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(cm.baseSize)
                .isSpace(false)
                .paragraphHead(true);
        // 문단 번호("1." 등)는 두 글자 이상이라 CharInfo 하나에 문자열로 담는다.
        if (head.text().length() > 1) {
            ci.text(head.text()).codePoint(head.text().codePointAt(0));
        }
        part.addCharInfo(ci);
        metrics.appendNonSpace(head.bodyOffset(), cm.descent);
    }

    /** 각주/미주 자동 번호(각주 본문 앞의 "3)")인가. 그림·표 등 다른 자동 번호는 제외한다. */
    private static boolean isFootnoteAutoNumber(Control ctrl) {
        if (!(ctrl instanceof ControlAutoNumber)) return false;
        NumberSort sort = ((ControlAutoNumber) ctrl).getHeader().getProperty().getNumberSort();
        return sort == NumberSort.FootNote || sort == NumberSort.EndNote;
    }

    private static String text(kr.dogfoot.hwplib.object.etc.HWPString s) {
        return s == null ? "" : s.toUTF16LEString();
    }

    /**
     * 번호 표기("3)")를 CharInfo 하나로 배치한다. 각주 앵커(본문 안, 위첨자)와
     * 각주 본문 앞 자동 번호(정상 크기)가 같은 경로를 쓴다 — {@code script} 로만 갈린다.
     *
     * <p>문단 번호({@link #addParagraphHead})와 같이 여러 글자를 CharInfo 하나에 담는다.
     * 폭은 글자별 측정값의 합에 첨자 배율을 곱한 것이다.</p>
     */
    private static void addNumberMark(TextPart part, Paragraph para, int ci,
                                       HWPFileReader reader, CharMetricsMeasurer measurer,
                                       LineMetrics metrics, String mark,
                                       ScriptStyle.Script script) {
        if (mark.isEmpty()) return;
        CharShape charShape = reader.getCharShape(para, ci);
        double scale = script != null ? script.scale : 1.0;

        double width = 0;
        double descent = 0;
        CharMetrics cm = null;
        for (int i = 0; i < mark.length(); i++) {
            cm = measurer.measure(mark.charAt(i), charShape);
            width += cm.width;
            if (cm.descent > descent) descent = cm.descent;
        }
        width *= scale;

        CharInfo charInfo = new CharInfo()
                .ch(mark.charAt(0))
                .text(mark)
                .codePoint(mark.codePointAt(0))
                .charShape(charShape)
                .x(metrics.charX())
                .width(width)
                .height(cm.height)
                .descent(descent)
                .baseSize(cm.baseSize)
                .isSpace(false)
                .charIdx(ci)
                .script(script);
        part.addCharInfo(charInfo);

        double spacing = measurer.charSpacing(charShape, mark.charAt(0), width);
        metrics.appendNonSpace(width + spacing, spacing, descent);
    }

    private static LineMetrics fillCharInfos(TextPart part, LineMetrics metrics,
                                              Paragraph para, List<HWPChar> charList,
                                              int textStart, int textEnd,
                                              HWPFileReader reader, CharMetricsMeasurer measurer,
                                              ParaLayoutContext ctx, ParaShape paraShape,
                                              double segmentWidth) {
        int end = Math.min(textEnd, charList.size());
        List<Control> controls = para.getControlList();
        int ctrlIdx = countControlExtendBefore(charList, textStart);

        for (int ci = textStart; ci < end; ci++) {
            HWPChar hwpChar = charList.get(ci);
            // 보충문자(surrogate pair): 고위 서로게이트 + 다음 저위 서로게이트를 하나의
            // 코드포인트로 묶어 처리한다(네모친 숫자 U+F02Bx 등). 묶은 뒤 다음 글자는 건너뛴다.
            if (hwpChar.getType() == HWPCharType.Normal
                    && Character.isHighSurrogate((char) hwpChar.getCode())
                    && ci + 1 < end && charList.get(ci + 1).getType() == HWPCharType.Normal
                    && Character.isLowSurrogate((char) charList.get(ci + 1).getCode())) {
                addSupplementaryChar(part, para, ci, (char) hwpChar.getCode(),
                        (char) charList.get(ci + 1).getCode(), reader, measurer, metrics);
                ci++;
                continue;
            }
            if (hwpChar.getType() == HWPCharType.Normal) {
                addCharInfo(part, para, ci, hwpChar, reader, measurer, metrics);
            } else if (HWPFileReader.isFixedWidthBlank(hwpChar)) {
                addFixedWidthBlank(part, para, ci, reader, measurer, metrics);
            } else if (isTab(hwpChar)) {
                addTab(part, para, ci, charList, end, reader, measurer, metrics,
                        paraShape, ctx.defaultTabGap(), segmentWidth);
            } else if (hwpChar.getType() == HWPCharType.ControlExtend) {
                if (controls != null && ctrlIdx < controls.size()) {
                    Control ctrl = controls.get(ctrlIdx);
                    CtrlHeaderGso header = gsoHeaderOf(ctrl);
                    if (header != null && header.getProperty().isLikeWord()) {
                        addInlineControlPlaceholder(part, ci, header, ctx, metrics);
                    } else if (ctrl instanceof kr.dogfoot.hwplib.object.bodytext.control.ControlOverlappingLetter) {
                        addOverlappingLetter(part, para, ci,
                                (kr.dogfoot.hwplib.object.bodytext.control.ControlOverlappingLetter) ctrl,
                                reader, measurer, metrics);
                    } else if (ctrl instanceof ControlFootnote) {
                        CtrlHeaderFootnote h = ((ControlFootnote) ctrl).getHeader();
                        addNumberMark(part, para, ci, reader, measurer, metrics,
                                FootnoteNumberFormatter.format(h.getNumber(), h.getNumberShape(),
                                        text(h.getBeforeDecorationLetter()), text(h.getAfterDecorationLetter())),
                                ScriptStyle.forFootnoteMark());
                    } else if (isFootnoteAutoNumber(ctrl)) {
                        CtrlHeaderAutoNumber h = ((ControlAutoNumber) ctrl).getHeader();
                        addNumberMark(part, para, ci, reader, measurer, metrics,
                                FootnoteNumberFormatter.format(h.getNumber(), h.getProperty().getNumberShape(),
                                        text(h.getBeforeDecorationLetter()), text(h.getAfterDecorationLetter())),
                                null);
                    }
                }
                ctrlIdx++;
            }
        }
        return metrics;
    }

    /** 탭 문자(인라인 컨트롤 code=9)인지. */
    /** 줄의 마지막 글자가 강제 줄나눔(코드 10)인가 — 그런 줄은 양쪽정렬로 늘리지 않는다. */
    private static boolean endsWithForcedLineBreak(List<HWPChar> charList, int textEnd) {
        int last = textEnd - 1;
        if (last < 0 || last >= charList.size()) return false;
        HWPChar ch = charList.get(last);
        return ch.getType() == HWPCharType.ControlChar && ch.getCode() == 10;
    }

    private static boolean isTab(HWPChar ch) {
        return ch.getType() == HWPCharType.ControlInline
                && ((HWPCharControlInline) ch).getCode() == 9;
    }

    /**
     * 탭을 배치한다: 탭 정의(ParaShape.tabDefId)의 탭 스톱까지 진행하고, 채우기 종류에
     * 따라 그 구간에 점/점선/실선 리더를 그리도록 표시한다.
     *
     * <p>탭 정렬 종류별 텍스트 배치(그림 기준):
     * <ul>
     *   <li>왼쪽(Left): 뒤따르는 텍스트가 탭 스톱에서 <b>시작</b>한다.</li>
     *   <li>가운데(Center): 텍스트의 중심이 탭 스톱에 온다.</li>
     *   <li>오른쪽(Right)/소수점(DecimalPoint): 텍스트가 탭 스톱에서 <b>끝난다</b>.</li>
     * </ul>
     * 오른쪽/가운데 정렬은 뒤따르는 텍스트(다음 탭 또는 줄 끝까지)의 폭을 미리 재서
     * 시작 위치를 당긴다(목차의 페이지 번호 우측 정렬).</p>
     *
     * <p><b>탭 스톱은 세그먼트 오른쪽 경계로 제한된다.</b> 탭 정의에 줄 폭보다 먼 위치가
     * 들어 있으면 한글은 그 자리에 세우지 않고 줄의 오른쪽 끝에 세운다. 실문서 R03
     * 목차가 이 경우다 — 15줄이 tabDef 4({@code pos=88150})와 6({@code pos=83268})으로
     * 서로 다른 정의를 쓰는데도 참조 출력에서는 페이지 번호가 모두 같은 x(796~815px)에
     * 모인다. 즉 정의된 위치가 아니라 공통의 오른쪽 경계가 실제 탭 스톱이다.
     * 제한하지 않으면 점선 리더가 셀 밖까지 이어지고 페이지 번호가 잘려 사라진다.
     * (NoLineSeg 경로는 {@code NoLineSegLayouter.processTab()}에서 이미 같은 제한을 한다.)</p>
     */
    private static void addTab(TextPart part, Paragraph para, int tabCi, List<HWPChar> charList,
                                int textEnd, HWPFileReader reader, CharMetricsMeasurer measurer,
                                LineMetrics metrics, ParaShape paraShape, long defaultTabGap,
                                double segmentWidth) {
        double currentX = metrics.charX();
        CharShape charShape = reader.getCharShape(para, tabCi);
        CharMetrics cm = measurer.measure(' ', charShape);

        TabInfo tab = TabStopHelper.findTabStop(reader, paraShape, currentX);
        double tabStopX = TabStopHelper.nextTabStopX(reader, paraShape, currentX, defaultTabGap);
        if (segmentWidth > 0 && tabStopX > segmentWidth) tabStopX = segmentWidth;
        TabSort sort;
        boolean dotFill;
        if (tab != null) {
            sort = tab.getTabSort();
            dotFill = tab.getFillSort() != null && tab.getFillSort() != BorderType.None;
        } else {
            sort = TabSort.Left;
            dotFill = false;
        }

        double followW = measureRunWidth(para, charList, tabCi + 1, textEnd, reader, measurer);
        double textStartX;
        switch (sort) {
            case Right:
            case DecimalPoint: textStartX = tabStopX - followW; break;
            case Center:       textStartX = tabStopX - followW / 2.0; break;
            default:           textStartX = tabStopX; break;   // Left
        }
        if (textStartX < currentX) textStartX = currentX;      // 이미 지난 탭 스톱: 진행 없음
        double fillWidth = textStartX - currentX;

        CharInfo ci = new CharInfo()
                .ch('\t')
                .charShape(charShape)
                .x(currentX)
                .width(fillWidth)
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(cm.baseSize)
                .isSpace(false)
                .dotLeader(dotFill && fillWidth > 0)
                .tabFillSort(dotFill ? tab.getFillSort() : null);
        part.addCharInfo(ci);
        metrics.appendNonSpace(fillWidth, cm.descent);
    }

    /** [from, to) 범위에서 다음 탭 또는 끝까지의 일반 글자 자연 폭 합. */
    private static double measureRunWidth(Paragraph para, List<HWPChar> charList, int from, int to,
                                           HWPFileReader reader, CharMetricsMeasurer measurer) {
        double w = 0;
        for (int i = from; i < to && i < charList.size(); i++) {
            HWPChar hc = charList.get(i);
            if (isTab(hc)) break;
            if (hc.getType() != HWPCharType.Normal) continue;
            char ch = HWPFileReader.normalizeChar((char) hc.getCode());
            CharShape cs = reader.getCharShape(para, i);
            CharMetrics cm = measurer.measure(ch, cs);
            w += cm.width + measurer.charSpacing(cs, ch, cm.width);
        }
        return w;
    }

    private static int countControlExtendBefore(List<HWPChar> charList, int pos) {
        int count = 0;
        for (int i = 0; i < pos && i < charList.size(); i++) {
            if (charList.get(i).getType() == HWPCharType.ControlExtend) count++;
        }
        return count;
    }

    private static void addInlineControlPlaceholder(TextPart part, int charIdx,
                                                     CtrlHeaderGso header,
                                                     ParaLayoutContext ctx, LineMetrics metrics) {
        long ctrlWidth = ControlAreaCalculator.resolveWidth(header, ctx.paperArea(), ctx.bodyArea());
        long ctrlHeight = ControlAreaCalculator.resolveHeight(header, ctx.paperArea(), ctx.bodyArea());
        long totalWidth = ctrlWidth + header.getOutterMarginLeft() + header.getOutterMarginRight();
        long totalHeight = ctrlHeight + header.getOutterMarginTop() + header.getOutterMarginBottom();

        CharInfo ci = new CharInfo()
                .ch('\u0000')
                .x(metrics.charX())
                .width(totalWidth)
                .height(totalHeight)
                .descent(0)
                .baseSize(0)
                .isSpace(false)
                .charIdx(charIdx)
                .inlineControl(true);
        part.addCharInfo(ci);
        metrics.appendNonSpace(totalWidth, 0);
    }

    private static CtrlHeaderGso gsoHeaderOf(Control ctrl) {
        switch (ctrl.getType()) {
            case Table: return ((ControlTable) ctrl).getHeader();
            case Gso:   return ((GsoControl) ctrl).getHeader();
            default:    return null;
        }
    }

    /**
     * 묶음 빈칸(30)/고정폭 빈칸(31)을 일반 공백의 절반 폭(baseSize/4)으로 배치한다 (한글 측정).
     * isSpace=false 로 두어 양쪽정렬 분배·공백 최소값 압축 대상에서 제외한다.
     */
    private static void addFixedWidthBlank(TextPart part, Paragraph para, int ci,
                                            HWPFileReader reader, CharMetricsMeasurer measurer,
                                            LineMetrics metrics) {
        CharShape charShape = reader.getCharShape(para, ci);
        CharMetrics cm = measurer.measure(' ', charShape);
        double width = cm.width / 2.0;

        CharInfo charInfo = new CharInfo()
                .ch(' ')
                .charShape(charShape)
                .x(metrics.charX())
                .width(width)
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(cm.baseSize)
                .isSpace(false);
        part.addCharInfo(charInfo);

        double spacing = measurer.charSpacing(charShape, ' ', width);
        metrics.appendNonSpace(width + spacing, spacing, cm.descent);
    }

    /** 보충문자(surrogate pair)를 하나의 CharInfo로 배치한다. 코드포인트로 폭·글리프를 얻는다. */
    private static void addSupplementaryChar(TextPart part, Paragraph para, int ci,
                                             char high, char low, HWPFileReader reader,
                                             CharMetricsMeasurer measurer, LineMetrics metrics) {
        addComposedChar(part, para, ci, new String(new char[]{high, low}),
                Character.toCodePoint(high, low), reader, measurer, metrics);
    }

    /**
     * 글자겹침 컨트롤(ControlExtend 코드 23)을 배치한다. 겹침 글자 목록이 하나의
     * 코드포인트를 이루는 경우(네모친 숫자 U+F02Bx가 서로게이트 반쪽 2개로 저장됨,
     * 실문서 R03 '보고서 전개방식/표준서식' heading)만 지원 — 보충문자와 동일하게
     * 전각 한 칸으로 배치한다. 그 외 형태(여러 글자 합성)는 아직 미지원.
     */
    private static void addOverlappingLetter(TextPart part, Paragraph para, int ci,
            kr.dogfoot.hwplib.object.bodytext.control.ControlOverlappingLetter ol,
            HWPFileReader reader, CharMetricsMeasurer measurer, LineMetrics metrics) {
        // 겹침 글자 하나가 서로게이트 반쪽(HWPString 2바이트) 단위로 쪼개져 있으므로,
        // 개별 디코드(고립 서로게이트 → U+FFFD 대체) 대신 바이트를 합쳐 한 번에 디코드한다.
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        for (kr.dogfoot.hwplib.object.etc.HWPString hs : ol.getHeader().getOverlappingLetterList()) {
            byte[] b = hs.getBytes();
            if (b != null) bytes.write(b, 0, b.length);
        }
        String text = new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_16LE);
        if (text.isEmpty()) return;
        if (text.codePointCount(0, text.length()) == 1) {
            addComposedChar(part, para, ci, text, text.codePointAt(0), reader, measurer, metrics);
        } else {
            addOverlappedChars(part, para, ci, text, reader, measurer, metrics);
        }
    }

    /**
     * 겹침 글자 여러 개를 <b>같은 자리에 포개어</b> 배치한다.
     *
     * <p>한글의 글자겹침은 여러 글자를 한 칸에 겹쳐 하나의 글자처럼 보이게 한다 —
     * 예: ⑮는 U+F0289(원)와 U+F0297(숫자)의 겹침이다(실문서 R15 ⑮~㉑).
     * 전각 한 칸만 차지하므로 진행(advance)은 한 번만 더하고, 폭이 다른 글자는
     * 가장 넓은 글자를 기준으로 가운데에 맞춘다.</p>
     */
    private static void addOverlappedChars(TextPart part, Paragraph para, int ci, String text,
                                           HWPFileReader reader, CharMetricsMeasurer measurer,
                                           LineMetrics metrics) {
        CharShape charShape = reader.getCharShape(para, ci);

        List<int[]> codePoints = new ArrayList<>();       // {codePoint}
        List<CharMetrics> sizes = new ArrayList<>();
        double maxWidth = 0, maxDescent = 0;
        for (int off = 0; off < text.length(); ) {
            int cp = text.codePointAt(off);
            CharMetrics cm = measurer.measureCodePoint(new String(Character.toChars(cp)), cp, charShape);
            codePoints.add(new int[]{cp});
            sizes.add(cm);
            maxWidth = Math.max(maxWidth, cm.width);
            maxDescent = Math.max(maxDescent, cm.descent);
            off += Character.charCount(cp);
        }

        // 폭은 첫 글자에만 싣는다 — 겹친 글자들이 한 칸의 폭을 나눠 갖는 게 아니므로,
        // 나머지를 0폭으로 두어야 줄 폭 계산(최소공백·양쪽정렬)이 부풀지 않는다.
        double baseX = metrics.charX();
        for (int i = 0; i < codePoints.size(); i++) {
            int cp = codePoints.get(i)[0];
            CharMetrics cm = sizes.get(i);
            String one = new String(Character.toChars(cp));
            part.addCharInfo(new CharInfo()
                    .ch(one.charAt(0))
                    .text(one)
                    .codePoint(cp)
                    .charShape(charShape)
                    .x(baseX + (maxWidth - cm.width) / 2.0)
                    .width(i == 0 ? maxWidth : 0)
                    .height(cm.height)
                    .descent(cm.descent)
                    .baseSize(cm.baseSize)
                    .isSpace(false)
                    .charIdx(ci));
        }

        double spacing = measurer.charSpacing(charShape, '가', maxWidth);
        metrics.appendNonSpace(maxWidth + spacing, spacing, maxDescent);
    }

    /** 합성 글자(text + codePoint)를 전각 한 칸 CharInfo로 배치한다. */
    private static void addComposedChar(TextPart part, Paragraph para, int ci,
                                        String text, int codePoint, HWPFileReader reader,
                                        CharMetricsMeasurer measurer, LineMetrics metrics) {
        CharShape charShape = reader.getCharShape(para, ci);
        CharMetrics cm = measurer.measureCodePoint(text, codePoint, charShape);

        CharInfo charInfo = new CharInfo()
                .ch(text.charAt(0))
                .text(text)
                .codePoint(codePoint)
                .charShape(charShape)
                .x(metrics.charX())
                .width(cm.width)
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(cm.baseSize)
                .isSpace(false)
                .charIdx(ci);
        part.addCharInfo(charInfo);

        double spacing = measurer.charSpacing(charShape, '가', cm.width);
        metrics.appendNonSpace(cm.width + spacing, spacing, cm.descent);
    }

    private static void addCharInfo(TextPart part, Paragraph para, int ci, HWPChar hwpChar,
                                     HWPFileReader reader, CharMetricsMeasurer measurer,
                                     LineMetrics metrics) {
        char ch = HWPFileReader.normalizeChar((char) hwpChar.getCode());
        CharShape charShape = reader.getCharShape(para, ci);
        CharMetrics cm = measurer.measure(ch, charShape);
        boolean isSpace = (ch == ' ' || ch == '\t');

        CharInfo charInfo = new CharInfo()
                .ch(ch)
                .charShape(charShape)
                .x(metrics.charX())
                .width(cm.width)
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(cm.baseSize)
                .isSpace(isSpace);
        part.addCharInfo(charInfo);

        double spacing = measurer.charSpacing(charShape, ch, cm.width);
        double advance = cm.width + spacing;
        if (isSpace) {
            // 밑줄 그은 공백은 가시(밑줄이 보임)이므로 정렬 기준 폭에 포함시킨다.
            metrics.appendSpace(advance, spacing, cm.descent, isUnderlined(charShape));
        } else {
            metrics.appendNonSpace(advance, spacing, cm.descent);
        }
    }

    /** 글자에 밑줄이 그어져 있는지 (None 이 아니면 밑줄 있음). */
    private static boolean isUnderlined(CharShape charShape) {
        kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort sort =
                charShape.getProperty().getUnderLineSort();
        return sort != null
                && sort != kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort.None;
    }

    private static void applyMinimumSpaceIfNeeded(TextPart part, ParaShape paraShape,
                                                   double segmentWidth, LineMetrics metrics) {
        double shrinkShift = MinimumSpaceProcessor.apply(part, paraShape, segmentWidth);
        if (shrinkShift != 0) {
            metrics.shiftX(-shrinkShift);
        }
    }

    private static void applyAlignmentAndStretch(TextPart part, TextLine.Alignment alignment,
                                                  boolean isLastLine, double segmentWidth,
                                                  LineMetrics metrics, double lineLeft) {
        double stretchShift = LineAlignmentProcessor.stretchJustify(
                part, alignment, isLastLine, segmentWidth, metrics.visibleCharX());
        if (stretchShift != 0) {
            metrics.shiftX(stretchShift);
        }
        LineAlignmentProcessor.applyAlignmentOffset(
                part, alignment, segmentWidth, metrics.visibleCharX(), lineLeft);
    }

    private static void finalizeLine(TextLine line, TextPart part, LineMetrics metrics,
                                      boolean isLastLine) {
        part.width(isLastLine ? metrics.visibleCharX() : metrics.charX());
        line.maxCharDescent(Math.round(metrics.maxDescent()));
        line.lastInPara(isLastLine);
    }
}
