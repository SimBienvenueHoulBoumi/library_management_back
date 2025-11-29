# 📚 Library Management System - Advanced Spring Boot Project

## 🎯 Objectif

Projet Spring Boot avancé démontrant :
- **Spring Boot Avancé** : Profiles, Actuator, Event Listeners
- **JPA Avancé** : Relations complexes, Entity Graphs, Specifications, Auditing
- **HATEOAS** : Navigation hypermédia complète avec Resource Assemblers

## 🚀 Technologies

- Spring Boot 3.4.0
- Spring Data JPA
- Spring HATEOAS
- Spring Boot Actuator
- H2 Database
- Swagger/OpenAPI (SpringDoc)
- Lombok

## 📦 Installation

```bash
cd library-management
./mvnw clean install
./mvnw spring-boot:run
```

## 🔗 Accès

- **Application** : http://localhost:8080
- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **H2 Console** : http://localhost:8080/h2-console
- **Actuator Health** : http://localhost:8080/actuator/health

## 🔐 Authentification & Utilisateurs

- Authentification HTTP Basic basée sur la table `users`
- Identifiants par défaut : `admin` / `user123` (créés automatiquement au démarrage)
- Seuls les endpoints `/api/books/**`, `/api/members/**`, `/api/loans/**` **et** `/api/users/**` nécessitent le rôle `ADMIN`
- En cas de 401, l'API renvoie un JSON (pas de popup navigateur) pour laisser le frontend afficher sa propre page ou modal de connexion
- Nouveaux endpoints `/api/users` pour gérer les comptes (CRUD, rôles, activation)

## 📚 Structure du Projet

```
src/main/java/com/simdev/library/
├── LibraryManagementApplication.java
├── config/
├── domain/
│   ├── model/
│   └── enums/
├── repository/
├── service/
├── controller/
├── assembler/
├── dto/
├── exception/
└── event/
```

## 🎨 Fonctionnalités

- Gestion de livres (Books) avec auteurs et catégories
- Gestion de membres (Members)
- Système d'emprunts (Loans) avec états
- Système d'avis (Reviews)
- Gestion des utilisateurs (création, rôles, activation/désactivation)
- API RESTful avec HATEOAS
- Documentation Swagger complète

## 👤 API Utilisateurs

| Méthode | Endpoint           | Description                     |
|---------|-------------------|---------------------------------|
| GET     | `/api/users`      | Liste paginée des utilisateurs  |
| GET     | `/api/users/{id}` | Détails d'un utilisateur        |
| POST    | `/api/users`      | Création (hash du mot de passe) |
| PUT     | `/api/users/{id}` | Mise à jour (option mot de passe) |
| DELETE  | `/api/users/{id}` | Suppression                     |

Tous les endpoints nécessitent le rôle `ADMIN`.

## 🔐 API Authentification

| Méthode | Endpoint            | Description                                                   |
|---------|--------------------|---------------------------------------------------------------|
| POST    | `/api/auth/login`  | Vérifie un couple `username/password` et retourne le profil. |
| POST    | `/api/auth/register` | Inscrit un nouvel utilisateur (rôle MEMBER par défaut).      |

Ces endpoints sont publics pour permettre l’intégration avec le frontend et apparaissent maintenant dans Swagger.

