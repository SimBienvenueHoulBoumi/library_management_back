# ⚙️ Config - Configuration Spring

## 🎯 Rôle

Ce répertoire contient toutes les classes de configuration Spring Boot pour personnaliser le comportement de l'application.

## 📋 Contenu

- **AsyncConfig** : Configuration pour le traitement asynchrone des événements (Event Listeners)
- **DataInitializer** : Initialise la base de données avec des données de démonstration au démarrage
- **JpaConfig** : Configuration JPA avec activation de l'auditing (suivi automatique des dates de création/modification)
- **LibraryHealthIndicator** : Health Indicator personnalisé pour Actuator qui vérifie l'état de la bibliothèque
- **ResourceNotFoundFilter** : Filtre pour ignorer silencieusement les requêtes vers des ressources statiques inexistantes

## 🔧 Fonctionnalités

### AsyncConfig
- Configure un `ExecutorService` pour le traitement asynchrone
- Permet aux Event Listeners de s'exécuter sans bloquer le thread principal

### DataInitializer
- Implémente `CommandLineRunner` pour s'exécuter au démarrage
- Crée des auteurs, catégories, livres et membres de test
- Vérifie si la base est déjà initialisée pour éviter les doublons

### JpaConfig
- Active l'auditing JPA avec `@EnableJpaAuditing`
- Permet l'utilisation de `@CreatedDate` et `@LastModifiedDate` sur les entités

### LibraryHealthIndicator
- Vérifie le nombre de livres disponibles
- Retourne `UP` ou `DOWN` selon l'état de la bibliothèque
- Accessible via `/actuator/health`

### ResourceNotFoundFilter
- Intercepte les requêtes vers `/wsagents`, `/api/pool/metrics`, etc.
- Retourne 404 silencieusement pour éviter les warnings inutiles

