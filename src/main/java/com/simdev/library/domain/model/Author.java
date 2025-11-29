package com.simdev.library.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "authors")
@Data
@EqualsAndHashCode(exclude = {"books"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    private String biography;
    
    @ManyToMany(mappedBy = "authors")
    @JsonIgnore
    @Builder.Default
    private Set<Book> books = new HashSet<>();
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

