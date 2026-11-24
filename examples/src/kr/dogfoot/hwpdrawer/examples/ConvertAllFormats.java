package kr.dogfoot.hwpdrawer.examples;

import kr.dogfoot.hwpdrawer.HWPDrawer;
import kr.dogfoot.hwpdrawer.HWPDrawerOption;
import kr.dogfoot.hwpdrawer.HWPDrawerOption.OutputType;

import java.io.File;
import java.util.Map;

import static java.util.Map.entry;

/**
 * HWP 파일 하나를 PNG, SVG, PDF, HTML, TXT 로 모두 변환하는 예제.
 *
 * <p>인자 없이 실행하면 함께 들어 있는 예제 문서({@code examples/sample.hwp})를 예제 글꼴
 * ({@code examples/fonts} — 나눔고딕·나눔고딕 ExtraBold·나눔명조, SIL OFL)로 변환해 {@code target/examples-output} 에
 * 저장한다. 프로젝트 폴더(pom.xml 이 있는 곳)에서 실행한다.</p>
 *
 * <p>문서가 쓰는 글꼴 중 함께 넣을 수 없는 것(바탕·굴림·HY·휴먼·함초롬 등)은 {@link HWPDrawerOption#fontSubstitute}
 * 로 나눔 글꼴을 대신 쓰고, 그래도 없는 글꼴은 {@link HWPDrawerOption#fallbackFont} 로 나눔고딕을 쓴다.
 * 글꼴 폴더나 시스템에 원래 글꼴이 있으면 원래 글꼴을 쓴다.</p>
 *
 * <pre>
 * mvn -q test-compile exec:java                                   예제 문서
 * mvn -q test-compile exec:java -Dexec.args="문서.hwp out 글꼴폴더"   다른 문서
 * </pre>
 *
 * <ul>
 *   <li>1번째 인자: 변환할 HWP 파일 (기본 {@value #SAMPLE_HWP})</li>
 *   <li>2번째 인자: 저장 폴더 — 형식마다 하위 폴더를 만든다: png, svg, pdf, html, txt (기본 {@value #DEFAULT_OUTPUT})</li>
 *   <li>3번째 인자: 문서가 쓰는 글꼴 파일(.ttf/.ttc)이 든 폴더 (기본 {@value #SAMPLE_FONTS}).
 *       폴더에 없는 글꼴은 시스템 글꼴, 그다음 위의 나눔 글꼴로 대신한다</li>
 * </ul>
 *
 * <p>결과 파일 이름은 {@link HWPDrawerOption#filePrefix(String)} 로 정한다 (기본 "result"):
 * PNG·SVG 는 쪽마다 result001.png …, PDF·HTML·TXT 는 파일 하나 result.pdf … 이다.</p>
 */
public final class ConvertAllFormats {

    static final String SAMPLE_HWP = "examples/sample.hwp";
    static final String SAMPLE_FONTS = "examples/fonts";
    static final String DEFAULT_OUTPUT = "target/examples-output";

    /**
     * 자주 쓰이지만 함께 넣을 수 없는(재배포할 수 없는) 글꼴 → examples/fonts 의 나눔 글꼴(SIL OFL).
     * 글꼴 폴더나 시스템에 원래 글꼴이 있으면 그것을 쓴다. 원래 글꼴과 모양·글자 폭이 조금 다르다.
     */
    private static final String MYEONGJO = "나눔명조";
    private static final String GOTHIC = "나눔고딕";
    private static final String HEADLINE = "나눔고딕 ExtraBold";
    private static final Map<String, String> NANUM_SUBSTITUTES = Map.ofEntries(
            entry("바탕", MYEONGJO), entry("바탕체", MYEONGJO), entry("휴먼명조", MYEONGJO),
            entry("함초롬바탕", MYEONGJO), entry("HY신명조", MYEONGJO),
            entry("굴림", GOTHIC), entry("새굴림", GOTHIC), entry("돋움", GOTHIC), entry("HY중고딕", GOTHIC),
            entry("함초롬돋움", GOTHIC), entry("맑은 고딕", GOTHIC), entry("휴먼고딕", GOTHIC),
            entry("HY헤드라인M", HEADLINE), entry("HY견고딕", HEADLINE));
    /** 위에도 없고 어디에서도 찾지 못한 글꼴 — 한글 글꼴이 없는 시스템에서도 글자가 □로 나오지 않게. */
    private static final String FALLBACK_FONT = GOTHIC;

    /** 변환할 형식과 그 결과를 담을 하위 폴더. */
    private enum Format {
        PNG(OutputType.Image, "png"),
        SVG(OutputType.SVG, "svg"),
        PDF(OutputType.PDF, "pdf"),
        HTML(OutputType.HTML, "html"),
        TXT(OutputType.Text, "txt");

        private final OutputType outputType;
        private final String folder;

        Format(OutputType outputType, String folder) {
            this.outputType = outputType;
            this.folder = folder;
        }
    }

    private ConvertAllFormats() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 3) {
            System.err.println("사용법: ConvertAllFormats [HWP 파일] [저장 폴더] [글꼴 폴더]");
            System.exit(2);
        }
        String hwpFile = argument(args, 0, SAMPLE_HWP);
        File outputDirectory = new File(argument(args, 1, DEFAULT_OUTPUT));
        String fontDirectory = argument(args, 2, SAMPLE_FONTS);
        if (!new File(hwpFile).isFile()) {
            System.err.println("HWP 파일이 없습니다: " + new File(hwpFile).getAbsolutePath()
                    + " — 프로젝트 폴더에서 실행했는지 확인하세요.");
            System.exit(1);
        }

        System.out.println(hwpFile + " (글꼴: " + fontDirectory + ")");
        for (Format format : Format.values()) {
            File directory = new File(outputDirectory, format.folder);
            int pages = HWPDrawer.draw(hwpFile, option(fontDirectory)
                    .outputType(format.outputType)
                    .directoryToSave(directory.getPath()));
            System.out.printf("%-4s %3d쪽 → %s%n", format, pages, directory.getPath());
        }
    }

    /** 글꼴 폴더 + 나눔 글꼴 대체. */
    private static HWPDrawerOption option(String fontDirectory) {
        HWPDrawerOption option = new HWPDrawerOption()
                .fontPath(fontDirectory)
                .fallbackFont(FALLBACK_FONT);
        NANUM_SUBSTITUTES.forEach(option::fontSubstitute);
        return option;
    }

    private static String argument(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
}
