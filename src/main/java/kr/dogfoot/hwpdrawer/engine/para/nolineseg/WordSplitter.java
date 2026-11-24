package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import java.util.ArrayList;
import java.util.List;

/**
 * 언어 경계로 word를 쪼개 처리 (spanningWord의 not-allByWord 분기).
 *
 * <p>≈ old_version {@code WordSplitter}.
 * 숫자는 언어 경계를 만들지 않고 현재 청크에 흡수된다 — "숫자는 단어로 인식".
 * 순수 숫자 청크는 영어 설정과 무관하게 항상 ByWord(단어)로 처리한다.</p>
 */
final class WordSplitter {
    private final WordLayouter wordLayouter;
    private final LineWriter line;
    private final boolean hangulByWord;
    private final boolean englishByWord;

    WordSplitter(WordLayouter wordLayouter, LineWriter line,
                 boolean hangulByWord, boolean englishByWord) {
        this.wordLayouter = wordLayouter;
        this.line = line;
        this.hangulByWord = hangulByWord;
        this.englishByWord = englishByWord;
    }

    void splitByLineAndAdd(List<PendingChar> wordChars) {
        List<LanguageChunk> chunks = splitByLanguage(wordChars);
        List<PendingChar> carryOver = new ArrayList<>();

        for (int chunkIdx = 0; chunkIdx < chunks.size(); chunkIdx++) {
            LanguageChunk chunk = chunks.get(chunkIdx);

            // 이전 청크에서 넘어온 행말금칙 글자를 이 청크 앞에 붙인다.
            if (!carryOver.isEmpty()) {
                chunk.chars.addAll(0, carryOver);
                for (PendingChar pc : carryOver) chunk.width += pc.charInfo.width();
                carryOver.clear();
            }

            // 순수 숫자 청크는 영어 설정과 무관하게 항상 단어(ByWord)로 다룬다 — "숫자는 단어로 인식".
            boolean effectiveByWord = chunk.allDigit
                    ? true
                    : (chunk.hangul ? hangulByWord : englishByWord);
            boolean splitByLetter = !effectiveByWord;
            if (splitByLetter) {
                addChunkCharByChar(chunk.chars);
            } else {
                addChunkAsOneWord(chunk);
            }

            // 행말금칙: 라인 끝이 여는 괄호 류이면 그 글자를 다음 청크로 이월한다.
            // (다음 청크가 없으면 단어 끝이므로 그대로 둔다.)
            if (chunkIdx + 1 < chunks.size()) {
                PendingChar popped = popLineEndForbiddenChar();
                if (popped != null) carryOver.add(popped);
            }
        }
    }

    /** 라인 마지막 글자가 행말금칙이면 팝해서 반환, 아니면 null. */
    private PendingChar popLineEndForbiddenChar() {
        java.util.List<kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo> charInfos =
                line.textPart().charInfos();
        if (charInfos.isEmpty()) return null;
        char lastCh = charInfos.get(charInfos.size() - 1).ch();
        if (!isLineEndForbidden(lastCh)) return null;
        return line.popLastChar();
    }

    /**
     * ByLetter 경로로 청크를 한 글자씩 현재 라인에 추가.
     *
     * <p>일반 글자 경계는 자유롭게 분할하지만 "연결 기호"(·) 바로 앞/뒤 경계에서는 분할하지 않는다.
     * 예: "제조·판매업을" + 한글=ByLetter → "제|조·판매업을"는 허용, "조|·"·"·|판"는 금지.
     * HWP 관습상 "·"는 줄 시작/끝에 올 수 없으므로 양쪽 글자와 붙어서 이동한다.</p>
     *
     * <p>overflow 체크는 applyMinimumSpace=true — 최소공백 축소를 반영한 라인 폭으로 판단해
     * 더 많은 글자가 한 줄에 들어가도록 한다.</p>
     */
    private void addChunkCharByChar(List<PendingChar> chars) {
        int n = chars.size();
        int start = 0;
        while (start < n) {
            int splitEnd = findLegalPrefixEnd(chars, start);
            if (splitEnd == start) {
                // 합법 경계로 들어갈 글자가 없다.
                if (!line.noDrawingChar()) {
                    wordLayouter.flow.saveTextLineAndNewLine();
                    continue;
                }
                // 빈 라인인데도 legal prefix가 0 → 첫 글자가 "·" 인접이거나 폭 부족.
                // 무한루프 방지로 최소 한 글자는 강제 추가.
                line.addChar(chars.get(start));
                start++;
                if (start < n) wordLayouter.flow.saveTextLineAndNewLine();
            } else {
                for (int k = start; k < splitEnd; k++) line.addChar(chars.get(k));
                start = splitEnd;
                if (start < n) wordLayouter.flow.saveTextLineAndNewLine();
            }
        }
    }

    /**
     * chars[start..] 에서 현재 라인에 들어가는 "합법" prefix의 end-exclusive index.
     *
     * <p>합법 경계 조건 (모두 만족해야 합법):
     * <ol>
     *   <li>"·" 인접이 아님 — 연결 기호 앞뒤는 분할 금지.</li>
     *   <li>"고립 숫자" break가 아님 — chars[nextIdx]가 숫자이고 chars[nextIdx+1]이 비-숫자인
     *       경우, 즉 분할 후 다음 라인이 비-숫자 바로 앞의 숫자 1개로 시작하게 되는 경우 금지.
     *       예: "제44기" → "제4|4기" (4 뒤가 기=비digit → 고립) 불법, "제|44기" 합법. ✓
     *           "万9000원" → "万9|000원" (9 뒤가 0=digit → 고립 아님) 합법. ✓</li>
     * </ol></p>
     */
    private int findLegalPrefixEnd(List<PendingChar> chars, int start) {
        int n = chars.size();
        int bestEnd = start;
        double accWidth = 0;
        for (int i = start; i < n; i++) {
            PendingChar pc = chars.get(i);
            if (line.isOverWidth(accWidth + pc.charInfo.width(), true)) {
                // 행두금칙(닫는 괄호·문장부호)이 연속돼 합법 break가 하나도 없었지만
                // 일부 글자는 현재 라인에 들어간 경우: 들어간 만큼은 현재 줄에 채우고
                // 넘치는 글자 앞에서 끊는다. (닫는 괄호 run "))!))))" 가 줄 끝에 걸칠 때,
                // 통째로 다음 줄로 미루지 않고 들어가는 "))!" 는 줄 끝에 유지 — HWP 동작.
                // 넘친 글자(chars[i])가 닫는 괄호류일 때만 적용해 "·"·고립숫자 결합은 보존.
                if (bestEnd == start && i > start
                        && isLineStartForbidden(pc.charInfo.ch())) {
                    return i;
                }
                return bestEnd;   // i번째 글자 넣으면 오버플로 → 지금까지의 합법 bestEnd 반환
            }
            accWidth += pc.charInfo.width();
            int nextIdx = i + 1;
            // 다음 위치(nextIdx)가 "고립 숫자"(lone digit)이면 분할 금지.
            // 고립 숫자: 현재 글자(pc)가 digit이고, chars[nextIdx]도 digit이고 chars[nextIdx+1]이 비-digit.
            //   "제44기": "제4|4기" 시도 시 pc='4'(digit), chars[2]='4'(digit), chars[3]='기'(비digit) → 고립 → 불법.
            //             "제|44기" 시도 시 pc='제'(비digit) → 숫자 시퀀스 중간 아님 → 합법. ✓
            //   "만9000원": "만9|000원" 시도 시 chars[2]='0'(digit), chars[3]='0'(digit) → 고립 아님 → 합법. ✓
            // ※ 현재 글자가 비-digit이면 숫자 앞에서 끊는 정상 분할이므로 막지 않는다.
            //   예: "우우우2우우" → "우우우|2우우" (pc='우' 비digit → 허용). 이전엔 "2" 다음이
            //   "우"라 고립으로 오판해 "우우|우2"로 후퇴하던 버그.
            boolean loneDigitBreak = pc.digit
                    && nextIdx < n
                    && chars.get(nextIdx).digit
                    && nextIdx + 1 < n
                    && !chars.get(nextIdx + 1).digit;
            // 행말금칙: 여는 괄호로 줄이 끝나면 안 됨 → 그 앞에서 끊는다.
            // 행두금칙: 닫는 괄호·문장부호로 줄이 시작되면 안 됨 → 그 앞에서 끊지 않는다.
            // 단 현재 글자가 문장 종결부호(. ! ?)이면 줄 끝에 와도 좋으므로, 뒤에 닫는 괄호가
            // 오더라도 그 뒤에서 끊을 수 있다(HWP 동작). 닫는 괄호 run "))!))))" 가 줄에 걸칠 때
            // "(제))!" 까지 줄 끝에 두고 나머지 "))))" 를 넘긴다.
            boolean lineEndForbidden = nextIdx < n && isLineEndForbidden(pc.charInfo.ch());
            boolean lineStartForbidden = nextIdx < n
                    && isLineStartForbidden(chars.get(nextIdx).charInfo.ch())
                    && !isSentenceTerminator(pc.charInfo.ch());
            if (nextIdx == n
                    || (!isJoiningPunct(pc.charInfo.ch())
                        && !isJoiningPunct(chars.get(nextIdx).charInfo.ch())
                        && !loneDigitBreak
                        && !lineEndForbidden
                        && !lineStartForbidden)) {
                bestEnd = nextIdx;
            }
        }
        return n;
    }

    private void addChunkAsOneWord(LanguageChunk chunk) {
        // old_version addEachLanguageWord: chunk 전체에 대한 overflow 체크도 applyMinSpace=true.
        if (line.isOverWidth(chunk.width, true) && !line.noDrawingChar()) {
            wordLayouter.flow.saveTextLineAndNewLine();
        }
        for (PendingChar pc : chunk.chars) {
            // 이미 새 라인인 경우에도 너무 긴 chunk는 개별 검사해 개행.
            if (line.isOverWidth(pc.charInfo.width(), true) && !line.noDrawingChar()) {
                wordLayouter.flow.saveTextLineAndNewLine();
            }
            line.addChar(pc);
        }
    }

    private List<LanguageChunk> splitByLanguage(List<PendingChar> wordChars) {
        List<LanguageChunk> out = new ArrayList<>();
        LanguageChunk current = null;
        boolean previousHangul = false;
        boolean hasPrevious = false;
        for (int idx = 0; idx < wordChars.size(); idx++) {
            PendingChar pc = wordChars.get(idx);
            // 숫자와 "연결 기호"(·, ~, /)는 언어 경계를 만들지 않고 현재 청크에 흡수된다.
            // 한글 사이의 "·"(가운뎃점)는 연결 부호로 취급되어 줄나눔을 유발하지 않는 것이
            // HWP 컨벤션. 숫자도 동일하게 "단어로 인식".
            // 문장부호('.' ',' 등)도 언어 경계를 만들지 않는다 — 별도 청크가 되면
            // findLegalPrefixEnd의 행두금칙 검사(같은 청크 내)가 우회되어
            // "다단 테스트입니다" / ".이것은~" 처럼 '.'이 줄 첫 글자가 된다.
            boolean neutral = pc.digit || isJoiningPunct(pc.charInfo.ch())
                    || isSentencePunct(pc.charInfo.ch());
            boolean pcHangul = neutral ? (hasPrevious ? previousHangul : false) : pc.hangul;
            if (current == null || (hasPrevious && previousHangul != pcHangul)) {
                if (current != null) {
                    current.hangul = previousHangul;
                    out.add(current);
                }
                current = new LanguageChunk();
            }
            current.chars.add(pc);
            current.width += pc.charInfo.width();
            if (!pc.digit) current.allDigit = false;
            previousHangul = pcHangul;
            hasPrevious = true;
        }
        if (current != null && !current.chars.isEmpty()) {
            current.hangul = previousHangul;
            out.add(current);
        }
        return out;
    }

    /** 문장부호 — 언어 경계를 만들지 않고 앞 글자에 붙는다(행두금칙: 줄 첫 글자 금지). */
    private static boolean isSentencePunct(char ch) {
        switch (ch) {
            case '.': case ',': case '!': case '?': case ';': case ':':
            case '。': case '、':
                return true;
            default:
                return false;
        }
    }

    /** 문장 종결부호 — 줄 끝에 올 수 있어, 뒤에 닫는 괄호가 와도 그 뒤에서 줄을 끊을 수 있다. */
    private static boolean isSentenceTerminator(char ch) {
        switch (ch) {
            case '.': case '!': case '?':
            case '。':
                return true;
            default:
                return false;
        }
    }

    /** 앞뒤 청크를 연결하는 기호 — 언어 경계를 만들지 않는다. */
    private static boolean isJoiningPunct(char ch) {
        return ch == '\u00B7'   // · MIDDLE DOT (가운뎃점)
            || ch == '\u2022'   // • BULLET
            || ch == '\u30FB';  // ・ KATAKANA MIDDLE DOT
    }

    /** 행말금칙: 줄 끝에 올 수 없는 여는 괄호 류. */
    private static boolean isLineEndForbidden(char ch) {
        switch (ch) {
            case '(': case '[': case '{': case '<':
            case '（': case '［': case '｛':   // （ ［ ｛
            case '「': case '『': case '【':   // 「 『 【
            case '〔': case '〈': case '《':   // 〔 〈 《
            case '‘': case '“':                   // ' "
                return true;
            default:
                return false;
        }
    }

    /** 행두금칙: 줄 앞에 올 수 없는 닫는 괄호·문장부호 류. */
    private static boolean isLineStartForbidden(char ch) {
        switch (ch) {
            case ')': case ']': case '}': case '>':
            case '）': case '］': case '｝':   // ） ］ ｝
            case '」': case '』': case '】':   // 」 』 】
            case '〕': case '〉': case '》':   // 〕 〉 》
            case '’': case '”':                   // ' "
            case '.': case ',': case '!': case '?': case ';': case ':':
            case '。': case '、':                   // 。 、
                return true;
            default:
                return false;
        }
    }

    private static final class LanguageChunk {
        boolean hangul;
        double width;
        boolean allDigit = true;   // 청크가 모두 숫자로만 이루어진 경우 true — ByWord로 강제.
        final List<PendingChar> chars = new ArrayList<>();
    }
}
