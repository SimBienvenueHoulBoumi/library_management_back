# Chart Helm pour library-management

Ce chart Helm permet de déployer l'application `library-management` dans un cluster Kubernetes via ArgoCD.

## Structure

```
kubernetes/charts/library-management/
├── Chart.yaml          # Métadonnées du chart
├── values.yaml         # Valeurs par défaut
├── templates/          # Templates Kubernetes
│   ├── _helpers.tpl    # Fonctions helper
│   ├── deployment.yaml # Déploiement de l'application
│   ├── service.yaml    # Service Kubernetes
│   ├── ingress.yaml    # Ingress (optionnel)
│   ├── serviceaccount.yaml
│   └── hpa.yaml        # Autoscaling (optionnel)
└── README.md
```

## Configuration

### Image Docker

Par défaut, le chart utilise l'image :
```
localhost:8083/simdev/library-management:0.0.1-SNAPSHOT
```

L'image est construite et poussée par le pipeline Jenkins dans Nexus.

### Ports

- **Port de l'application** : `8075` (configuré dans `application.yml`)
- **Health checks** : `/actuator/health` sur le port `8075`

### Variables d'environnement

Vous pouvez ajouter des variables d'environnement dans `values.yaml` :

```yaml
config:
  env:
    - name: SPRING_PROFILES_ACTIVE
      value: "prod"
    - name: SPRING_DATASOURCE_URL
      value: "jdbc:postgresql://postgres:5432/librarydb"
```

## Utilisation

### Installation manuelle avec Helm

```bash
helm install library-management ./kubernetes/charts/library-management \
  --set image.tag=0.0.1-SNAPSHOT \
  --namespace default
```

### Mise à jour

```bash
helm upgrade library-management ./kubernetes/charts/library-management \
  --set image.tag=0.0.2-SNAPSHOT \
  --namespace default
```

### Via ArgoCD (recommandé)

Le chart est automatiquement déployé par ArgoCD via le pipeline Jenkins.

1. Le pipeline Jenkins construit et pousse l'image Docker
2. ArgoCD synchronise le chart depuis le repository Git
3. L'application est déployée dans le cluster Kubernetes

### Accès à l'application (Swagger UI) sans port-forward

Avec l'infra (Traefik + Kind configuré avec `kind-config.yaml`) :

- **URL :** https://library-management.localhost/swagger-ui.html  
- **Health :** https://library-management.localhost/actuator/health  

Le sous-domaine `.localhost` est résolu automatiquement vers 127.0.0.1 (pas besoin de modifier `/etc/hosts`).  
Le Service est exposé en NodePort 30075 ; Traefik route `library-management.localhost` vers ce port.

Pour un **nouveau cluster Kind**, créer le cluster avec la config qui expose le port :  
`cd infra && ./main.sh cluster-create kind` (utilise `kind-config.yaml`).

## Personnalisation

Modifiez `values.yaml` pour personnaliser :
- Nombre de replicas
- Ressources (CPU/Memory)
- Variables d'environnement
- Ingress
- Autoscaling

## Notes

- L'image Docker doit être accessible depuis le cluster Kubernetes
- Pour un cluster local (kind/k3d), utilisez `host.docker.internal:8083` dans l'image
- Pour un cluster distant, configurez un registry accessible (Nexus, Docker Hub, etc.)

### Manifeste ArgoCD à coller dans l’UI

Utilise **host.docker.internal** (aligné avec le Jenkinsfile et `kind load`). Avant la Sync, charger l’image dans Kind :

```bash
./main.sh load-app-image   # depuis infra/
# ou : ./infra/main.sh load-app-image   # depuis la racine des projets
```
(Depuis la racine du projet **infra** ; charge l’image dans Kind une fois après le 1er build Jenkins.)

Puis créer l’application dans ArgoCD (NEW APP → EDIT AS YAML) et coller le YAML ci‑dessous.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: library-management
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  source:
    repoURL: 'https://github.com/SimBienvenueHoulBoumi/library_management_back.git'
    path: kubernetes/charts/library-management
    targetRevision: HEAD
    helm:
      releaseName: library-management
      valueFiles: []
      values: |
        image:
          repository: host.docker.internal:8083/repository/docker-hosted/simdev/library-management
          tag: latest
          pullPolicy: Never
        service:
          type: NodePort
          nodePort: 30075
        readinessProbe:
          initialDelaySeconds: 45
          failureThreshold: 6
        livenessProbe:
          initialDelaySeconds: 60
  destination:
    server: https://kubernetes.default.svc
    namespace: default
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```
