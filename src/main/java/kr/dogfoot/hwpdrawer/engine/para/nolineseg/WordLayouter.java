package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import java.util.ArrayList;
import java.util.List;

/**
 * word 버퍼링 & flush 결정.
 *
 * <p>≈ old_version {@code WordDrawer}. 비공백은 {@code charsOfWord} 버퍼에 쌓고,
 * 공백을 만나면 {@link #addWordToLine(PendingChar)}이 버퍼된 word 폭을 보고 세 갈래로
 * 분기한다:
 * <ol>
 *   <li>word가 그대로 들어감 → word + space 추가</li>
 *   <li>최소공백 축소로 맞음 → word만 추가 + 축소 후 개행</li>
 *   <li>그래도 안 맞음 → {@link WordSplitter}로 언어 단위 분리</li>
 * </ol>
 * </p>
 */
final class WordLayouter {
    final NoLineSegLayouter flow;
    private final LineWriter line;
    private final WordSplitter splitter;
    private final int minSpaceProp;

    private final List<PendingChar> charsOfWord = new ArrayList<>();
    private double wordWidth;

    WordLayouter(NoLineSegLayouter flow, LineWriter line, ParaShape paraShape,
                 boolean hangulByWord, boolean englishByWord) {
        this.flow = flow;
        this.line = line;
        this.splitter = new WordSplitter(this, line, hangulByWord, englishByWord);
        this.minSpaceProp = paraShape.getProperty1().getMinimumSpace();
    }

    boolean isEmpty() { return charsOfWord.isEmpty(); }

    /** 비공백 글자를 word 버퍼에 추가. */
    void addCharOfWord(PendingChar pc) {
        charsOfWord.add(pc);
        // old_version WordDrawer와 동일: buffer는 plain width만 누적한다.
        // (line쪽 wordsWidth/spacesWidth는 charSpacing 포함 advance로 누적됨)
        wordWidth += pc.charInfo.width();
    }

    /**
     * 공백을 만났을 때 버퍼의 word를 라인에 반영.
     * spaceChar가 null이면 파라/라인 break 직전의 flush.
     */
    void addWordToLine(PendingChar spaceChar) {
        if (charsOfWord.isEmpty()) {
            addSpaceChar(spaceChar);
            return;
        }

        if (!line.isOverWidth(wordWidth, false)) {
            // 1) word가 그대로 들어감 → word + space 추가
            addAllWordChars(/*checkOverRight*/ false, /*applyMinSpace*/ false);
            addSpaceChar(spaceChar);
        } else {
            if (minSpaceCanFit()) {
                // 2) 최소공백 축소로 word가 맞음 → word만 추가하고 라인 마감.
                //    (공백은 shrink 자리이므로 라인에 추가하지 않음.)
                addAllWordChars(false, true);
                flow.saveTextLineAndNewLine();
            } else {
                spanningWord(spaceChar);
            }
        }
        reset();
    }

    private boolean minSpaceCanFit() {
        if (minSpaceProp <= 0) return false;
        return !line.isOverWidth(wordWidth, true);
    }

    /** 공백(space/tab) 한 글자를 현재 라인에 추가. */
    private void addSpaceChar(PendingChar spaceChar) {
        if (spaceChar == null) return;
        // 공백은 overflow 체크 없이 항상 추가 — trailing space는 허용.
        line.addChar(spaceChar);
    }

    /**
     * 버퍼의 모든 글자를 라인에 추가.
     * checkOverRight=true면 매 글자마다 isOverWidth 검사해 넘치면 라인 교체.
     */
    void addAllWordChars(boolean checkOverRight, boolean applyMinimumSpace) {
        for (PendingChar pc : charsOfWord) {
            addCharToLineChecking(pc, checkOverRight, applyMinimumSpace);
        }
    }

    private void addCharToLineChecking(PendingChar pc, boolean checkOverRight, boolean applyMinSpace) {
        // old_version과 동일: isOverWidth 인자는 plain width. charSpacing은 line쪽 누적에만 반영.
        if (checkOverRight && line.isOverWidth(pc.charInfo.width(), applyMinSpace)) {
            flow.saveTextLineAndNewLine();
        }
        line.addChar(pc);
    }

    private void spanningWord(PendingChar spaceChar) {
        if (flow.isAllLineDivideByWord()) {
            // 양쪽 모두 ByWord → word 통째로 다음 라인에 올림.
            if (!line.noDrawingChar()) {
                flow.saveTextLineAndNewLine();
            }
            addAllWordChars(/*checkOverRight*/ true, /*applyMinSpace*/ false);
        } else {
            // 한쪽이라도 ByLetter → 언어별로 쪼개 처리.
            splitter.splitByLineAndAdd(charsOfWord);
        }
        addSpaceChar(spaceChar);
    }

    private void reset() {
        charsOfWord.clear();
        wordWidth = 0;
    }
}
