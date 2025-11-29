# 🔢 Enums - Énumérations

## 🎯 Rôle

Ce répertoire contient les **énumérations** utilisées dans le modèle de domaine pour représenter des valeurs fixes.

## 📋 Énumérations

### LoanStatus
Représente les différents statuts possibles d'un emprunt :
- **PENDING** : Emprunt en attente
- **ACTIVE** : Emprunt actif (livre emprunté)
- **RETURNED** : Livre retourné
- **OVERDUE** : Emprunt en retard

## 💡 Utilisation

L'énumération est utilisée dans l'entité `Loan` avec `@Enumerated(EnumType.STRING)` pour stocker la valeur comme chaîne de caractères dans la base de données.

```java
@Enumerated(EnumType.STRING)
private LoanStatus status = LoanStatus.PENDING;
```

## 🔍 Recherche

L'énumération est utilisée dans les requêtes JPA pour filtrer les emprunts par statut, notamment pour trouver les emprunts en retard.

