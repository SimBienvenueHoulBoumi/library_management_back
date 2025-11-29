package com.simdev.library.controller;

import com.simdev.library.assembler.BookResourceAssembler;
import com.simdev.library.domain.model.Book;
import com.simdev.library.repository.BookSpecifications;
import com.simdev.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "API pour la gestion des livres")
public class BookController {
    
    private final BookService bookService;
    private final BookResourceAssembler bookResourceAssembler;
    private final PagedResourcesAssembler<Book> pagedResourcesAssembler;
    
    @GetMapping
    @Operation(summary = "Récupérer tous les livres", description = "Récupère une page de livres avec pagination")
    @SuppressWarnings("null")
    public ResponseEntity<PagedModel<EntityModel<Book>>> getAllBooks(Pageable pageable) {
        Page<Book> books = bookService.findAll(pageable);
        PagedModel<EntityModel<Book>> pagedModel = pagedResourcesAssembler.toModel(books, bookResourceAssembler);
        
        pagedModel.add(linkTo(methodOn(BookController.class).getAllBooks(pageable)).withSelfRel());
        if (books.hasNext()) {
            pagedModel.add(linkTo(methodOn(BookController.class).getAllBooks(books.nextPageable())).withRel("next"));
        }
        if (books.hasPrevious()) {
            pagedModel.add(linkTo(methodOn(BookController.class).getAllBooks(books.previousOrFirstPageable())).withRel("prev"));
        }
        
        return ResponseEntity.ok(pagedModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un livre par ID")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Book>> getBook(@PathVariable Long id) {
        return bookService.findById(id)
            .map(bookResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Créer un nouveau livre")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Book>> createBook(@RequestBody Book book) {
        Book createdBook = bookService.createBook(book);
        EntityModel<Book> bookModel = bookResourceAssembler.toModel(createdBook);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookModel);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un livre")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Book>> updateBook(@PathVariable Long id, @RequestBody Book book) {
        Book updatedBook = bookService.updateBook(id, book);
        EntityModel<Book> bookModel = bookResourceAssembler.toModel(updatedBook);
        return ResponseEntity.ok(bookModel);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un livre")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    @Operation(summary = "Rechercher des livres", description = "Recherche avancée avec filtres multiples")
    @SuppressWarnings({"null", "unchecked"})
    public ResponseEntity<PagedModel<EntityModel<Book>>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String isbn,
            Pageable pageable) {
        
        Specification<Book> spec = BookSpecifications.combine(
            title != null ? BookSpecifications.hasTitle(title) : null,
            author != null ? BookSpecifications.hasAuthor(author) : null,
            category != null ? BookSpecifications.hasCategory(category) : null,
            available != null && available ? BookSpecifications.isAvailable() : null,
            isbn != null ? BookSpecifications.hasIsbn(isbn) : null
        );
        
        Page<Book> books = bookService.searchBooks(spec, pageable);
        PagedModel<EntityModel<Book>> pagedModel = pagedResourcesAssembler.toModel(books, bookResourceAssembler);
        
        return ResponseEntity.ok(pagedModel);
    }
    
    @GetMapping("/available")
    @Operation(summary = "Récupérer les livres disponibles")
    @SuppressWarnings("null")
    public ResponseEntity<CollectionModel<EntityModel<Book>>> getAvailableBooks() {
        List<Book> books = bookService.findAvailableBooks();
        CollectionModel<EntityModel<Book>> collectionModel = CollectionModel.of(
            books.stream().map(bookResourceAssembler::toModel).toList(),
            linkTo(methodOn(BookController.class).getAvailableBooks()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
}


