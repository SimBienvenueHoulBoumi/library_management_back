package com.simdev.library.controller;

import com.simdev.library.assembler.LoanResourceAssembler;
import com.simdev.library.domain.model.Loan;
import com.simdev.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "API pour la gestion des emprunts")
public class LoanController {
    
    private final LoanService loanService;
    private final LoanResourceAssembler loanResourceAssembler;
    
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Récupérer tous les emprunts")
    public ResponseEntity<CollectionModel<EntityModel<Loan>>> getAllLoans() {
        List<Loan> loans = loanService.findAll();
        CollectionModel<EntityModel<Loan>> collectionModel = CollectionModel.of(
            loans.stream().map(loanResourceAssembler::toModel).toList(),
            linkTo(methodOn(LoanController.class).getAllLoans()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un emprunt par ID")
    public ResponseEntity<EntityModel<Loan>> getLoan(@PathVariable Long id) {
        return loanService.findById(id)
            .map(loanResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Créer un nouvel emprunt")
    public ResponseEntity<EntityModel<Loan>> createLoan(
            @RequestParam Long memberId,
            @RequestParam Long bookId,
            @RequestBody(required = false) Object body) {
        Loan loan = loanService.createLoan(memberId, bookId);
        EntityModel<Loan> loanModel = loanResourceAssembler.toModel(loan);
        return ResponseEntity.status(HttpStatus.CREATED).body(loanModel);
    }
    
    @PutMapping("/{id}/return")
    @Operation(summary = "Retourner un livre")
    public ResponseEntity<EntityModel<Loan>> returnBook(@PathVariable Long id) {
        Loan returnedLoan = loanService.returnBook(id);
        EntityModel<Loan> loanModel = loanResourceAssembler.toModel(returnedLoan);
        return ResponseEntity.ok(loanModel);
    }
    
    @GetMapping("/member/{memberId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Récupérer les emprunts d'un membre")
    public ResponseEntity<CollectionModel<EntityModel<Loan>>> getLoansByMember(@PathVariable Long memberId) {
        List<Loan> loans = loanService.findByMemberId(memberId);
        CollectionModel<EntityModel<Loan>> collectionModel = CollectionModel.of(
            loans.stream().map(loanResourceAssembler::toModel).toList(),
            linkTo(methodOn(LoanController.class).getLoansByMember(memberId)).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/book/{bookId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Récupérer les emprunts d'un livre")
    public ResponseEntity<CollectionModel<EntityModel<Loan>>> getLoansByBook(@PathVariable Long bookId) {
        List<Loan> loans = loanService.findByBookId(bookId);
        CollectionModel<EntityModel<Loan>> collectionModel = CollectionModel.of(
            loans.stream().map(loanResourceAssembler::toModel).toList(),
            linkTo(methodOn(LoanController.class).getLoansByBook(bookId)).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
}


