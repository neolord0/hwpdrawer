package kr.dogfoot.hwpdrawer.engine.charinfo;

import kr.dogfoot.hwpdrawer.util.ScriptStyle;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;

/**
 * 레이아웃 계산에 사용되는 글자 정보.
 * 위치/크기는 정밀도 보존을 위해 double (HWP 단위).
 */
public class CharInfo {
    private char ch;
    private CharShape charShape;
    private double x;          // 라인 내 x 위치 (HWP 단위)
    private double width;      // 글자 폭 (HWP 단위)
    private double height;     // 글자 높이 (HWP 단위)
    private double descent;    // 디센트 (HWP 단위)
    private double baseSize;   // 기본 크기 (HWP 단위)
    private boolean isSpace;
    private int charIdx = -1;
    private boolean inlineControl = false;
    private boolean dotLeader = false;   // 탭 채우기(점선 리더): width 구간에 채우기를 그린다.
    private BorderType tabFillSort = null;  // 탭 채우기 종류 (Dash, Solid 등). null이면 점선.
    private String text;                 // 보충문자(surrogate pair) 등 char 하나로 표현 못하는 글자. null이면 ch 사용.
    private int codePoint = -1;          // 보충문자의 코드포인트(폰트 폴백/canDisplay용).
    private boolean paragraphHead = false; // 엔진이 만든 문단 머리("1.", "가)", "•"). 본문 들여쓰기 폭을 차지한다.
    /**
     * CharShape의 첨자 비트를 무시하고 적용할 첨자 규칙. null이면 CharShape 기준(기존 동작).
     * 각주 앵커처럼 "파일에는 첨자 표시가 없는데 한글은 첨자로 그리는" 글자에만 설정된다.
     */
    private ScriptStyle.Script script = null;

    public CharInfo() {}

    public char ch() { return ch; }
    public CharInfo ch(char ch) { this.ch = ch; return this; }

    public CharShape charShape() { return charShape; }
    public CharInfo charShape(CharShape cs) { this.charShape = cs; return this; }

    public double x() { return x; }
    public CharInfo x(double x) { this.x = x; return this; }

    public double width() { return width; }
    public CharInfo width(double w) { this.width = w; return this; }

    public double height() { return height; }
    public CharInfo height(double h) { this.height = h; return this; }

    public double descent() { return descent; }
    public CharInfo descent(double d) { this.descent = d; return this; }

    public double baseSize() { return baseSize; }
    public CharInfo baseSize(double s) { this.baseSize = s; return this; }

    public boolean isSpace() { return isSpace; }
    public CharInfo isSpace(boolean space) { this.isSpace = space; return this; }

    public int charIdx() { return charIdx; }
    public CharInfo charIdx(int idx) { this.charIdx = idx; return this; }

    public boolean inlineControl() { return inlineControl; }
    public CharInfo inlineControl(boolean b) { this.inlineControl = b; return this; }

    public boolean dotLeader() { return dotLeader; }
    public CharInfo dotLeader(boolean b) { this.dotLeader = b; return this; }

    public BorderType tabFillSort() { return tabFillSort; }
    public CharInfo tabFillSort(BorderType fill) { this.tabFillSort = fill; return this; }

    public String text() { return text; }
    public CharInfo text(String t) { this.text = t; return this; }
    public int codePoint() { return codePoint; }
    public CharInfo codePoint(int cp) { this.codePoint = cp; return this; }
    public boolean paragraphHead() { return paragraphHead; }
    public CharInfo paragraphHead(boolean b) { this.paragraphHead = b; return this; }

    public ScriptStyle.Script script() { return script; }
    public CharInfo script(ScriptStyle.Script s) { this.script = s; return this; }
}
