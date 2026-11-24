package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;

/**
 * WordLayouter 버퍼에 담긴 한 글자.
 *
 * <p>advance = width + 자간, charIdx는 문단 내 글자 index.
 * hangul/digit은 {@link WordSplitter}의 언어 청크 분리에 쓰인다.</p>
 */
final class PendingChar {
    final CharInfo charInfo;
    final double advance;
    final boolean hangul;
    final boolean digit;
    final int charIdx;

    PendingChar(CharInfo charInfo, double advance, boolean hangul, boolean digit, int charIdx) {
        this.charInfo = charInfo;
        this.advance = advance;
        this.hangul = hangul;
        this.digit = digit;
        this.charIdx = charIdx;
    }
}
