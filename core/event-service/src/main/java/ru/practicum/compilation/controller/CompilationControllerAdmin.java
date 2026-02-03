package ru.practicum.compilation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.model.dto.NewCompilationDto;
import ru.practicum.compilation.model.dto.UpdateCompilationRequest;
import ru.practicum.compilation.service.CompilationService;

@RestController
@RequestMapping("/admin/compilations")
public class CompilationControllerAdmin {
    private final CompilationService compilationService;
    private static final String COMP_ID_PATH = "/{compId}";

    public CompilationControllerAdmin(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto postCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        CompilationDto compilationDto = compilationService.create(newCompilationDto);
        return compilationDto;
    }

    @DeleteMapping(COMP_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable(name = "compId") int compId) {
        compilationService.deleteById(compId);
    }

    @PatchMapping(COMP_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto patchCompilation(@PathVariable(name = "compId") int compId,
                                           @Valid @RequestBody UpdateCompilationRequest updateRequest) {
        CompilationDto compilationDto = compilationService.update(compId, updateRequest);
        return compilationDto;
    }
}
