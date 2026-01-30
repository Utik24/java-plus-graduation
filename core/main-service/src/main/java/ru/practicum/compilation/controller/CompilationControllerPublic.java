package ru.practicum.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
public class CompilationControllerPublic {

    private final CompilationService compilationService;
    private static final String COMP_ID_PATH = "/{compId}";

    @Autowired
    public CompilationControllerPublic(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(name = "pinned", defaultValue = "false") boolean pinned,
                                                @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(name = "size", defaultValue = "10") @Positive int size) {
        List<CompilationDto> compnDtoList = compilationService.getAllComps(pinned, from, size);
        return compnDtoList;
    }

    @GetMapping(COMP_ID_PATH)
    public CompilationDto getCompilations(@PathVariable int compId) {
        CompilationDto compilationDto = compilationService.getCompById(compId);
        return compilationDto;
    }
}