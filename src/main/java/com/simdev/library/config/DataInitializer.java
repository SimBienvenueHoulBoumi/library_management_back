package com.simdev.library.config;

import com.simdev.library.domain.model.*;
import com.simdev.library.domain.enums.UserRole;
import com.simdev.library.domain.model.UserAccount;
import com.simdev.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        ensureDefaultAdminUser();

        try {
            // Vérifier si la base est déjà initialisée
            long bookCount = bookRepository.count();
            if (bookCount > 0) {
                log.info("Database already initialized with {} books", bookCount);
                return;
            }
            
            log.info("Database is empty, initializing with sample data...");
        } catch (Exception e) {
            log.warn("Error checking database state (this is normal on first startup), will initialize: {}", e.getMessage());
            // Continuer l'initialisation même en cas d'erreur (tables n'existent pas encore)
        }
        
        try {
            // Créer des auteurs
        Author author1 = Author.builder()
            .firstName("Josh")
            .lastName("Long")
            .biography("Spring Framework expert")
            .build();
        author1 = authorRepository.save(author1);
        
        Author author2 = Author.builder()
            .firstName("Craig")
            .lastName("Walls")
            .biography("Spring in Action author")
            .build();
        author2 = authorRepository.save(author2);
        
        // Créer des catégories
        Category category1 = Category.builder()
            .name("Programming")
            .description("Books about programming")
            .build();
        category1 = categoryRepository.save(category1);
        
        Category category2 = Category.builder()
            .name("Spring Framework")
            .description("Spring Framework related books")
            .build();
        category2 = categoryRepository.save(category2);
        
        // Créer des livres
        Book book1 = Book.builder()
            .title("Spring Boot in Action")
            .isbn("978-1617292545")
            .description("A comprehensive guide to Spring Boot")
            .totalCopies(5)
            .availableCopies(5)
            .available(true)
            .build();
        book1.addAuthor(author1);
        book1.addCategory(category1);
        book1.addCategory(category2);
        book1 = bookRepository.save(book1);
        
        Book book2 = Book.builder()
            .title("Spring in Action")
            .isbn("978-1617294945")
            .description("The definitive guide to Spring Framework")
            .totalCopies(3)
            .availableCopies(3)
            .available(true)
            .build();
        book2.addAuthor(author2);
        book2.addCategory(category1);
        book2.addCategory(category2);
        book2 = bookRepository.save(book2);
        
        // Créer des membres
        Member member1 = Member.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("+1234567890")
            .maxLoans(5)
            .build();
        member1 = memberRepository.save(member1);
        
        Member member2 = Member.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .phoneNumber("+0987654321")
            .maxLoans(5)
            .build();
        member2 = memberRepository.save(member2);
        
            log.info("Database initialized successfully with {} books, {} authors, {} categories, {} members",
                bookRepository.count(),
                authorRepository.count(),
                categoryRepository.count(),
                memberRepository.count());
        } catch (Exception e) {
            log.error("Error during database initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void ensureDefaultAdminUser() {
        if (userAccountRepository.existsByUsername("admin")) {
            return;
        }
        UserAccount admin = UserAccount.builder()
                .username("admin")
                .email("admin@library.local")
                .firstName("Super")
                .lastName("Admin")
                .password(passwordEncoder.encode("user123"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();
        userAccountRepository.save(admin);
        log.info("Default admin user created (username: admin / password: user123)");
    }
}


