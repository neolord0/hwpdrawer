package kr.dogfoot.hwpdrawer.document.text;

import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 텍스트 조각. 동일한 정렬 단위 내의 글자 정보 목록.
 * 좌표는 double (HWP 단위).
 */
public class TextPart {
    private double startX;
    private double width;
    private double segmentLeft;
    private double segmentRight;
    private final List<CharInfo> charInfos = new ArrayList<>();

    public double startX() { return startX; }
    public TextPart startX(double x) { this.startX = x; return this; }

    public double width() { return width; }
    public TextPart width(double w) { this.width = w; return this; }

    /**
     * 이 파트가 속한 수평 세그먼트의 좌우 경계 (indent 적용 전, 장애물 기준 원시값).
     * LineSegSynthesizer가 startPositionFromColumn / segmentWidth를 파생할 때 사용.
     */
    public double segmentLeft() { return segmentLeft; }
    public TextPart segmentLeft(double v) { this.segmentLeft = v; return this; }

    public double segmentRight() { return segmentRight; }
    public TextPart segmentRight(double v) { this.segmentRight = v; return this; }

    public List<CharInfo> charInfos() { return charInfos; }

    public void addCharInfo(CharInfo charInfo) {
        charInfos.add(charInfo);
    }
}
