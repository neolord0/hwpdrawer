package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.docinfo.Bullet;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.Numbering;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.numbering.LevelNumbering;
import kr.dogfoot.hwplib.object.docinfo.numbering.ParagraphHeadInfo;
import kr.dogfoot.hwplib.object.docinfo.numbering.ParagraphHeadInfoProperty;
import kr.dogfoot.hwplib.object.docinfo.numbering.ValueType;
import kr.dogfoot.hwplib.object.docinfo.parashape.ParaHeadShape;
import kr.dogfoot.hwpdrawer.engine.CharMetrics;
import kr.dogfoot.hwpdrawer.engine.CharMetricsMeasurer;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.util.List;

/**
 * 문단 머리(글머리표) — 문단 텍스트에는 없지만 문단 첫 줄 앞에 그려지는 기호.
 *
 * <p>한글의 "문단 번호/글머리표" 중 <b>글머리표</b>({@link ParaHeadShape#Bullet})만 지원한다.
 * 개요·문단 번호({@link ParaHeadShape#Outline}/{@link ParaHeadShape#Numbering})는 수준별
 * 번호 서식을 조립해야 하므로 아직 미지원 — 그런 문단은 {@link #of}가 null을 돌려준다.</p>
 *
 * <p><b>본문 오프셋:</b> 글머리표는 문단 시작 위치에 놓이고 본문은 그 오른쪽
 * {@code 글머리표 폭 + 본문과의 거리}부터 시작한다. 이 거리는 글머리표 정의
 * ({@link ParagraphHeadInfo#getDistanceFromBody()})에 담기며, 값 종류가
 * {@link ValueType#RatioForLetter}이면 글자 크기에 대한 <b>%</b>, 아니면 절대값(HWPUNIT)이다.
 * 문단의 내어쓰기(indent)는 이 오프셋과 <b>별도로</b> 둘째 줄부터 추가 적용된다.</p>
 */
public final class ParagraphHead {

    /** 글머리표 문자모양 ID가 이 값이면 "본문 글자모양을 따름"을 뜻한다. */
    private static final long CHAR_SHAPE_ID_FOLLOW_BODY = 0xFFFFFFFFL;

    private final String text;
    private final CharShape charShape;
    private final CharMetrics metrics;
    private final double bodyOffset;

    private ParagraphHead(String text, CharShape charShape, CharMetrics metrics, double bodyOffset) {
        this.text = text;
        this.charShape = charShape;
        this.metrics = metrics;
        this.bodyOffset = bodyOffset;
    }

    /** 문단 머리 문자열. 글머리표는 한 글자, 문단 번호는 "1." 처럼 여러 글자일 수 있다. */
    public String text() { return text; }

    /** 문단 머리의 첫 글자 — 폰트·장평 조회용. */
    public char ch() { return text.charAt(0); }

    /** 글머리표를 그릴 글자모양. */
    public CharShape charShape() { return charShape; }

    /** 글머리표 글자의 측정값(폭/높이/디센트). */
    public CharMetrics metrics() { return metrics; }

    /** 문단 시작 위치에서 본문 첫 글자까지의 거리(HWP 단위). */
    public double bodyOffset() { return bodyOffset; }

    /**
     * 문단에 글머리표가 있으면 그 정보를, 없거나 미지원 종류이면 null을 반환한다.
     */
    public static ParagraphHead of(HWPFileReader reader, Paragraph para, ParaShape paraShape,
                                   CharMetricsMeasurer measurer, ParagraphNumberer numberer) {
        switch (paraShape.getProperty1().getParaHeadShape()) {
            case Bullet:
                return bulletHead(reader, para, paraShape, measurer);
            case Numbering:
            case Outline:
                return numberingHead(reader, para, paraShape, measurer, numberer);
            default:
                return null;
        }
    }

    /** 글머리표(기호) 머리. */
    private static ParagraphHead bulletHead(HWPFileReader reader, Paragraph para,
                                            ParaShape paraShape, CharMetricsMeasurer measurer) {
        Bullet bullet = bulletOf(reader, paraShape.getParaHeadId());
        if (bullet == null || bullet.getBulletChar() == null) return null;
        String text = bullet.getBulletChar().toUTF16LEString();
        if (text == null || text.isEmpty()) return null;
        return build(reader, para, text.substring(0, 1), bullet.getParagraphHeadInfo(), measurer);
    }

    /**
     * 문단 번호 머리. 번호 문자열은 {@link ParagraphNumberer}가 문서 순서로 미리 확정해 둔 값을
     * 쓴다 — 번호는 앞 문단에 의존하는 누적 상태라 문단 하나만 보고 정할 수 없다.
     */
    private static ParagraphHead numberingHead(HWPFileReader reader, Paragraph para,
                                               ParaShape paraShape, CharMetricsMeasurer measurer,
                                               ParagraphNumberer numberer) {
        if (numberer == null) return null;
        String text = numberer.headTextOf(para);
        if (text == null || text.isEmpty()) return null;

        Numbering numbering = numberingOf(reader, paraShape.getParaHeadId());
        if (numbering == null) return null;
        // 수준 계산 규칙은 ParagraphNumberer.levelOf()와 같아야 한다(두 필드 중 큰 값).
        int level = Math.max((int) paraShape.getParaLevel(), paraShape.getProperty1().getParaLevel());
        List<LevelNumbering> levels = numbering.getLevelNumberingList();
        if (level < 0 || level >= levels.size()) return null;

        return build(reader, para, text, levels.get(level).getParagraphHeadInfo(), measurer);
    }

    private static ParagraphHead build(HWPFileReader reader, Paragraph para, String text,
                                       ParagraphHeadInfo info, CharMetricsMeasurer measurer) {
        CharShape charShape = charShapeOf(reader, para, info);
        if (charShape == null) return null;

        CharMetrics metrics = measure(text, charShape, measurer);
        double bodyOffset = headWidth(info, charShape, metrics)
                + distanceFromBody(info, charShape.getBaseSize());
        return new ParagraphHead(text, charShape, metrics, bodyOffset);
    }

    /** 문자열 전체의 측정값 — 폭은 글자 폭의 합, 높이·디센트는 최대값. */
    private static CharMetrics measure(String text, CharShape charShape,
                                       CharMetricsMeasurer measurer) {
        CharMetrics total = new CharMetrics();
        for (int i = 0; i < text.length(); i++) {
            CharMetrics m = measurer.measure(text.charAt(i), charShape);
            total.width += m.width;
            total.height = Math.max(total.height, m.height);
            total.descent = Math.max(total.descent, m.descent);
            total.baseSize = Math.max(total.baseSize, m.baseSize);
        }
        return total;
    }

    /**
     * 번호 서식의 자리표시자 {@code ^N}(N = 1~7, N번째 수준의 번호)을 실제 번호로 바꾼다.
     * 예: {@code "^1."} → {@code "1."}
     *
     * <p><b>한계:</b> 각 수준의 <b>시작 번호</b>를 그대로 쓴다. 문서를 순회하며 번호를
     * 증가시키는 카운터는 없으므로, 같은 수준의 번호 문단이 여러 개면 두 번째부터 번호가
     * 틀린다. 현재 테스트 문서(실문서 R17·R18)는 번호 문단이 문서당 하나뿐이다.</p>
     */
    private static String expandNumberFormat(String format, List<LevelNumbering> levels) {
        StringBuilder out = new StringBuilder(format.length());
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            int levelIndex = (c == '^' && i + 1 < format.length())
                    ? Character.digit(format.charAt(i + 1), 10) - 1 : -1;
            if (levelIndex < 0 || levelIndex >= levels.size()) {
                out.append(c);
                continue;
            }
            out.append(levels.get(levelIndex).getStartNumber());
            i++;   // 자리표시자의 숫자까지 소비
        }
        return out.toString();
    }

    /** paraHeadId는 1부터 시작한다(0 = 없음). */
    private static Numbering numberingOf(HWPFileReader reader, int paraHeadId) {
        if (paraHeadId <= 0) return null;
        try {
            return reader.hwpFile().getDocInfo().getNumberingList().get(paraHeadId - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** paraHeadId는 1부터 시작한다(0 = 없음). */
    private static Bullet bulletOf(HWPFileReader reader, int paraHeadId) {
        if (paraHeadId <= 0) return null;
        try {
            return reader.hwpFile().getDocInfo().getBulletList().get(paraHeadId - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** 글머리표 글자모양. "본문 따름"이면 문단 첫 글자의 글자모양을 쓴다. */
    private static CharShape charShapeOf(HWPFileReader reader, Paragraph para,
                                         ParagraphHeadInfo info) {
        if (info != null && info.getCharShapeID() != CHAR_SHAPE_ID_FOLLOW_BODY) {
            try {
                return reader.hwpFile().getDocInfo().getCharShapeList()
                        .get((int) info.getCharShapeID());
            } catch (IndexOutOfBoundsException ignored) {
                // 잘못된 참조 — 본문 글자모양으로 대체
            }
        }
        return reader.getCharShape(para, 0);
    }

    /**
     * 글머리표가 차지하는 폭(HWP 단위).
     *
     * <p>"글자 폭 따름"({@link ParagraphHeadInfoProperty#isFollowStringWidth()})이 켜져 있으면
     * 글머리표 글자의 실제 advance를 쓴다. 꺼져 있으면 한글은 글자 폭과 무관한 고정 슬롯에
     * 글머리표를 넣는데, 실문서 R12(11pt '-' 글머리표) 한글 출력과 맞춰 보면 그 폭이
     * <b>글자 크기의 절반</b>이다. 표본이 이 한 건뿐이라 다른 크기·기호에서 어긋날 수 있다.</p>
     */
    private static double headWidth(ParagraphHeadInfo info, CharShape charShape,
                                    CharMetrics metrics) {
        boolean followStringWidth = info != null && info.getProperty() != null
                && info.getProperty().isFollowStringWidth();
        return followStringWidth ? metrics.width : charShape.getBaseSize() / 2.0;
    }

    /**
     * 글머리표와 본문 사이 거리(HWP 단위).
     *
     * <p>{@code baseSize}는 <b>글자 크기</b>(CharShape.baseSize)여야 한다 —
     * {@link CharMetrics#baseSize}는 측정된 ascent라서 여기에 쓰면 간격이 좁아진다.</p>
     */
    private static double distanceFromBody(ParagraphHeadInfo info, double baseSize) {
        if (info == null) return 0;
        int value = info.getDistanceFromBody();
        boolean ratio = info.getProperty() != null
                && info.getProperty().getValueTypeForDistanceFromBody() == ValueType.RatioForLetter;
        return ratio ? baseSize * value / 100.0 : value;
    }
}
