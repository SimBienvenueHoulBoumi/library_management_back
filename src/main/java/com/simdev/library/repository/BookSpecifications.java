package com.simdev.library.repository;

import com.simdev.library.domain.model.Book;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecifications {
    
    public static Specification<Book> hasTitle(String title) {
        return (root, query, cb) -> 
            cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }
    
    public static Specification<Book> isAvailable() {
        return (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("available"), true),
                cb.greaterThan(root.get("availableCopies"), 0)
            );
    }
    
    public static Specification<Book> hasAuthor(String authorName) {
        return (root, query, cb) -> {
            var authorJoin = root.join("authors", JoinType.INNER);
            return cb.or(
                cb.like(cb.lower(authorJoin.get("firstName")), "%" + authorName.toLowerCase() + "%"),
                cb.like(cb.lower(authorJoin.get("lastName")), "%" + authorName.toLowerCase() + "%")
            );
        };
    }
    
    public static Specification<Book> hasCategory(String categoryName) {
        return (root, query, cb) -> {
            var categoryJoin = root.join("categories", JoinType.INNER);
            return cb.like(cb.lower(categoryJoin.get("name")), "%" + categoryName.toLowerCase() + "%");
        };
    }
    
    public static Specification<Book> hasIsbn(String isbn) {
        return (root, query, cb) -> 
            cb.like(cb.lower(root.get("isbn")), "%" + isbn.toLowerCase() + "%");
    }
    
    public static Specification<Book> combine(Specification<Book>... specs) {
        Specification<Book> combined = null;
        for (Specification<Book> spec : specs) {
            if (spec != null) {
                combined = combined == null ? spec : combined.and(spec);
            }
        }
        return combined;
    }
}

