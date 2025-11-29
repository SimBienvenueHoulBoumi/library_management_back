package com.simdev.library.config;

import com.simdev.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LibraryHealthIndicator implements HealthIndicator {
    
    private final BookRepository bookRepository;
    
    @Override
    public Health health() {
        long availableBooks = bookRepository.countByAvailableTrue();
        long totalBooks = bookRepository.count();
        double availabilityRate = totalBooks > 0 ? (availableBooks * 100.0 / totalBooks) : 0;
        
        Health.Builder status = totalBooks > 0 && availabilityRate > 20 
            ? Health.up() 
            : Health.down();
        
        return status
            .withDetail("availableBooks", availableBooks)
            .withDetail("totalBooks", totalBooks)
            .withDetail("availabilityRate", String.format("%.2f%%", availabilityRate))
            .build();
    }
}


