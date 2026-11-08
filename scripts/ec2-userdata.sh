#!/usr/bin/env bash
# ==========================================================================
# cloud-init (user-data) de la instancia EC2.
#
# Prepara el host para que el despliegue vía SSM Run Command solo tenga que
# hacer:  docker pull && docker rm -f && docker run
#
# Amazon Linux 2023 ya trae el SSM Agent instalado; aquí solo instalamos Docker.
# ==========================================================================
set -euo pipefail

echo "== Instalando Docker =="
dnf install -y docker
systemctl enable docker
systemctl start docker

echo "== Configurando rotación de logs de Docker =="
# Sin esto, el json-file de Docker grows sin límite y termina llenando el disco.
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
JSON
systemctl restart docker

echo "== Creando el directorio de trabajo =="
mkdir -p /opt/tecnicaltest
chown ec2-user:ec2-user /opt/tecnicaltest

echo "== Instancia lista =="
docker --version
