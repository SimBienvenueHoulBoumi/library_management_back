# 📦 Assembler - Resource Assemblers HATEOAS

## 🎯 Rôle

Ce répertoire contient les **Resource Assemblers** qui implémentent le pattern HATEOAS (Hypermedia as the Engine of Application State). Ces classes sont responsables de la création de ressources hypermédia avec des liens de navigation.

## 📋 Contenu

- **BookResourceAssembler** : Transforme les entités `Book` en `EntityModel<Book>` avec des liens vers les ressources associées (auteurs, catégories, avis, emprunts)
- **LoanResourceAssembler** : Transforme les entités `Loan` en `EntityModel<Loan>` avec des liens vers le membre, le livre, et les actions possibles (retour)
- **MemberResourceAssembler** : Transforme les entités `Member` en `EntityModel<Member>` avec des liens vers les emprunts

## 🔗 HATEOAS

Chaque assembler implémente `RepresentationModelAssembler<T, EntityModel<T>>` et ajoute automatiquement :
- Un lien `self` vers la ressource elle-même
- Des liens vers les collections (`rel="books"`, `rel="loans"`, etc.)
- Des liens conditionnels basés sur l'état de l'entité (ex: lien "borrow" seulement si le livre est disponible)

## 💡 Exemple

```java
EntityModel<Book> bookModel = bookResourceAssembler.toModel(book);
// Contient automatiquement des liens vers :
// - /api/books/{id} (self)
// - /api/books (collection)
// - /api/authors?bookId={id} (si auteurs présents)
// - /api/loans (si disponible)
```

