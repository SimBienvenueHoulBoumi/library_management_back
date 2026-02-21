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
        PROJECT_NAME       = 'test-app'
        PROJECT_VERSION    = ''

        // ─── SCM ────────────────────────────────────────────────────────
        GIT_REPO_URL       = 'git@github.com:SimBienvenueHoulBoumi/library_management_back.git'
        GIT_CREDENTIALS    = 'JENKINS_AGENT'

        // ─── Container Registry ─────────────────────────────────────────
        NEXUS_REGISTRY     = 'localhost:8083'  // ← À sécuriser / passer en HTTPS si possible
        REGISTRY_CRED      = 'NEXUS_CREDENTIALS'
        IMAGE_REPO         = "${NEXUS_REGISTRY}/simdev/${PROJECT_NAME}"

        // ─── Quality & Security ─────────────────────────────────────────
        SONAR_URL          = 'http://sonarqube:9000'
        SONAR_CRED         = 'SONARTOKEN'
        SONAR_PROJECT_KEY  = 'library-management'

        // FAIL_ON_SONAR contrôle si le build échoue en cas d'échec du quality gate
        // true = le build échoue si le quality gate échoue
        // false = le build continue même si le quality gate échoue (warning seulement)
        FAIL_ON_SONAR      = 'true'
        FAIL_ON_SNYK       = 'false'
        FAIL_ON_TRIVY      = 'false'

        // ─── GitOps (ArgoCD) ────────────────────────────────────────────
        ARGOCD_ENABLED     = 'true'
        ARGOCD_SERVER      = 'argocd-proxy:8084'   // ou ingress avec TLS
        ARGOCD_CRED        = 'ARGOCD_PASSWORD'
        ARGOCD_APP         = "${PROJECT_NAME}"
        ARGOCD_NS          = 'default'
        ARGOCD_CHART_PATH  = "kubernetes/charts/${PROJECT_NAME}"
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
                sh './mvnw verify -DskipITs=false -DskipUnitTests=true'
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

        stage('🔐 Security Scans') {
            when { expression { shouldBuildAndPush() } }
            parallel {
                stage('Snyk') {
            steps {
                        withCredentials([string(credentialsId: 'SNYK_TOKEN', variable: 'TOKEN')]) {
                            sh """
                        mkdir -p reports/snyk
                                snyk container test ${IMAGE_REPO}:${BUILD_NUMBER} \
                                    --json-file-output=reports/snyk/snyk.json \
                                    --severity-threshold=high || echo "Snyk issues found (fail=${FAIL_ON_SNYK})"
                                snyk container monitor ${IMAGE_REPO}:${BUILD_NUMBER} || true
                            """
                        }
                    }
                    post { always { archiveArtifacts artifacts: 'reports/snyk/**', allowEmptyArchive: true } }
                }

                stage('Trivy') {
                    steps {
                        sh """
                            mkdir -p reports/trivy
                            trivy image --format json --output reports/trivy/trivy.json \
                                --severity CRITICAL,HIGH \
                                --exit-code ${FAIL_ON_TRIVY == 'true' ? '1' : '0'} \
                                ${IMAGE_REPO}:${BUILD_NUMBER}
                        """
                    }
                    post { always { archiveArtifacts artifacts: 'reports/trivy/**', allowEmptyArchive: true } }
                }
            }
        }

        stage('📦 Push Images') {
            when { expression { shouldBuildAndPush() } }
            steps {
                withCredentials([usernamePassword(credentialsId: REGISTRY_CRED, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                        echo "\${PASS}" | docker login ${NEXUS_REGISTRY} -u "\${USER}" --password-stdin || {
                            echo "Registry login failed → check insecure-registries on Docker host"
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
                                    echo "[ARGOCD] Connexion à ${host}..."
                                    yes | argocd login ${host} \\
                                        --username admin \\
                                        --password "\${ARGOCD_PASS}" \\
                                        --plaintext \\
                                        --grpc-web \\
                                        --insecure || {
                                        echo "[ARGOCD] ❌ Échec de connexion"
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
                                // Convertir l'URL SSH en HTTPS pour ArgoCD (en Groovy pour éviter les problèmes d'interpolation)
                                def gitRepoHttps = GIT_REPO_URL
                                    .replace('git@github.com:', 'https://github.com/')
                                    .replaceAll(/\.git$/, '') + '.git'

                                sh """
                                    # Vérifier si la session est toujours valide, sinon se reconnecter
                                    if ! argocd account get --grpc-web &>/dev/null; then
                                        echo "[ARGOCD] Session expirée, reconnexion..."
                                        yes | argocd login ${host} \\
                                            --username admin \\
                                            --password "\${ARGOCD_PASS}" \\
                                            --plaintext \\
                                            --grpc-web \\
                                            --insecure || exit 1
                                    fi

                                    echo "[ARGOCD] Vérification de l'application ${ARGOCD_APP}..."
                                    # Utiliser app list pour vérifier l'existence (plus fiable que app get)
                                    if argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}"; then
                                        echo "[ARGOCD] ✅ L'application ${ARGOCD_APP} existe déjà"
                                        # Essayer d'obtenir les détails, mais ne pas échouer si permission refusée
                                        argocd app get ${ARGOCD_APP} --grpc-web 2>&1 || {
                                            echo "[ARGOCD] ⚠️  Application trouvée mais permissions insuffisantes pour les détails"
                                            echo "[ARGOCD]    Vérifiez les permissions RBAC de l'utilisateur admin dans ArgoCD"
                                        }
                                    else
                                        echo "[ARGOCD] ⚠️  L'application ${ARGOCD_APP} n'existe pas encore"
                                        echo "[ARGOCD]    Tentative de création..."
                                        
                                        echo "[ARGOCD] URL Git: ${gitRepoHttps}"
                                        echo "[ARGOCD] Chart path: ${ARGOCD_CHART_PATH}"
                                        
                                        # Vérifier si le repo existe dans ArgoCD
                                        if ! argocd repo get "${gitRepoHttps}" --grpc-web &>/dev/null; then
                                            echo "[ARGOCD] Ajout du repository Git..."
                                            argocd repo add "${gitRepoHttps}" \\
                                                --name ${PROJECT_NAME}-repo \\
                                                --insecure-skip-server-verification --grpc-web || {
                                                echo "[ARGOCD] ⚠️  Échec de l'ajout du repository (peut-être déjà existant)"
                                            }
                                        fi
                                        
                                        # Créer l'application
                                        argocd app create ${ARGOCD_APP} \\
                                            --repo "${gitRepoHttps}" \\
                                            --path ${ARGOCD_CHART_PATH} \\
                                            --dest-server https://kubernetes.default.svc \\
                                            --dest-namespace ${ARGOCD_NS} \\
                                            --sync-policy automated \\
                                            --self-heal \\
                                            --auto-prune \\
                                            --grpc-web || {
                                            echo "[ARGOCD] ⚠️  Échec de la création de l'application"
                                            echo "[ARGOCD]    Vérifiez que le repository et le chart path existent"
                                            exit 0
                                        }
                                        echo "[ARGOCD] ✅ Application ${ARGOCD_APP} créée avec succès"
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
                                def host = ARGOCD_SERVER.replaceAll('^https?://', '')

                                sh """
                                    # Vérifier si la session est toujours valide, sinon se reconnecter
                                    if ! argocd account get --grpc-web &>/dev/null; then
                                        echo "[ARGOCD] Session expirée, reconnexion..."
                                        yes | argocd login ${host} \\
                                            --username admin \\
                                            --password "\${ARGOCD_PASS}" \\
                                            --plaintext \\
                                            --grpc-web \\
                                            --insecure || exit 1
                                    fi

                                    echo "[ARGOCD] Synchronisation de l'application ${ARGOCD_APP}..."
                                    # Utiliser app list pour vérifier l'existence (plus fiable que app get)
                                    if argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}"; then
                                        echo "[ARGOCD] Tentative de synchronisation..."
                                        argocd app sync ${ARGOCD_APP} \\
                                            --grpc-web \\
                                            --force || {
                                            echo "[ARGOCD] ⚠️  Échec de la synchronisation"
                                            echo "[ARGOCD]    Vérifiez les permissions RBAC de l'utilisateur admin dans ArgoCD"
                                            exit 1
                                        }
                                        echo "[ARGOCD] ✅ Application synchronisée avec succès"
                                    else
                                        echo "[ARGOCD] ⚠️  Impossible de synchroniser : application inexistante"
                                        echo "[ARGOCD]    Applications disponibles:"
                                        argocd app list --grpc-web 2>&1 | head -10 || echo "   (impossible de lister les applications)"
                                        exit 0
                                    fi
                                """
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
                // Archiver les rapports JUnit s'ils existent
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
        failure  { echo "❌ Pipeline failed – check reports (Sonar, Snyk, Trivy)" }
        aborted  { echo "⏹️ Pipeline aborted" }
    }
}

// ─── Helper Functions ────────────────────────────────────────────────────────

def shouldBuildAndPush() {
    return (env.BRANCH_NAME == null || env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release/') || env.BRANCH_NAME.startsWith('hotfix/'))
}