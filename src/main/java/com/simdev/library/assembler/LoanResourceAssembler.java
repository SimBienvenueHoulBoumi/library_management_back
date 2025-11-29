package com.simdev.library.assembler;

import com.simdev.library.controller.BookController;
import com.simdev.library.controller.LoanController;
import com.simdev.library.controller.MemberController;
import com.simdev.library.domain.enums.LoanStatus;
import com.simdev.library.domain.model.Loan;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class LoanResourceAssembler implements RepresentationModelAssembler<Loan, EntityModel<Loan>> {
    
    @Override
    @NonNull
    @SuppressWarnings("null")
    public EntityModel<Loan> toModel(@NonNull Loan loan) {
        EntityModel<Loan> loanModel = EntityModel.of(loan,
            linkTo(methodOn(LoanController.class).getLoan(loan.getId())).withSelfRel(),
            linkTo(methodOn(LoanController.class).getAllLoans()).withRel("loans"),
            linkTo(methodOn(MemberController.class).getMember(loan.getMember().getId())).withRel("member"),
            linkTo(methodOn(BookController.class).getBook(loan.getBook().getId())).withRel("book")
        );
        
        // Lien pour retourner (seulement si actif)
        if (loan.getStatus() == LoanStatus.ACTIVE) {
            loanModel.add(linkTo(methodOn(LoanController.class).returnBook(loan.getId()))
                .withRel("return"));
        }
        
        return loanModel;
    }
}

