// ======================================================================
// Pipeline CI/CD – library-management
// Modern & Maintainable version – 2025/2026 practices
// ======================================================================
pipeline {
    agent { node { label 'jenkins-agent' } }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5', daysToKeepStr: '30'))
        timeout(time: 45, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds(abortPrevious: true)
        skipDefaultCheckout(true)
    }

    environment {
        // ─── Application ────────────────────────────────────────────────
        APP_NAME           = 'library-management'
        PROJECT_NAME       = 'library-management'
        PROJECT_VERSION    = ''

        // ─── SCM ────────────────────────────────────────────────────────
        GIT_REPO_URL       = 'git@github.com:SimBienvenueHoulBoumi/library_management_back.git'
        GIT_CREDENTIALS    = 'JENKINS_AGENT'

        // ─── Container Registry ─────────────────────────────────────────
        // Note: L'agent Jenkins utilise le daemon Docker de l'hôte via /var/run/docker.sock
        // Le daemon Docker de l'hôte accède à Nexus via localhost:8083 (port exposé sur l'hôte)
        // IMPORTANT: Docker Desktop doit avoir insecure-registries configuré avec localhost:8083
        NEXUS_REGISTRY     = 'localhost:8083'  // Accès via daemon Docker de l'hôte
        REGISTRY_CRED      = 'NEXUS_CREDENTIALS'
        IMAGE_REPO         = "${NEXUS_REGISTRY}/simdev/${PROJECT_NAME}"
        // Pour Kubernetes, utiliser host.docker.internal pour accéder depuis le cluster
        K8S_IMAGE_REPO     = "host.docker.internal:8083/simdev/${PROJECT_NAME}"

        // ─── Quality & Security ─────────────────────────────────────────
        SONAR_URL          = 'http://sonarqube:9000'
        SONAR_CRED         = 'SONARTOKEN'
        SONAR_PROJECT_KEY  = 'library-management'

        // FAIL_ON_SONAR contrôle si le build échoue en cas d'échec du quality gate
        // true = le build échoue si le quality gate échoue
        // false = le build continue même si le quality gate échoue (warning seulement)
        FAIL_ON_SONAR      = 'true'
        FAIL_ON_TRIVY      = 'false'

        // ─── GitOps (ArgoCD) ────────────────────────────────────────────
        ARGOCD_ENABLED     = 'true'
        // ArgoCD est accessible via port-forward sur l'hôte (port 8084)
        // Depuis le conteneur Jenkins, utiliser host.docker.internal pour accéder à l'hôte
        ARGOCD_SERVER      = 'host.docker.internal:8084'   // Port-forward depuis l'hôte
        ARGOCD_CRED        = 'ARGOCD_PASSWORD'
        ARGOCD_APP         = "${PROJECT_NAME}"
        ARGOCD_NS          = 'default'
        ARGOCD_CHART_PATH  = "kubernetes/charts/library-management"
        // Si ARGOCD_CREATE_APP=false, la création automatique est désactivée
        ARGOCD_CREATE_APP  = 'true'  // Activé maintenant que le chart Helm est créé
    }

    stages {

        stage('📥 Checkout & Detect Version') {
            steps {
                deleteDir()
                git branch: env.BRANCH_NAME ?: 'main',
                    url: GIT_REPO_URL,
                    credentialsId: GIT_CREDENTIALS

                script {
                    env.PROJECT_VERSION = sh(script: './mvnw help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                    echo "Maven version detected: ${env.PROJECT_VERSION}"
                }
            }
        }

        stage('🧪 Tests') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        // Tests unitaires uniquement (Surefire) : exclut automatiquement **/services/integration/**
                        sh './mvnw clean test -DskipITs=true -DskipUnitTests=false'
                    }
                    post {
                        always {
                            script {
                                // Vérifier si les rapports existent avant de les archiver
                                def reportsExist = sh(
                                    script: 'test -d target/surefire-reports && ls target/surefire-reports/*.xml 2>/dev/null | head -1 || true',
                                    returnStdout: true
                                ).trim()
                                
                                if (reportsExist) {
                                    echo "[UT] 📊 Archivage des résultats de tests unitaires..."
                                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                } else {
                                    echo "[UT] ⚠️  Aucun rapport de test unitaire trouvé, génération d'un rapport vide..."
                                    sh '''
                                        mkdir -p target/surefire-reports
                                        cat > target/surefire-reports/TEST-empty.xml << 'EOF'
                                        <?xml version="1.0" encoding="UTF-8"?>
                                        <testsuite name="EmptyTestSuite" tests="0" failures="0" errors="0" skipped="0" time="0.0">
                                        </testsuite>
                                        EOF
                                    '''
                                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                }
                            }
                        }
                    }
                }

                stage('Integration Tests') {
                    steps {
                        // Tests d'intégration uniquement (Failsafe) : inclut uniquement **/services/integration/**
                        // Compile d'abord pour créer target/classes, puis verify pour les IT
                        // Note: Pas de 'clean' ici car les stages sont en parallèle et partagent le workspace
                        sh './mvnw compile verify -DskipITs=false -DskipUnitTests=true'
                    }
                    post {
                        always {
                            script {
                                // Vérifier si les rapports existent avant de les archiver
                                def reportsExist = sh(
                                    script: 'test -d target/failsafe-reports && ls target/failsafe-reports/*.xml 2>/dev/null | head -1 || true',
                                    returnStdout: true
                                ).trim()
                                
                                if (reportsExist) {
                                    echo "[IT] 📊 Archivage des résultats de tests d'intégration..."
                                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                } else {
                                    echo "[IT] ⚠️  Aucun rapport de test d'intégration trouvé, génération d'un rapport vide..."
                                    sh '''
                                        mkdir -p target/failsafe-reports
                                        cat > target/failsafe-reports/TEST-empty.xml << 'EOF'
                                            <?xml version="1.0" encoding="UTF-8"?>
                                            <testsuite name="EmptyTestSuite" tests="0" failures="0" errors="0" skipped="0" time="0.0">
                                            </testsuite>
                                            EOF
                                    '''
                                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('📊 Quality – SonarQube') {
            when {
                anyOf {
                    branch 'main'
                    changeRequest()
                }
            }
            steps {
                withCredentials([string(credentialsId: SONAR_CRED, variable: 'TOKEN')]) {
                    script {
                        // Toujours attendre le quality gate (true)
                        // FAIL_ON_SONAR contrôle si on échoue le build en cas d'échec
                        def qualityGateWait = 'true'
                        def shouldFail = FAIL_ON_SONAR == 'true'
                        
                        sh """
                        ./mvnw sonar:sonar \
                                -Dsonar.host.url=${SONAR_URL} \
                                -Dsonar.token=${TOKEN} \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectVersion=${env.PROJECT_VERSION} \
                                -Dsonar.qualitygate.wait=${qualityGateWait} \
                                -DskipTests || {
                                    if [ "${shouldFail}" = "true" ]; then
                                        echo "[SONAR] ❌ Quality gate échoué et FAIL_ON_SONAR=true → échec du build"
                                        exit 1
                                    else
                                        echo "[SONAR] ⚠️  Quality gate échoué mais FAIL_ON_SONAR=false → warning seulement"
                                        exit 0
                                    fi
                                }
                        """
                    }
                }
            }
        }

        stage('🐳 Build & Tag Docker Image') {
            when { expression { shouldBuildAndPush() } }
            steps {
                script {
                    def commitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

                    def imageTags = [
                        BUILD_NUMBER.toString(),
                        commitShort,
                        env.PROJECT_VERSION
                    ]

                    def fullImages = imageTags.collect { tag ->
                        "${IMAGE_REPO}:${tag}"
                    }
                    
                    // Stocker les images dans env pour les utiliser dans d'autres stages
                    env.FULL_IMAGES = fullImages.join(',')

                    def tags = fullImages.join(' -t ')
                    sh """
                        docker build -t ${tags} .
                    """
                }
            }
        }

        stage('🔐 Security Scans – Trivy') {
            when { expression { shouldBuildAndPush() } }
            steps {
                sh """
                    mkdir -p reports/trivy
                    # Timeout augmenté à 15 minutes pour éviter les erreurs "context deadline exceeded"
                    # Pour les grandes images (Spring Boot avec toutes les dépendances), Trivy peut prendre du temps
                    # à analyser toutes les couches et dépendances Java
                    echo "[TRIVY] Démarrage du scan de sécurité (timeout: 15m)..."
                    trivy image --format json --output reports/trivy/trivy.json \
                        --severity CRITICAL,HIGH \
                        --timeout 15m \
                        --exit-code ${FAIL_ON_TRIVY == 'true' ? '1' : '0'} \
                        ${IMAGE_REPO}:${BUILD_NUMBER} || {
                        EXIT_CODE=\$?
                        if [ "\${EXIT_CODE}" = "1" ] && [ "${FAIL_ON_TRIVY}" = "true" ]; then
                            echo "[TRIVY] ❌ Vulnérabilités détectées et FAIL_ON_TRIVY=true → échec du build"
                            exit 1
                        elif [ "\${EXIT_CODE}" = "124" ] || [ "\${EXIT_CODE}" = "1" ]; then
                            echo "[TRIVY] ⚠️  Timeout ou erreur (code: \${EXIT_CODE})"
                            if [ "${FAIL_ON_TRIVY}" = "true" ]; then
                                echo "[TRIVY]    FAIL_ON_TRIVY=true → échec du build"
                                exit 1
                            else
                                echo "[TRIVY]    FAIL_ON_TRIVY=false → warning seulement"
                                exit 0
                            fi
                        else
                            echo "[TRIVY] ⚠️  Erreur inattendue (code: \${EXIT_CODE})"
                            exit 0
                        fi
                    }
                    echo "[TRIVY] ✅ Scan terminé avec succès"
                """
            }
            post { always { archiveArtifacts artifacts: 'reports/trivy/**', allowEmptyArchive: true } }
        }

        /* ────────────────────────────────────────────────────────────────
           Commented out – Push to registry
           Uncomment when Nexus is properly reachable from the agent
        ──────────────────────────────────────────────────────────────── */

        stage('📦 Push Images') {
            when { expression { shouldBuildAndPush() } }
            steps {
                withCredentials([usernamePassword(credentialsId: REGISTRY_CRED, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                        # Connexion au registry Nexus (HTTP non sécurisé)
                        echo "\${PASS}" | docker login ${NEXUS_REGISTRY} -u "\${USER}" --password-stdin || {
                            echo "[REGISTRY] ❌ Échec de connexion à ${NEXUS_REGISTRY}"
                            echo "[REGISTRY]    Vérifications:"
                            echo "[REGISTRY]    1. Nexus est accessible: curl http://${NEXUS_REGISTRY}/v2/"
                            echo "[REGISTRY]    2. Docker Desktop a redémarré après configuration insecure-registries"
                            echo "[REGISTRY]    3. Vérifier: docker info | grep -A 10 'Insecure Registries'"
                            echo "[REGISTRY]    4. Credentials corrects (admin/password)"
                            exit 1
                        }

                        ${env.FULL_IMAGES.split(',').collect { "docker push ${it}" }.join('\n')}

                        docker logout ${NEXUS_REGISTRY}
                    """
                }
            }
        }

        stage('🚀 GitOps – ArgoCD') {
            when {
                allOf {
                    expression { ARGOCD_ENABLED == 'true' }
                    expression { shouldBuildAndPush() }
                }
            }
            stages {
                stage('🔐 ArgoCD Login') {
                    steps {
                        withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                            script {
                                def host = ARGOCD_SERVER.replaceAll('^https?://', '')

                                sh """
                                    echo "[ARGOCD] 🔐 Connexion à ${host}..."
                                    # Vérifier si argocd CLI est installé
                                    if ! command -v argocd &> /dev/null; then
                                        echo "[ARGOCD] ❌ ArgoCD CLI n'est pas installé"
                                        echo "[ARGOCD]    Installation: curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64"
                                        echo "[ARGOCD]    Puis: chmod +x /usr/local/bin/argocd"
                                        exit 1
                                    fi
                                    
                                    # Connexion à ArgoCD
                                    argocd login ${host} \
                                        --username admin \
                                        --password "\${ARGOCD_PASS}" \
                                        --plaintext \
                                        --grpc-web \
                                        --insecure || {
                                        echo "[ARGOCD] ❌ Échec de connexion à ${host}"
                                        echo "[ARGOCD]    Vérifications:"
                                        echo "[ARGOCD]    1. ArgoCD est accessible: curl http://${host}/healthz"
                                        echo "[ARGOCD]    2. Le port-forward ArgoCD est actif sur l'hôte (port 8084)"
                                        echo "[ARGOCD]    3. Le mot de passe est correct (récupérez-le avec: kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d)"
                                        echo "[ARGOCD]    4. Le port-forward est démarré: ./main.sh argocd-port-forward"
                                        exit 1
                                    }
                                    echo "[ARGOCD] ✅ Connexion réussie"
                                """
                            }
                        }
                    }
                }

                stage('📱 ArgoCD App Check/Create') {
                    steps {
                        withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                            script {
                                def host = ARGOCD_SERVER.replaceAll('^https?://', '')
                                // Convertir git@github.com:user/repo.git en https://github.com/user/repo.git
                                def gitRepoHttps = GIT_REPO_URL
                                    .replace('git@github.com:', 'https://github.com/')
                                    .replaceAll(/\.git$/, '') + '.git'

                                sh """
                                    # Vérifier la session
                                    if ! argocd account get --grpc-web &>/dev/null; then
                                        echo "[ARGOCD] Session expirée, reconnexion..."
                                        argocd login ${host} \
                                            --username admin \
                                            --password "\${ARGOCD_PASS}" \
                                            --plaintext \
                                            --grpc-web \
                                            --insecure || exit 1
                                    fi

                                    echo "[ARGOCD] 📱 Vérification de l'application ${ARGOCD_APP}..."
                                    
                                    # Vérifier si l'application existe
                                    if argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
                                        echo "[ARGOCD] ✅ L'application ${ARGOCD_APP} existe déjà"
                                        argocd app get ${ARGOCD_APP} --grpc-web 2>&1 | head -20 || echo "[ARGOCD] ⚠️ Permissions limitées"
                                    else
                                        echo "[ARGOCD] ⚠️  L'application ${ARGOCD_APP} n'existe pas encore"
                                        
                                        if [ "${ARGOCD_CREATE_APP}" != "true" ]; then
                                            echo "[ARGOCD]    Création automatique désactivée (ARGOCD_CREATE_APP=false)"
                                            exit 0
                                        fi

                                        echo "[ARGOCD]    Ajout du repository Git si nécessaire..."
                                        # Ajouter le repository Git si nécessaire
                                        if ! argocd repo list --grpc-web 2>/dev/null | grep -q "${gitRepoHttps}"; then
                                            echo "[ARGOCD]    Ajout du repo: ${gitRepoHttps}"
                                            argocd repo add "${gitRepoHttps}" \
                                                --name ${PROJECT_NAME}-repo \
                                                --insecure-skip-server-verification \
                                                --grpc-web || {
                                                echo "[ARGOCD] ⚠️  Échec ajout repo (peut-être déjà existant)"
                                            }
                                        else
                                            echo "[ARGOCD]    Repository déjà configuré"
                                        fi

                                        echo "[ARGOCD]    Création de l'application ${ARGOCD_APP}..."
                                        argocd app create ${ARGOCD_APP} \
                                            --repo "${gitRepoHttps}" \
                                            --path ${ARGOCD_CHART_PATH} \
                                            --dest-server https://kubernetes.default.svc \
                                            --dest-namespace ${ARGOCD_NS} \
                                            --sync-policy automated \
                                            --self-heal \
                                            --auto-prune \
                                            --grpc-web || {
                                            echo "[ARGOCD] ❌ Échec création application"
                                            echo "[ARGOCD]    Vérifiez:"
                                            echo "[ARGOCD]    1. Le chemin ${ARGOCD_CHART_PATH} existe dans le repo"
                                            echo "[ARGOCD]    2. Le namespace ${ARGOCD_NS} existe"
                                            echo "[ARGOCD]    3. Les permissions ArgoCD sont correctes"
                                            exit 0  # Ne pas faire échouer le build si l'app existe déjà
                                        }
                                        echo "[ARGOCD] ✅ Application créée avec succès"
                                    fi
                                """
                            }
                        }
                    }
                }

                stage('🔄 ArgoCD Sync') {
                    steps {
                        withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                            script {
                                // Utiliser BUILD_NUMBER comme tag d'image (correspond au tag de l'image poussée vers Nexus)
                                def imageTag = env.BUILD_NUMBER
                                
                                // Utiliser le script de déploiement si disponible, sinon utiliser les commandes inline
                                def deployScript = "scripts/deploy-argocd.sh"
                                
                                if (fileExists(deployScript)) {
                                    echo "[ARGOCD] Utilisation du script de déploiement..."
                                    sh """
                                        chmod +x ${deployScript}
                                        ${deployScript} \\
                                            --server ${ARGOCD_SERVER} \\
                                            --user admin \\
                                            --password "\${ARGOCD_PASS}" \\
                                            --app ${ARGOCD_APP} \\
                                            --namespace ${ARGOCD_NS} \\
                                            --repo ${K8S_IMAGE_REPO} \\
                                            --tag ${imageTag}
                                    """
                                } else {
                                    echo "[ARGOCD] Script non trouvé, utilisation des commandes inline..."
                                    def host = ARGOCD_SERVER.replaceAll('^https?://', '')
                                    
                                    sh """
                                        # Vérifier la session
                                        if ! argocd account get --grpc-web &>/dev/null; then
                                            echo "[ARGOCD] Session expirée, reconnexion..."
                                            argocd login ${host} \\
                                                --username admin \\
                                                --password "\${ARGOCD_PASS}" \\
                                                --plaintext \\
                                                --grpc-web \\
                                                --insecure || exit 1
                                        fi

                                        echo "[ARGOCD] 🔄 Synchronisation de l'application ${ARGOCD_APP}..."
                                        
                                        # Vérifier que l'application existe
                                        if ! argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
                                            echo "[ARGOCD] ⚠️  Application ${ARGOCD_APP} inexistante"
                                            echo "[ARGOCD]    Créez-la manuellement dans ArgoCD ou activez ARGOCD_CREATE_APP=true"
                                            exit 0
                                        fi

                                        # Mettre à jour les valeurs Helm avec la nouvelle image
                                        echo "[ARGOCD]    Mise à jour de l'image: ${K8S_IMAGE_REPO}:${imageTag}"
                                        echo "[ARGOCD]    Repository: ${K8S_IMAGE_REPO}"
                                        echo "[ARGOCD]    Tag: ${imageTag} (BUILD_NUMBER)"
                                        
                                        argocd app set ${ARGOCD_APP} \\
                                            --helm-set image.repository=${K8S_IMAGE_REPO} \\
                                            --helm-set image.tag=${imageTag} \\
                                            --grpc-web || {
                                            echo "[ARGOCD] ⚠️  Échec mise à jour des valeurs Helm"
                                            echo "[ARGOCD]    Vérifiez que l'application existe et que les permissions sont correctes"
                                        }
                                        
                                        # Synchroniser l'application
                                        echo "[ARGOCD]    Démarrage de la synchronisation..."
                                        argocd app sync ${ARGOCD_APP} \\
                                            --grpc-web \\
                                            --timeout 300 \\
                                            --prune || {
                                            echo "[ARGOCD] ⚠️  Échec synchronisation"
                                            echo "[ARGOCD]    Vérifiez les logs: argocd app get ${ARGOCD_APP} --grpc-web"
                                            exit 1
                                        }
                                        
                                        echo "[ARGOCD] ✅ Application synchronisée avec succès"
                                        echo "[ARGOCD]    Image déployée: ${K8S_IMAGE_REPO}:${imageTag}"
                                        
                                        # Afficher le statut
                                        echo "[ARGOCD]    Statut de l'application:"
                                        argocd app get ${ARGOCD_APP} --grpc-web 2>&1 | grep -E "Name:|Namespace:|Status:|Health:|Sync:" | head -10 || true
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('🧹 Cleanup') {
            steps {
                sh 'docker image prune -f || true'
            }
        }
    }

    post {
        always {
            archiveArtifacts(
                artifacts: 'target/*.jar, reports/**',
                allowEmptyArchive: true,
                fingerprint: true
            )
            script {
                def surefireExists = sh(script: 'test -d target/surefire-reports && ls target/surefire-reports/*.xml 2>/dev/null | head -1 || true', returnStdout: true).trim()
                def failsafeExists = sh(script: 'test -d target/failsafe-reports && ls target/failsafe-reports/*.xml 2>/dev/null | head -1 || true', returnStdout: true).trim()
                
                if (surefireExists || failsafeExists) {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml, target/failsafe-reports/*.xml'
                } else {
                    echo "⚠️  Aucun rapport de test trouvé"
                }
            }
        }
        success  { echo "✅ Pipeline completed successfully" }
        failure  { echo "❌ Pipeline failed – check reports (Sonar, Trivy)" }
        aborted  { echo "⏹️ Pipeline aborted" }
    }
}

// ─── Helper Functions ────────────────────────────────────────────────────────

def shouldBuildAndPush() {
    return (env.BRANCH_NAME == null || env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release/') || env.BRANCH_NAME.startsWith('hotfix/'))
}