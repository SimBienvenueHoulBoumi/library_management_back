package com.simdev.library.event;

import com.simdev.library.service.LoanServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoanEventListener {
    
    @EventListener
    @Async
    public void handleLoanCreated(LoanServiceImpl.LoanCreatedEvent event) {
        log.info("📚 Nouvel emprunt créé : Livre ID={}, Membre ID={}, Date d'emprunt={}",
            event.getLoan().getBook().getId(),
            event.getLoan().getMember().getId(),
            event.getLoan().getLoanDate());
        
        // Ici on pourrait envoyer une notification, un email, etc.
    }
    
    @EventListener
    @Async
    public void handleLoanReturned(LoanServiceImpl.LoanReturnedEvent event) {
        log.info("✅ Livre retourné : Livre ID={}, Membre ID={}, Date de retour={}",
            event.getLoan().getBook().getId(),
            event.getLoan().getMember().getId(),
            event.getLoan().getReturnDate());
        
        // Ici on pourrait mettre à jour des statistiques, envoyer une notification, etc.
    }
}

