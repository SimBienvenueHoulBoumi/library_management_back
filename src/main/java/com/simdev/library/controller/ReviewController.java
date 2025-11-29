package com.simdev.library.controller;

import com.simdev.library.domain.model.Review;
import com.simdev.library.repository.ReviewRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "API pour la gestion des avis")
public class ReviewController {
    
    private final ReviewRepository reviewRepository;
    
    @GetMapping("/book/{bookId}")
    @Operation(summary = "Récupérer les avis d'un livre")
    @SuppressWarnings("null")
    public ResponseEntity<CollectionModel<EntityModel<Review>>> getReviewsByBook(@PathVariable Long bookId) {
        List<Review> reviews = reviewRepository.findByBookId(bookId);
        CollectionModel<EntityModel<Review>> collectionModel = CollectionModel.of(
            reviews.stream().map(review -> EntityModel.of(review,
                linkTo(methodOn(ReviewController.class).getReview(review.getId())).withSelfRel(),
                linkTo(methodOn(ReviewController.class).getReviewsByBook(bookId)).withRel("reviews")
            )).toList(),
            linkTo(methodOn(ReviewController.class).getReviewsByBook(bookId)).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un avis par ID")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Review>> getReview(@PathVariable Long id) {
        return reviewRepository.findById(id)
            .map(review -> EntityModel.of(review,
                linkTo(methodOn(ReviewController.class).getReview(id)).withSelfRel()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Créer un nouvel avis")
    @SuppressWarnings("null")
    public ResponseEntity<EntityModel<Review>> createReview(@RequestBody Review review) {
        Review createdReview = reviewRepository.save(review);
        EntityModel<Review> reviewModel = EntityModel.of(createdReview,
            linkTo(methodOn(ReviewController.class).getReview(createdReview.getId())).withSelfRel()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewModel);
    }
}


