// ======================================================================
// Pipeline CI/CD – library-management
// Modern & Maintainable version – 2025/2026 practices
// ======================================================================
// Nécessite un nœud "jenkins-agent" (créé par init-agent.groovy au démarrage de Jenkins).
// Si le job reste en "Still waiting to schedule task" : vérifier que le conteneur jenkins-agent
// tourne et est connecté (Manage Jenkins → Nodes). Depuis l'hôte : ./main.sh restart-jenkins-agent
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

        // ─── Container Registry (Nexus docker-hosted) ────────────────────
        // Nexus URL: https://nexus.localhost/repository/docker-hosted/
        // OBLIGATOIRE: Dans Jenkins → Manage Jenkins → Credentials, créer une credential
        // de type "Username with password" (pas "Secret text") avec ID = NEXUS_CREDENTIALS,
        // username = admin Nexus, password = mot de passe admin (./main.sh nexus-password).
        // host.docker.internal:8083 car le daemon Docker est sur l'hôte (socket monté) ; le hostname "nexus" n'est pas résolu par le daemon.
        NEXUS_REGISTRY_HOST = 'host.docker.internal:8083'
        NEXUS_REGISTRY      = "${NEXUS_REGISTRY_HOST}/repository/docker-hosted"
        REGISTRY_CRED       = 'NEXUS_CREDENTIALS'
        IMAGE_REPO          = "${NEXUS_REGISTRY}/simdev/${PROJECT_NAME}"
        // Même référence que IMAGE_REPO : le chart Helm / ArgoCD doit utiliser ce chemin pour que le pull (ou kind load) corresponde.
        K8S_IMAGE_REPO      = "${IMAGE_REPO}"
        // Nom du cluster Kind pour l'étape "Load image into Kind" (évite le pull depuis Nexus par les nœuds).
        KIND_CLUSTER_NAME   = 'dev'

        // ─── Quality & Security ─────────────────────────────────────────
        SONAR_URL          = 'http://sonarqube:9000'
        SONAR_CRED         = 'SONARTOKEN'
        SONAR_PROJECT_KEY  = 'library-management'

        // FAIL_ON_SONAR contrôle si le build échoue en cas d'échec du quality gate
        // true = le build échoue si le quality gate échoue
        // false = le build continue même si le quality gate échoue (warning seulement)
        FAIL_ON_SONAR      = 'true'
        FAIL_ON_TRIVY      = 'false'
    }

    stages {
        /**
         * Checkout du code source et détection de la version Maven
         * - Nettoie le workspace
         * - Clone le repository Git
         * - Extrait la version depuis pom.xml
         */
        stage('📥 Checkout & Detect Version') {
            steps {
                deleteDir()
                git branch: env.BRANCH_NAME ?: 'main',
                    url: GIT_REPO_URL,
                    credentialsId: GIT_CREDENTIALS

                script {
                    env.PROJECT_VERSION = sh(script: './mvnw help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                }
            }
        }

        /**
         * Nettoyage du projet avant les tests
         * - Supprime le répertoire target pour éviter les conflits entre tests parallèles
         */
        stage('🧹 Clean') {
            steps {
                sh './mvnw clean || rm -rf target || true'
            }
        }

        /**
         * Exécution des tests en parallèle
         * - Unit Tests: Tests unitaires via Surefire (exclut services/integration et services/unit)
         * - Integration Tests: Tests d'intégration via Failsafe (inclut uniquement services/integration et services/unit)
         * Les rapports JUnit sont archivés automatiquement
         */
        stage('🧪 Tests') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw test -DskipITs=true -DskipUnitTests=false'
            }
            post {
                always {
                            script {
                                def reportsExist = sh(
                                    script: 'test -d target/surefire-reports && ls target/surefire-reports/*.xml 2>/dev/null | head -1 || true',
                                    returnStdout: true
                                ).trim()
                                
                                if (reportsExist) {
                                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                } else {
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
                        sh './mvnw compile verify -DskipITs=false -DskipUnitTests=true'
            }
            post {
                always {
                            script {
                                def reportsExist = sh(
                                    script: 'test -d target/failsafe-reports && ls target/failsafe-reports/*.xml 2>/dev/null | head -1 || true',
                                    returnStdout: true
                                ).trim()
                                
                                if (reportsExist) {
                                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
                                } else {
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

        /**
         * Analyse de qualité du code avec SonarQube
         * - Exécuté sur main, release/*, hotfix/* et les pull requests
         * - Attend le quality gate (sonar.qualitygate.wait=true)
         * - FAIL_ON_SONAR contrôle si le build échoue en cas d'échec du quality gate
         */
        stage('📊 Quality – SonarQube') {
            when {
                anyOf {
                    branch 'main'
                    branch pattern: 'release/.*', comparator: 'REGEXP'
                    branch pattern: 'hotfix/.*', comparator: 'REGEXP'
                    changeRequest()
                }
            }
            steps {
                withCredentials([string(credentialsId: SONAR_CRED, variable: 'TOKEN')]) {
                    script {
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
                                        exit 1
                                    else
                                        exit 0
                                    fi
                                }
                        """
                    }
                }
            }
        }

        /**
         * Construction et tag des images Docker
         * - Construit l'image avec plusieurs tags: BUILD_NUMBER, commit SHA, version
         * - Exécuté uniquement sur main, release/* et hotfix/*
         */
        stage('🐳 Build & Tag Docker Image') {
            when { expression { shouldBuildAndPush() } }
            steps {
                script {
                    def commitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

                    def imageTags = [
                        BUILD_NUMBER.toString(),
                        commitShort,
                        'latest'   // pour ArgoCD / déploiement K8s (application utilise tag: latest)
                    ]
                    
                    // Ajouter la version seulement si elle n'est pas vide ou null
                    if (env.PROJECT_VERSION && env.PROJECT_VERSION != 'null' && env.PROJECT_VERSION.trim() != '') {
                        imageTags.add(env.PROJECT_VERSION)
                    }

                    def fullImages = imageTags.collect { tag ->
                        "${IMAGE_REPO}:${tag}"
                    }
                    
                    env.FULL_IMAGES = fullImages.join(',')

                    def tags = fullImages.join(' -t ')
                    sh """
                        export DOCKER_BUILDKIT=0
                        docker build -t ${tags} .
                    """
                }
            }
        }

        /**
         * Scan de sécurité des images Docker avec Trivy
         * - Analyse les vulnérabilités CRITICAL et HIGH
         * - Timeout de 15 minutes pour les grandes images Spring Boot
         * - FAIL_ON_TRIVY contrôle si le build échoue en cas de vulnérabilités
         */
        stage('🔐 Security Scans – Trivy') {
            when { expression { shouldBuildAndPush() } }
            steps {
                sh """
                    mkdir -p reports/trivy
                    trivy image --format json --output reports/trivy/trivy.json \
                        --severity CRITICAL,HIGH \
                        --timeout 15m \
                        --exit-code ${FAIL_ON_TRIVY == 'true' ? '1' : '0'} \
                        ${IMAGE_REPO}:${BUILD_NUMBER} || {
                        EXIT_CODE=\$?
                        if [ "\${EXIT_CODE}" = "1" ] && [ "${FAIL_ON_TRIVY}" = "true" ]; then
                            exit 1
                        elif [ "\${EXIT_CODE}" = "124" ] || [ "\${EXIT_CODE}" = "1" ]; then
                            if [ "${FAIL_ON_TRIVY}" = "true" ]; then
                                exit 1
                            else
                                exit 0
                            fi
                        else
                          exit 0
                        fi
                    }
                """
            }
            post { always { archiveArtifacts artifacts: 'reports/trivy/**', allowEmptyArchive: true } }
        }

        /**
         * Push des images Docker vers Nexus Registry
         * - Connexion au registry HTTP (nexus:8082, même réseau Docker que l'agent)
         * - Push de toutes les images taggées (BUILD_NUMBER, commit SHA, version)
         */
        stage('📦 Push Images') {
            when { expression { shouldBuildAndPush() } }
            steps {
                withCredentials([usernamePassword(credentialsId: REGISTRY_CRED, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh """
                        echo "\${PASS}" | docker login http://${NEXUS_REGISTRY_HOST} -u "\${USER}" --password-stdin || exit 1

                        ${env.FULL_IMAGES.split(',').collect { "docker push ${it}" }.join('\n')}

                        docker logout ${NEXUS_REGISTRY_HOST}
                    """
                }
            }
        }

        /**
         * Charge l'image dans le cluster Kind pour éviter le pull depuis Nexus (réseau Kind ↔ hôte souvent instable).
         * Utilise la même référence d'image que le chart Helm (IMAGE_REPO) pour que le pod utilise l'image locale (imagePullPolicy: IfNotPresent).
         * Prérequis : kind en PATH sur l'agent, cluster "${KIND_CLUSTER_NAME}" existant.
         */
        stage('📤 Load Image into Kind') {
            when { expression { shouldBuildAndPush() } }
            steps {
                script {
                    def tagToLoad = env.PROJECT_VERSION?.trim() ?: env.BUILD_NUMBER
                    def imageRef = "${IMAGE_REPO}:${tagToLoad}"
                    def deployTag = 'latest'
                    sh """
                        if command -v kind >/dev/null 2>&1; then
                            kind load docker-image '${imageRef}' --name ${KIND_CLUSTER_NAME} || true
                            kind load docker-image '${IMAGE_REPO}:${deployTag}' --name ${KIND_CLUSTER_NAME} || true
                        else
                            echo "⚠️ kind non trouvé. Pour éviter ImagePullBackOff, exécuter sur l'hôte :"
                            echo "   kind load docker-image '${IMAGE_REPO}:${deployTag}' --name ${KIND_CLUSTER_NAME}"
                        fi
                    """
                }
            }
        }

        /**
         * Déploiement via ArgoCD (script deploy-argocd.sh).
         * Met à jour l'app library-management (ou la crée si absente) avec les mêmes paramètres que le manifeste README-argocd (NodePort 30075, etc.) puis Sync.
         * Prérequis : argocd CLI sur l'agent ; credential "ARGOCD_ADMIN_PASSWORD" (Secret text = mot de passe admin, ex. ./main.sh argocd-password).
         */
        stage('🚀 Deploy to ArgoCD') {
            when { expression { shouldBuildAndPush() } }
            steps {
                script {
                    def hasArgocd = sh(script: 'command -v argocd', returnStatus: true) == 0
                    if (!hasArgocd) {
                        echo "⚠️ ArgoCD CLI non installé sur l'agent → étape Deploy to ArgoCD ignorée."
                        return
                    }
                    try {
                        withCredentials([string(credentialsId: 'ARGOCD_ADMIN_PASSWORD', variable: 'ARGOCD_PASS')]) {
                            sh """
                                export ARGOCD_PASS
                                export IMAGE_REPO="${IMAGE_REPO}"
                                export IMAGE_TAG=latest
                                export ARGOCD_SERVER=host.docker.internal:8084
                                ./scripts/deploy-argocd.sh
                            """
                        }
                    } catch (Exception e) {
                        echo "⚠️ Deploy to ArgoCD échoué (vérifier credential ARGOCD_ADMIN_PASSWORD et accès à ArgoCD). Build continu."
                    }
                }
            }
        }

        /**
         * Nettoyage des images Docker non utilisées
         */
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