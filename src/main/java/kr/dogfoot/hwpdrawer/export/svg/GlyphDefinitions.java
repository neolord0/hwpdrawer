package kr.dogfoot.hwpdrawer.export.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 한 SVG 페이지 안의 글자 윤곽선 정의({@code <defs>}). 같은 윤곽선은 한 번만 정의하고 id로 다시 쓴다.
 *
 * <p>윤곽선은 글자 원점(기준선 왼쪽 끝) 기준 좌표로 받는다 — 같은 글꼴·크기·장평의 같은 글자는 어디에 놓여도
 * 경로가 같아 하나로 합쳐진다. 숫자는 SVG의 다른 좌표와 같은 소수 둘째 자리이고, JVM 로캘과 무관하게 ASCII다.</p>
 */
final class GlyphDefinitions {
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    private final String idPrefix;
    /** 경로({@link GlyphPath}) → 정의 id (등장 순서 유지). */
    private final Map<GlyphPath, String> idsByPath = new LinkedHashMap<>();
    private final DecimalFormat number = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    /** @param idPrefix 페이지마다 다른 접두어 (예: "p5_") — 한 HTML에 여러 페이지를 넣어도 id가 겹치지 않는다. */
    GlyphDefinitions(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    /** 원점 기준 윤곽선을 등록하고 정의 id를 돌려준다 (같은 윤곽선이면 같은 id). */
    String register(Shape outlineAtOrigin) {
        GlyphPath path = pathOf(outlineAtOrigin);
        return idsByPath.computeIfAbsent(path, p -> idPrefix + "g" + idsByPath.size());
    }

    String formatNumber(double value) {
        return number.format(value);
    }

    boolean isEmpty() {
        return idsByPath.isEmpty();
    }

    /** {@code <defs><path id='…' d='…'/>…</defs>} */
    Element toDefsElement(Document document) {
        Element defs = document.createElementNS(SVG_NAMESPACE, "defs");
        for (Map.Entry<GlyphPath, String> definition : idsByPath.entrySet()) {
            Element path = document.createElementNS(SVG_NAMESPACE, "path");
            path.setAttribute("id", definition.getValue());
            if (definition.getKey().evenOdd) path.setAttribute("fill-rule", "evenodd");
            path.setAttribute("d", definition.getKey().data);
            defs.appendChild(path);
        }
        return defs;
    }

    private GlyphPath pathOf(Shape shape) {
        PathIterator iterator = shape.getPathIterator(null);
        boolean evenOdd = iterator.getWindingRule() == PathIterator.WIND_EVEN_ODD;
        StringBuilder data = new StringBuilder();
        double[] coords = new double[6];
        for (; !iterator.isDone(); iterator.next()) {
            appendSegment(data, iterator.currentSegment(coords), coords);
        }
        return new GlyphPath(data.toString(), evenOdd);
    }

    private void appendSegment(StringBuilder data, int type, double[] coords) {
        switch (type) {
            case PathIterator.SEG_MOVETO: data.append('M'); appendPoints(data, coords, 1); break;
            case PathIterator.SEG_LINETO: data.append('L'); appendPoints(data, coords, 1); break;
            case PathIterator.SEG_QUADTO: data.append('Q'); appendPoints(data, coords, 2); break;
            case PathIterator.SEG_CUBICTO: data.append('C'); appendPoints(data, coords, 3); break;
            case PathIterator.SEG_CLOSE: data.append('Z'); break;
            default: throw new IllegalStateException("알 수 없는 경로 구간: " + type);
        }
    }

    private void appendPoints(StringBuilder data, double[] coords, int pointCount) {
        for (int i = 0; i < pointCount * 2; i++) {
            if (i > 0) data.append(',');
            data.append(number.format(coords[i]));
        }
    }

    /** 경로 데이터와 채우기 규칙 — 같으면 같은 글자 모양이다. */
    private static final class GlyphPath {
        final String data;
        final boolean evenOdd;

        GlyphPath(String data, boolean evenOdd) {
            this.data = data;
            this.evenOdd = evenOdd;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GlyphPath)) return false;
            GlyphPath other = (GlyphPath) o;
            return evenOdd == other.evenOdd && data.equals(other.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode() * 31 + (evenOdd ? 1 : 0);
        }
    }
}
