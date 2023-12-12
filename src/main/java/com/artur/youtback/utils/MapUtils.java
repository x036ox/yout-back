package com.artur.youtback.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtils {

    public static <E> LinkedHashMap<E, Integer> sortMapByValueDec(Map<E, Integer> map){
        return map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
