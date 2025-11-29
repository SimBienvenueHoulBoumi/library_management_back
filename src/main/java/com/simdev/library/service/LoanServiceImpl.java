package com.simdev.library.service;

import com.simdev.library.domain.enums.LoanStatus;
import com.simdev.library.domain.model.Book;
import com.simdev.library.domain.model.Loan;
import com.simdev.library.domain.model.Member;
import com.simdev.library.repository.BookRepository;
import com.simdev.library.repository.LoanRepository;
import com.simdev.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanServiceImpl implements LoanService {
    
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public Loan createLoan(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));
        
        // Validation métier
        if (!member.canBorrow()) {
            throw new RuntimeException("Member has reached maximum loan limit");
        }
        
        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("Book is not available");
        }
        
        Loan loan = Loan.builder()
            .member(member)
            .book(book)
            .status(LoanStatus.ACTIVE)
            .loanDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(14)) // 2 semaines
            .build();
        
        // Mettre à jour le livre
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        if (book.getAvailableCopies() == 0) {
            book.setAvailable(false);
        }
        bookRepository.save(book);
        
        Loan savedLoan = loanRepository.save(loan);
        
        // Publier un événement
        eventPublisher.publishEvent(new LoanCreatedEvent(savedLoan));
        
        return savedLoan;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Loan> findById(Long id) {
        return loanRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Loan> findAll() {
        return loanRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Loan> findByMemberId(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Loan> findByBookId(Long bookId) {
        return loanRepository.findByBookId(bookId);
    }
    
    @Override
    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));
        
        // Vérifier si l'emprunt est en retard avant de le retourner
        if (loan.getStatus() == LoanStatus.ACTIVE && loan.isOverdue()) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
        }
        
        // Permettre le retour des emprunts ACTIVE ou OVERDUE
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new RuntimeException("Loan is not active or overdue");
        }
        
        loan.markAsReturned();
        Book book = loan.getBook();
        bookRepository.save(book);
        
        Loan returnedLoan = loanRepository.save(loan);
        
        // Publier un événement
        eventPublisher.publishEvent(new LoanReturnedEvent(returnedLoan));
        
        return returnedLoan;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Loan> findOverdueLoans() {
        return loanRepository.findOverdueLoans(LoanStatus.ACTIVE, LocalDate.now());
    }
    
    @Override
    public void checkAndUpdateOverdueLoans() {
        List<Loan> overdueLoans = findOverdueLoans();
        for (Loan loan : overdueLoans) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
        }
    }
    
    // Classe d'événement interne
    public static class LoanCreatedEvent {
        private final Loan loan;
        
        public LoanCreatedEvent(Loan loan) {
            this.loan = loan;
        }
        
        public Loan getLoan() {
            return loan;
        }
    }
    
    public static class LoanReturnedEvent {
        private final Loan loan;
        
        public LoanReturnedEvent(Loan loan) {
            this.loan = loan;
        }
        
        public Loan getLoan() {
            return loan;
        }
    }
}

