package com.simdev.library.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "members")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@EqualsAndHashCode(exclude = {"loans", "reviews"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String firstName;
    
    @NotBlank
    @Column(nullable = false)
    private String lastName;
    
    @Email
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer maxLoans = 5;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Prevent infinite recursion
    @Builder.Default
    private Set<Loan> loans = new HashSet<>();
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Prevent infinite recursion
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    @JsonIgnore
    @Transient
    private Long activeLoans;

    @Transient
    private Integer remainingLoans;

    public Long getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(Long activeLoans) {
        this.activeLoans = activeLoans;
    }

    public Integer getRemainingLoans() {
        return remainingLoans;
    }

    public void setRemainingLoans(Integer remainingLoans) {
        this.remainingLoans = remainingLoans;
    }

    @JsonIgnore
    public boolean canBorrow() {
        int max = maxLoans != null ? maxLoans : 0;
        long active = activeLoans != null ? activeLoans : 0L;
        return active < max;
    }
}

