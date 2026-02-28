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
        // Le repository Docker dans Nexus est accessible via le chemin /repository/docker-hosted
        // Port mapping: 8083 (hôte) → 8082 (conteneur Nexus) = Registry Docker
        // Port mapping: 8082 (hôte) → 8081 (conteneur Nexus) = UI Nexus
        NEXUS_REGISTRY     = 'localhost:8083/repository/docker-hosted'  // Chemin complet du repository Docker
        REGISTRY_CRED      = 'NEXUS_CREDENTIALS'
        IMAGE_REPO         = "${NEXUS_REGISTRY}/simdev/${PROJECT_NAME}"
        // Pour Kubernetes, utiliser host.docker.internal pour accéder depuis le cluster
        K8S_IMAGE_REPO     = "host.docker.internal:8083/repository/docker-hosted/simdev/${PROJECT_NAME}"

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
        // UI : https://argocd.localhost/applications — Pipeline tente d'abord cette URL, puis fallback host.docker.internal:8084.
        ARGOCD_SERVER      = 'argocd.localhost'
        ARGOCD_CRED        = 'ARGOCD_TOKEN'
        ARGOCD_APP         = "${PROJECT_NAME}"
        ARGOCD_NS          = 'default'
        ARGOCD_CHART_PATH  = "kubernetes/charts/library-management"
        // Si ARGOCD_CREATE_APP=false, la création automatique est désactivée
        ARGOCD_CREATE_APP  = 'true'  // Activé maintenant que le chart Helm est créé
        // Credential Jenkins (Secret file) contenant le kubeconfig pour le port-forward in-agent. Si vide, on utilise /home/jenkins/.kube/config (montage volume).
        KUBECONFIG_CREDENTIALS_ID = ''
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

        // /**
        //  * Nettoyage du projet avant les tests
        //  * - Supprime le répertoire target pour éviter les conflits entre tests parallèles
        //  */
        // stage('🧹 Clean') {
        //     steps {
        //         sh './mvnw clean || rm -rf target || true'
        //     }
        // }

        // /**
        //  * Exécution des tests en parallèle
        //  * - Unit Tests: Tests unitaires via Surefire (exclut services/integration et services/unit)
        //  * - Integration Tests: Tests d'intégration via Failsafe (inclut uniquement services/integration et services/unit)
        //  * Les rapports JUnit sont archivés automatiquement
        //  */
        // stage('🧪 Tests') {
        //     parallel {
        //         stage('Unit Tests') {
        //             steps {
        //                 sh './mvnw test -DskipITs=true -DskipUnitTests=false'
        //     }
        //     post {
        //         always {
        //                     script {
        //                         def reportsExist = sh(
        //                             script: 'test -d target/surefire-reports && ls target/surefire-reports/*.xml 2>/dev/null | head -1 || true',
        //                             returnStdout: true
        //                         ).trim()
                                
        //                         if (reportsExist) {
        //                             junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
        //                         } else {
        //                             sh '''
        //                                 mkdir -p target/surefire-reports
        //                                 cat > target/surefire-reports/TEST-empty.xml << 'EOF'
        //                                 <?xml version="1.0" encoding="UTF-8"?>
        //                                 <testsuite name="EmptyTestSuite" tests="0" failures="0" errors="0" skipped="0" time="0.0">
        //                                 </testsuite>
        //                                 EOF
        //                             '''
        //                             junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
        //                         }
        //                     }
        //                 }
        //             }
        //         }

        //         stage('Integration Tests') {
        //     steps {
        //                 sh './mvnw compile verify -DskipITs=false -DskipUnitTests=true'
        //     }
        //     post {
        //         always {
        //                     script {
        //                         def reportsExist = sh(
        //                             script: 'test -d target/failsafe-reports && ls target/failsafe-reports/*.xml 2>/dev/null | head -1 || true',
        //                             returnStdout: true
        //                         ).trim()
                                
        //                         if (reportsExist) {
        //                             junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
        //                         } else {
        //                             sh '''
        //                                 mkdir -p target/failsafe-reports
        //                                 cat > target/failsafe-reports/TEST-empty.xml << 'EOF'
        //                                 <?xml version="1.0" encoding="UTF-8"?>
        //                                 <testsuite name="EmptyTestSuite" tests="0" failures="0" errors="0" skipped="0" time="0.0">
        //                                 </testsuite>
        //                                 EOF
        //                             '''
        //                             junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true, keepLongStdio: true
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        // /**
        //  * Analyse de qualité du code avec SonarQube
        //  * - Exécuté sur main, release/*, hotfix/* et les pull requests
        //  * - Attend le quality gate (sonar.qualitygate.wait=true)
        //  * - FAIL_ON_SONAR contrôle si le build échoue en cas d'échec du quality gate
        //  */
        // stage('📊 Quality – SonarQube') {
        //     when {
        //         anyOf {
        //             branch 'main'
        //             branch pattern: 'release/.*', comparator: 'REGEXP'
        //             branch pattern: 'hotfix/.*', comparator: 'REGEXP'
        //             changeRequest()
        //         }
        //     }
        //     steps {
        //         withCredentials([string(credentialsId: SONAR_CRED, variable: 'TOKEN')]) {
        //             script {
        //                 def qualityGateWait = 'true'
        //                 def shouldFail = FAIL_ON_SONAR == 'true'
                        
        //                 sh """
        //                 ./mvnw sonar:sonar \
        //                         -Dsonar.host.url=${SONAR_URL} \
        //                         -Dsonar.token=${TOKEN} \
        //                         -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
        //                         -Dsonar.projectVersion=${env.PROJECT_VERSION} \
        //                         -Dsonar.qualitygate.wait=${qualityGateWait} \
        //                         -DskipTests || {
        //                             if [ "${shouldFail}" = "true" ]; then
        //                                 exit 1
        //                             else
        //                                 exit 0
        //                             fi
        //                         }
        //                 """
        //             }
        //         }
        //     }
        // }

        // /**
        //  * Construction et tag des images Docker
        //  * - Construit l'image avec plusieurs tags: BUILD_NUMBER, commit SHA, version
        //  * - Exécuté uniquement sur main, release/* et hotfix/*
        //  */
        // stage('🐳 Build & Tag Docker Image') {
        //     when { expression { shouldBuildAndPush() } }
        //     steps {
        //         script {
        //             def commitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

        //             def imageTags = [
        //                 BUILD_NUMBER.toString(),
        //                 commitShort
        //             ]
                    
        //             // Ajouter la version seulement si elle n'est pas vide ou null
        //             if (env.PROJECT_VERSION && env.PROJECT_VERSION != 'null' && env.PROJECT_VERSION.trim() != '') {
        //                 imageTags.add(env.PROJECT_VERSION)
        //             }

        //             def fullImages = imageTags.collect { tag ->
        //                 "${IMAGE_REPO}:${tag}"
        //             }
                    
        //             env.FULL_IMAGES = fullImages.join(',')

        //             def tags = fullImages.join(' -t ')
        //             sh """
        //                 export DOCKER_BUILDKIT=0
        //                 docker build -t ${tags} .
        //             """
        //         }
        //     }
        // }

        // /**
        //  * Scan de sécurité des images Docker avec Trivy
        //  * - Analyse les vulnérabilités CRITICAL et HIGH
        //  * - Timeout de 15 minutes pour les grandes images Spring Boot
        //  * - FAIL_ON_TRIVY contrôle si le build échoue en cas de vulnérabilités
        //  */
        // stage('🔐 Security Scans – Trivy') {
        //     when { expression { shouldBuildAndPush() } }
        //     steps {
        //         sh """
        //             mkdir -p reports/trivy
        //             trivy image --format json --output reports/trivy/trivy.json \
        //                 --severity CRITICAL,HIGH \
        //                 --timeout 15m \
        //                 --exit-code ${FAIL_ON_TRIVY == 'true' ? '1' : '0'} \
        //                 ${IMAGE_REPO}:${BUILD_NUMBER} || {
        //                 EXIT_CODE=\$?
        //                 if [ "\${EXIT_CODE}" = "1" ] && [ "${FAIL_ON_TRIVY}" = "true" ]; then
        //                     exit 1
        //                 elif [ "\${EXIT_CODE}" = "124" ] || [ "\${EXIT_CODE}" = "1" ]; then
        //                     if [ "${FAIL_ON_TRIVY}" = "true" ]; then
        //                         exit 1
        //                     else
        //                         exit 0
        //                     fi
        //                 else
        //                   exit 0
        //                 fi
        //             }
        //         """
        //     }
        //     post { always { archiveArtifacts artifacts: 'reports/trivy/**', allowEmptyArchive: true } }
        // }

        // /**
        //  * Push des images Docker vers Nexus Registry
        //  * - Connexion au registry HTTP (localhost:8083)
        //  * - Push de toutes les images taggées (BUILD_NUMBER, commit SHA, version)
        //  */
        // stage('📦 Push Images') {
        //     when { expression { shouldBuildAndPush() } }
        //     steps {
        //         withCredentials([usernamePassword(credentialsId: REGISTRY_CRED, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        //             sh """
        //                 echo "\${PASS}" | docker login http://${NEXUS_REGISTRY} -u "\${USER}" --password-stdin || exit 1

        //                 ${env.FULL_IMAGES.split(',').collect { "docker push ${it}" }.join('\n')}

        //                 docker logout ${NEXUS_REGISTRY}
        //             """
        //         }
        //     }
        // }


        /**
         * Déploiement GitOps via ArgoCD
         * - Connexion à ArgoCD via host.docker.internal:8084 (port-forward)
         * - Vérification/création de l'application
         * - Mise à jour des valeurs Helm et synchronisation
         * - Utilise le script scripts/deploy-argocd.sh si disponible
         */
        stage('🚀 GitOps – ArgoCD') {
            when {
                allOf {
                    expression { ARGOCD_ENABLED == 'true' }
                    expression { shouldBuildAndPush() }
                }
            }
            stages {
                /**
                 * Connexion à ArgoCD. URL UI : https://argocd.localhost/applications
                 * Depuis l'agent Docker, argocd.localhost:443 est souvent injoignable ; fallback host.docker.internal:8084.
                 * Prérequis : port-forward sur l'hôte (./main.sh argocd-port-forward 8084) pour le fallback.
                 */
                stage('🔐 ArgoCD Login') {
                    steps {
                        withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_TOKEN')]) {
                            sh """
                                if [ ! -x /usr/local/bin/argocd ]; then
                                    echo "ArgoCD CLI not found at /usr/local/bin/argocd"
                                    exit 1
                                fi
                                ok=0
                                ARGOCD_HOST=''
                                PROTO=''
                                LOGIN_EXTRA=''
                                for try_https in 1 0; do
                                  if [ "\$try_https" = "1" ]; then
                                    ARGOCD_HOST='argocd.localhost:443'
                                    PROTO='https'
                                    LOGIN_EXTRA=''
                                    echo "Tentative https://argocd.localhost..."
                                  else
                                    ARGOCD_HOST='host.docker.internal:8084'
                                    PROTO='http'
                                    LOGIN_EXTRA='--plaintext'
                                    echo "Fallback http://host.docker.internal:8084..."
                                  fi
                                  max=\$([ "\$try_https" = "1" ] && echo 5 || echo 20)
                                  for i in \$(seq 1 \$max); do
                                    if curl -fksS --connect-timeout 5 "\$PROTO://\$ARGOCD_HOST/healthz" >/dev/null 2>&1; then
                                      ok=1
                                      break
                                    fi
                                    sleep 2
                                  done
                                  [ "\$ok" = "1" ] && break
                                done
                                if [ "\$ok" != "1" ]; then
                                    echo "ArgoCD unreachable (argocd.localhost et host.docker.internal:8084)"
                                    echo "Sur l'HÔTE : cd infra && ./main.sh argocd-port-forward 8084"
                                    exit 1
                                fi
                                echo "ArgoCD joignable sur \$PROTO://\$ARGOCD_HOST"
                                sleep 3
                                login_ok=0
                                for attempt in 1 2 3 4 5; do
                                    if /usr/local/bin/argocd login "\$ARGOCD_HOST" \\
                                        --username admin \\
                                        --password "\${ARGOCD_TOKEN}" \\
                                        \$LOGIN_EXTRA \\
                                        --grpc-web \\
                                        --insecure 2>/dev/null; then
                                        login_ok=1
                                        break
                                    fi
                                    echo "Tentative \$attempt/5 échouée, nouvel essai dans 3s..."
                                    sleep 3
                                done
                                if [ "\$login_ok" != "1" ]; then
                                    echo "ArgoCD login échoué après 5 tentatives. Erreur CLI :"
                                    /usr/local/bin/argocd login "\$ARGOCD_HOST" --username admin --password "\${ARGOCD_TOKEN}" \$LOGIN_EXTRA --grpc-web --insecure || true
                                    exit 1
                                fi
                                echo "ArgoCD login OK (\$ARGOCD_HOST)"
                            """
                        }
                    }
                }

                // /**
                // * Vérification et création de l'application ArgoCD
                // * - Vérifie si l'application existe
                // * - Crée l'application si ARGOCD_CREATE_APP=true
                // * - Ajoute le repository Git si nécessaire
                // */
                // stage('📱 ArgoCD App Check/Create') {
                //     steps {
                //         withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                //             script {
                //                 def host = ARGOCD_SERVER.replaceAll('^https?://', '')
                //                 def gitRepoHttps = GIT_REPO_URL
                //                     .replace('git@github.com:', 'https://github.com/')
                //                     .replaceAll(/\.git$/, '') + '.git'

                //                 sh """
                //                     if ! /usr/local/bin/argocd account get --grpc-web &>/dev/null; then
                //                         /usr/local/bin/argocd login ${host} \\
                //                             --username admin \\
                //                             --password "\${ARGOCD_PASS}" \\
                //                             --plaintext \\
                //                             --grpc-web \\
                //                             --insecure || exit 1
                //                     fi

                //                     if /usr/local/bin/argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
                //                         /usr/local/bin/argocd app get ${ARGOCD_APP} --grpc-web 2>&1 | head -20 || true
                //                     else
                //                         if [ "${ARGOCD_CREATE_APP}" != "true" ]; then
                //                             exit 0
                //                         fi

                //                         if ! /usr/local/bin/argocd repo list --grpc-web 2>/dev/null | grep -q "${gitRepoHttps}"; then
                //                             /usr/local/bin/argocd repo add "${gitRepoHttps}" \\
                //                                 --name ${PROJECT_NAME}-repo \\
                //                                 --insecure-skip-server-verification \\
                //                                 --grpc-web || true
                //                         fi

                //                         /usr/local/bin/argocd app create ${ARGOCD_APP} \\
                //                             --repo "${gitRepoHttps}" \\
                //                             --path ${ARGOCD_CHART_PATH} \\
                //                             --dest-server https://kubernetes.default.svc \\
                //                             --dest-namespace ${ARGOCD_NS} \\
                //                             --sync-policy automated \\
                //                             --self-heal \\
                //                             --auto-prune \\
                //                             --grpc-web || exit 0
                //                     fi
                //                 """
                //             }
                //         }
                //     }
                // }

                // /**
                // * Synchronisation de l'application ArgoCD
                // * - Met à jour les valeurs Helm (image.repository et image.tag)
                // * - Utilise BUILD_NUMBER comme tag d'image
                // * - Utilise scripts/deploy-argocd.sh si disponible, sinon commandes inline
                // */
                // stage('🔄 ArgoCD Sync') {
                //   steps {
                //             withCredentials([string(credentialsId: ARGOCD_CRED, variable: 'ARGOCD_PASS')]) {
                //                 script {
                //                     def imageTag = env.BUILD_NUMBER
                //                     def deployScript = "scripts/deploy-argocd.sh"
                                    
                //                     if (fileExists(deployScript)) {
                //                         sh """
                //                             chmod +x ${deployScript}
                //                             ${deployScript} \\
                //                                 --server ${ARGOCD_SERVER} \\
                //                                 --user admin \\
                //                                 --password "\${ARGOCD_PASS}" \\
                //                                 --app ${ARGOCD_APP} \\
                //                                 --namespace ${ARGOCD_NS} \\
                //                                 --repo ${K8S_IMAGE_REPO} \\
                //                                 --tag ${imageTag} \\
                //                                 --chart-path ${ARGOCD_CHART_PATH} \\
                //                                 --git-repo-url ${GIT_REPO_URL} \\
                //                                 --create-app ${ARGOCD_CREATE_APP}
                //                         """
                //                     } else {
                //                         def host = ARGOCD_SERVER.replaceAll('^https?://', '')
                                        
                //                         sh """
                //                             if ! /usr/local/bin/argocd account get --grpc-web &>/dev/null; then
                //                                 /usr/local/bin/argocd login ${host} \\
                //                                     --username admin \\
                //                                     --password "\${ARGOCD_PASS}" \\
                //                                     --plaintext \\
                //                                     --grpc-web \\
                //                                     --insecure || exit 1
                //                             fi

                //                             if ! /usr/local/bin/argocd app list --grpc-web 2>/dev/null | grep -q "^${ARGOCD_APP}\\s"; then
                //                                 exit 0
                //                             fi

                //                             /usr/local/bin/argocd app set ${ARGOCD_APP} \\
                //                                 --helm-set image.repository=${K8S_IMAGE_REPO} \\
                //                                 --helm-set image.tag=${imageTag} \\
                //                                 --grpc-web || true
                                            
                //                             /usr/local/bin/argocd app sync ${ARGOCD_APP} \\
                //                                 --grpc-web \\
                //                                 --timeout 300 \\
                //                                 --prune || exit 1
                                            
                //                             /usr/local/bin/argocd app get ${ARGOCD_APP} --grpc-web 2>&1 | grep -E "Name:|Namespace:|Status:|Health:|Sync:" | head -10 || true
                //                         """
                //                     }
                //                 }
                //             }
                //         }
                // }
            }
        }

        /*
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