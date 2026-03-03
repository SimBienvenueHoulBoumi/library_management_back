# Créer l’application ArgoCD pour library-management

Créez l’application **library-management** depuis l’interface ArgoCD en collant le manifeste ci‑dessous.

## Étapes

1. Ouvrez l’UI ArgoCD : **https://argocd.localhost**
2. Connectez-vous (mot de passe : `./main.sh argocd-password` depuis le projet `infra`).
3. Cliquez sur **"+ NEW APP"** (ou **"Create"** → **"Application"**).
4. En bas à droite, choisissez **"EDIT AS YAML"**.
5. **Supprimez** le contenu par défaut et **collez** le YAML ci‑dessous.
6. Enregistrez / **Create** pour créer l’application.

ArgoCD va alors suivre le chart Helm `kubernetes/charts/library-management` du dépôt Git et déployer l’app dans le namespace `default`.

---

## Manifeste à coller dans l’UI ArgoCD

L’image Docker est fixée via `source.helm.parameters`. Si le pull échoue (**EOF**, timeout), voir la section *Contournement : charger l’image dans Kind* ci‑dessous.

**Tag de déploiement :** le chart utilise le tag **`latest`** (dernière image poussée par le pipeline, sans SNAPSHOT). Le pipeline Jenkins pousse et charge dans Kind l’image avec ce tag.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: library-management
  namespace: argocd
spec:
  destination:
    name: ''
    namespace: default
    server: https://kubernetes.default.svc
  source:
    path: kubernetes/charts/library-management
    repoURL: https://github.com/SimBienvenueHoulBoumi/library_management_back.git
    targetRevision: HEAD
    helm:
      parameters:
        - name: image.repository
          value: host.docker.internal:8083/repository/docker-hosted/simdev/library-management
        - name: image.tag
          value: "latest"
  project: default
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

---

## Pod « waiting to start: trying and failing to pull image »

Si le pod reste en attente ou en **ImagePullBackOff**, mettre le paramètre Helm **image.tag** à **`latest`** dans l’app ArgoCD, puis charger l’image dans Kind **sur la machine hôte** :

```bash
kind load docker-image host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest --name dev
```

Puis dans ArgoCD : **Sync** l’application. Vérifier que l’app a bien `image.repository` = `host.docker.internal:8083/repository/docker-hosted/simdev/library-management` et `image.tag` = `latest`.

---

## Si le pull échoue (EOF, timeout)

Les nœuds Kind ne joignent pas toujours le registry Nexus sur l’hôte (`host.docker.internal:8083`). Utilisez l’une des options suivantes.

### Option A : Charger l’image dans Kind (recommandé en local)

Aucun pull depuis le registry : l’image est chargée dans le cluster une fois.

1. **Construire et taguer l’image** (ou la récupérer depuis Nexus) :
   ```bash
   # Depuis le repo library-management, après un build Jenkins ou local :
   docker build -t host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest .
   # Si l’image est déjà dans Nexus, la tirer d’abord :
   # docker pull host.docker.internal:8083/simdev/library-management:0.0.1-SNAPSHOT
   ```

2. **Charger l’image dans le cluster Kind** (remplacez `dev` par le nom de votre cluster) :
   ```bash
   kind load docker-image host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest --name dev
   ```

3. **Forcer l’utilisation de l’image locale** : dans l’app ArgoCD, ajouter le paramètre Helm suivant (ou éditer l’app après création) :
   - `image.pullPolicy` = `IfNotPresent`  
   Le chart utilise déjà `IfNotPresent` par défaut, donc une fois l’image chargée, le pod utilisera la version locale sans refaire de pull.

4. Relancer une **Sync** de l’application dans l’UI ArgoCD.

### Option B : Tester avec le gateway Docker (172.17.0.1)

Dans l’app ArgoCD (Edit → Params), remplacer `image.repository` par :
`172.17.0.1:8083/repository/docker-hosted/simdev/library-management`  
au lieu de `host.docker.internal:8083/repository/docker-hosted/simdev/library-management`.  
Sur certaines machines, le nœud Kind atteint mieux le host via cette IP.

### Vérifier Nexus et l’image

- Nexus doit tourner et exposer le port 8083 : `docker ps | grep nexus`
- Depuis l’hôte : `curl -s http://localhost:8083/v2/_catalog` ou l’URL du repository Docker Nexus
- L’image doit exister (poussée par le pipeline Jenkins ou par `docker push ...`)

---

## Vérification

- L’app doit apparaître dans la liste des applications.
- Après la première sync, les ressources (Deployment, Service, etc.) sont créées dans le namespace `default`.
- URL de l’app (si Traefik + Kind sont configurés) : **https://library-management.localhost**
