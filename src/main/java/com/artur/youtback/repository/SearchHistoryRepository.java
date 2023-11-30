package com.artur.youtback.repository;

import com.artur.youtback.entity.SearchHistory;
import org.springframework.data.repository.ListCrudRepository;

public interface SearchHistoryRepository extends ListCrudRepository<SearchHistory, Long> {
}
