package kr.dogfoot.hwpdrawer.export.vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * 그림을 벡터 형식(SVG/PDF) 라이브러리가 제대로 옮길 수 있는 모양으로 바꿔 넘기는 래퍼.
 *
 * <ul>
 *   <li><b>원본의 일부만 그리기</b>(그림 자르기, {@code drawImage(…, sx1, sy1, sx2, sy2, …)}):
 *       잘라낸 부분 그림을 만들어 대상 사각형에 그린다. pdfbox-graphics2d 3.0.5는 이 호출의 변환식이
 *       틀려(자르기가 없을 때만 맞는다) PDF에서 그림이 엉뚱한 자리·크기로 나온다. SVG에도 잘라낸 그림만
 *       원본 해상도로 들어간다.</li>
 *   <li><b>PNG로 저장할 수 없는 색 공간</b>(CMYK JPEG 등): sRGB로 바꾼다. SVG는 그림을 PNG data URI로 넣으므로
 *       ({@code svg/PngImageHandler}) PNG로 쓸 수 있어야 한다. 변환은 PNG 출력이 캔버스에 그릴 때 하는 색 변환과 같다.</li>
 * </ul>
 */
public class PortableImageGraphics2D extends DelegatingGraphics2D {
    private static final String PNG = "png";

    public PortableImageGraphics2D(Graphics2D target) {
        super(target);
    }

    @Override
    protected DelegatingGraphics2D wrap(Graphics2D newTarget) {
        return new PortableImageGraphics2D(newTarget);
    }

    // ───── 원본 일부 그리기 → 잘라낸 그림 ─────

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2, ImageObserver obs) {
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null, obs);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver obs) {
        if (!isSimpleRegion(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2)) {
            return target.drawImage(portable(img), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, obs);
        }
        BufferedImage region = ((BufferedImage) portable(img)).getSubimage(sx1, sy1, sx2 - sx1, sy2 - sy1);
        return drawImage(region, dx1, dy1, dx2 - dx1, dy2 - dy1, bgcolor, obs);
    }

    /** 뒤집기 없이(좌표 순서대로), 원본 안쪽 구간을 자르는 경우만 잘라낸 그림으로 바꿀 수 있다. */
    private static boolean isSimpleRegion(Image img, int dx1, int dy1, int dx2, int dy2,
                                          int sx1, int sy1, int sx2, int sy2) {
        if (!(img instanceof BufferedImage)) return false;
        BufferedImage image = (BufferedImage) img;
        boolean ordered = dx1 < dx2 && dy1 < dy2 && sx1 < sx2 && sy1 < sy2;
        boolean inside = sx1 >= 0 && sy1 >= 0 && sx2 <= image.getWidth() && sy2 <= image.getHeight();
        return ordered && inside;
    }

    // ───── 색 공간 정리 ─────

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver obs) {
        return target.drawImage(portable(img), x, y, obs);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver obs) {
        return target.drawImage(portable(img), x, y, width, height, obs);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver obs) {
        if (bgcolor == null) return drawImage(img, x, y, obs);
        return target.drawImage(portable(img), x, y, bgcolor, obs);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver obs) {
        if (bgcolor == null) return drawImage(img, x, y, width, height, obs);
        return target.drawImage(portable(img), x, y, width, height, bgcolor, obs);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return target.drawImage(portable(img), xform, obs);
    }

    /** PNG로 저장할 수 있으면 그대로, 아니면 sRGB(ARGB)로 바꾼 사본. */
    private static Image portable(Image img) {
        if (!(img instanceof BufferedImage)) return img;
        BufferedImage image = (BufferedImage) img;
        return isPngEncodable(image) ? image : toArgb(image);
    }

    private static boolean isPngEncodable(BufferedImage image) {
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        return ImageIO.getImageWriters(type, PNG).hasNext();
    }

    private static BufferedImage toArgb(BufferedImage image) {
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = converted.createGraphics();
        try {
            g2d.drawImage(image, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return converted;
    }
}
