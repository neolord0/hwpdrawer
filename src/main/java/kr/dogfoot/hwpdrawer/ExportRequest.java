package kr.dogfoot.hwpdrawer;

import kr.dogfoot.hwpdrawer.HWPDrawerOption.OutputType;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.PageRange;

import java.util.Objects;

/**
 * 한 번의 내보내기 요청: 형식, 저장 위치, 페이지 범위, 문서 제목.
 *
 * <p>옵션에서 문서와 무관하게 정해지는 값을 HWP 파일을 읽기 전에 모두 검증해 만든다 — 설정 오류(잘못된 범위,
 * 저장 폴더 누락 등)를 긴 레이아웃 계산 뒤가 아니라 바로 알린다.</p>
 */
final class ExportRequest {
    private final OutputType outputType;
    private final ExportTarget target;
    private final PageRange pageRange;
    private final String documentTitle;

    private ExportRequest(OutputType outputType, ExportTarget target, PageRange pageRange, String documentTitle) {
        this.outputType = outputType;
        this.target = target;
        this.pageRange = pageRange;
        this.documentTitle = documentTitle;
    }

    /**
     * @throws IllegalArgumentException 페이지 범위가 잘못되었을 때
     * @throws NullPointerException 출력 형식이나 저장 폴더가 없을 때
     */
    static ExportRequest of(HWPDrawerOption option, String documentTitle) {
        OutputType outputType = Objects.requireNonNull(option.outputType(), "출력 형식(outputType)을 지정해야 합니다.");
        ExportTarget target = new ExportTarget(option.directoryToSave(), option.filePrefix());
        return new ExportRequest(outputType, target, option.pageRange(), documentTitle);
    }

    OutputType outputType() {
        return outputType;
    }

    ExportTarget target() {
        return target;
    }

    PageRange pageRange() {
        return pageRange;
    }

    String documentTitle() {
        return documentTitle;
    }
}
