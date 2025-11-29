package com.simdev.library.service;

import com.simdev.library.domain.model.Author;
import com.simdev.library.domain.model.Book;
import com.simdev.library.domain.model.Category;
import com.simdev.library.repository.AuthorRepository;
import com.simdev.library.repository.BookRepository;
import com.simdev.library.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {
    
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }
    
    @Override
    public Book createBook(Book book) {
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        if (book.getAvailableCopies() > 0) {
            book.setAvailable(true);
        }

        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            Set<Author> resolvedAuthors = book.getAuthors().stream()
                .map(this::resolveAuthor)
                .collect(Collectors.toSet());
            book.getAuthors().clear();
            resolvedAuthors.forEach(book::addAuthor);
        }

        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            Set<Category> resolvedCategories = book.getCategories().stream()
                .map(this::resolveCategory)
                .collect(Collectors.toSet());
            book.getCategories().clear();
            resolvedCategories.forEach(book::addCategory);
        }

        return bookRepository.save(book);
    }
    
    @Override
    public Book updateBook(Long id, Book bookDetails) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        
        // Mettre à jour les propriétés de base
        book.setTitle(bookDetails.getTitle());
        book.setIsbn(bookDetails.getIsbn());
        book.setDescription(bookDetails.getDescription());
        book.setTotalCopies(bookDetails.getTotalCopies());
        
        // Mettre à jour les auteurs si fournis
        if (bookDetails.getAuthors() != null) {
            Set<Author> authorsToRemove = new HashSet<>(book.getAuthors());
            authorsToRemove.forEach(book::removeAuthor);

            bookDetails.getAuthors().stream()
                .map(this::resolveAuthor)
                .forEach(book::addAuthor);
        }
        
        // Mettre à jour les catégories si fournies
        if (bookDetails.getCategories() != null) {
            Set<Category> categoriesToRemove = new HashSet<>(book.getCategories());
            categoriesToRemove.forEach(book::removeCategory);

            bookDetails.getCategories().stream()
                .map(this::resolveCategory)
                .forEach(book::addCategory);
        }
        
        // Mettre à jour les exemplaires disponibles
        // On conserve le nombre actuel d'exemplaires disponibles pour ne pas perdre l'information des emprunts
        // Si totalCopies diminue, on ajuste availableCopies en conséquence
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        } else {
            int currentAvailable = book.getAvailableCopies();
            int currentTotal = book.getTotalCopies();
            int newTotal = bookDetails.getTotalCopies();
            
            // Calculer la différence
            int difference = newTotal - currentTotal;
            // Ajuster les exemplaires disponibles en fonction de la différence
            book.setAvailableCopies(Math.max(0, currentAvailable + difference));
        }
        
        book.setAvailable(book.getAvailableCopies() > 0);
        
        return bookRepository.save(book);
    }
    
    @Override
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        bookRepository.delete(book);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Book> searchBooks(Specification<Book> spec, Pageable pageable) {
        return bookRepository.findAll(spec, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Book> findAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }
    
    @Override
    public Book addAuthorToBook(Long bookId, Long authorId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
        Author author = authorRepository.findById(authorId)
            .orElseThrow(() -> new RuntimeException("Author not found with id: " + authorId));
        
        book.addAuthor(author);
        return bookRepository.save(book);
    }
    
    @Override
    public Book addCategoryToBook(Long bookId, Long categoryId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        
        book.addCategory(category);
        return bookRepository.save(book);
    }

    private Author resolveAuthor(Author authorDetail) {
        if (authorDetail.getId() != null) {
            return authorRepository.findById(authorDetail.getId())
                .orElseThrow(() -> new RuntimeException("Auteur introuvable (id " + authorDetail.getId() + ")."));
        }

        String firstName = sanitize(authorDetail.getFirstName(), "Auteur");
        String lastName = sanitize(authorDetail.getLastName(), "Inconnu");

        return authorRepository.findByFirstNameAndLastName(firstName, lastName)
            .orElseGet(() -> authorRepository.save(
                Author.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .biography(authorDetail.getBiography())
                    .build()
            ));
    }

    private Category resolveCategory(Category categoryDetail) {
        if (categoryDetail.getId() != null) {
            return categoryRepository.findById(categoryDetail.getId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable (id " + categoryDetail.getId() + ")."));
        }

        String name = sanitize(categoryDetail.getName(), "Catégorie");
        return categoryRepository.findByName(name)
            .orElseGet(() -> categoryRepository.save(
                Category.builder()
                    .name(name)
                    .description(categoryDetail.getDescription())
                    .build()
            ));
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}

