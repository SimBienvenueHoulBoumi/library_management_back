# 📢 Event - Event Listeners

## 🎯 Rôle

Ce répertoire contient les **Event Listeners** qui écoutent et traitent les événements métier de l'application de manière asynchrone.

## 📋 Contenu

### LoanEventListener
Écoute les événements liés aux emprunts :
- **LoanCreatedEvent** : Déclenché lorsqu'un emprunt est créé
- **LoanReturnedEvent** : Déclenché lorsqu'un livre est retourné

## ⚡ Traitement Asynchrone

Les listeners sont configurés pour s'exécuter de manière **asynchrone** via `@Async` :
- N'bloquent pas le thread principal
- Améliorent les performances
- Permettent un traitement en arrière-plan

## 🔧 Configuration

L'asynchrone est configuré dans `AsyncConfig` avec un `ExecutorService` dédié.

## 💡 Exemple d'Usage

```java
@EventListener
@Async
public void handleLoanCreated(LoanCreatedEvent event) {
    // Traitement asynchrone (ex: envoi d'email, notification, etc.)
    log.info("Loan created: {}", event.getLoan().getId());
}
```

## 🎯 Avantages

- **Découplage** : Les services métier ne sont pas couplés aux actions secondaires
- **Performance** : Les opérations longues ne bloquent pas les requêtes HTTP
- **Extensibilité** : Facile d'ajouter de nouveaux listeners pour de nouveaux événements

