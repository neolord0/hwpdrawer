package kr.dogfoot.hwpdrawer.export.vector;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;

/**
 * 빈 클립을 "모두 가림"으로 확실히 넘기는 래퍼. PDF 내보내기에 쓴다.
 *
 * <p>Java2D(PNG)는 넓이가 없는 클립(음수 크기 사각형, 겹치지 않는 두 클립의 교집합 등)을 빈 영역으로 보고
 * 아무것도 그리지 않는다. 렌더러는 나뉜 표 셀 조각처럼 높이가 음수인 영역에 클립을 거는 일이 있다.
 * 그런데 pdfbox-graphics2d는 그런 도형의 경로가 비어 있으면 클립을 아예 걸지 않아, 가려져야 할 내용이 PDF에
 * 그대로 보인다. 그래서 빈 클립은 넓이 0인 사각형(경로가 있는 빈 영역)으로 바꿔 넘긴다.</p>
 */
public class StrictClipGraphics2D extends DelegatingGraphics2D {

    public StrictClipGraphics2D(Graphics2D target) {
        super(target);
    }

    @Override
    protected DelegatingGraphics2D wrap(Graphics2D newTarget) {
        return new StrictClipGraphics2D(newTarget);
    }

    @Override
    public void setClip(Shape clip) {
        target.setClip(clip == null ? null : nonDegenerate(clip));
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        clip(new Rectangle(x, y, width, height));
    }

    @Override
    public void clip(Shape shape) {
        Shape current = getClip();
        if (current == null) {
            setClip(shape);
            return;
        }
        Area intersection = new Area(current);
        intersection.intersect(new Area(shape));
        setClip(intersection);
    }

    /** 넓이가 없는 클립은 Java2D와 같은 뜻(모두 가림)이 되도록 경로가 있는 넓이 0 사각형으로 바꾼다. */
    private static Shape nonDegenerate(Shape clip) {
        return clip.getBounds2D().isEmpty() ? new Rectangle() : clip;
    }
}
