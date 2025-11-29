# 🎯 Service - Couche Métier

## 🎯 Rôle

Ce répertoire contient les **services métier** qui implémentent la logique business de l'application. Les services font le lien entre les controllers et les repositories.

## 📋 Contenu

### Interfaces de Service
- **BookService** : Interface définissant les opérations sur les livres
- **LoanService** : Interface définissant les opérations sur les emprunts

### Implémentations
- **BookServiceImpl** : Implémentation de `BookService`
- **LoanServiceImpl** : Implémentation de `LoanService`

## 🔧 Fonctionnalités

### BookService
- **CRUD** : Création, lecture, mise à jour, suppression de livres
- **Recherche** : Recherche avec Specifications JPA (critères dynamiques)
- **Pagination** : Support de la pagination avec `Pageable`
- **Entity Graphs** : Utilisation des Entity Graphs pour optimiser les requêtes

### LoanService
- **Création d'emprunt** : Validation métier (membre peut emprunter, livre disponible)
- **Retour de livre** : Mise à jour automatique des copies disponibles
- **Recherche** : Recherche d'emprunts par membre, livre, statut
- **Emprunts en retard** : Détection et mise à jour automatique
- **Événements** : Publication d'événements (`LoanCreatedEvent`, `LoanReturnedEvent`)

## 🔒 Validation Métier

### LoanService
- Vérifie que le membre n'a pas atteint sa limite d'emprunts (`canBorrow()`)
- Vérifie que le livre est disponible (`availableCopies > 0`)
- Met à jour automatiquement les copies disponibles lors du retour

## ⚡ Transactions

Toutes les méthodes sont annotées avec `@Transactional` :
- **Lecture seule** : `@Transactional(readOnly = true)` pour les méthodes de recherche
- **Écriture** : Transaction complète pour les modifications

## 📢 Événements

`LoanServiceImpl` publie des événements via `ApplicationEventPublisher` :
- Permet le découplage avec les Event Listeners
- Facilite l'ajout de nouvelles fonctionnalités (notifications, logs, etc.)

## 💡 Exemple

```java
@Transactional
public Loan createLoan(Long memberId, Long bookId) {
    // Validation métier
    if (!member.canBorrow()) {
        throw new RuntimeException("Member has reached maximum loan limit");
    }
    
    // Création de l'emprunt
    Loan loan = Loan.builder()...build();
    
    // Publication d'événement
    eventPublisher.publishEvent(new LoanCreatedEvent(loan));
    
    return loan;
}
```

