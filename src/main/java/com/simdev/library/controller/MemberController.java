package com.simdev.library.controller;

import com.simdev.library.assembler.MemberResourceAssembler;
import com.simdev.library.domain.enums.LoanStatus;
import com.simdev.library.domain.model.Member;
import com.simdev.library.repository.LoanRepository;
import com.simdev.library.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "API pour la gestion des membres")
public class MemberController {
    
    private final MemberRepository memberRepository;
    private final MemberResourceAssembler memberResourceAssembler;
    private final LoanRepository loanRepository;
    
    @GetMapping
    @Operation(summary = "Récupérer tous les membres")
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public ResponseEntity<CollectionModel<EntityModel<Member>>> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        members.forEach(this::populateLoanStats);
        CollectionModel<EntityModel<Member>> collectionModel = CollectionModel.of(
            members.stream().map(memberResourceAssembler::toModel).toList(),
            linkTo(methodOn(MemberController.class).getAllMembers()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un membre par ID")
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Member>> getMember(@PathVariable Long id) {
        return memberRepository.findById(id)
            .map(member -> {
                populateLoanStats(member);
                return memberResourceAssembler.toModel(member);
            })
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Créer un nouveau membre")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Member>> createMember(@RequestBody Member member) {
        Member createdMember = memberRepository.save(member);
        populateLoanStats(createdMember);
        EntityModel<Member> memberModel = memberResourceAssembler.toModel(createdMember);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberModel);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable @NonNull Long id) {
        if (!memberRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
        }
        memberRepository.deleteById(id);
    }

    private void populateLoanStats(Member member) {
        if (member == null || member.getId() == null) {
            return;
        }
        long activeLoans = loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE)
            + loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.OVERDUE);
        member.setActiveLoans(activeLoans);
        int maxLoans = member.getMaxLoans() != null ? member.getMaxLoans() : 0;
        int remaining = (int) Math.max(0, (long) maxLoans - activeLoans);
        member.setRemainingLoans(remaining);
    }
}


