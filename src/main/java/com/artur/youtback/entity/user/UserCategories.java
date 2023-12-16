package com.artur.youtback.entity.user;

import com.artur.youtback.converter.StringIntegerMapConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Embeddable
public class UserCategories {


    public UserCategories(Map<String, Integer> language) {
    }

    public UserCategories() {
    }

}
