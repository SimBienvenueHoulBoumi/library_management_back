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

## ArgoCD

Pour déployer l'application dans un cluster Kubernetes via ArgoCD à partir du chart Helm de ce dépôt :

1. **Prérequis** : ArgoCD installé et accessible (ex. https://argocd.localhost), cluster Kind avec l'image chargée si besoin : `./main.sh load-app-image` (depuis le projet `infra`).
2. **Créer l'application** : dans l'interface ArgoCD, **NEW APP** → **EDIT AS YAML**.
3. **Coller le manifeste** ci-dessous (il pointe vers ce repo, le path du chart et les paramètres Helm : image, NodePort 30075, probes).
4. **Créer** puis laisser ArgoCD synchroniser ; l'app sera disponible sur https://library-management.localhost (avec Traefik).

### Manifeste Application ArgoCD (YAML à coller dans l'UI)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: library-management
  namespace: argocd
spec:
  project: default

  source:
    repoURL: https://github.com/SimBienvenueHoulBoumi/library_management_back.git
    path: kubernetes/charts/library-management
    targetRevision: HEAD
    helm:
      parameters:
        - name: image.repository
          value: host.docker.internal:8083/repository/docker-hosted/simdev/library-management
        - name: image.tag
          value: latest
        - name: image.pullPolicy
          value: Never
        - name: service.type
          value: NodePort
        - name: service.nodePort
          value: "30075"
        - name: readinessProbe.initialDelaySeconds
          value: "45"
        - name: livenessProbe.initialDelaySeconds
          value: "60"

  destination:
    server: https://kubernetes.default.svc
    namespace: default

  links:
    - title: Swagger UI
      url: https://library-management.localhost/swagger-ui.html
    - title: API Health
      url: https://library-management.localhost/actuator/health
    - title: API (base)
      url: https://library-management.localhost

  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

