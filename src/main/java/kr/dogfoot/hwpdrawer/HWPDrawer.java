package kr.dogfoot.hwpdrawer;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.engine.LayoutEngine;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.html.HtmlExporter;
import kr.dogfoot.hwpdrawer.export.image.ImageExporter;
import kr.dogfoot.hwpdrawer.export.pdf.PdfExporter;
import kr.dogfoot.hwpdrawer.export.svg.SvgExporter;
import kr.dogfoot.hwpdrawer.export.text.TextExporter;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.FontLoader;
import kr.dogfoot.hwpdrawer.util.FontManager;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.io.File;
import java.util.List;

/**
 * HWP Drawer 메인 진입점.
 *
 * 파이프라인: HWP파일 → Reader → Engine → Document → Export(PNG/SVG/PDF/HTML)
 */
public class HWPDrawer {
    private static final String HWP_EXTENSION = ".hwp";

    /**
     * HWP 파일을 읽어서 지정된 형식으로 내보낸다.
     *
     * @param hwpFilePath HWP 파일 경로
     * @param option 설정 (출력 형식, 저장 폴더, 페이지 범위 등)
     * @return 내보낸 페이지 수 (페이지 범위를 지정하지 않으면 문서 전체 페이지 수)
     * @throws IllegalArgumentException 페이지 범위가 잘못되었거나 시작 페이지가 문서 페이지 수보다 클 때
     */
    public static int draw(String hwpFilePath, HWPDrawerOption option) throws Exception {
        ExportRequest request = ExportRequest.of(option, documentTitleOf(hwpFilePath));
        applySettings(option);
        return draw(new HWPFileReader().loadFile(hwpFilePath), request);
    }

    /**
     * HWPFile 객체에서 직접 내보낸다. 문서 제목(PDF/HTML)은 파일 이름 접두어(filePrefix)를 쓴다.
     *
     * @return 내보낸 페이지 수
     */
    public static int draw(HWPFile hwpFile, HWPDrawerOption option) throws Exception {
        ExportRequest request = ExportRequest.of(option, option.filePrefix());
        applySettings(option);
        return draw(new HWPFileReader().hwpFile(hwpFile), request);
    }

    private static void applySettings(HWPDrawerOption option) {
        UnitConverter.setZoomRate(option.zoomRate());
        UnitConverter.setOffset(option.offsetX(), option.offsetY());
        FontLoader.instance().fontPath(option.fontPath());
        FontLoader.instance().substitutes(option.fontSubstitutes());
        FontLoader.instance().fallbackFont(option.fallbackFont());
    }

    private static int draw(HWPFileReader reader, ExportRequest request) throws Exception {
        FontManager.instance().hwpFile(reader.hwpFile());
        try {
            Document document = layout(reader);
            List<Page> pages = request.pageRange().select(document.pages());
            createExporter(reader, request).export(pages);
            return pages.size();
        } finally {
            FontManager.instance().clear();
        }
    }

    /** 레이아웃은 항상 문서 전체를 계산한다 — 앞 페이지의 흐름이 뒤 페이지 위치를 정하기 때문. */
    private static Document layout(HWPFileReader reader) throws Exception {
        LayoutEngine engine = new LayoutEngine(reader);
        Document document = engine.layout();
        engine.dispose();
        return document;
    }

    private static DocumentExporter createExporter(HWPFileReader reader, ExportRequest request) {
        ExportTarget target = request.target();
        switch (request.outputType()) {
            case Image: return new ImageExporter(reader, target);
            case SVG:   return new SvgExporter(reader, target);
            case PDF:   return new PdfExporter(reader, target, request.documentTitle());
            case HTML:  return new HtmlExporter(reader, target, request.documentTitle());
            case Text:  return new TextExporter(reader, target);
            default:
                throw new IllegalArgumentException("지원하지 않는 출력 형식: " + request.outputType());
        }
    }

    /** "…/보고서.hwp" → "보고서" */
    private static String documentTitleOf(String hwpFilePath) {
        String fileName = new File(hwpFilePath).getName();
        boolean hasExtension = fileName.toLowerCase().endsWith(HWP_EXTENSION);
        return hasExtension ? fileName.substring(0, fileName.length() - HWP_EXTENSION.length()) : fileName;
    }
}
