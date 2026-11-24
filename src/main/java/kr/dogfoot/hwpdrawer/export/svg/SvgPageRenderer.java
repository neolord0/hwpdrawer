package kr.dogfoot.hwpdrawer.export.svg;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.export.image.PagePainter;
import kr.dogfoot.hwpdrawer.export.vector.PortableImageGraphics2D;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGIDGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.util.Locale;

/**
 * 한 페이지를 {@code <svg>…</svg>} 요소 문자열로 그린다 (Apache Batik SVG 생성기). SVG 파일과 HTML 내보내기가 함께 쓴다.
 *
 * <p>좌표계는 PNG와 같은 렌더러 픽셀이고(viewBox), 표시 크기(width/height)는 실제 용지 크기(mm)다.
 * 글자는 윤곽선 경로로 들어가고, 같은 글자의 윤곽선은 페이지마다 한 번만 정의해 다시 쓴다 ({@link GlyphReuse}).
 * 그림은 PNG data URI로 들어간다 ({@link PortableImageGraphics2D}가 CMYK 등을 sRGB로 바꾸고 자른 그림은 잘라서 넘긴다).
 * 같은 페이지는 실행할 때마다 같은 문자열이 나온다.</p>
 */
public class SvgPageRenderer {
    /** 좌표 숫자의 소수 자릿수 (글자 윤곽선 정의도 같다 — {@link GlyphDefinitions}). */
    private static final int PRECISION = 2;
    /** Batik이 루트 {@code <defs>}에 붙이는 고정 id. 쪽마다 같아 한 HTML에 여러 쪽을 넣으면 겹친다. */
    private static final String BATIK_DEFS_ID = "genericDefs";
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    private static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

    private final PagePainter painter;

    public SvgPageRenderer(PagePainter painter) {
        this.painter = painter;
    }

    /** {@code PageGeometry.of(page).hasArea()}인 페이지만 넘긴다. */
    public String render(Page page) {
        PageGeometry geometry = PageGeometry.of(page);
        // 클립 경로·그라데이션·글자 정의의 id. 한 HTML에 여러 페이지를 넣어도 겹치지 않고, 실행마다 같은 결과가
        // 나오게 쪽 번호로 정한다.
        String idPrefix = "p" + page.pageNo() + "_";
        GlyphReuse glyphReuse = new GlyphReuse(idPrefix);
        GlyphSvgGraphics2D svg = new GlyphSvgGraphics2D(generatorContext(idPrefix), glyphReuse);
        svg.setSVGCanvasSize(new Dimension(geometry.widthPx(), geometry.heightPx()));

        Graphics2D g2d = new ReusedGlyphGraphics2D(new PortableImageGraphics2D(svg), glyphReuse);
        try {
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle2D.Float(0, 0, geometry.exactWidthPx(), geometry.exactHeightPx()));
            painter.paint(page, g2d);
        } finally {
            g2d.dispose();
        }

        Element root = svg.getRoot();
        glyphReuse.finish(root);
        setRootAttributes(root, geometry);
        return serialize(root);
    }

    private static SVGGeneratorContext generatorContext(String idPrefix) {
        SVGGeneratorContext context = SVGGeneratorContext.createDefault(newDocument());
        context.setIDGenerator(new PageIdGenerator(idPrefix));
        context.setImageHandler(new PngImageHandler());
        context.setPrecision(PRECISION);
        context.setComment(null);
        context.setEmbeddedFontsOn(false);
        return context;
    }

    private static Document newDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML 문서를 만들 수 없습니다.", e);
        }
    }

    /** 실제 크기(mm)와 소수 픽셀 viewBox를 넣고, 쪽마다 같은 Batik 고정 id를 뺀다. */
    private static void setRootAttributes(Element root, PageGeometry geometry) {
        root.setAttribute("width", String.format(Locale.ROOT, "%.2fmm", geometry.widthMm()));
        root.setAttribute("height", String.format(Locale.ROOT, "%.2fmm", geometry.heightMm()));
        root.setAttribute("viewBox", String.format(Locale.ROOT, "0 0 %.2f %.2f",
                geometry.exactWidthPx(), geometry.exactHeightPx()));
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && BATIK_DEFS_ID.equals(((Element) child).getAttribute("id"))) {
                ((Element) child).removeAttribute("id");
            }
        }
    }

    /**
     * {@code <svg>} 요소만 쓴다. Batik의 stream()은 XML 선언과 DOCTYPE까지 써서 HTML에 넣을 수 없다.
     * 이름공간 선언은 루트에 직접 둔다 — 직렬화기가 요소마다 다시 선언하지 않게.
     */
    private static String serialize(Element root) {
        root.setAttributeNS(XMLNS_NAMESPACE, "xmlns", SVG_NAMESPACE);
        root.setAttributeNS(XMLNS_NAMESPACE, "xmlns:xlink", XLINK_NAMESPACE);
        StringWriter out = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(new DOMSource(root), new StreamResult(out));
        } catch (TransformerException e) {
            throw new IllegalStateException("SVG를 쓸 수 없습니다.", e);
        }
        return out.toString();
    }

    /** Batik 기본 id(clipPath1, linearGradient1 …) 앞에 쪽 접두어를 붙인다. */
    private static final class PageIdGenerator extends SVGIDGenerator {
        private final String idPrefix;

        PageIdGenerator(String idPrefix) {
            this.idPrefix = idPrefix;
        }

        @Override
        public String generateID(String prefix) {
            return idPrefix + super.generateID(prefix);
        }
    }
}
