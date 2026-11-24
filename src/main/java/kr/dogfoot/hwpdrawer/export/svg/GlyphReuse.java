package kr.dogfoot.hwpdrawer.export.svg;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * 한 SVG 페이지에서 같은 글자의 윤곽선을 한 번만 정의하고 {@code <use>}로 다시 쓴다.
 *
 * <p>왜: SVG에는 글자가 윤곽선 경로로 들어가는데, 명조(바탕) 글자 하나의 경로는 약 1.7KB다. 글자마다 경로를 통째로
 * 쓰면 글자가 많은 페이지는 수 MB가 된다. 같은 글자는 위치만 다르므로 원점 기준 윤곽선을 {@code <defs>}에 한 번 두고
 * 위치만 적는다.</p>
 *
 * <p>방법: 글자 자리에 {@link GlyphUse}(넓이 없는 도형)를 채운다. {@link GlyphSvgGraphics2D}가 그 자리에
 * {@code <use href x y>}를 넣는데, Batik이 다른 도형과 똑같이 그 순간의 색·변환·클립 그룹({@code <g>})에 담는다.
 * 다 그린 뒤 {@link #finish}가 윤곽선 정의를 {@code <svg>} 맨 앞에 넣는다.</p>
 */
final class GlyphReuse {
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    private final GlyphDefinitions definitions;

    GlyphReuse(String idPrefix) {
        this.definitions = new GlyphDefinitions(idPrefix);
    }

    /**
     * 글자 하나를 등록하고, 그 자리에 채울 표식을 돌려준다.
     *
     * @param outlineAtOrigin 글자 원점 기준 윤곽선
     * @param x 글자 원점의 x (현재 사용자 좌표)
     * @param y 글자 원점의 y (기준선)
     */
    GlyphUse markGlyph(Shape outlineAtOrigin, double x, double y) {
        return new GlyphUse(definitions.register(outlineAtOrigin), x, y);
    }

    /** {@code <use xlink:href='#정의' x='…' y='…'/>} */
    Element useElement(GlyphUse glyph, Document document) {
        Element use = document.createElementNS(SVG_NAMESPACE, "use");
        use.setAttributeNS(XLINK_NAMESPACE, "xlink:href", "#" + glyph.definitionId());
        use.setAttribute("x", definitions.formatNumber(glyph.x()));
        use.setAttribute("y", definitions.formatNumber(glyph.y()));
        return use;
    }

    /**
     * 다 그린 {@code <svg>} 요소를 마무리한다: 맨 앞에 글자 윤곽선 정의를 넣고(글자가 없으면 넣지 않는다),
     * 그리기 맥락이 직접 붙은 {@code <use>}를 감싼다 ({@link #isolateGlyphPositions}).
     */
    void finish(Element svgRoot) {
        if (definitions.isEmpty()) return;
        isolateGlyphPositions(svgRoot);
        svgRoot.insertBefore(definitions.toDefsElement(svgRoot.getOwnerDocument()), svgRoot.getFirstChild());
    }

    /**
     * Batik은 요소가 하나뿐인 그룹을 없애고 그룹의 맥락(클립·변환·그라데이션 채우기 …)을 그 요소에 직접 붙인다.
     * 그런데 {@code <use>}의 x·y는 그 요소의 좌표계에 이동으로 더해지므로, 같은 요소에 붙은 클립·그라데이션도 글자
     * 위치만큼 밀린다 — 표 칸의 클립이 칸 밖으로 밀려 칸 안의 글자가 모두 사라졌다. 그래서 {@code url(…)}을 가리키는
     * 속성이 붙은 {@code <use>}는 x·y·href 말고 모든 속성을 감싸는 {@code <g>}로 옮긴다.
     */
    private static void isolateGlyphPositions(Element svgRoot) {
        NodeList uses = svgRoot.getElementsByTagNameNS(SVG_NAMESPACE, "use");
        List<Element> toWrap = new ArrayList<>();
        for (int i = 0; i < uses.getLength(); i++) {
            Element use = (Element) uses.item(i);
            if (refersToDefinition(use)) toWrap.add(use);
        }
        for (Element use : toWrap) wrapContext(use);
    }

    private static boolean refersToDefinition(Element use) {
        NamedNodeMap attributes = use.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (!isPositionAttribute(attribute) && attribute.getNodeValue().contains("url(")) return true;
        }
        return false;
    }

    private static void wrapContext(Element use) {
        Element group = use.getOwnerDocument().createElementNS(SVG_NAMESPACE, "g");
        NamedNodeMap attributes = use.getAttributes();
        List<Attr> moved = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!isPositionAttribute(attribute)) moved.add(attribute);
        }
        for (Attr attribute : moved) {
            use.removeAttributeNode(attribute);
            group.setAttributeNode(attribute);
        }
        use.getParentNode().replaceChild(group, use);
        group.appendChild(use);
    }

    /** {@code <use>}에 남겨야 하는 속성: 위치(x, y)와 가리키는 정의(xlink:href). */
    private static boolean isPositionAttribute(Node attribute) {
        String name = attribute.getLocalName() != null ? attribute.getLocalName() : attribute.getNodeName();
        return name.equals("x") || name.equals("y") || name.equals("href");
    }
}
