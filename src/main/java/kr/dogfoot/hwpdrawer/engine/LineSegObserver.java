package kr.dogfoot.hwpdrawer.engine;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;

import java.util.List;

/**
 * 레이아웃 과정에서 각 문단별로 (원본 LineSeg vs 합성 LineSeg) 쌍을 관찰하기 위한 훅.
 *
 * <p>주로 합성기(LineSegSynthesizer)의 정확도를 검증하는 테스트에서 사용한다.
 * 옵저버가 설정되어 있으면 {@link LayoutEngine}은 lineseg가 있는 문단에 대해서도
 * 합성을 수행하여 원본과 함께 전달한다. 옵저버가 없으면 합성은 생략된다.</p>
 */
public interface LineSegObserver {
    void onPair(int paraIdx, Paragraph para, List<LineSegItem> originalItems,
                List<LineSegItem> synthesizedItems);
}
