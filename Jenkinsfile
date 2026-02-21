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

        FAIL_ON_SONAR      = 'false'
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
                    PROJECT_VERSION = sh(script: './mvnw help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                    echo "Maven version detected: ${PROJECT_VERSION}"
                }
            }
        }

        stage('🧪 Tests') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw clean test -DskipITs'
                    }
                    post {
                        always {
                            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }

                stage('Integration Tests') {
                    steps {
                        sh './mvnw verify -DskipUnitTests'
                    }
                    post {
                        always {
                            junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
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
                    sh """
                        ./mvnw sonar:sonar \
                            -Dsonar.host.url=${SONAR_URL} \
                            -Dsonar.token=${TOKEN} \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.projectVersion=${PROJECT_VERSION} \
                            -Dsonar.qualitygate.wait=${FAIL_ON_SONAR} \
                            -DskipTests
                    """
                }
            }
        }

        stage('🐳 Build & Tag Docker Image') {
            when { expression { shouldBuildAndPush() } }
            steps {
                script {
                    def commitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

                    env.IMAGE_TAGS = [
                        BUILD_NUMBER,
                        commitShort,
                        PROJECT_VERSION
                    ]

                    def fullImages = env.IMAGE_TAGS.collect { tag ->
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

        stage('🚀 GitOps – Trigger ArgoCD') {
            when {
                allOf {
                    expression { ARGOCD_ENABLED == 'true' }
                    expression { shouldBuildAndPush() }
                }
            }
            steps {
                withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                    script {
                        def host = ARGOCD_SERVER.replaceAll('^https?://', '')

                        sh """
                            argocd login ${host} \
                                --username admin \
                                --password '${ARGOCD_PASS}' \
                                --plaintext --insecure

                            argocd app sync ${ARGOCD_APP} --force || true
                        """
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
            junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml, **/failsafe-reports/*.xml'
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