package com.simdev.library.repository;

import com.simdev.library.domain.enums.LoanStatus;
import com.simdev.library.domain.model.Loan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    @Override
    @EntityGraph(value = "Loan.withMemberAndBookDetails")
    @NonNull
    Optional<Loan> findById(@NonNull Long id);
    
    @Override
    @EntityGraph(value = "Loan.withMemberAndBookDetails")
    @NonNull
    List<Loan> findAll();
    
    @EntityGraph(value = "Loan.withMemberAndBookDetails")
    List<Loan> findByMemberId(Long memberId);
    
    @EntityGraph(value = "Loan.withMemberAndBookDetails")
    List<Loan> findByBookId(Long bookId);
    
    @EntityGraph(value = "Loan.withMemberAndBookDetails")
    List<Loan> findByStatus(LoanStatus status);
    
    @Query("SELECT l FROM Loan l JOIN FETCH l.member JOIN FETCH l.book WHERE l.status = :status AND l.dueDate < :date")
    List<Loan> findOverdueLoans(@Param("status") LoanStatus status, @Param("date") LocalDate date);
    
    long countByMemberIdAndStatus(Long memberId, LoanStatus status);
}

