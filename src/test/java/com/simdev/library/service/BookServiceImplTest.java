package com.simdev.library.service;

import com.simdev.library.domain.model.Author;
import com.simdev.library.domain.model.Book;
import com.simdev.library.domain.model.Category;
import com.simdev.library.repository.AuthorRepository;
import com.simdev.library.repository.BookRepository;
import com.simdev.library.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    @DisplayName("findAll retourne une page de livres depuis le repository")
    void findAll_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book = Book.builder().title("Clean Code").totalCopies(3).availableCopies(3).available(true).build();
        Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);

        given(bookRepository.findAll(pageable)).willReturn(page);

        Page<Book> result = bookService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).findAll(pageable);
    }

    @Test
    @DisplayName("createBook initialise correctement les copies disponibles et les relations simples")
    void createBook_initializesAvailabilityAndRelations() {
        Author author = Author.builder().firstName("Robert").lastName("Martin").build();
        Category category = Category.builder().name("Software").build();

        Book input = Book.builder()
            .title("Clean Architecture")
            .totalCopies(5)
            // Utiliser des ensembles mutables, car le service fait un clear() dessus
            .authors(new HashSet<>(List.of(author)))
            .categories(new HashSet<>(List.of(category)))
            .build();

        given(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .willReturn(Optional.of(author));
        given(categoryRepository.findByName("Software"))
            .willReturn(Optional.of(category));
        given(bookRepository.save(any(Book.class)))
            .willAnswer(invocation -> invocation.getArgument(0, Book.class));

        Book created = bookService.createBook(input);

        assertThat(created.getAvailableCopies()).isEqualTo(5);
        assertThat(created.getAvailable()).isTrue();
        assertThat(created.getAuthors()).hasSize(1);
        assertThat(created.getCategories()).hasSize(1);
    }

    @Test
    @DisplayName("updateBook lève une exception si le livre n'existe pas")
    void updateBook_throwsIfBookNotFound() {
        given(bookRepository.findById(42L)).willReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> bookService.updateBook(42L, Book.builder().build()));

        assertThat(ex.getMessage()).contains("Book not found with id: 42");
    }

    @Test
    @DisplayName("deleteBook supprime le livre existant")
    void deleteBook_deletesExistingBook() {
        Book book = Book.builder().id(10L).title("To delete").build();
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));

        bookService.deleteBook(10L);

        verify(bookRepository).delete(book);
    }
}


