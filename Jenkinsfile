// Pipeline CI/CD pour "library-management"
// 1) Checkout + lecture version Maven
// 2) Tests unitaires + build
// 3) Tests d'intégration
// 4) Analyse SonarQube
// 5) Build & tag Docker (BUILD / SHA / VERSION)
// 6) Scans sécurité (Snyk, Trivy)
// 7) Push image vers Nexus (seulement sur main)
// 8) Nettoyage local des images
pipeline {
    agent {
        node {
            label 'jenkins-agent'
        }
    }

    options {
        // Garder uniquement les 10 derniers builds et leurs artefacts
        buildDiscarder(logRotator(
            numToKeepStr: '10',
            artifactNumToKeepStr: '10',
            daysToKeepStr: '30'  // Supprimer les builds de plus de 30 jours même s'il y en a moins de 10
        ))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        skipDefaultCheckout(true)
    }

    environment {
        // --- Identité applicative ---
        APP_NAME        = "library-management"
        PROJECT_NAME    = "library-management"
        PROJECT_VERSION = ""

        // --- SCM (GitHub) ---
        // BRANCH_NAME (multibranch) sinon "main"
        GIT_REPO_URL    = "git@github.com:SimBienvenueHoulBoumi/library_management_back.git"
        GIT_BRANCH      = "${BRANCH_NAME ?: 'main'}"
        GIT_CRED_ID     = "JENKINS_AGENT"

        // --- Docker / Nexus (même machine que Jenkins / ArgoCD) ---
        // Note: Nexus utilise HTTP (pas HTTPS), configurez Docker daemon.json avec insecure-registries
        // Depuis le conteneur jenkins-agent, on peut utiliser soit:
        // - nexus:8082 (nom du service Docker dans docker-compose)
        // - host.docker.internal:8083 (depuis le daemon Docker de l'hôte via socket monté)
        // On essaie d'abord nexus:8082, puis host.docker.internal:8083 en fallback
        NEXUS_REGISTRY  = "nexus:8082"
        NEXUS_REGISTRY_FALLBACK = "host.docker.internal:8083"
        AUTHORITY       = "simdev"
        IMAGE_REPO      = "${NEXUS_REGISTRY}/${AUTHORITY}/${PROJECT_NAME}"

        // Tags "locaux" (sans registry)
        IMAGE_TAG_BUILD   = "${APP_NAME}:${BUILD_NUMBER}"
        IMAGE_TAG_SHA     = ""
        IMAGE_TAG_VERSION = "${APP_NAME}:${PROJECT_VERSION}"

        NEXUS_CREDENTIALS = "NEXUS_CREDENTIALS"

        // --- SonarQube (analyse qualité) ---
        SONAR_SERVER       = "SonarQube"
        SONAR_URL          = "http://sonarqube:9000"
        SONAR_PROJECT_KEY  = "library-management"
        SONAR_PROJECT_NAME = "library-management"
        SONAR_PROJECT_VERSION = ""
        SONAR_SOURCES = "src/"
        SONAR_JAVA_BINARIES = "target/classes"
        SONAR_JUNIT_REPORTS_PATH = "target/surefire-reports/"
        SONAR_COVERAGE_JACOCO_XML_REPORT_PATHS = "target/jacoco/jacoco.xml"
        SONAR_JAVA_CHECKSTYLE_REPORT_PATHS = "target/checkstyle-result.xml"
        SONAR_EXCLUSIONS = "**/target/**,**/test/**,**/*.json,**/*.yml"

        // --- Outils sécurité (Snyk / Trivy) ---
        SNYK_CLI          = "snyk"
        SNYK_ORG          = "967f8e17-af81-450e-98d1-e19b3e27f316"
        SNYK_PROJECT_NAME_CONTAINER = "library-management-container"

        // --- Feature flags de durcissement (ON/OFF) ---
        FAIL_ON_SONAR_QGATE  = "false"
        FAIL_ON_SNYK_VULNS   = "false"
        FAIL_ON_TRIVY_VULNS  = "false"

        // --- ArgoCD (déploiement Kubernetes) ---
        ARGOCD_SERVER        = "argocd-server.argocd.svc.cluster.local:80"
        ARGOCD_APP_NAME      = "${PROJECT_NAME}"
        ARGOCD_CREDENTIALS   = "ARGOCD_PASSWORD"
        ARGOCD_NAMESPACE     = "default"  // Namespace Kubernetes de destination
        ARGOCD_CHART_PATH    = "kubernetes/charts/${PROJECT_NAME}"  // Chemin vers le chart Helm dans le repo
    }

    stages {

        stage('📥 Checkout') {
            // Nettoyage workspace + checkout Git + lecture de la version Maven
            steps {
                deleteDir()
                git branch: "${GIT_BRANCH}",
                    url: "${GIT_REPO_URL}",
                    credentialsId: "${GIT_CRED_ID}"

                script {
                    def v = sh(
                        script: './mvnw help:evaluate -Dexpression=project.version -q -DforceStdout',
                        returnStdout: true
                    ).trim()

                    env.PROJECT_VERSION = v
                    env.SONAR_PROJECT_VERSION = v

                    echo "Version Maven détectée : ${env.PROJECT_VERSION}"
                }
            }
        }

        stage('🧪 Unit Tests & Build') {
            // Tests unitaires + build du jar (sans tests d'intégration)
            steps {
                sh './mvnw clean verify -DskipITs=true -DskipUnitTests=false'
            }
            post {
                always {
                    script {
                        // Vérifier que les rapports existent avant de les archiver
                        def reportsExist = sh(
                            script: 'test -d target/surefire-reports && ls target/surefire-reports/*.xml 2>/dev/null | head -1',
                            returnStatus: true
                        ) == 0
                        
                        if (reportsExist) {
                            echo "[UT] 📊 Archivage des résultats de tests unitaires..."
                            sh 'ls -la target/surefire-reports/*.xml || true'
                            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true, keepLongStdio: true
                        } else {
                            echo "[UT] ⚠️  Aucun rapport de test unitaire trouvé dans target/surefire-reports/"
                        }
                    }
                }
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('🔗 Integration Tests (IT)') {
            // Tests d'intégration (Failsafe), les TU sont déjà exécutés
            steps {
                sh './mvnw verify -DskipITs=false -DskipUnitTests=true'
            }
            post {
                always {
                    script {
                        // Vérifier que les rapports existent avant de les archiver
                        def reportsExist = sh(
                            script: 'test -d target/failsafe-reports && ls target/failsafe-reports/*.xml 2>/dev/null | head -1',
                            returnStatus: true
                        ) == 0
                        
                        if (reportsExist) {
                            echo "[IT] 📊 Archivage des résultats de tests d'intégration..."
                            sh 'ls -la target/failsafe-reports/*.xml || true'
                            junit testResults: 'target/failsafe-reports/TEST-*.xml', allowEmptyResults: true, keepLongStdio: true
                        } else {
                            echo "[IT] ⚠️  Aucun rapport de test d'intégration trouvé dans target/failsafe-reports/"
                        }
                    }
                }
            }
        }

        stage('📊 SonarQube') {
            // Analyse qualité (SonarQube) avec Quality Gate optionnelle
            steps {
                echo '[Étape 1] Vérification DNS SonarQube'
                sh '''
                    echo "[INFO] Test DNS SonarQube avec curl"
                    curl -s -o /dev/null -w "%{http_code}\\n" "$SONAR_URL/api/system/status" || echo "ECHEC"
                '''

                echo '[Étape 2] Analyse SonarQube'
                withCredentials([string(credentialsId: 'SONARTOKEN', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        ./mvnw sonar:sonar \
                          -Dsonar.host.url="$SONAR_URL" \
                          -Dsonar.token="$SONAR_TOKEN" \
                          -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                          -Dsonar.projectName=$SONAR_PROJECT_NAME \
                          -Dsonar.projectVersion=$SONAR_PROJECT_VERSION \
                          -Dsonar.sources=$SONAR_SOURCES \
                          -Dsonar.java.binaries=$SONAR_JAVA_BINARIES \
                          -Dsonar.junit.reportsPath=$SONAR_JUNIT_REPORTS_PATH \
                          -Dsonar.coverage.jacoco.xmlReportPaths=$SONAR_COVERAGE_JACOCO_XML_REPORT_PATHS \
                          -Dsonar.java.checkstyle.reportPaths=$SONAR_JAVA_CHECKSTYLE_REPORT_PATHS \
                          -Dsonar.exclusions=$SONAR_EXCLUSIONS \
                          -Dsonar.qualitygate.wait=$FAIL_ON_SONAR_QGATE \
                          -DskipTests
                    '''
                }
            }
        }

        stage('🐳 Docker Build & Tag') {
            // Build de l'image Docker et tagging (BUILD / SHA / VERSION)
            steps {
                script {
                    def commit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

                    env.IMAGE_TAG_BUILD   = "${APP_NAME}:${BUILD_NUMBER}"
                    env.IMAGE_TAG_SHA     = "${APP_NAME}:${commit}"
                    env.IMAGE_TAG_VERSION = "${APP_NAME}:${env.PROJECT_VERSION}"

                    env.IMAGE_NAME_BUILD   = "${IMAGE_REPO}:${BUILD_NUMBER}"
                    env.IMAGE_NAME_SHA     = "${IMAGE_REPO}:${commit}"
                    env.IMAGE_NAME_VERSION = "${IMAGE_REPO}:${env.PROJECT_VERSION}"

                    sh """
                        docker build \\
                          -t ${IMAGE_NAME_BUILD} \\
                          -t ${IMAGE_NAME_SHA} \\
                          -t ${IMAGE_NAME_VERSION} \\
                          .
                    """
                }
            }
        }

        stage('🔐 Snyk Scan') {
            // Scan de vulnérabilités avec Snyk (container)
            steps {
                withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'SNYK_TOKEN')]) {
                    sh '''
                        set +e
                        mkdir -p reports/snyk

                        export SNYK_TOKEN="$SNYK_TOKEN"

                        IMAGE_TO_SCAN="${IMAGE_NAME_BUILD}"

                        echo "[SNYK] Lancement snyk container test sur ${IMAGE_TO_SCAN}..."
                        ${SNYK_CLI} container test "${IMAGE_TO_SCAN}" --severity-threshold=high --org="$SNYK_ORG" --json > reports/snyk/snyk-report.json
                        SNYK_EXIT=$?

                        echo "[SNYK] Lancement snyk container monitor..."
                        ${SNYK_CLI} container monitor "${IMAGE_TO_SCAN}" --org="$SNYK_ORG" --project-name="$SNYK_PROJECT_NAME_CONTAINER" || true

                        # Génération du rapport HTML si le script existe dans le repo
                        if [ -f scripts/generate_snyk_report.py ]; then
                          echo "[SNYK] Génération rapport HTML..."
                          python3 scripts/generate_snyk_report.py || true
                        else
                          echo "[SNYK] Script scripts/generate_snyk_report.py absent - seul le JSON sera archivé."
                        fi

                        if [ "$FAIL_ON_SNYK_VULNS" = "true" ] && [ "$SNYK_EXIT" -ne 0 ]; then
                          echo "[SNYK] Vulnérabilités détectées et FAIL_ON_SNYK_VULNS=true -> échec pipeline"
                          exit "$SNYK_EXIT"
                        else
                          echo "[SNYK] Exit code = $SNYK_EXIT (FAIL_ON_SNYK_VULNS=$FAIL_ON_SNYK_VULNS)"
                          exit 0
                        fi
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reports/snyk/**', allowEmptyArchive: true
                }
            }
        }

        stage('🔬 Trivy') {
            // Scan de vulnérabilités avec Trivy (container)
            steps {
                sh '''
                    set +e
                    mkdir -p reports/trivy

                    echo "[TRIVY] Scan de l'image ${IMAGE_NAME_BUILD} (CRITICAL,HIGH)..."
                    trivy image --severity CRITICAL,HIGH --format json --exit-code 1 \
                      -o reports/trivy/trivy-report.json ${IMAGE_NAME_BUILD}
                    TRIVY_EXIT=$?

                    # Génération du rapport HTML si le script existe dans le repo
                    if [ -f scripts/generate_trivy_report.py ]; then
                      echo "[TRIVY] Génération rapport HTML..."
                      python3 scripts/generate_trivy_report.py || true
                    else
                      echo "[TRIVY] Script scripts/generate_trivy_report.py absent - seul le JSON sera archivé."
                    fi

                    if [ "$FAIL_ON_TRIVY_VULNS" = "true" ] && [ "$TRIVY_EXIT" -ne 0 ]; then
                      echo "[TRIVY] Vulnérabilités détectées et FAIL_ON_TRIVY_VULNS=true -> échec pipeline"
                      exit "$TRIVY_EXIT"
                    else
                      echo "[TRIVY] Exit code = $TRIVY_EXIT (FAIL_ON_TRIVY_VULNS=$FAIL_ON_TRIVY_VULNS)"
                      exit 0
                    fi
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reports/trivy/**', allowEmptyArchive: true
                }
            }
        }

        stage('📦 Push to Nexus') {
            when {
                // Push de l'image uniquement sur "main" (branches feature = CI only)
                expression { env.BRANCH_NAME == null || env.BRANCH_NAME == 'main' }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${NEXUS_CREDENTIALS}",
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh '''
                        # Nexus utilise HTTP (pas HTTPS), Docker doit être configuré pour accepter ce registry comme insecure
                        # Si vous obtenez l'erreur "server gave HTTP response to HTTPS client", configurez Docker daemon.json
                        echo "[DOCKER] 🔍 Diagnostic Docker..."
                        echo "[DOCKER] OS: $(uname -a)"
                        echo "[DOCKER] Docker version: $(docker --version || echo 'N/A')"
                        echo "[DOCKER] Registry cible: ${NEXUS_REGISTRY}"
                        
                        # Vérifier la configuration Docker
                        echo "[DOCKER] Configuration Docker actuelle:"
                        if docker info 2>/dev/null | grep -i "insecure" || docker info 2>/dev/null | grep -i "registry"; then
                            docker info 2>/dev/null | grep -i "insecure" || docker info 2>/dev/null | grep -i "registry" || echo "  (aucune config insecure-registries trouvée)"
                        else
                            echo "  ⚠️  Aucune configuration 'insecure-registries' détectée"
                        fi
                        
                        # Vérifier si daemon.json existe
                        if [ -f /etc/docker/daemon.json ]; then
                            echo "[DOCKER] Fichier /etc/docker/daemon.json trouvé:"
                            cat /etc/docker/daemon.json | head -20
                        else
                            echo "[DOCKER] ⚠️  /etc/docker/daemon.json n'existe pas"
                        fi
                        
                        echo "[DOCKER] Tentative de connexion à ${NEXUS_REGISTRY}..."
                        
                        # Essayer d'abord avec nexus:8082 (nom du service Docker)
                        # Si ça échoue, essayer avec host.docker.internal:8083 (depuis le daemon Docker de l'hôte)
                        REGISTRY_TO_USE="${NEXUS_REGISTRY}"
                        LOGIN_SUCCESS=false
                        
                        if echo "$PASS" | docker login ${NEXUS_REGISTRY} -u "$USER" --password-stdin 2>&1; then
                            echo "[DOCKER] ✅ Connexion réussie avec ${NEXUS_REGISTRY}"
                            LOGIN_SUCCESS=true
                        else
                            echo "[DOCKER] ⚠️  Échec avec ${NEXUS_REGISTRY}, tentative avec ${NEXUS_REGISTRY_FALLBACK}..."
                            if echo "$PASS" | docker login ${NEXUS_REGISTRY_FALLBACK} -u "$USER" --password-stdin 2>&1; then
                                echo "[DOCKER] ✅ Connexion réussie avec ${NEXUS_REGISTRY_FALLBACK}"
                                REGISTRY_TO_USE="${NEXUS_REGISTRY_FALLBACK}"
                                LOGIN_SUCCESS=true
                            fi
                        fi
                        
                        if [ "$LOGIN_SUCCESS" = false ]; then
                            echo ""
                            echo "═══════════════════════════════════════════════════════════════"
                            echo "[DOCKER] ❌ ÉCHEC DE CONNEXION"
                            echo "═══════════════════════════════════════════════════════════════"
                            echo ""
                            echo "Problème: Docker essaie HTTPS alors que Nexus utilise HTTP"
                            echo "Registries testés: ${NEXUS_REGISTRY} et ${NEXUS_REGISTRY_FALLBACK}"
                            echo ""
                            echo "🔧 SOLUTION: Configurez Docker daemon.json sur l'HÔTE DOCKER (Mac)"
                            echo ""
                            echo "L'agent Jenkins monte /var/run/docker.sock, donc il utilise le daemon Docker de l'hôte."
                            echo "Vous devez configurer Docker Desktop sur votre Mac."
                            echo ""
                            
                            # Détecter l'OS
                            if [[ "$(uname)" == "Darwin" ]]; then
                                echo "📱 Détecté: macOS (Docker Desktop)"
                                echo ""
                                echo "1. Ouvrez Docker Desktop"
                                echo "2. Allez dans Settings (⚙️) > Docker Engine"
                                echo "3. Ajoutez/modifiez la configuration JSON:"
                                echo ""
                                echo '   {'
                                echo '     "insecure-registries": ['
                                echo '       "nexus:8082",'
                                echo '       "host.docker.internal:8083"'
                                echo '     ]'
                                echo '   }'
                                echo ""
                                echo "4. Cliquez sur 'Apply & Restart'"
                                echo "5. Attendez que Docker redémarre complètement (30-60 secondes)"
                                echo "6. Vérifiez: docker info | grep -i insecure"
                            else
                                echo "🐧 Détecté: Linux"
                                echo ""
                                echo "1. Connectez-vous à la machine où Docker tourne (hôte de l'agent)"
                                echo "2. Éditez /etc/docker/daemon.json (ou créez-le):"
                                echo ""
                                echo '   sudo nano /etc/docker/daemon.json'
                                echo ""
                                echo "3. Ajoutez la configuration:"
                                echo ""
                                echo '   {'
                                echo '     "insecure-registries": ['
                                echo '       "nexus:8082",'
                                echo '       "host.docker.internal:8083"'
                                echo '     ]'
                                echo '   }'
                                echo ""
                                echo "4. Redémarrez Docker:"
                                echo ""
                                echo "   sudo systemctl restart docker"
                                echo ""
                                echo "5. Vérifiez que Docker a redémarré:"
                                echo ""
                                echo "   sudo systemctl status docker"
                            fi
                            echo ""
                            echo "═══════════════════════════════════════════════════════════════"
                            exit 1
                        fi

                        # Reconstruire les noms d'images avec le registry qui a fonctionné
                        # Si le registry a changé, on doit retagger les images
                        if [ "${REGISTRY_TO_USE}" != "${NEXUS_REGISTRY}" ]; then
                            echo "[DOCKER] Retagging images avec le nouveau registry ${REGISTRY_TO_USE}..."
                            # Extraire les tags depuis les noms d'images existants
                            TAG_BUILD=$(echo ${IMAGE_NAME_BUILD} | cut -d: -f2)
                            TAG_SHA=$(echo ${IMAGE_NAME_SHA} | cut -d: -f2)
                            TAG_VERSION=$(echo ${IMAGE_NAME_VERSION} | cut -d: -f2)
                            
                            NEW_IMAGE_BUILD="${REGISTRY_TO_USE}/${AUTHORITY}/${PROJECT_NAME}:${TAG_BUILD}"
                            NEW_IMAGE_SHA="${REGISTRY_TO_USE}/${AUTHORITY}/${PROJECT_NAME}:${TAG_SHA}"
                            NEW_IMAGE_VERSION="${REGISTRY_TO_USE}/${AUTHORITY}/${PROJECT_NAME}:${TAG_VERSION}"
                            
                            docker tag ${IMAGE_NAME_BUILD} ${NEW_IMAGE_BUILD}
                            docker tag ${IMAGE_NAME_SHA} ${NEW_IMAGE_SHA}
                            docker tag ${IMAGE_NAME_VERSION} ${NEW_IMAGE_VERSION}
                            
                            IMAGE_NAME_BUILD=${NEW_IMAGE_BUILD}
                            IMAGE_NAME_SHA=${NEW_IMAGE_SHA}
                            IMAGE_NAME_VERSION=${NEW_IMAGE_VERSION}
                        fi
                        
                        echo "[DOCKER] Pushing images vers ${REGISTRY_TO_USE}..."
                        docker push ${IMAGE_NAME_BUILD}
                        docker push ${IMAGE_NAME_SHA}
                        docker push ${IMAGE_NAME_VERSION}

                        docker logout ${REGISTRY_TO_USE}
                    '''
                }
            }
        }

        stage('🚀 Deploy with ArgoCD') {
            when {
                // Déployer uniquement sur "main" après push réussi
                expression { env.BRANCH_NAME == null || env.BRANCH_NAME == 'main' }
            }
            steps {
                script {
                    echo "[ARGOCD] Déploiement de ${ARGOCD_APP_NAME} vers le cluster Kubernetes..."
                    
                    // Vérifier si ArgoCD CLI est disponible
                    def argocdAvailable = sh(
                        script: 'which argocd || command -v argocd',
                        returnStdout: true
                    ).trim()
                    
                    if (!argocdAvailable) {
                        echo "[ARGOCD] ⚠️ ArgoCD CLI non trouvé - installation..."
                        sh '''
                            curl -sSL -o /tmp/argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
                            sudo install -m 555 /tmp/argocd-linux-amd64 /usr/local/bin/argocd
                            rm /tmp/argocd-linux-amd64
                        '''
                    }
                    
                    // Se connecter à ArgoCD et créer/synchroniser l'application
                    withCredentials([string(credentialsId: "${ARGOCD_CREDENTIALS}", variable: 'ARGOCD_PASS')]) {
                        sh """
                            echo "[ARGOCD] Connexion à ${ARGOCD_SERVER}..."
                            
                            # Se connecter à ArgoCD
                            argocd login ${ARGOCD_SERVER} \\
                                --username admin \\
                                --password "\${ARGOCD_PASS}" \\
                                --insecure || {
                                echo "[ARGOCD] ❌ Échec de connexion"
                                exit 1
                            }
                            
                            # Vérifier et configurer le repo Git si nécessaire
                            echo "[ARGOCD] Vérification du repo Git dans ArgoCD..."
                            if ! argocd repo get ${GIT_REPO_URL} &>/dev/null; then
                                echo "[ARGOCD] 📦 Ajout du repo Git à ArgoCD..."
                                # Pour un repo SSH, ArgoCD utilisera les credentials du cluster
                                # Si besoin de credentials spécifiques, utilisez: --ssh-private-key-path
                                argocd repo add ${GIT_REPO_URL} \\
                                    --name ${PROJECT_NAME}-repo \\
                                    --insecure-skip-server-verification || {
                                    echo "[ARGOCD] ⚠️ Échec d'ajout du repo (peut-être déjà présent ou besoin de credentials)"
                                    echo "[ARGOCD] Vérifiez manuellement: argocd repo list"
                                }
                            else
                                echo "[ARGOCD] ✅ Repo Git déjà configuré"
                            fi
                            
                            # Vérifier si l'application existe
                            if argocd app get ${ARGOCD_APP_NAME} &>/dev/null; then
                                echo "[ARGOCD] ✅ Application ${ARGOCD_APP_NAME} existe déjà"
                            else
                                echo "[ARGOCD] 📦 Création de l'application ${ARGOCD_APP_NAME}..."
                                
                                # Créer l'application ArgoCD automatiquement
                                argocd app create ${ARGOCD_APP_NAME} \\
                                    --repo ${GIT_REPO_URL} \\
                                    --path ${ARGOCD_CHART_PATH} \\
                                    --dest-server https://kubernetes.default.svc \\
                                    --dest-namespace ${ARGOCD_NAMESPACE} \\
                                    --sync-policy automated \\
                                    --self-heal \\
                                    --auto-prune || {
                                    echo "[ARGOCD] ❌ Échec de création de l'application"
                                    echo "[ARGOCD] Vérifiez que:"
                                    echo "  1. Le repo Git est accessible depuis ArgoCD"
                                    echo "  2. Le chemin ${ARGOCD_CHART_PATH} existe dans le repo"
                                    echo "  3. Le chart Helm est valide"
                                    exit 1
                                }
                                
                                echo "[ARGOCD] ✅ Application ${ARGOCD_APP_NAME} créée avec succès"
                            fi
                            
                            # Mettre à jour l'image via paramètres Helm
                            echo "[ARGOCD] Mise à jour de l'image vers ${IMAGE_NAME_VERSION}..."
                            argocd app set ${ARGOCD_APP_NAME} \\
                                --helm-set image.repository=${IMAGE_REPO} \\
                                --helm-set image.tag=${PROJECT_VERSION} \\
                                --sync || true
                            
                            # Attendre la synchronisation
                            echo "[ARGOCD] Attente de la synchronisation (timeout 5min)..."
                            argocd app wait ${ARGOCD_APP_NAME} \\
                                --timeout 300 \\
                                --health || {
                                echo "[ARGOCD] ⚠️ Timeout ou problème de santé - vérifiez manuellement"
                            }
                            
                            # Afficher le statut final
                            echo "[ARGOCD] === Statut de l'application ==="
                            argocd app get ${ARGOCD_APP_NAME}
                        """
                    }
                    
                    echo "[ARGOCD] ✅ Déploiement terminé"
                }
            }
            post {
                success {
                    echo "[ARGOCD] ✅ Application ${ARGOCD_APP_NAME} déployée avec succès"
                }
                failure {
                    echo "[ARGOCD] ❌ Échec du déploiement - vérifiez les logs ArgoCD"
                }
            }
        }

        stage('🧹 Cleanup') {
            steps {
                sh '''
                    echo "[CLEANUP] Suppression des images locales construites..."
                    docker rmi ${IMAGE_NAME_BUILD} || true
                    docker rmi ${IMAGE_NAME_SHA} || true
                    docker rmi ${IMAGE_NAME_VERSION} || true

                    # Pas de docker system prune ici: trop agressif sur un agent partagé.
                '''
            }
        }
    }

    post {
        failure {
            echo "[Pipeline] ❌ Build échoué — consulte les logs et rapports (JUnit, Sonar, Snyk, Trivy)."
        }
        always {
            archiveArtifacts artifacts: 'target/*.jar, target/surefire-reports/**, target/failsafe-reports/**, reports/**', allowEmptyArchive: true
        }
    }
}


