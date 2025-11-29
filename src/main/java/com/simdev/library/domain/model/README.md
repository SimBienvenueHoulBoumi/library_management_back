# 📦 Model - Entités JPA

## 🎯 Rôle

Ce répertoire contient toutes les **entités JPA** qui représentent les tables de la base de données. Ces classes utilisent les annotations JPA pour le mapping objet-relationnel.

## 📋 Entités

### Author
- **Table** : `authors`
- **Relations** : Many-to-Many avec `Book`
- **Champs** : firstName, lastName, biography

### Book
- **Table** : `books`
- **Relations** : Many-to-Many avec `Author` et `Category`, One-to-Many avec `Loan` et `Review`
- **Champs** : title, isbn, description, totalCopies, availableCopies, available
- **Auditing** : createdDate, lastModifiedDate
- **Entity Graph** : `Book.withAuthorsAndCategories`

### Category
- **Table** : `categories`
- **Relations** : Many-to-Many avec `Book`, auto-référence (parent-enfant)
- **Champs** : name, description, parent

### Loan
- **Table** : `loans`
- **Relations** : Many-to-One avec `Member` et `Book`
- **Champs** : status, loanDate, dueDate, returnDate
- **Auditing** : createdDate, lastModifiedDate
- **Méthodes métier** : `isOverdue()`, `markAsReturned()`

### Member
- **Table** : `members`
- **Relations** : One-to-Many avec `Loan` et `Review`
- **Champs** : firstName, lastName, email, phoneNumber, maxLoans
- **Méthodes métier** : `canBorrow()`, `getActiveLoansCount()`

### Review
- **Table** : `reviews`
- **Relations** : Many-to-One avec `Book` et `Member`
- **Champs** : rating (1-5), comment
- **Auditing** : createdDate, lastModifiedDate

## 🔧 Annotations Utilisées

- `@Entity` : Marque la classe comme entité JPA
- `@Table` : Spécifie le nom de la table
- `@Id` et `@GeneratedValue` : Clé primaire auto-générée
- `@ManyToMany`, `@OneToMany`, `@ManyToOne` : Relations JPA
- `@JoinTable` : Table de jointure pour Many-to-Many
- `@CreatedDate`, `@LastModifiedDate` : Auditing automatique
- `@EqualsAndHashCode(exclude = {...})` : Évite les boucles infinies dans hashCode()

