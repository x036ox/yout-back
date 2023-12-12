package com.artur.youtback.entity.user;

import com.artur.youtback.utils.MapUtils;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
public class UserMetadata {

    @Id
    private Long id;

    @OneToOne(mappedBy = "userMetadata")
    @MapsId
    @JoinColumn(name = "id")
    private UserEntity userEntity;

    @Embedded
    private UserLanguage languages;
    @Embedded
    private UserCategories categories;

    public UserMetadata(UserEntity userEntity) {
        this();
        this.userEntity = userEntity;
    }

    public UserMetadata() {
        this.languages = new UserLanguage(new HashMap<>());
        this.categories = new UserCategories(new HashMap<>());
    }

    public void addLanguage(String language){
        if(language == null || language.isEmpty()) return;
        Map<String, Integer> languages = this.languages.getLanguages();
        languages.merge(language, 1, Integer::sum);
        this.languages.setLanguageMerged(languages.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
    }

    public void addCategory(String category){
        if(category == null || category.isEmpty()) return;
        Map<String, Integer> categories = this.categories.getCategories();
        categories.merge(category, 1, Integer::sum);
        this.categories.setCategoriesMerged(categories.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
    }

    public Map<String, Integer> getCategories() {
        return categories.getCategories();
    }

    public void setCategories(UserCategories categories) {
        this.categories = categories;
    }

    public Optional<String> findMostPopularLanguage(){
        var mostPopularLang = this.languages.getLanguages().entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
        return mostPopularLang.map(Map.Entry::getKey).or(Optional::empty);
    }


    public LinkedHashMap<String, Integer> getLanguagesDec() {
        return MapUtils.sortMapByValueDec(this.languages.getLanguages());
    }

    public LinkedHashMap<String, Integer> getCategoriesDec() {
        return MapUtils.sortMapByValueDec(this.categories.getCategories());
    }


    public Map<String, Integer> getLanguages() {
        return this.languages.getLanguages();
    }

    public void setLanguages(UserLanguage languages) {
        this.languages = languages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }


}
