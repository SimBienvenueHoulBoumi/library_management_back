#!/bin/bash
# Script de test pour Trivy en local
# Usage: ./test-trivy.sh [IMAGE_TAG]

set -e

IMAGE_TAG="${1:-65}"  # Tag par défaut
IMAGE_NAME="localhost:8083/simdev/library-management:${IMAGE_TAG}"
REPORT_DIR="reports/trivy"
REPORT_JSON="${REPORT_DIR}/trivy-test-${IMAGE_TAG}.json"
REPORT_TABLE="${REPORT_DIR}/trivy-test-${IMAGE_TAG}.txt"

echo "🔍 Test de Trivy sur l'image: ${IMAGE_NAME}"
echo ""

# Créer le répertoire de rapports
mkdir -p "${REPORT_DIR}"

# Vérifier que l'image existe
if ! docker images | grep -q "library-management.*${IMAGE_TAG}"; then
    echo "❌ Image non trouvée: ${IMAGE_NAME}"
    echo "   Images disponibles:"
    docker images | grep "library-management" | head -5
    exit 1
fi

echo "✅ Image trouvée"
echo "📊 Démarrage du scan (timeout: 15 minutes)..."
echo ""

# Exécuter Trivy avec timeout de 15 minutes
# Générer deux rapports : JSON (pour traitement) et Table (pour lecture)
echo "📊 Génération du rapport JSON..."
if command -v gtimeout &> /dev/null; then
    gtimeout 900 trivy image \
        --format json \
        --output "${REPORT_JSON}" \
        --severity CRITICAL,HIGH \
        --timeout 15m \
        "${IMAGE_NAME}" > /dev/null 2>&1
else
    trivy image \
        --format json \
        --output "${REPORT_JSON}" \
        --severity CRITICAL,HIGH \
        --timeout 15m \
        "${IMAGE_NAME}" > /dev/null 2>&1
fi

EXIT_CODE_JSON=$?

# Générer aussi un rapport table (format lisible)
echo "📋 Génération du rapport table (format lisible)..."
trivy image \
    --format table \
    --severity CRITICAL,HIGH \
    --timeout 15m \
    "${IMAGE_NAME}" 2>&1 | tee "${REPORT_TABLE}"

EXIT_CODE_TABLE=$?

# Utiliser le code de sortie le plus défavorable
EXIT_CODE=$((EXIT_CODE_JSON || EXIT_CODE_TABLE))

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Scan terminé avec succès"
    echo ""
    echo "📄 Rapports générés:"
    echo "   - JSON (pour traitement): ${REPORT_JSON}"
    echo "   - Table (lisible): ${REPORT_TABLE}"
    echo ""
    
    # Afficher un résumé si jq est disponible
    if command -v jq &> /dev/null && [ -f "${REPORT_JSON}" ]; then
        echo "📊 Résumé des vulnérabilités:"
        jq -r '.Results[]? | select(.Vulnerabilities != null) | "   \(.Target): \(.Vulnerabilities | length) vulnérabilités"' "${REPORT_JSON}" || true
        echo ""
    fi
    
    # Afficher les premières lignes du rapport table
    echo "📋 Aperçu du rapport (voir ${REPORT_TABLE} pour le rapport complet):"
    head -30 "${REPORT_TABLE}" || true
    echo ""
    echo "💡 Pour visualiser le rapport complet:"
    echo "   cat ${REPORT_TABLE}"
    echo ""
    echo "💡 Pour convertir le JSON en HTML (si vous avez un convertisseur):"
    echo "   # Option 1: Utiliser trivy convert (si disponible)"
    echo "   # Option 2: Utiliser des outils comme DefectDojo, Harbor, ou des scripts Python"
else
    echo "❌ Scan échoué (code: $EXIT_CODE)"
    if [ -f "${REPORT_TABLE}" ]; then
        echo "📄 Rapport partiel disponible: ${REPORT_TABLE}"
    fi
    exit $EXIT_CODE
fi
