package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.util.ArrayList;

public class WordSplitter {
    private ParagraphDrawer paragraphDrawer;
    private DrawingInfo info;

    private int letterCountBeforeNewLine;
    private boolean hasNewLine;

    private ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    public WordSplitter(ParagraphDrawer paragraphDrawer, DrawingInfo info) {
        this.paragraphDrawer = paragraphDrawer;
        this.info = info;

        charsOfWord = new ArrayList<>();
    }

    public void resetWord() {
        charsOfWord.clear();
        wordWidth = 0;
    }

    public void addCharOfWord(CharInfo charInfo) {
        charsOfWord.add(charInfo);
        wordWidth += charInfo.width();
    }

    public boolean noChar() {
        return charsOfWord.isEmpty();
    }

    public ArrayList<CharInfo> charsOfWord() {
        return charsOfWord;
    }

    public long wordWidth() {
        return wordWidth;
    }

    public int split() throws Exception {
        letterCountBeforeNewLine = 0;
        hasNewLine = false;

        boolean splitByEnglishLetter = info.paraShape().getProperty1().getLineDivideForEnglish() != LineDivideForEnglish.ByWord;
        boolean splitByHangulLetter = info.paraShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByLetter;
        ArrayList<WordsCharByLanguage> wordCharsByLanguages = splitByLanguage(charsOfWord);
        for (WordsCharByLanguage wcl: wordCharsByLanguages) {
            boolean splitByLetter = (wcl.hangul) ? splitByHangulLetter : splitByEnglishLetter;
            if (splitByLetter) {
                addWordAllCharsToLine(wcl.wordChars, true, true);
            } else {
                addEachLanguageWordToLine(wcl.wordChars, wcl.wordWidth);
            }
        }
        return letterCountBeforeNewLine;
    }

    private ArrayList<WordsCharByLanguage> splitByLanguage(ArrayList<CharInfo> wordChars) {
        if (wordChars == null || wordChars.size() == 0) {
            return null;
        }

        ArrayList<WordsCharByLanguage> list = new ArrayList<>();
        WordsCharByLanguage item = new WordsCharByLanguage();
        boolean previousHangul = false;
        int count = wordChars.size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = wordChars.get(index);
            if (index > 0 && previousHangul != charInfo.character().isHangul()) {
                item.hangul = previousHangul;
                list.add(item);
                item = new WordsCharByLanguage();
            }

            item.wordWidth += charInfo.width();
            item.wordChars.add(charInfo);

            previousHangul = charInfo.character().isHangul();
        }
        if (item.wordChars.size() > 0) {
            item.hangul = previousHangul;
            list.add(item);
        }

        return list;
    }

    private void addEachLanguageWordToLine(ArrayList<CharInfo> wordChars, long wordWidth) throws Exception {
        if (wordChars.size() > 0) {
            if (!paragraphDrawer.isOverRight(wordWidth, true)) {
                addWordAllCharsToLine(wordChars, false, true);
            } else {
                hasNewLine = true;

                if (!paragraphDrawer.noNormalCharAtTextLine()) {
                    paragraphDrawer.drawTextAndNewLine();
                }
                addWordAllCharsToLine(wordChars,true, true);
            }
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            if (paragraphDrawer.addCharToLine(charInfo, checkOverRight, applyMinimumSpace)) {
                hasNewLine = true;
            }
            if (hasNewLine == false) {
                letterCountBeforeNewLine++;
            }
        }
    }

    private static class WordsCharByLanguage {
        public boolean hangul;
        public long wordWidth;
        public ArrayList<CharInfo> wordChars;

        public WordsCharByLanguage() {
            wordChars = new ArrayList<>();
        }
    }
}
