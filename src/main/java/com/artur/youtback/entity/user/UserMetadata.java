package com.artur.youtback.entity.user;

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

    public UserMetadata(UserEntity userEntity) {
        this.userEntity = userEntity;
        this.languages = new UserLanguage(new HashMap<>());
    }

    public UserMetadata() {
    }

    public void addLanguage(String language){
        System.out.println("ADding language " + language);
        Map<String, Integer> languages = this.languages.getLanguages();
        languages.merge(language, 1, Integer::sum);
        this.languages.setLanguageMerged(languages.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
    }

    public Optional<String> findMostPopularLanguage(){
        var mostPopularLang = this.languages.getLanguages().entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
        return mostPopularLang.map(Map.Entry::getKey).or(Optional::empty);
    }


    public LinkedHashMap<String, Integer> getLanguagesDec() {
        return this.languages.getLanguages().entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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
