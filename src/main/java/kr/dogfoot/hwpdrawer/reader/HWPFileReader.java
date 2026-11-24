package kr.dogfoot.hwpdrawer.reader;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.docinfo.BinData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.sectiondefine.SectionDefineHeaderProperty;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.FootEndNoteShape;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.ParaCharShape;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;

/**
 * HWP 파일을 읽고 구조를 탐색하는 Reader.
 */
public class HWPFileReader {
    private HWPFile hwpFile;
    private final Map<Integer, BufferedImage> imageCache = new HashMap<>();

    public HWPFileReader() {}

    public HWPFile hwpFile() { return hwpFile; }

    public HWPFileReader loadFile(String filePath) throws Exception {
        this.hwpFile = HWPReader.fromFile(filePath);
        return this;
    }

    public HWPFileReader hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
        return this;
    }

    /**
     * 섹션 목록을 반환한다.
     */
    public ArrayList<Section> sections() {
        return hwpFile.getBodyText().getSectionList();
    }

    /**
     * 섹션의 PageDef를 가져온다.
     * 첫 번째 문단의 첫 번째 컨트롤에서 SectionDefine을 찾는다.
     */
    public PageDef getPageDef(Section section) {
        ControlSectionDefine secDef = findSectionDefine(section);
        return secDef != null ? secDef.getPageDef() : null;
    }

    /**
     * 섹션의 SectionDefine 헤더 프로퍼티(hideEmptyLine 등)를 가져온다.
     */
    public SectionDefineHeaderProperty getSectionProperty(Section section) {
        ControlSectionDefine secDef = findSectionDefine(section);
        return secDef != null ? secDef.getHeader().getProperty() : null;
    }

    /**
     * 섹션의 기본 탭 간격(HWPUNIT)을 가져온다. 문단의 명시적 탭 정의를 넘어선 탭은
     * 이 간격의 배수 위치로 진행한다. HWP 표준 기본값은 4000(=40pt)이지만 문서가
     * 커스텀할 수 있다(예: 8000=80pt). SectionDefine을 못 찾거나 값이 0이면 4000 반환.
     */
    public long getDefaultTabGap(Section section) {
        ControlSectionDefine secDef = findSectionDefine(section);
        if (secDef == null) return 4000;
        long gap = secDef.getHeader().getDefaultTabGap();
        return gap > 0 ? gap : 4000;
    }

    /**
     * 섹션의 각주 모양(구분선 스타일·여백·시작 번호)을 가져온다.
     *
     * <p>SectionDefine 에는 FOOTNOTE_SHAPE 레코드가 <b>2개</b> 들어 있다 — 각주용과 미주용이다.
     * hwplib 은 이를 {@code getFootNoteShape()} / {@code getEndNoteShape()} 로 나눠 준다.
     * (실문서 R21 실측: 미주용은 DivideLineLength=56000, BetweenNotesMargin=0 으로
     * 각주용과 값이 달라 잘못 고르면 배치가 통째로 어긋난다.)</p>
     */
    public FootEndNoteShape getFootNoteShape(Section section) {
        ControlSectionDefine secDef = findSectionDefine(section);
        return secDef != null ? secDef.getFootNoteShape() : null;
    }

    private ControlSectionDefine findSectionDefine(Section section) {
        if (section.getParagraphCount() == 0) return null;
        Paragraph firstPara = section.getParagraph(0);
        if (firstPara.getControlList() == null) return null;
        for (Object ctrl : firstPara.getControlList()) {
            if (ctrl instanceof ControlSectionDefine) {
                return (ControlSectionDefine) ctrl;
            }
        }
        return null;
    }

    /**
     * PageDef에서 용지 영역을 계산한다.
     */
    public Area getPaperArea(PageDef pageDef) {
        return new Area(0, 0, pageDef.getPaperWidth(), pageDef.getPaperHeight());
    }

    /**
     * PageDef에서 본문 영역을 계산한다 (여백 제외).
     */
    public Area getBodyArea(PageDef pageDef) {
        long left = pageDef.getLeftMargin() + pageDef.getGutterMargin();
        long top = pageDef.getTopMargin() + pageDef.getHeaderMargin();
        long right = pageDef.getPaperWidth() - pageDef.getRightMargin();
        long bottom = pageDef.getPaperHeight() - pageDef.getBottomMargin() - pageDef.getFooterMargin();
        return new Area(left, top, right, bottom);
    }

    /**
     * 머리말이 그려질 영역. 본문 좌우 폭과 동일하며, 세로는 topMargin 위에서 본문 상단까지(headerMargin 높이).
     * 텍스트는 영역 상단부터 top-aligned 로 그려진다.
     */
    public Area getHeaderArea(PageDef pageDef) {
        long left = pageDef.getLeftMargin() + pageDef.getGutterMargin();
        long right = pageDef.getPaperWidth() - pageDef.getRightMargin();
        long top = pageDef.getTopMargin();
        long bottom = pageDef.getTopMargin() + pageDef.getHeaderMargin();
        return new Area(left, top, right, bottom);
    }

    /**
     * 꼬리말이 그려질 영역. 본문 좌우 폭과 동일하며, 세로는 본문 하단부터 bottomMargin 위까지(footerMargin 높이).
     * 텍스트는 영역 하단에 bottom-aligned 로 그려진다.
     */
    public Area getFooterArea(PageDef pageDef) {
        long left = pageDef.getLeftMargin() + pageDef.getGutterMargin();
        long right = pageDef.getPaperWidth() - pageDef.getRightMargin();
        long top = pageDef.getPaperHeight() - pageDef.getBottomMargin() - pageDef.getFooterMargin();
        long bottom = pageDef.getPaperHeight() - pageDef.getBottomMargin();
        return new Area(left, top, right, bottom);
    }

    /**
     * 문단의 CharShape를 가져온다.
     *
     * ParaCharShape의 position은 텍스트 스트림 위치(charSize 누적값)이다.
     * ControlExtend(charSize=8)는 8 위치를 차지하고,
     * Normal/ControlChar(charSize=1)는 1 위치를 차지한다.
     * charIndex(charList 인덱스)를 텍스트 스트림 위치로 변환하여 매핑한다.
     */
    public CharShape getCharShape(Paragraph para, int charIndex) {
        ParaCharShape paraCharShape = para.getCharShape();
        if (paraCharShape == null) {
            return hwpFile.getDocInfo().getCharShapeList().get(0);
        }

        // charIndex를 텍스트 스트림 위치로 변환
        long textStreamPos = calcTextStreamPosition(para, charIndex);

        ArrayList<CharPositionShapeIdPair> pairs = paraCharShape.getPositonShapeIdPairList();

        int shapeIndex = 0;
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i).getPosition() <= textStreamPos) {
                shapeIndex = i;
            } else {
                break;
            }
        }

        int charShapeId = (int) pairs.get(shapeIndex).getShapeId();
        return hwpFile.getDocInfo().getCharShapeList().get(charShapeId);
    }

    /**
     * charList 인덱스를 텍스트 스트림 위치로 변환한다.
     */
    public static long calcTextStreamPosition(Paragraph para, int charIndex) {
        long pos = 0;
        ArrayList<HWPChar> charList = para.getText().getCharList();
        for (int i = 0; i < charIndex && i < charList.size(); i++) {
            pos += charList.get(i).getCharSize();
        }
        return pos;
    }

    /**
     * 텍스트 스트림 위치를 charList 인덱스로 변환한다.
     */
    public static int charIndexFromTextStreamPosition(Paragraph para, long textStreamPos) {
        long pos = 0;
        if (para.getText() == null) return 0;
        ArrayList<HWPChar> charList = para.getText().getCharList();
        for (int i = 0; i < charList.size(); i++) {
            if (pos >= textStreamPos) return i;
            pos += charList.get(i).getCharSize();
        }
        return charList.size();
    }

    /**
     * 문단의 ParaShape를 가져온다.
     */
    public ParaShape getParaShape(Paragraph para) {
        int paraShapeId = (int) para.getHeader().getParaShapeId();
        return hwpFile.getDocInfo().getParaShapeList().get(paraShapeId);
    }

    /**
     * HWPChar에서 실제 문자를 추출한다.
     */
    public static char extractChar(HWPChar hwpChar) {
        if (hwpChar.getType() == HWPCharType.Normal) {
            return normalizeChar((char) hwpChar.getCode());
        }
        return 0;
    }

    /**
     * 일부 한글 문서가 특정 기호를 폰트별 PUA(사용자 정의 영역) 코드포인트로 저장한다.
     * 원본 폰트는 그 PUA에 글리프를 매핑하지만 우리가 로드한 폰트엔 없어 .notdef(⊠ 박스)로
     * 깨지고, 폭이 넓어져 줄바꿈까지 어긋난다. 이런 코드포인트를 동일 의미의 표준 문자로
     * 정규화한다.
     *
     * <p>U+F09E → U+00B7(가운뎃점 ·): 실문서 R03 "편집여백 : 위·아래 …"의 가운뎃점.</p>
     */
    public static char normalizeChar(char ch) {
        if (ch == '\uF09E') return '\u00B7';   // 가운뎃점
        return ch;
    }

    /**
     * BinItemID로 내장 이미지를 가져온다. 결과는 캐싱된다.
     * old_version DrawingInput.image() 로직을 이식.
     */
    public BufferedImage getEmbeddedImage(int binItemId) {
        BufferedImage cached = imageCache.get(binItemId);
        if (cached != null) return cached;

        try {
            java.util.ArrayList<BinData> binDataList = hwpFile.getDocInfo().getBinDataList();
            if (binItemId < 1 || binItemId > binDataList.size()) return null;
            BinData binData = binDataList.get(binItemId - 1);
            int binDataId = binData.getBinDataID();
            if (binDataId <= 0) return null;

            String targetName = String.format("BIN%04X", binDataId);
            EmbeddedBinaryData found = null;
            for (EmbeddedBinaryData emb : hwpFile.getBinData().getEmbeddedBinaryDataList()) {
                if (emb.getName() != null && emb.getName().toUpperCase().contains(targetName)) {
                    found = emb;
                    break;
                }
            }
            if (found == null || found.getData() == null) return null;

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(found.getData()));
            if (img != null) imageCache.put(binItemId, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 문자가 한글인지 확인한다.
     */
    public static boolean isHangul(char ch) {
        return (ch >= 0xAC00 && ch <= 0xD7AF) ||  // 한글 음절
               (ch >= 0x3131 && ch <= 0x318E);     // 한글 자모
    }

    /**
     * 묶음 빈칸(code 30)/고정폭 빈칸(code 31) 제어문자인지 확인한다.
     * 한글은 이를 일반 공백과 같은 폭으로 그리되, 양쪽정렬에서 늘리지 않고
     * 단어를 나누지 않는다.
     */
    public static boolean isFixedWidthBlank(HWPChar hwpChar) {
        return hwpChar.getType() == HWPCharType.ControlChar
                && (hwpChar.getCode() == 30 || hwpChar.getCode() == 31);
    }
}
