package com.simdev.library.services.integration;

import com.simdev.library.domain.model.Book;
import com.simdev.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class LibraryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/books crée un livre et le persiste en base (IT)")
    void createBook_persistsBook() throws Exception {
        long before = bookRepository.count();

        String json = """
            {
              "title": "Clean Code",
              "isbn": "9780132350884",
              "description": "A Handbook of Agile Software Craftsmanship",
              "totalCopies": 3
            }
            """;

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("id").isNumber())
            .andExpect(jsonPath("title").value("Clean Code"));

        long after = bookRepository.count();
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("GET /api/books retourne les livres persistés (IT)")
    void getBooks_returnsPersistedBooks() throws Exception {
        Book book = Book.builder()
            .title("Refactoring")
            .isbn("9780201485677")
            .description("Improving the Design of Existing Code")
            .totalCopies(2)
            .availableCopies(2)
            .available(true)
            .build();
        book = bookRepository.save(book);

        mockMvc.perform(get("/api/books").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("_embedded.bookList[0].id").isNumber())
            .andExpect(jsonPath("_embedded.bookList[0].title").value("Refactoring"));
    }
}


