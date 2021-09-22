package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfo;

import java.util.Map;
import java.util.TreeMap;

public class CharInfoBuffer {
    private final Map<Integer, Map<Integer, CharInfo>> charMap;

    public CharInfoBuffer() {
        charMap = new TreeMap<>();
    }

    public CharInfo get(int paraIndex, int charIndex) {
        return mapForPara(paraIndex).get(charIndex);
    }

    private Map<Integer, CharInfo> mapForPara(int paraIndex) {
        if (charMap.containsKey(paraIndex)) {
            return charMap.get(paraIndex);
        } else {
            Map<Integer, CharInfo> mapForPara = new TreeMap<>();
            charMap.put(paraIndex, mapForPara);
            return mapForPara;
        }
    }

    public boolean add(int paraIndex, int charIndex, CharInfo charInfo) {
        Map<Integer, CharInfo> mapForPara = mapForPara(paraIndex);
        if (!mapForPara.containsKey(charIndex)) {
            mapForPara.put(charIndex, charInfo);
            return true;
        }
        return false;
    }

    public void clearUntilPreviousPara() {
        if (charMap.size() < 2) {
            return;
        }

        Integer paraIndexList[] = charMap.keySet().toArray(new Integer[0]);
        for (int index = 0; index < paraIndexList.length - 1; index++) {
            charMap.remove(paraIndexList[index]);
        }
    }

    public void clear() {
        charMap.clear();
    }
}
