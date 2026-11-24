package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 선택한 페이지의 글자를 AI 검색(RAG)에 넣기 좋은 텍스트 파일 하나로 만든다: {@code {접두어}.txt} (UTF-8).
 *
 * <p>내용과 형식은 {@link DocumentText}가 정한다 — 쪽 표시, 읽는 순서대로 놓인 문단, Markdown 표, 각주.</p>
 */
public class TextExporter implements DocumentExporter {
    private static final String EXTENSION = "txt";

    private final HWPFileReader reader;
    private final ExportTarget target;

    public TextExporter(HWPFileReader reader, ExportTarget target) {
        this.reader = reader;
        this.target = target;
    }

    @Override
    public void export(List<Page> pages) throws IOException {
        target.prepareDirectory();
        byte[] content = DocumentText.of(pages, reader.hwpFile()).getBytes(StandardCharsets.UTF_8);
        target.writeDocumentFile(EXTENSION, file -> Files.write(file.toPath(), content));
    }
}
