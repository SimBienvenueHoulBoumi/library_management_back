# 🎮 Controller - REST Controllers

## 🎯 Rôle

Ce répertoire contient tous les **REST Controllers** qui exposent l'API RESTful de l'application. Chaque controller gère les opérations CRUD pour une ressource spécifique.

## 📋 Contenu

- **AuthorController** : Gestion des auteurs (`/api/authors`)
- **BookController** : Gestion des livres (`/api/books`) avec pagination et recherche avancée
- **CategoryController** : Gestion des catégories (`/api/categories`)
- **LoanController** : Gestion des emprunts (`/api/loans`)
- **MemberController** : Gestion des membres (`/api/members`)
- **ReviewController** : Gestion des avis (`/api/reviews`)

## 🔗 HATEOAS

Tous les controllers utilisent les **Resource Assemblers** pour retourner des réponses avec des liens hypermédia :
- Lien `self` vers la ressource
- Liens vers les collections
- Liens vers les ressources associées
- Liens conditionnels selon l'état

## 📊 Fonctionnalités Avancées

### BookController
- **Pagination** : `GET /api/books?page=0&size=10`
- **Recherche avancée** : `GET /api/books/search?title=Spring&author=Long&available=true`
- **Entity Graphs** : Chargement optimisé des relations (auteurs, catégories)
- **Specifications JPA** : Requêtes dynamiques avec Criteria API

### LoanController
- Création d'emprunts avec validation métier
- Retour de livres avec mise à jour automatique des copies disponibles
- Recherche d'emprunts en retard

## 📝 Documentation

Tous les endpoints sont documentés avec **Swagger/OpenAPI** :
- Accessible via `/swagger-ui.html`
- Annotations `@Operation` et `@Tag` pour la documentation

## 🔒 Validation

- Utilisation de `@Valid` pour la validation des données
- Gestion des erreurs via `GlobalExceptionHandler`

