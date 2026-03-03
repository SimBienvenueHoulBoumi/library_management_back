#!/bin/bash

# ============================================================================
# Script de déploiement Jenkins → ArgoCD → Kubernetes
# Utilisé par le pipeline Jenkins pour déployer l'application dans le cluster
# ============================================================================

set -e

# Variables (peuvent être surchargées par les arguments ou variables d'environnement)
ARGOCD_SERVER="${ARGOCD_SERVER:-host.docker.internal:8084}"
ARGOCD_USER="${ARGOCD_USER:-admin}"
ARGOCD_PASS="${ARGOCD_PASS:-}"
ARGOCD_APP="${ARGOCD_APP:-library-management}"
ARGOCD_NS="${ARGOCD_NS:-default}"
# Aligné avec le chart / README-argocd : repository docker-hosted complet
IMAGE_REPO="${IMAGE_REPO:-host.docker.internal:8083/repository/docker-hosted/simdev/library-management}"
IMAGE_TAG="${IMAGE_TAG:-}"
GIT_REPO_URL="${GIT_REPO_URL:-https://github.com/SimBienvenueHoulBoumi/library_management_back.git}"
CHART_PATH="${CHART_PATH:-kubernetes/charts/library-management}"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[ARGOCD]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[ARGOCD]${NC} ✅ $1"
}

log_error() {
    echo -e "${RED}[ARGOCD]${NC} ❌ $1"
}

log_warning() {
    echo -e "${YELLOW}[ARGOCD]${NC} ⚠️  $1"
}

# Références aux scripts d'infra
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
INFRA_MAIN_SCRIPT="$PROJECT_ROOT/infra/main.sh"

strip_proto() {
    local url="$1"
    url="${url#http://}"
    url="${url#https://}"
    url="${url%/}"
    echo "$url"
}

detect_argocd_host() {
    if ! command -v curl &> /dev/null; then
        return 1
    fi

    local hosts=()
    local seen=()

    for candidate in "$@"; do
        candidate="$(strip_proto "$candidate")"
        [ -z "$candidate" ] && continue
        if printf '%s\n' "${seen[@]}" | grep -qx "$candidate"; then
            continue
        fi
        seen+=("$candidate")
        hosts+=("$candidate")
    done

    for candidate in "${hosts[@]}"; do
        if curl -s --connect-timeout 2 "http://${candidate}/healthz" >/dev/null 2>&1; then
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

ensure_port_forward() {
    local resolved
    local candidates=("${ARGOCD_SERVER:-}" "localhost:8084" "host.docker.internal:8084")

    if resolved=$(detect_argocd_host "${candidates[@]}"); then
        ARGOCD_SERVER="$resolved"
        return 0
    fi

    if [ -x "$INFRA_MAIN_SCRIPT" ]; then
        log_info "Démarrage du port-forward ArgoCD via $INFRA_MAIN_SCRIPT"
        "$INFRA_MAIN_SCRIPT" argocd-port-forward
    else
        log_warning "Impossible de lancer '$INFRA_MAIN_SCRIPT' (introuvable)"
    fi

    sleep 2

    if resolved=$(detect_argocd_host "localhost:8084" "host.docker.internal:8084"); then
        ARGOCD_SERVER="$resolved"
        return 0
    fi

    log_error "ArgoCD reste inaccessible (port-forward ou port incorrect)"
    log_info "  - Vérifiez que '$INFRA_MAIN_SCRIPT argocd-port-forward' est lancé"
    log_info "  - Vérifiez que le service argocd-server existe: kubectl get svc -n argocd"
    exit 1
}

# Afficher l'aide
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Déploie une application dans Kubernetes via ArgoCD.

Options:
  -s, --server HOST:PORT     Serveur ArgoCD (défaut: host.docker.internal:8084)
  -u, --user USERNAME        Utilisateur ArgoCD (défaut: admin)
  -p, --password PASSWORD    Mot de passe ArgoCD (ou utiliser ARGOCD_PASS env var)
  -a, --app APP_NAME         Nom de l'application ArgoCD (défaut: library-management)
  -n, --namespace NS         Namespace Kubernetes (défaut: default)
  -r, --repo IMAGE_REPO      Repository d'image Docker (défaut: .../repository/docker-hosted/simdev/library-management)
  -t, --tag IMAGE_TAG        Tag de l'image Docker (requis, ex: latest ou BUILD_NUMBER)
  -h, --help                 Afficher cette aide

Variables d'environnement:
  ARGOCD_SERVER, ARGOCD_USER, ARGOCD_PASS, ARGOCD_APP, ARGOCD_NS
  IMAGE_REPO, IMAGE_TAG      Image à déployer
  GIT_REPO_URL, CHART_PATH   Repo et chemin du chart (création automatique de l'app si absente)

Comportement:
  - Si l'application n'existe pas, elle est créée avec le même spec que README-argocd (NodePort 30075, probes, etc.).
  - Les paramètres Helm (image, service.nodePort, pullPolicy, probes) sont alignés sur le chart qui fonctionne.

Exemples:
  $0 --tag latest --password "\$(cat /run/secrets/argocd-admin)"
  ARGOCD_PASS=xxx IMAGE_TAG=latest $0

EOF
}

# Parser les arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--server)
            ARGOCD_SERVER="$2"
            shift 2
            ;;
        -u|--user)
            ARGOCD_USER="$2"
            shift 2
            ;;
        -p|--password)
            ARGOCD_PASS="$2"
            shift 2
            ;;
        -a|--app)
            ARGOCD_APP="$2"
            shift 2
            ;;
        -n|--namespace)
            ARGOCD_NS="$2"
            shift 2
            ;;
        -r|--repo)
            IMAGE_REPO="$2"
            shift 2
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Option inconnue: $1"
            show_help
            exit 1
            ;;
    esac
done

# Vérifier les prérequis
if [ -z "$IMAGE_TAG" ]; then
    log_error "Le tag d'image est requis (--tag ou IMAGE_TAG)"
    exit 1
fi

if [ -z "$ARGOCD_PASS" ]; then
    log_error "Le mot de passe ArgoCD est requis (--password ou ARGOCD_PASS)"
    exit 1
fi

if ! command -v argocd &> /dev/null; then
    log_error "ArgoCD CLI n'est pas installé"
    log_info "Installation:"
    log_info "  curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64"
    log_info "  chmod +x /usr/local/bin/argocd"
    exit 1
fi

# Fonction pour se connecter à ArgoCD
argocd_login() {
    ensure_port_forward
    local host
    host="$(strip_proto "$ARGOCD_SERVER")"
    
    log_info "Connexion à ArgoCD (${host})..."
    
    argocd login "$host" \
        --username "$ARGOCD_USER" \
        --password "$ARGOCD_PASS" \
        --plaintext \
        --grpc-web \
        --insecure || {
        log_error "Échec de connexion à ArgoCD"
        log_info "Vérifications:"
        log_info "  1. ArgoCD est accessible: curl http://${host}/healthz"
        log_info "  2. Le port-forward est actif: ./main.sh argocd-port-forward"
        log_info "  3. Le mot de passe est correct"
        exit 1
    }
    
    log_success "Connexion réussie"
}

# Paramètres Helm alignés avec le manifeste README-argocd (NodePort 30075, probes, pullPolicy)
helm_set_args() {
    echo \
        --helm-set "image.repository=${IMAGE_REPO}" \
        --helm-set "image.tag=${IMAGE_TAG}" \
        --helm-set "image.pullPolicy=Never" \
        --helm-set "service.type=NodePort" \
        --helm-set "service.nodePort=30075" \
        --helm-set "readinessProbe.initialDelaySeconds=45" \
        --helm-set "livenessProbe.initialDelaySeconds=60"
}

# Fonction pour vérifier si l'application existe
argocd_check_app() {
    if argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
        return 0
    else
        return 1
    fi
}

# Créer l'application ArgoCD (même spec que le manifeste README-argocd)
argocd_create_app() {
    log_info "Création de l'application ${ARGOCD_APP} (repo: ${GIT_REPO_URL}, path: ${CHART_PATH})..."
    argocd app create "$ARGOCD_APP" \
        --repo "$GIT_REPO_URL" \
        --path "$CHART_PATH" \
        --dest-server "https://kubernetes.default.svc" \
        --dest-namespace "$ARGOCD_NS" \
        --revision HEAD \
        $(helm_set_args) \
        --self-heal \
        --auto-prune \
        --grpc-web || {
        log_error "Échec de création de l'application"
        exit 1
    }
    log_success "Application ${ARGOCD_APP} créée"
}

# Mettre à jour les paramètres Helm et synchroniser
argocd_deploy() {
    log_info "Déploiement de l'image ${IMAGE_REPO}:${IMAGE_TAG}..."
    log_info "  Repository: ${IMAGE_REPO}"
    log_info "  Tag: ${IMAGE_TAG}"

    argocd app set "$ARGOCD_APP" $(helm_set_args) --grpc-web || {
        log_warning "Échec mise à jour des valeurs Helm (peut-être déjà à jour)"
    }

    log_info "Synchronisation de l'application..."
    argocd app sync "$ARGOCD_APP" \
        --grpc-web \
        --timeout 300 \
        --prune || {
        log_error "Échec de synchronisation"
        log_info "Vérifiez les logs: argocd app get ${ARGOCD_APP} --grpc-web"
        exit 1
    }

    log_success "Application synchronisée avec succès"
    log_info "Image déployée: ${IMAGE_REPO}:${IMAGE_TAG}"

    echo ""
    log_info "Statut de l'application:"
    argocd app get "$ARGOCD_APP" --grpc-web 2>&1 | grep -E "Name:|Namespace:|Status:|Health:|Sync:" | head -10 || true
}

# Main
main() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "🚀 Déploiement via ArgoCD"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    log_info "Configuration:"
    log_info "  Application: ${ARGOCD_APP}"
    log_info "  Namespace: ${ARGOCD_NS}"
    log_info "  Image: ${IMAGE_REPO}:${IMAGE_TAG}"
    log_info "  ArgoCD Server: ${ARGOCD_SERVER}"
    echo ""

    argocd_login

    if ! argocd_check_app; then
        log_warning "L'application ${ARGOCD_APP} n'existe pas, création..."
        argocd_create_app
    else
        log_success "L'application ${ARGOCD_APP} existe"
    fi

    argocd_deploy

    echo ""
    log_success "Déploiement terminé !"
    echo ""
}

# Exécuter
main
