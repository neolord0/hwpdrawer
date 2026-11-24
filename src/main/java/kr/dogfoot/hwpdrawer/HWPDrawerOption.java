package kr.dogfoot.hwpdrawer;

import kr.dogfoot.hwpdrawer.export.PageRange;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * HWP Drawer 설정.
 */
public class HWPDrawerOption {
    public enum OutputType {
        /** 페이지마다 PNG 파일 하나 ({접두어}001.png, ...). */
        Image,
        /** 선택한 페이지를 모두 담은 HTML 파일 하나 ({접두어}.html). 페이지는 인라인 SVG로 들어간다. */
        HTML,
        /** 선택한 페이지를 모두 담은 PDF 파일 하나 ({접두어}.pdf). */
        PDF,
        /** 페이지마다 SVG 파일 하나 ({접두어}001.svg, ...). */
        SVG,
        /**
         * 선택한 페이지의 글자를 읽는 순서대로 담은 UTF-8 텍스트 파일 하나 ({접두어}.txt). AI 검색(RAG)용 —
         * 쪽 표시, 문단, Markdown 표, 각주가 들어간다.
         */
        Text
    }

    private OutputType outputType = OutputType.Image;
    private String directoryToSave;
    private String fontPath;
    private final Map<String, String> fontSubstitutes = new LinkedHashMap<>();
    private String fallbackFont;
    private int zoomRate = 100;
    private int offsetX = 0;
    private int offsetY = 0;
    private String filePrefix = "result";
    private int startPage = 1;
    private OptionalInt endPage = OptionalInt.empty();

    public OutputType outputType() { return outputType; }
    public HWPDrawerOption outputType(OutputType type) { this.outputType = type; return this; }

    public String directoryToSave() { return directoryToSave; }
    public HWPDrawerOption directoryToSave(String dir) { this.directoryToSave = dir; return this; }

    public String fontPath() { return fontPath; }
    public HWPDrawerOption fontPath(String path) { this.fontPath = path; return this; }

    /**
     * 문서의 글꼴 {@code fontName}이 폰트 폴더에도 시스템에도 없을 때 대신 쓸 글꼴 (예: "휴먼명조" → "나눔명조").
     * 대신 쓸 글꼴은 폰트 폴더나 시스템에 있는 글꼴 이름(한국어·영어 이름 모두 된다)이다. 내장 대체 글꼴보다 먼저 보고,
     * 대신 쓸 글꼴도 없으면 내장 대체로 넘어간다. 여러 번 부르면 쌓인다.
     */
    public HWPDrawerOption fontSubstitute(String fontName, String substituteFontName) {
        fontSubstitutes.put(Objects.requireNonNull(fontName, "fontName"),
                Objects.requireNonNull(substituteFontName, "substituteFontName"));
        return this;
    }

    /** 지정한 대체 글꼴 (글꼴 이름 → 대신 쓸 글꼴 이름, 지정한 순서). */
    public Map<String, String> fontSubstitutes() { return Collections.unmodifiableMap(fontSubstitutes); }

    /**
     * 폰트 폴더·시스템·대체 글꼴 어디에서도 찾지 못한 글꼴을 그릴 글꼴 (폰트 폴더나 시스템에 있는 글꼴 이름).
     * 지정하지 않으면 Java 기본 글꼴(Dialog)로 그린다 — 한글 글꼴이 없는 시스템에서는 글자가 □로 나올 수 있다.
     */
    public HWPDrawerOption fallbackFont(String fontName) { this.fallbackFont = fontName; return this; }

    public String fallbackFont() { return fallbackFont; }

    public int zoomRate() { return zoomRate; }
    public HWPDrawerOption zoomRate(int rate) { this.zoomRate = rate; return this; }

    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public HWPDrawerOption offset(int x, int y) { this.offsetX = x; this.offsetY = y; return this; }

    public String filePrefix() { return filePrefix; }
    public HWPDrawerOption filePrefix(String prefix) { this.filePrefix = prefix; return this; }

    /** 내보낼 첫 페이지 (1부터, 기본 1). */
    public HWPDrawerOption startPage(int page) { this.startPage = page; return this; }

    /** 내보낼 마지막 페이지 (포함). 지정하지 않으면 문서의 마지막 페이지까지. */
    public HWPDrawerOption endPage(int page) { this.endPage = OptionalInt.of(page); return this; }

    /** 내보낼 페이지 범위 startPage ~ endPage (1부터, 양끝 포함). */
    public HWPDrawerOption pageRange(int startPage, int endPage) {
        return startPage(startPage).endPage(endPage);
    }

    /**
     * 설정된 페이지 범위.
     *
     * @throws IllegalArgumentException 시작 페이지가 1보다 작거나 끝 페이지가 시작 페이지보다 작을 때
     */
    public PageRange pageRange() {
        return endPage.isPresent()
                ? PageRange.of(startPage, endPage.getAsInt())
                : PageRange.from(startPage);
    }
}
