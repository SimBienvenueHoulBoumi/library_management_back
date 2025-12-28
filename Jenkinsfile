pipeline {
    agent {
        node {
            label 'jenkins-agent'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        // On désactive le checkout SCM automatique
        skipDefaultCheckout(true)
    }

    environment {
        // --- App ---
        APP_NAME        = "library-management"
        PROJECT_NAME    = "library-management"
        PROJECT_VERSION = ""          // récupérée dynamiquement depuis le pom.xml

        // SCM
        GIT_REPO_URL    = "git@github.com:SimBienvenueHoulBoumi/library_management_back.git"
        GIT_BRANCH      = "main"
        GIT_CRED_ID     = "JENKINS_AGENT"
    }

    stages {

        stage('📥 Checkout') {
            steps {
                deleteDir()
                git branch: "${GIT_BRANCH}",
                    url: "${GIT_REPO_URL}",
                    credentialsId: "${GIT_CRED_ID}"

                script {
                    // Récupère la version Maven déclarée dans le pom.xml
                    def v = sh(
                        script: './mvnw help:evaluate -Dexpression=project.version -q -DforceStdout',
                        returnStdout: true
                    ).trim()

                    env.PROJECT_VERSION = v
                    echo "Version Maven détectée : ${env.PROJECT_VERSION}"
                }
            }
        }

        stage('🧪 Unit Tests & Build') {
            steps {
                // Tests unitaires + build, en sautant les tests d'intégration (Failsafe)
                sh './mvnw clean verify -DskipITs=true -DskipUnitTests=false'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('🔗 Integration Tests (IT)') {
            steps {
                // Tests d'intégration uniquement (Failsafe), on saute les tests unitaires
                sh './mvnw verify -DskipITs=false -DskipUnitTests=true'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }
    }

    post {
        failure {
            echo "[Pipeline] ❌ Build échoué — vérifie les tests unitaires et d'intégration."
        }
        always {
            // Archivage ciblé : jar et rapports de tests
            archiveArtifacts artifacts: 'target/*.jar, target/surefire-reports/**, target/failsafe-reports/**', allowEmptyArchive: true
        }
    }
}


