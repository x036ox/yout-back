package com.artur.youtback.entity.user;

import jakarta.persistence.Embeddable;
import jakarta.persistence.PostLoad;

import java.util.*;
import java.util.stream.Collectors;

@Embeddable
class UserCategories {

    private String categoriesMerged;

    //Indicates how many times did user watch videos in each language
    private transient Map<String, Integer> categories = new HashMap<>();

    public UserCategories(Map<String, Integer> language) {
        this.categories = language;
    }

    public UserCategories() {
    }

    @PostLoad
    public void splitToLanguageAndRepeats(){
        if(categoriesMerged.isEmpty()) return;
        String[] languageAndRepeats = this.categoriesMerged.split(",");
        Arrays.stream(languageAndRepeats).forEach(language -> {
            String[] data = language.split(":");
            this.categories.put(data[0], Integer.parseInt(data[1]));
        });
    }

    public Map<String, Integer> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Integer> languages) {
        this.categories = languages;
    }

    public void setCategoriesMerged(String languageMerged) {
        this.categoriesMerged = languageMerged;
    }

    @Override
    public String toString() {
        return categories.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(";"));
    }
}
