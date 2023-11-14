package com.artur.youtback.repository;

import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchHistoryRepository extends ListCrudRepository<SearchHistory, Long> {
}
