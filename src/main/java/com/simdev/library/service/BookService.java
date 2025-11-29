package com.simdev.library.service;

import com.simdev.library.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface BookService {
    Page<Book> findAll(Pageable pageable);
    Optional<Book> findById(Long id);
    Book createBook(Book book);
    Book updateBook(Long id, Book book);
    void deleteBook(Long id);
    Page<Book> searchBooks(Specification<Book> spec, Pageable pageable);
    List<Book> findAvailableBooks();
    Book addAuthorToBook(Long bookId, Long authorId);
    Book addCategoryToBook(Long bookId, Long categoryId);
}

