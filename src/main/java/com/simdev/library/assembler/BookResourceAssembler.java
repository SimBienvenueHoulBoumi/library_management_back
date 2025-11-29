package com.simdev.library.assembler;

import com.simdev.library.controller.AuthorController;
import com.simdev.library.controller.BookController;
import com.simdev.library.controller.CategoryController;
import com.simdev.library.controller.LoanController;
import com.simdev.library.controller.ReviewController;
import com.simdev.library.domain.model.Book;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class BookResourceAssembler implements RepresentationModelAssembler<Book, EntityModel<Book>> {
    
    @Override
    @NonNull
    @SuppressWarnings("null")
    public EntityModel<Book> toModel(@NonNull Book book) {
        EntityModel<Book> bookModel = EntityModel.of(book,
            linkTo(methodOn(BookController.class).getBook(book.getId())).withSelfRel(),
            linkTo(methodOn(BookController.class).getAllBooks(null)).withRel("books")
        );
        
        // Liens conditionnels
        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            bookModel.add(linkTo(methodOn(AuthorController.class).getAuthorsByBook(book.getId())).withRel("authors"));
        }
        
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            bookModel.add(linkTo(methodOn(CategoryController.class).getCategoriesByBook(book.getId())).withRel("categories"));
        }
        
        // Toujours ajouter le lien vers les reviews (pas besoin de vérifier la collection pour éviter lazy loading)
        bookModel.add(linkTo(methodOn(ReviewController.class).getReviewsByBook(book.getId())).withRel("reviews"));
        
        // Lien pour emprunter (seulement si disponible)
        if (book.getAvailable() != null && book.getAvailable() && book.getAvailableCopies() > 0) {
            bookModel.add(linkTo(methodOn(LoanController.class).createLoan(null, book.getId(), null))
                .withRel("borrow"));
        }
        
        return bookModel;
    }
}

