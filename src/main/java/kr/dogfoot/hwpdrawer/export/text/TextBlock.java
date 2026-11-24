package kr.dogfoot.hwpdrawer.export.text;

/** 출력의 한 덩어리 (문단 하나, Markdown 표 하나). 앞뒤는 빈 줄로 띄운다. */
interface TextBlock {
    /** 출력할 글자. 빈 문자열이면 출력하지 않는다. */
    String text();

    /**
     * {@link #text()} 안에 일부러 한 번 더 쓴 글자 — 여러 줄 머리 행을 합칠 때 열마다 되풀이한 묶음 이름
     * ("제출대상 > 사단 | 제출대상 > 재단"의 두 번째 "제출대상"). 글자를 빠짐·중복 없이 썼는지 검사할 때 뺀다.
     */
    default String repeatedText() {
        return "";
    }

    /** 배치됐지만 바탕과 같은 색이라 보이지 않아 뺀 글자 (흰 바탕의 흰 글씨). 빠짐·중복 검사에서 뺀다. */
    default String droppedText() {
        return "";
    }

    /** 글자 없이 뺀 글자만 남기는 블록 (표 셀처럼 블록 밖에서 글자를 쓴 곳의 기록용). */
    static TextBlock droppedOnly(String droppedText) {
        return new TextBlock() {
            @Override
            public String text() {
                return "";
            }

            @Override
            public String droppedText() {
                return droppedText;
            }
        };
    }

    static TextBlock fixed(String text) {
        return () -> text;
    }

    static TextBlock fixed(String text, String repeatedText) {
        return new TextBlock() {
            @Override
            public String text() {
                return text;
            }

            @Override
            public String repeatedText() {
                return repeatedText;
            }
        };
    }
}
