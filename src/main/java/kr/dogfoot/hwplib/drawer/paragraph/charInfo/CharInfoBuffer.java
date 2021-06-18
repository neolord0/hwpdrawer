package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import java.util.Map;
import java.util.TreeMap;

public class CharInfoBuffer {
    private final Map<Integer, Map<Integer, CharInfo>> map;

    public CharInfoBuffer() {
        map = new TreeMap<>();
    }

    public CharInfo get(int paraIndex, int charIndex) {
        return mapForPara(paraIndex).get(charIndex);
    }

    private Map<Integer, CharInfo> mapForPara(int paraIndex) {
        if (map.containsKey(paraIndex)) {
            return map.get(paraIndex);
        } else {
            Map<Integer, CharInfo> mapForPara = new TreeMap<>();
            map.put(paraIndex, mapForPara);
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
        if (map.size() < 2) {
            return;
        }

        Integer paraIndexList[] = map.keySet().toArray(new Integer[0]);
        for (int index = 0; index < paraIndexList.length - 1; index++) {
            map.remove(paraIndexList[index]);
        }
    }

    public void clear() {
        map.clear();
    }
}
