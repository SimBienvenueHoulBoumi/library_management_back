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
IMAGE_REPO="${IMAGE_REPO:-host.docker.internal:8083/simdev/library-management}"
IMAGE_TAG="${IMAGE_TAG:-}"

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
  -r, --repo IMAGE_REPO      Repository d'image Docker
  -t, --tag IMAGE_TAG        Tag de l'image Docker (requis)
  -h, --help                 Afficher cette aide

Variables d'environnement:
  ARGOCD_SERVER              Serveur ArgoCD
  ARGOCD_USER                Utilisateur ArgoCD
  ARGOCD_PASS                Mot de passe ArgoCD
  ARGOCD_APP                 Nom de l'application
  ARGOCD_NS                  Namespace Kubernetes
  IMAGE_REPO                 Repository d'image
  IMAGE_TAG                  Tag de l'image

Exemples:
  # Utilisation basique
  $0 --tag 123 --password "mon-mot-de-passe"

  # Avec toutes les options
  $0 --server host.docker.internal:8084 \\
      --user admin \\
      --password "mon-mot-de-passe" \\
      --app library-management \\
      --namespace default \\
      --repo host.docker.internal:8083/simdev/library-management \\
      --tag 123

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

# Fonction pour vérifier/créer l'application
argocd_check_app() {
    log_info "Vérification de l'application ${ARGOCD_APP}..."
    
    if argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
        log_success "L'application ${ARGOCD_APP} existe"
        argocd app get "$ARGOCD_APP" --grpc-web 2>&1 | head -20 || true
        return 0
    else
        log_warning "L'application ${ARGOCD_APP} n'existe pas"
        return 1
    fi
}

# Fonction pour mettre à jour et synchroniser l'application
argocd_deploy() {
    log_info "Déploiement de l'image ${IMAGE_REPO}:${IMAGE_TAG}..."
    
    # Vérifier que l'application existe
    if ! argocd_check_app; then
        log_error "L'application ${ARGOCD_APP} n'existe pas"
        log_info "Créez-la manuellement dans ArgoCD ou utilisez le pipeline Jenkins avec ARGOCD_CREATE_APP=true"
        exit 1
    fi
    
    # Mettre à jour les valeurs Helm
    log_info "Mise à jour des valeurs Helm..."
    log_info "  Repository: ${IMAGE_REPO}"
    log_info "  Tag: ${IMAGE_TAG}"
    
    argocd app set "$ARGOCD_APP" \
        --helm-set image.repository="${IMAGE_REPO}" \
        --helm-set image.tag="${IMAGE_TAG}" \
        --grpc-web || {
        log_warning "Échec mise à jour des valeurs Helm (peut-être déjà à jour)"
    }
    
    # Synchroniser l'application
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
    
    # Afficher le statut
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
    
    # Se connecter à ArgoCD
    argocd_login
    
    # Déployer
    argocd_deploy
    
    echo ""
    log_success "Déploiement terminé !"
    echo ""
}

# Exécuter
main
