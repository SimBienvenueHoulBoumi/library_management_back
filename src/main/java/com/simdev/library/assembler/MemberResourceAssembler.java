package com.simdev.library.assembler;

import com.simdev.library.controller.LoanController;
import com.simdev.library.controller.MemberController;
import com.simdev.library.domain.model.Member;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MemberResourceAssembler implements RepresentationModelAssembler<Member, EntityModel<Member>> {
    
    @Override
    @NonNull
    @SuppressWarnings("null")
    public EntityModel<Member> toModel(@NonNull Member member) {
        return EntityModel.of(member,
            linkTo(methodOn(MemberController.class).getMember(member.getId())).withSelfRel(),
            linkTo(methodOn(MemberController.class).getAllMembers()).withRel("members"),
            linkTo(methodOn(LoanController.class).getLoansByMember(member.getId())).withRel("loans")
        );
    }
}

