# 🏗️ Domain - Modèle de Domaine

## 🎯 Rôle

Ce répertoire contient le **modèle de domaine** de l'application, c'est-à-dire les entités métier et les énumérations qui représentent les concepts du domaine "Bibliothèque".

## 📋 Structure

```
domain/
├── model/          # Entités JPA
└── enums/          # Énumérations
```

## 📦 Contenu

### model/
- **Author** : Auteur de livres (relation Many-to-Many avec Book)
- **Book** : Livre avec ISBN, copies disponibles, etc. (relations avec Author, Category, Loan, Review)
- **Category** : Catégorie de livres avec hiérarchie parent-enfant
- **Loan** : Emprunt d'un livre par un membre avec dates et statut
- **Member** : Membre de la bibliothèque avec limite d'emprunts
- **Review** : Avis/note d'un membre sur un livre

### enums/
- **LoanStatus** : Statuts possibles d'un emprunt (PENDING, ACTIVE, RETURNED, OVERDUE)

## 🔗 Relations JPA

### Relations Many-to-Many
- `Book` ↔ `Author` : Un livre peut avoir plusieurs auteurs, un auteur peut écrire plusieurs livres
- `Book` ↔ `Category` : Un livre peut avoir plusieurs catégories, une catégorie peut contenir plusieurs livres

### Relations One-to-Many
- `Book` → `Loan` : Un livre peut avoir plusieurs emprunts
- `Book` → `Review` : Un livre peut avoir plusieurs avis
- `Member` → `Loan` : Un membre peut avoir plusieurs emprunts
- `Member` → `Review` : Un membre peut écrire plusieurs avis
- `Category` → `Category` : Hiérarchie parent-enfant pour les catégories

## ✨ Fonctionnalités Avancées

### Auditing
- `@CreatedDate` et `@LastModifiedDate` sur `Book`, `Loan`, `Review`
- Suivi automatique des dates de création et modification

### Entity Graphs
- `Book.withAuthorsAndCategories` : Chargement optimisé des relations

### Méthodes Métier
- `Member.canBorrow()` : Vérifie si un membre peut emprunter
- `Loan.isOverdue()` : Vérifie si un emprunt est en retard
- `Loan.markAsReturned()` : Marque un emprunt comme retourné

### Gestion des Collections
- Méthodes `addAuthor()`, `removeAuthor()`, `addCategory()`, etc. pour gérer les relations bidirectionnelles

