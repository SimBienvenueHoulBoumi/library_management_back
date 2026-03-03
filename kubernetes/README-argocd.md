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

**Pour une nouvelle création** : ce manifeste fixe le Service en **NodePort 30075**, aligné avec Traefik (`https://library-management.localhost`). Les liens Swagger / API fonctionnent dès la première Sync (pas de Bad Gateway).

- Image et probes : voir les paramètres Helm ci‑dessous. Charger l’image dans Kind avant la Sync : `./main.sh load-app-image` (depuis le projet infra).
- Si le pull échoue (EOF, timeout), voir la section *Contournement : charger l’image dans Kind* plus bas.

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

**Automatisation Jenkins** : le pipeline exécute `scripts/deploy-argocd.sh` après Push et Load Image. Le script crée l’app si elle n’existe pas (même spec : NodePort 30075, probes, etc.) puis met à jour l’image et lance la Sync. Prérequis : **argocd CLI** sur l’agent, credential **ARGOCD_ADMIN_PASSWORD** (Secret text = mot de passe admin ArgoCD).

---

## Obligatoire : charger l’image dans Kind avant la Sync

Avec **imagePullPolicy: Never**, le pod **ne fait jamais de pull** (évite l’erreur EOF). L’image doit déjà être présente dans le cluster. **Automatisé avec Ansible (une seule commande)** :

```bash
# Depuis la racine du projet infra :
./main.sh load-app-image

# Ou depuis la racine des projets (parent de infra) :
./infra/main.sh load-app-image
```

(Si l’image existe en local, elle est chargée dans Kind ; sinon le script indique de lancer un build Jenkins puis de relancer cette commande.)

**Manuel** (équivalent) :

```bash
kind load docker-image host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest --name dev
```

- Si l’image `:latest` n’existe pas encore en local : lancer un build Jenkins (qui pousse et tague `latest`), ou `docker build -t host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest .` puis la commande ci‑dessus.
- Dans l’app ArgoCD : **image.tag** = `latest`, **image.pullPolicy** = `Never`. Puis **Sync**.

---

## « Image not present with pull policy of Never »

L’erreur indique la **référence exacte** demandée par le déploiement (ex. `172.17.0.1:8083/repository/docker-hosted/simdev/library-management:latest`). Avec **Never**, Kind n’utilise que l’image déjà chargée **sous cette même référence**.

**À faire :** charger l’image dans Kind avec **exactement** la même référence que dans l’erreur.

- **Si l’app ArgoCD utilise `host.docker.internal:8083/...`** (comme le manifeste ci‑dessus) :
  ```bash
  kind load docker-image host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest --name dev
  ```

- **Si l’app ArgoCD utilise `172.17.0.1:8083/...`** (Option B du README) : l’image en local est souvent taguée `host.docker.internal:8083/...`. Taguer puis charger :
  ```bash
  docker tag host.docker.internal:8083/repository/docker-hosted/simdev/library-management:latest \
    172.17.0.1:8083/repository/docker-hosted/simdev/library-management:latest
  kind load docker-image 172.17.0.1:8083/repository/docker-hosted/simdev/library-management:latest --name dev
  ```
  Si l’image n’existe pas encore en local : build ou pull sous `host.docker.internal:8083/...`, puis faire le `docker tag` + `kind load` ci‑dessus.

Ensuite : **Sync** (ou redémarrage du déploiement) dans ArgoCD. Pour éviter la confusion, garder **une seule** référence dans l’app (de préférence `host.docker.internal:8083/...`, comme le manifeste et le Jenkinsfile).

---

## Accès à l’API (Swagger, routes, autre app)

**library-management est bien dans Traefik** : la route est définie dans le fichier `traefik/dynamic.yml` de l’infra (router `library-management-router` → service `library-management-service` → `http://host.docker.internal:30075`). Pour le voir dans le dashboard Traefik : **https://traefik.localhost/dashboard/** (ou http://localhost:8081). Onglet **HTTP** → **Routers** : `library-management-router`.

Une fois le pod **Running** et le Service en **NodePort** (chart par défaut), l’API est exposée via Traefik. Vérifier que l’entrée existe dans `/etc/hosts` : `127.0.0.1 library-management.localhost` (ou `./main.sh ensure-hosts` depuis l’infra).

| Usage | URL |
|-------|-----|
| **Swagger UI** | **https://library-management.localhost/swagger-ui.html** |
| **Swagger API JSON** | https://library-management.localhost/v3/api-docs |
| **Health** | https://library-management.localhost/actuator/health |
| **Base API** (pour appels HTTP) | **https://library-management.localhost** |

**Exemples de routes (préfixe `/api`)** : `/api/books`, `/api/members`, `/api/loans`, `/api/users`.  
Authentification : HTTP Basic (voir README du projet pour les identifiants par défaut).

**Depuis une autre application** :
- **Navigateur ou front (même machine)** : `fetch('https://library-management.localhost/api/books')` (gérer CORS si besoin).
- **Pod dans le même cluster Kubernetes** : utiliser l’URL interne du service : `http://library-management.default.svc.cluster.local:8075` (pas de HTTPS, pas de Traefik).

---

## Bad Gateway en accédant à Swagger / library-management.localhost

Traefik envoie le trafic vers **host.docker.internal:30075**. Si tu as un **Bad Gateway**, vérifier dans l’ordre :

**1. Le Service est bien en NodePort avec le port 30075**

```bash
kubectl get svc library-management -n default
```

Tu dois voir **NodePort** et **30075** (ex. `8075:30075/TCP`).  
- Si tu vois un autre port (ex. `8075:31484/TCP`) : Traefik pointe vers 30075, donc 502. **Correction** : dans l’app ArgoCD, ajouter dans les values Helm : `service.type: NodePort` et `service.nodePort: 30075`, puis supprimer le Service et re-Sync pour qu’il soit recréé avec le bon port :
  ```bash
  kubectl delete svc library-management -n default
  ```
  Puis dans ArgoCD : **Sync** (l’app recrée le Service avec `nodePort: 30075`).  
- Si le type est **ClusterIP**, faire une **Sync** en ayant `service.type: NodePort` (et `service.nodePort: 30075`) dans les values de l’app.

**2. L’API répond directement sur l’hôte (sans Traefik)**

```bash
curl -s http://localhost:30075/actuator/health
```

- Si **connection refused** : le cluster Kind n’expose pas le port 30075 sur l’hôte. Il faut avoir créé le cluster avec le **kind-config.yaml** de l’infra (il contient `extraPortMappings` pour 30075). Recréer le cluster avec :  
  `kind create cluster --name dev --config <chemin-vers-kind-config.yaml>`
- Si **curl** renvoie du JSON (ex. `{"status":"UP"}`) : l’API est OK ; le blocage est entre Traefik et l’hôte. Redémarrer Traefik : depuis l’infra `./main.sh restart-traefik`, puis réessayer https://library-management.localhost/swagger-ui.html

**3. Vérifier /etc/hosts**

```bash
grep library-management.localhost /etc/hosts
```

Doit contenir : `127.0.0.1 library-management.localhost`. Sinon : `./main.sh ensure-hosts` (depuis le projet infra).

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
