package com.simdev.library.controller;

import com.simdev.library.domain.model.Category;
import com.simdev.library.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API pour la gestion des catégories")
public class CategoryController {
    
    private final CategoryRepository categoryRepository;
    
    @GetMapping
    @Operation(summary = "Récupérer toutes les catégories")
    public ResponseEntity<CollectionModel<EntityModel<Category>>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        CollectionModel<EntityModel<Category>> collectionModel = CollectionModel.of(
            categories.stream().map(category -> EntityModel.of(category,
                linkTo(methodOn(CategoryController.class).getCategory(category.getId())).withSelfRel(),
                linkTo(methodOn(CategoryController.class).getAllCategories()).withRel("categories")
            )).toList(),
            linkTo(methodOn(CategoryController.class).getAllCategories()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une catégorie par ID")
    public ResponseEntity<EntityModel<Category>> getCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
            .map(category -> EntityModel.of(category,
                linkTo(methodOn(CategoryController.class).getCategory(id)).withSelfRel(),
                linkTo(methodOn(CategoryController.class).getAllCategories()).withRel("categories")
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/book/{bookId}")
    @Operation(summary = "Récupérer les catégories d'un livre")
    public ResponseEntity<CollectionModel<EntityModel<Category>>> getCategoriesByBook(@PathVariable Long bookId) {
        // Cette méthode sera implémentée avec le service Book
        return ResponseEntity.ok(CollectionModel.empty());
    }
}


