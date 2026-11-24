package kr.dogfoot.hwpdrawer.engine.para;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.document.text.TextLine;

/**
 * 한 문단을 레이아웃하는 컴포넌트의 공통 인터페이스.
 *
 * 구현체는 ctx.currentY와 ctx.currentPage를 진행 상태에 맞게 갱신한다.
 */
public interface ParagraphLayouter {
    /**
     * 문단을 레이아웃한다.
     *
     * @param para      대상 문단
     * @param paraIdx   문단 인덱스
     * @param paraShape 문단 서식
     * @param leftMargin  본문 영역 기준 라인 좌측 (절대 X)
     * @param rightMargin 본문 영역 기준 라인 우측 (절대 X)
     * @param indent    첫 줄 들여쓰기 (HWP 단위)
     * @param alignment 정렬 방식
     * @param ctx       공유 컨텍스트 (currentY, currentPage 등)
     */
    void layout(Paragraph para, int paraIdx, ParaShape paraShape,
                long leftMargin, long rightMargin, long indent,
                TextLine.Alignment alignment, ParaLayoutContext ctx);
}
