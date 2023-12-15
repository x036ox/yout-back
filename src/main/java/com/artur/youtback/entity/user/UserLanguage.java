package com.artur.youtback.entity.user;

import jakarta.persistence.Embeddable;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Embeddable
class UserLanguage {

    @NotNull
    private String languageMerged;

    //Indicates how many times did user watch videos in each language
    private transient Map<String, Integer> languages = new HashMap<>();

    public UserLanguage(Map<String, Integer> language) {
        this.languages = language;
        this.languageMerged = languages.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(","));
    }

    public UserLanguage() {
    }

    @PostLoad
    public void splitToLanguageAndRepeats(){
        if(languageMerged.isEmpty()) return;
        String[] languageAndRepeats = this.languageMerged.split(",");
        Arrays.stream(languageAndRepeats).forEach(language -> {
            String[] data = language.split(":");
            this.languages.put(data[0], Integer.parseInt(data[1]));
        });
    }

    public Map<String, Integer> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<String, Integer> languages) {
        this.languages = languages;
    }

    public void setLanguageMerged(String languageMerged) {
        this.languageMerged = languageMerged;
    }

    @Override
    public String toString() {
        return languages.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(";"));
    }
}
