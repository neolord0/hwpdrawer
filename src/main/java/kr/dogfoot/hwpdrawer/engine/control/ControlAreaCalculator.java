package kr.dogfoot.hwpdrawer.engine.control;

import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.RelativeArrange;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwpdrawer.util.Area;

/**
 * CtrlHeaderGso의 위치/크기 정보를 실제 페이지 좌표로 변환한다.
 * old_version PositionCalculator를 new_version 스타일로 이식.
 */
public final class ControlAreaCalculator {

    private ControlAreaCalculator() {}

    public static Area calculate(CtrlHeaderGso h, Area paperArea, Area bodyArea,
                                  Area paraArea, long paraTopY) {
        long width  = calcWidth(h, paperArea, bodyArea, paraArea);
        long height = calcHeight(h, paperArea, bodyArea);
        long x = calcX(h, width, paperArea, bodyArea, paraArea) + h.getOutterMarginLeft();
        long y = calcY(h, height, paperArea, bodyArea, paraTopY) + h.getOutterMarginTop();
        return new Area(x, y, x + width, y + height);
    }

    public static long resolveWidth(CtrlHeaderGso h, Area paperArea, Area bodyArea) {
        long w = h.getWidth();
        switch (h.getProperty().getWidthCriterion()) {
            case Paper:  return paperArea.width() * w / 10000;
            case Page:   return bodyArea.width()  * w / 10000;
            default:     return w;
        }
    }

    public static long resolveHeight(CtrlHeaderGso h, Area paperArea, Area bodyArea) {
        long ht = h.getHeight();
        switch (h.getProperty().getHeightCriterion()) {
            case Paper: return paperArea.height() * ht / 10000;
            case Page:  return bodyArea.height()  * ht / 10000;
            default:    return ht;
        }
    }

    private static long calcWidth(CtrlHeaderGso h, Area paper, Area body, Area para) {
        long w = h.getWidth();
        switch (h.getProperty().getWidthCriterion()) {
            case Paper:  return paper.width() * w / 10000;
            case Page:   return body.width()  * w / 10000;
            case Column:
            case Para:   return para.width()  * w / 10000;
            default:     return w;
        }
    }

    private static long calcHeight(CtrlHeaderGso h, Area paper, Area body) {
        long ht = h.getHeight();
        switch (h.getProperty().getHeightCriterion()) {
            case Paper: return paper.height() * ht / 10000;
            case Page:  return body.height()  * ht / 10000;
            default:    return ht;
        }
    }

    /**
     * hwplib이 4바이트 offset을 unsigned long으로 읽어 음수 INT32 값이 2^31~2^32-1
     * 범위의 큰 양수로 들어오는 경우를 signed int 값으로 정규화한다. HWP 스펙상
     * x/yOffset은 signed INT32이므로 이 변환이 안전하다.
     */
    private static long signedOffset(long raw) {
        return (int) raw;
    }

    private static long calcX(CtrlHeaderGso h, long width, Area paper, Area body, Area para) {
        Area ref = horizRef(h, paper, body, para);
        long xOff = signedOffset(h.getxOffset());
        RelativeArrange arrange = h.getProperty().getHorzRelativeArrange();
        switch (arrange) {
            case Center:      return ref.left() + (ref.width() - width) / 2 + xOff;
            case BottomOrRight: return ref.right() - xOff - width;
            case Outside:     return ref.right() - width + xOff;
            default:          return ref.left() + xOff;
        }
    }

    private static Area horizRef(CtrlHeaderGso h, Area paper, Area body, Area para) {
        switch (h.getProperty().getHorzRelTo()) {
            case Paper:  return paper;
            case Page:   return body;
            default:     return para;
        }
    }

    private static long calcY(CtrlHeaderGso h, long height, Area paper, Area body, long paraTopY) {
        long yOff = signedOffset(h.getyOffset());
        switch (h.getProperty().getVertRelTo()) {
            case Paper: return applyVertArrange(h.getProperty().getVertRelativeArrange(), paper, height, yOff);
            case Page:  return applyVertArrange(h.getProperty().getVertRelativeArrange(), body,  height, yOff);
            default:    // Para: Y relative to paragraph top
                // yOff는 음수일 수 있다(컨트롤이 anchor 문단보다 위로 올라감).
                // signedOffset으로 정규화했으므로 그대로 더한다.
                //
                // 단 자리차지(TakePlace)는 개체가 본문 흐름에서 자기 자리를 차지하므로
                // anchor 문단보다 위로 올라갈 수 없다 — 한글은 음수 오프셋을 0으로 본다.
                // 실문서 R07 page2 상단 로고 2개(yOff=-2726·-2354)가 그 예로,
                // 그대로 적용하면 각각 45px·39px 위로 떠서 참조와 어긋난다(실측 -46px·-40px).
                // 코퍼스 519개 GSO 중 Para/TakePlace 이면서 yOff!=0 인 것은 13개인데,
                // 양수 10개는 모두 픽셀 단위 회귀 검사 문서에서 지금 정상이므로
                // 양수는 건드리지 않고 음수만 자른다. 떠 있는 개체(InFrontOfText 등)는
                // 실제로 위로 올라가므로(표나누기/셀나누기 yOff=-3629, 현재 정상) 제외한다.
                if (yOff < 0
                        && h.getProperty().getTextFlowMethod() == TextFlowMethod.TakePlace) {
                    yOff = 0;
                }
                return paraTopY + yOff;
        }
    }

    private static long applyVertArrange(RelativeArrange arrange, Area ref, long height, long yOff) {
        switch (arrange) {
            case Center:        return ref.top() + (ref.height() - height) / 2 + yOff;
            case BottomOrRight:
            case Outside:       return ref.bottom() - height + yOff;
            default:            return ref.top() + yOff;
        }
    }
}
