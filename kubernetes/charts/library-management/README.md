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
