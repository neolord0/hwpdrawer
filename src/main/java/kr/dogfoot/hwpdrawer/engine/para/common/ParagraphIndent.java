package kr.dogfoot.hwpdrawer.engine.para.common;

/**
 * 문단 들여쓰기/내어쓰기(indent)를 라인별 offset으로 변환한다.
 *
 * <ul>
 *   <li><b>들여쓰기 (indent &gt; 0)</b>: 첫 라인만 오른쪽으로 indent만큼 밀림</li>
 *   <li><b>내어쓰기 (indent &lt; 0)</b>: 첫 라인 이외의 라인이 |indent|만큼 밀림</li>
 *   <li><b>indent == 0</b>: 아무 효과 없음</li>
 * </ul>
 */
public final class ParagraphIndent {
    private ParagraphIndent() {}

    /**
     * 주어진 라인에 적용할 indent offset을 반환한다. 항상 ≥ 0.
     * @param indent     paraShape에서 읽은 indent (양수=들여쓰기, 음수=내어쓰기)
     * @param firstLine  이 라인이 문단의 첫 라인인지
     */
    public static double offsetFor(double indent, boolean firstLine) {
        if (firstLine) {
            return (indent > 0) ? indent : 0;
        }
        return (indent < 0) ? -indent : 0;
    }
}
