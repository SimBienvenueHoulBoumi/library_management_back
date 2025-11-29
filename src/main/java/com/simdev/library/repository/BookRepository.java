package com.simdev.library.repository;

import com.simdev.library.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    
    @Override
    @EntityGraph(value = "Book.withAuthorsAndCategories")
    @NonNull
    Optional<Book> findById(@NonNull Long id);
    
    @Override
    @EntityGraph(value = "Book.withAuthorsAndCategories")
    @NonNull
    Page<Book> findAll(@NonNull Pageable pageable);
    
    @EntityGraph(value = "Book.withAuthorsAndCategories")
    @NonNull
    Page<Book> findAll(@Nullable Specification<Book> spec, @NonNull Pageable pageable);
    
    @EntityGraph(value = "Book.withAuthorsAndCategories")
    @Query("SELECT b FROM Book b WHERE b.available = true AND b.availableCopies > 0")
    List<Book> findAvailableBooks();
    
    @Query("SELECT b FROM Book b WHERE b.isbn = :isbn")
    @NonNull
    Optional<Book> findByIsbn(@Param("isbn") @NonNull String isbn);
    
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Book> findByTitleContainingIgnoreCase(@Param("title") String title);
    
    long countByAvailableTrue();
    
    long countByAvailableFalse();
}

