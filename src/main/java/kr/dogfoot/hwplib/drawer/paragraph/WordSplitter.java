package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.util.ArrayList;

public class WordSplitter {
    private ParaListDrawer paraListDrawer;
    private TextLineDrawer textLineDrawer;
    private WordDrawer wordDrawer;

    private int letterCountBeforeNewLine;
    private boolean hasNewLine;
    private boolean stopAddingChar;


    public WordSplitter(ParaListDrawer paraListDrawer, TextLineDrawer textLineDrawer, WordDrawer wordDrawer) {
        this.paraListDrawer = paraListDrawer;
        this.textLineDrawer = textLineDrawer;
        this.wordDrawer = wordDrawer;
    }

    public int splitByLineAndAdd(ArrayList<CharInfo> wordChars, ParaShape paraShape) throws Exception {
        stopAddingChar = false;

        letterCountBeforeNewLine = 0;
        hasNewLine = false;

        boolean splitByEnglishLetter = paraShape.getProperty1().getLineDivideForEnglish() != LineDivideForEnglish.ByWord;
        boolean splitByHangulLetter = paraShape.getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByLetter;

        ArrayList<WordsCharByLanguage> wordCharsByLanguages = splitByLanguage(wordChars);
        for (WordsCharByLanguage wcl : wordCharsByLanguages) {
            boolean splitByLetter = (wcl.hangul) ? splitByHangulLetter : splitByEnglishLetter;
            if (splitByLetter) {
                addWordAllChars(wcl.wordChars, true);
            } else {
                addEachLanguageWord(wcl.wordChars, wcl.wordWidth);
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

    private void addWordAllChars(ArrayList<CharInfo> wordChars, boolean checkOverRight) throws Exception {
        for (CharInfo charInfo : wordChars) {
            if (stopAddingChar == true) {
                break;
            }

            if (wordDrawer.addChar(charInfo, checkOverRight, true)) {
                hasNewLine = true;
            }
            if (!hasNewLine) {
                letterCountBeforeNewLine++;
            }
        }
    }

    private void addEachLanguageWord(ArrayList<CharInfo> wordChars, long wordWidth) throws Exception {
        if (wordChars.size() > 0) {
            if (!textLineDrawer.isOverWidth(wordWidth, true)) {
                addWordAllChars(wordChars, false);
            } else {
                hasNewLine = true;

                if (!textLineDrawer.noDrawingCharacter()) {
                    paraListDrawer.saveTextLineAndNewLine();
                }
                addWordAllChars(wordChars, true);
            }
        }
    }

    public void stopAddingChar() {
        stopAddingChar = true;
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
