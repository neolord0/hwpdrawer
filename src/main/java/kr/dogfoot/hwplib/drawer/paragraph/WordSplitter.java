package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class WordSplitter {
    private final DrawingInput input;
    private final ParagraphDrawer paragraphDrawer;
    private final TextLineDrawer textLineDrawer;

    private int letterCountBeforeNewLine;
    private boolean hasNewLine;

    private final ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    public WordSplitter(DrawingInput input, ParagraphDrawer paragraphDrawer, TextLineDrawer textLineDrawer) {
        this.input = input;
        this.paragraphDrawer = paragraphDrawer;
        this.textLineDrawer = textLineDrawer;

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

        boolean splitByEnglishLetter = input.paraShape().getProperty1().getLineDivideForEnglish() != LineDivideForEnglish.ByWord;
        boolean splitByHangulLetter = input.paraShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByLetter;
        ArrayList<WordsCharByLanguage> wordCharsByLanguages = splitByLanguage(charsOfWord);
        for (WordsCharByLanguage wcl : wordCharsByLanguages) {
            boolean splitByLetter = (wcl.hangul) ? splitByHangulLetter : splitByEnglishLetter;
            if (splitByLetter) {
                addWordAllCharsToLine(wcl.wordChars, true);
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
            if (!textLineDrawer.isOverWidth(wordWidth, true)) {
                addWordAllCharsToLine(wordChars, false);
            } else {
                hasNewLine = true;

                if (!textLineDrawer.noDrawingCharacter()) {
                    paragraphDrawer.saveTextLineAndNewLine();
                }
                addWordAllCharsToLine(wordChars, true);
            }
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight) throws Exception {
        for (CharInfo charInfo : wordChars) {
            if (paragraphDrawer.addCharToLine(charInfo, checkOverRight, true)) {
                hasNewLine = true;
            }
            if (!hasNewLine) {
                letterCountBeforeNewLine++;
            }
        }
    }

    public String test() {
        StringBuilder sb = new StringBuilder();
        for (CharInfo charInfo : charsOfWord) {
            sb.append(testCharInfo(charInfo));
        }
        return sb.toString();
    }

    private String testCharInfo(CharInfo charInfo) {
        StringBuilder sb = new StringBuilder();
        if (charInfo.type() == CharInfo.Type.Normal) {
            NormalCharInfo normalCharInfo = (NormalCharInfo) charInfo;
            try {
                sb
                        .append(normalCharInfo.normalCharacter().getCh())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;
            if (controlCharInfo.control() == null) {
                sb
                        .append(controlCharInfo.character().getCode())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");

            } else {
                sb
                        .append(controlCharInfo.control().getType())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            }

        }
        return sb.toString();
    }

    public void adjustControlAreaAtNewPage() {
        for (CharInfo charInfo : charsOfWord) {
            if (charInfo.type() == CharInfo.Type.Control) {
                ((ControlCharInfo) charInfo).area(input);
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
