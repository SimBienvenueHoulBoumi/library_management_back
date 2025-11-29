package com.simdev.library.service;

import com.simdev.library.domain.model.Loan;
import com.simdev.library.domain.enums.LoanStatus;

import java.util.List;
import java.util.Optional;

public interface LoanService {
    Loan createLoan(Long memberId, Long bookId);
    Optional<Loan> findById(Long id);
    List<Loan> findAll();
    List<Loan> findByMemberId(Long memberId);
    List<Loan> findByBookId(Long bookId);
    Loan returnBook(Long loanId);
    List<Loan> findOverdueLoans();
    void checkAndUpdateOverdueLoans();
}

