package ru.practicum.category.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.category.model.Category;
import ru.practicum.category.model.dto.CategoryDto;
import ru.practicum.category.model.dto.CategoryRequest;
import ru.practicum.category.model.mapper.CategoryMapper;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ConstraintException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CategoryServiceImp implements CategoryService {

    private final CategoryRepository repository;
    private final EventRepository eventRepository;
    private final String errorMessageNotFound = "Category with id = %d was not found";
    private final String errorMessageAlreadyExist = "Category with name = %s is already exists";

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        PageRequest page = PageRequest.of(from, size, Sort.by("id").ascending());
        List<Category> categories = repository.findAll(page).getContent();
        return categories.stream().map(CategoryMapper::toCategoryDto).toList();
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    public CategoryDto create(CategoryRequest categoryRequest) {
        if (repository.findAll().stream().map(Category::getName).anyMatch(name -> name.equals(categoryRequest.getName()))) {
            throw new ConstraintException(String.format(errorMessageAlreadyExist, categoryRequest.getName()));
        }
        return CategoryMapper.toCategoryDto(repository.save(CategoryMapper.toCategory(categoryRequest)));
    }

    @Override
    public CategoryDto update(Long categoryId, CategoryRequest categoryRequest) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        Optional<Category> existingCategory = repository.findByName(categoryRequest.getName());
        if (existingCategory.isPresent() && !existingCategory.get().getId().equals(categoryId)) {
            throw new ConflictException(String.format(errorMessageAlreadyExist, categoryRequest.getName()));
        }
        category.setName(categoryRequest.getName());
        return CategoryMapper.toCategoryDto(repository.save(category));
    }

    @Override
    public void delete(Long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow(() ->
                new NotFoundException(String.format(errorMessageNotFound, categoryId))
        );
        if (eventRepository.existsByCategory(category)) {
            throw new ConflictException("Cannot delete category: there are events associated with this category");
        }
        repository.deleteById(categoryId);
    }
}