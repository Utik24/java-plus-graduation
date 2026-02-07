package ru.practicum.category.service;

import ru.practicum.category.model.dto.CategoryDto;
import ru.practicum.category.model.dto.CategoryRequest;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAll(Integer from, Integer size);

    CategoryDto getById(Long categoryId);

    CategoryDto create(CategoryRequest categoryRequest);

    CategoryDto update(Long categoryId, CategoryRequest categoryRequest);

    void delete(Long categoryId);
}
