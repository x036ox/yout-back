package com.artur.youtback.entity;

import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.utils.TimeOperations;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class SearchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String searchOption;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity historyOwner;

    @NotNull
    private Long dateAdded;

    public SearchHistory(Long id, String searchOption, UserEntity historyOwner) {
        this.id = id;
        this.searchOption = searchOption.toLowerCase();
        this.historyOwner = historyOwner;
        this.dateAdded = TimeOperations.toSeconds(System.currentTimeMillis());
    }

    public SearchHistory(String searchOption) {
        this.searchOption = searchOption;
    }

    public SearchHistory() {
    }

    public Long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded() {
        this.dateAdded = TimeOperations.toSeconds(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption.toLowerCase();
    }

    public UserEntity getHistoryOwner() {
        return historyOwner;
    }

    public void setHistoryOwner(UserEntity historyOwner) {
        this.historyOwner = historyOwner;
    }
}
