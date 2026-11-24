package kr.dogfoot.hwpdrawer.export.svg;

import org.apache.batik.svggen.ImageHandlerBase64Encoder;
import org.apache.batik.svggen.SVGGraphics2DIOException;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 그림을 PNG data URI({@code data:image/png;base64,…})로 넣는다. PNG 인코딩은 JDK ImageIO로 한다 —
 * Batik 기본 인코더는 batik-codec 모듈(브리지·트랜스코더까지 끌고 온다)이 있어야 한다.
 */
final class PngImageHandler extends ImageHandlerBase64Encoder {

    @Override
    public void encodeImage(RenderedImage image, OutputStream out) throws SVGGraphics2DIOException {
        try {
            if (!ImageIO.write(image, "png", out)) {
                throw new SVGGraphics2DIOException("PNG로 저장할 수 없는 그림입니다: " + image);
            }
        } catch (IOException e) {
            throw new SVGGraphics2DIOException(e);
        }
    }
}
