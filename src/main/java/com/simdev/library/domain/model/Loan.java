package com.simdev.library.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simdev.library.domain.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Loan.withMemberAndBook",
        attributeNodes = {
            @NamedAttributeNode("member"),
            @NamedAttributeNode("book")
        }
    ),
    @NamedEntityGraph(
        name = "Loan.withMemberAndBookDetails",
        attributeNodes = {
            @NamedAttributeNode("member"),
            @NamedAttributeNode(value = "book", subgraph = "book.authorsAndCategories")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "book.authorsAndCategories",
                attributeNodes = {
                    @NamedAttributeNode("authors"),
                    @NamedAttributeNode("categories")
                }
            )
        }
    )
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"member", "book"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDate loanDate;
    
    private LocalDate dueDate;
    
    private LocalDate returnDate;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;
    
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && 
               dueDate != null && 
               LocalDate.now().isAfter(dueDate);
    }
    
    public void markAsReturned() {
        this.status = LoanStatus.RETURNED;
        this.returnDate = LocalDate.now();
        if (book != null) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            if (book.getAvailableCopies() > 0) {
                book.setAvailable(true);
            }
        }
    }
}

