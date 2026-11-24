package kr.dogfoot.hwpdrawer.export;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

/**
 * 내보낸 파일을 저장할 위치(폴더 + 파일 이름 접두어)와 파일 이름 규칙.
 *
 * <ul>
 *   <li>페이지마다 파일을 만드는 형식(PNG/SVG): {@code {접두어}{쪽 번호 3자리}.{확장자}} — 예: result005.png</li>
 *   <li>문서 하나를 파일 하나로 만드는 형식(PDF/HTML): {@code {접두어}.{확장자}} — 예: result.pdf</li>
 * </ul>
 *
 * <p>쪽 번호는 페이지 범위로 일부만 내보내도 문서 전체 기준 번호를 쓴다. 숫자는 JVM 로캘과 상관없이
 * ASCII 숫자다 (아랍어 로캘 등에서 result٠٠٥.png가 되지 않게).</p>
 */
public final class ExportTarget {
    /** 숨김 파일로, File.createTempFile의 접두어 최소 길이(3)도 채운다. */
    private static final String TEMPORARY_PREFIX = ".hwpdrawer-";
    private static final String TEMPORARY_SUFFIX = ".tmp";

    private final File directory;
    private final String filePrefix;

    /** 문서 파일의 내용을 주어진 파일에 쓴다. */
    @FunctionalInterface
    public interface ContentWriter {
        void writeTo(File file) throws IOException;
    }

    public ExportTarget(String directory, String filePrefix) {
        Objects.requireNonNull(directory, "저장할 폴더(directoryToSave)를 지정해야 합니다.");
        this.directory = new File(directory);
        this.filePrefix = Objects.requireNonNull(filePrefix, "파일 이름 접두어(filePrefix)가 null입니다.");
    }

    /** 저장 폴더가 없으면 만든다. */
    public void prepareDirectory() throws IOException {
        if (directory.isDirectory()) return;
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("저장 폴더를 만들 수 없습니다: " + directory.getAbsolutePath());
        }
    }

    public File pageFile(int pageNo, String extension) {
        return new File(directory, String.format(Locale.ROOT, "%s%03d.%s", filePrefix, pageNo, extension));
    }

    public File documentFile(String extension) {
        return new File(directory, filePrefix + "." + extension);
    }

    /**
     * 문서 파일({@code {접두어}.{확장자}})을 같은 폴더의 임시 파일에 다 쓴 뒤 제자리로 옮긴다.
     * 쓰는 도중 실패해도 이전에 만들어 둔 결과물이 잘리거나 지워지지 않는다.
     */
    public void writeDocumentFile(String extension, ContentWriter writer) throws IOException {
        Path destination = documentFile(extension).toPath();
        // File.createTempFile은 umask를 따른다(보통 rw-r--r--). Files.createTempFile은 rw------- 라서
        // 옮긴 뒤 결과물을 다른 사용자가 읽을 수 없게 된다.
        Path temporary = File.createTempFile(TEMPORARY_PREFIX + filePrefix, "." + extension + TEMPORARY_SUFFIX,
                directory).toPath();
        try {
            writer.writeTo(temporary.toFile());
            moveReplacing(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveReplacing(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
