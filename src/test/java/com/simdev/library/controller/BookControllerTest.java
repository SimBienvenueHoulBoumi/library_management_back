package com.simdev.library.controller;

import com.simdev.library.assembler.BookResourceAssembler;
import com.simdev.library.domain.model.Book;
import com.simdev.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookService bookService;

    @Mock
    private BookResourceAssembler bookResourceAssembler;

    @Mock
    private PagedResourcesAssembler<Book> pagedResourcesAssembler;

    @InjectMocks
    private BookController bookController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookController)
            // Permet de résoudre automatiquement les paramètres Pageable dans les contrôleurs
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    @DisplayName("GET /api/books retourne une page de livres")
    void getAllBooks_returnsPagedBooks() throws Exception {
        Book book = Book.builder().id(1L).title("Clean Code").build();
        Page<Book> page = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);

        PagedModel<EntityModel<Book>> pagedModel =
            PagedModel.of(List.of(EntityModel.of(book)), new PagedModel.PageMetadata(1, 0, 1));

        Mockito.when(bookService.findAll(Mockito.any(Pageable.class))).thenReturn(page);
        Mockito.when(pagedResourcesAssembler.toModel(Mockito.eq(page), Mockito.eq(bookResourceAssembler)))
                .thenReturn(pagedModel);

        mockMvc.perform(get("/api/books").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books/{id} retourne un livre existant")
    void getBook_returnsExistingBook() throws Exception {
        Book book = Book.builder().id(2L).title("Domain-Driven Design").build();
        EntityModel<Book> model = EntityModel.of(book);

        Mockito.when(bookService.findById(2L)).thenReturn(Optional.of(book));
        Mockito.when(bookResourceAssembler.toModel(book)).thenReturn(model);

        mockMvc.perform(get("/api/books/{id}", 2L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("id").value(2L))
            .andExpect(jsonPath("title").value("Domain-Driven Design"));
    }
}


