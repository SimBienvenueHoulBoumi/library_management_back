# 💾 Repository - Couche d'Accès aux Données

## 🎯 Rôle

Ce répertoire contient les **repositories JPA** qui gèrent l'accès aux données et les requêtes personnalisées.

## 📋 Contenu

### Repositories de Base
- **AuthorRepository** : Accès aux auteurs
- **BookRepository** : Accès aux livres avec Entity Graphs et requêtes personnalisées
- **CategoryRepository** : Accès aux catégories
- **LoanRepository** : Accès aux emprunts avec requêtes métier
- **MemberRepository** : Accès aux membres
- **ReviewRepository** : Accès aux avis

### Spécifications JPA
- **BookSpecifications** : Specifications pour requêtes dynamiques avec Criteria API

## 🔧 Fonctionnalités Avancées

### Entity Graphs
Utilisés dans `BookRepository` pour optimiser le chargement des relations :
- `findById(Long id)` : Charge automatiquement les auteurs et catégories
- `findAll(Pageable pageable)` : Charge les relations lors de la pagination

### Requêtes Personnalisées
- **JPQL** : Requêtes en langage objet (`@Query`)
- **Méthodes dérivées** : Requêtes générées automatiquement à partir du nom de méthode
- **Specifications** : Requêtes dynamiques avec Criteria API

### Exemples

#### BookRepository
```java
@Query("SELECT b FROM Book b WHERE b.available = true AND b.availableCopies > 0")
List<Book> findAvailableBooks();

@Query("SELECT b FROM Book b WHERE b.isbn = :isbn")
Optional<Book> findByIsbn(@Param("isbn") String isbn);
```

#### BookSpecifications
Permet de construire des requêtes dynamiques :
```java
Specification<Book> spec = BookSpecifications.combine(
    BookSpecifications.hasTitle("Spring"),
    BookSpecifications.hasAuthor("Long"),
    BookSpecifications.isAvailable()
);
```

## 📊 Pagination

Les repositories étendent `JpaRepository` qui fournit automatiquement :
- `findAll(Pageable)` : Pagination
- `findAll(Sort)` : Tri
- Support natif de Spring Data JPA

## 🔍 Recherche Avancée

`BookSpecifications` utilise le pattern **Specification** pour :
- Construire des requêtes dynamiques
- Combiner plusieurs critères
- Utiliser le Criteria API de JPA

