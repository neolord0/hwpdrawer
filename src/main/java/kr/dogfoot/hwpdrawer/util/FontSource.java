package kr.dogfoot.hwpdrawer.util;

import java.awt.Font;
import java.io.File;
import java.util.Locale;

/**
 * 폰트 폴더의 파일에서 읽은 폰트의 출처.
 *
 * <p>PDF처럼 폰트를 문서에 임베드하는 형식은 Java {@link Font} 객체가 아니라 원본 폰트 파일이 필요하다.
 * {@link FontLoader}가 폰트를 고를 때 그 파일을 함께 기록해 둔다.</p>
 */
public final class FontSource {
    private final File file;
    private final int collectionIndex;

    /** @param collectionIndex 폰트 모음(.ttc) 안 순번. 단일 폰트 파일은 0. */
    public FontSource(File file, int collectionIndex) {
        this.file = file;
        this.collectionIndex = collectionIndex;
    }

    public File file() {
        return file;
    }

    public boolean isCollection() {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".ttc");
    }

    /**
     * 폰트 모음(.ttc) 안에서의 순번 (0부터). {@link Font#createFonts(File)}가 돌려주는 순서와 같다.
     * 이름으로 찾지 않는 이유: Java의 PostScript 이름과 fontbox가 읽는 이름이 다른 폰트가 있다
     * (예: 'Menlo Regular' ↔ 'Menlo-Regular').
     */
    public int collectionIndex() {
        return collectionIndex;
    }
}
