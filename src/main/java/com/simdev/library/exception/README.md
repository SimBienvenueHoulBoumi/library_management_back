# ⚠️ Exception - Gestion Globale des Erreurs

## 🎯 Rôle

Ce répertoire contient le **Global Exception Handler** qui centralise la gestion des exceptions dans l'application.

## 📋 Contenu

### GlobalExceptionHandler
Classe annotée avec `@RestControllerAdvice` qui intercepte toutes les exceptions levées dans les controllers et retourne des réponses HTTP standardisées.

## 🔧 Fonctionnalités

### Gestion des Exceptions
- **RuntimeException** : Retourne une réponse `400 Bad Request`
- **Exception** : Retourne une réponse `500 Internal Server Error`

### Format de Réponse Standardisé
Toutes les erreurs retournent un format JSON cohérent :
```json
{
  "timestamp": "2025-11-24T22:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Message d'erreur",
  "path": "/api/..."
}
```

## 💡 Avantages

- **Cohérence** : Toutes les erreurs suivent le même format
- **Centralisation** : Un seul endroit pour gérer les exceptions
- **Maintenabilité** : Facile d'ajouter de nouveaux handlers
- **Sécurité** : Évite l'exposition de détails techniques sensibles

## 🔒 Bonnes Pratiques

- Ne pas exposer les stack traces en production
- Fournir des messages d'erreur clairs et utiles
- Utiliser les codes HTTP appropriés
- Logger les exceptions pour le débogage

