package com.artur.youtback.converter;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StringIntegerMapConverter implements AttributeConverter<Map<String,Integer>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Integer> attribute) {
        if (attribute.isEmpty()){
            return "";
        }
        return attribute.entrySet().stream().map(entry -> String.join(":", entry.getKey(), entry.getValue().toString())).collect(Collectors.joining(","));
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String dbData) {
        if(dbData.isEmpty()){
            return new HashMap<>();
        }
        return Arrays.stream(dbData.split(",")).map(entryString -> {
            String[] keyValue = entryString.split(":");
            return Map.entry(keyValue[0], Integer.parseInt(keyValue[1]));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
