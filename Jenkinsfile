// Pipeline declarativo del Entregable 4 - Programacion Avanzada (UM, 2026)
// Catalogo de Videojuegos: build -> pruebas unitarias -> empaquetado ->
// pruebas de integracion -> imagen Docker -> despliegue en produccion simulada.
// El pipeline vive versionado junto al codigo (pipeline as code).

pipeline {

    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    triggers {
        // Dispara el pipeline cuando GitHub notifica un push (webhook).
        githubPush()
        // Red de seguridad: si no hay webhook (Jenkins local sin IP publica),
        // Jenkins revisa el repositorio cada 2 minutos.
        pollSCM('H/2 * * * *')
    }

    environment {
        IMAGEN          = 'catalogo-videojuegos'
        CONTENEDOR      = 'catalogo-prod'
        RED             = 'catalogo-red'
        VOLUMEN_DATOS   = 'catalogo-datos-prod'
        PUERTO_PROD     = '8091'
        PUERTO_INTERNO  = '8090'
        // Repositorio local de Maven dentro del workspace de Jenkins
        MAVEN_OPTS      = '-Dmaven.repo.local=/var/jenkins_home/.m2/repository'
    }

    stages {

        stage('Checkout') {
            steps {
                script {
                    env.COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.AUTOR  = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                    env.MENSAJE = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
                }
                echo "Commit ${env.COMMIT} de ${env.AUTOR}: ${env.MENSAJE}"
            }
        }

        stage('Compilar') {
            steps {
                // Se usa 'install' y no 'package': las etapas siguientes invocan los
                // plugins de prueba por separado, y para eso cada modulo necesita
                // encontrar en el repositorio local el jar de los modulos de los que
                // depende (modulo-clasificacion depende de modulo-catalogo).
                sh './mvnw -B -ntp -DskipTests clean install'
            }
        }

        stage('Pruebas unitarias') {
            steps {
                // Ejecuta surefire: *Test.java en los tres modulos
                sh './mvnw -B -ntp surefire:test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Pruebas de integracion') {
            steps {
                // Ejecuta failsafe: *IT.java (levantan la aplicacion completa)
                sh './mvnw -B -ntp failsafe:integration-test failsafe:verify'
            }
            post {
                always {
                    junit testResults: '**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Archivar artefacto') {
            steps {
                archiveArtifacts artifacts: 'app-web/target/app-web.jar', fingerprint: true
            }
        }

        stage('Construir imagen Docker') {
            steps {
                sh '''
                    docker build \
                        --build-arg JAR_FILE=app-web/target/app-web.jar \
                        -t ${IMAGEN}:${BUILD_NUMBER} \
                        -t ${IMAGEN}:latest .
                '''
            }
        }

        stage('Desplegar a produccion') {
            steps {
                script {
                    // Se guarda la imagen que esta corriendo para poder volver atras
                    env.IMAGEN_ANTERIOR = sh(
                            script: "docker inspect -f '{{.Config.Image}}' ${CONTENEDOR} 2>/dev/null || true",
                            returnStdout: true).trim()
                    echo "Version actualmente desplegada: ${env.IMAGEN_ANTERIOR ?: 'ninguna (primer despliegue)'}"
                }
                sh '''
                    docker network inspect ${RED} >/dev/null 2>&1 || docker network create ${RED}
                    docker rm -f ${CONTENEDOR} >/dev/null 2>&1 || true
                    docker run -d \
                        --name ${CONTENEDOR} \
                        --network ${RED} \
                        -p ${PUERTO_PROD}:${PUERTO_INTERNO} \
                        -v ${VOLUMEN_DATOS}:/app/datos \
                        -e APP_AMBIENTE=PRODUCCION \
                        --restart unless-stopped \
                        ${IMAGEN}:${BUILD_NUMBER}
                '''
            }
        }

        stage('Smoke test de produccion') {
            steps {
                // Jenkins y la aplicacion comparten la red catalogo-red, por eso
                // se puede llamar al contenedor por su nombre.
                sh '''
                    echo "Esperando a que la aplicacion responda..."
                    for i in $(seq 1 30); do
                        if curl -fs http://${CONTENEDOR}:${PUERTO_INTERNO}/actuator/health | grep -q UP; then
                            echo "La aplicacion esta arriba (intento $i)"
                            curl -fs http://${CONTENEDOR}:${PUERTO_INTERNO}/api/juegos > /dev/null
                            echo "La API responde correctamente"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "ERROR: la aplicacion no respondio a tiempo"
                    docker logs --tail 50 ${CONTENEDOR} || true
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo """
            ====================================================================
             DESPLIEGUE EXITOSO  -  build #${env.BUILD_NUMBER}  -  commit ${env.COMMIT}
             Produccion: http://localhost:${env.PUERTO_PROD}
            ====================================================================
            """
        }
        failure {
            script {
                echo """
                ====================================================================
                 PIPELINE FALLIDO  -  build #${env.BUILD_NUMBER}  -  commit ${env.COMMIT}
                 Autor del cambio: ${env.AUTOR}
                ====================================================================
                """
                // Si el fallo ocurrio en las etapas previas al despliegue, produccion
                // sigue sirviendo la version anterior: no se hace nada.
                // Si fallo el smoke test, se vuelve a la imagen anterior (rollback).
                def contenedorNuevo = sh(script: "docker ps -q -f name=${CONTENEDOR}", returnStdout: true).trim()
                if (contenedorNuevo && env.IMAGEN_ANTERIOR) {
                    echo "Haciendo rollback a ${env.IMAGEN_ANTERIOR}"
                    sh """
                        docker rm -f ${CONTENEDOR} || true
                        docker run -d --name ${CONTENEDOR} --network ${RED} \
                            -p ${PUERTO_PROD}:${PUERTO_INTERNO} \
                            -v ${VOLUMEN_DATOS}:/app/datos \
                            -e APP_AMBIENTE=PRODUCCION \
                            --restart unless-stopped ${env.IMAGEN_ANTERIOR}
                    """
                } else {
                    echo "No se modifico produccion: el fallo ocurrio antes del despliegue."
                }
            }
        }
        always {
            echo "Resultado del build #${env.BUILD_NUMBER}: ${currentBuild.currentResult}"
        }
    }
}
