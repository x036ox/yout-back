package com.artur.youtback.utils;

public class Utils {

    public static SortOption processSortOptions(Integer sortOption){
        if(sortOption == null){
            return null;
        }
        switch (sortOption) {
            case 1 -> {
                return SortOption.BY_VIEWS_FROM_MOST;
            }
            case 2 -> {
                return SortOption.BY_VIEWS_FROM_LEAST;
            }
            case 3 -> {
                return SortOption.BY_UPLOAD_DATE_FROM_OLDEST;
            }
            case 4 -> {
                return SortOption.BY_UPLOAD_DATE_FROM_NEWEST;
            }
            case 5 -> {
                return SortOption.BY_DURATION_FROM_MOST;
            }
            case 6 -> {
                return SortOption.BY_DURATION_FROM_LEAST;
            }
            default -> {
                return null;
            }
        }
    }
}
