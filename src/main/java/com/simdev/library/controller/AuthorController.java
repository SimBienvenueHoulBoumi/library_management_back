package com.simdev.library.controller;

import com.simdev.library.domain.model.Author;
import com.simdev.library.repository.AuthorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "API pour la gestion des auteurs")
public class AuthorController {
    
    private final AuthorRepository authorRepository;
    
    @GetMapping
    @Operation(summary = "Récupérer tous les auteurs")
    @SuppressWarnings("null")
    public ResponseEntity<CollectionModel<EntityModel<Author>>> getAllAuthors() {
        List<Author> authors = authorRepository.findAll();
        CollectionModel<EntityModel<Author>> collectionModel = CollectionModel.of(
            authors.stream().map(author -> EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthor(author.getId())).withSelfRel(),
                linkTo(methodOn(AuthorController.class).getAllAuthors()).withRel("authors")
            )).toList(),
            linkTo(methodOn(AuthorController.class).getAllAuthors()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un auteur par ID")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Author>> getAuthor(@PathVariable Long id) {
        return authorRepository.findById(id)
            .map(author -> EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthor(id)).withSelfRel(),
                linkTo(methodOn(AuthorController.class).getAllAuthors()).withRel("authors")
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/book/{bookId}")
    @Operation(summary = "Récupérer les auteurs d'un livre")
    public ResponseEntity<CollectionModel<EntityModel<Author>>> getAuthorsByBook(@PathVariable Long bookId) {
        // Cette méthode sera implémentée avec le service Book
        return ResponseEntity.ok(CollectionModel.empty());
    }
    
    @PostMapping
    @Operation(summary = "Créer un nouvel auteur")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Author>> createAuthor(@RequestBody Author author) {
        Author createdAuthor = authorRepository.save(author);
        EntityModel<Author> authorModel = EntityModel.of(createdAuthor,
            linkTo(methodOn(AuthorController.class).getAuthor(createdAuthor.getId())).withSelfRel()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(authorModel);
    }
}


