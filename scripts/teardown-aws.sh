#!/usr/bin/env bash
# ==========================================================================
# Teardown: borra TODOS los recursos de la demo en AWS.
#
#   ./scripts/teardown-aws.sh
#
# ¡OJO! sin este script, la t3.small se queda cobrando (~0,017 USD/hora).
# La imagen de la instancia tiene DeleteOnTermination en el volumen, así que
# al terminar la instancia también se borra su disco.
# ==========================================================================
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
ACCOUNT="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
PREFIX="tecnicaltest"

echo "== Buscando recursos con prefijo '$PREFIX' en $REGION =="

INSTANCE_ID=$(aws ec2 describe-instances --region "$REGION" \
  --filters "Name=tag:Name,Values=${PREFIX}-app" "Name=instance-state-name,Values=running,stopped,pending,stopping" \
  --query 'Reservations[].Instances[].InstanceId' --output text 2>/dev/null || true)

SG_ID=$(aws ec2 describe-security-groups --region "$REGION" \
  --filters "Name=group-name,Values=${PREFIX}-ec2-sg" \
  --query 'SecurityGroups[].GroupId' --output text 2>/dev/null || true)

EIP_ALLOC=$(aws ec2 describe-addresses --region "$REGION" \
  --filters "Name=tag:Name,Values=${PREFIX}-app" \
  --query 'Addresses[].AllocationId' --output text 2>/dev/null || true)

echo "Instancia: ${INSTANCE_ID:-<ninguna>}"
echo "Security group: ${SG_ID:-<ninguno>}"
echo "Elastic IP: ${EIP_ALLOC:-<ninguna>}"

read -r -p "¿Borrar estos recursos? [s/N] " answer
if [[ "${answer,,}" != "s" ]]; then
    echo "Cancelado. No se borra nada."
    exit 0
fi

if [[ -n "${INSTANCE_ID}" && "${INSTANCE_ID}" != "None" ]]; then
    echo "== Terminando la instancia =="
    aws ec2 terminate-instances --region "$REGION" --instance-ids $INSTANCE_ID
    aws ec2 wait instance-terminated --region "$REGION" --instance-ids $INSTANCE_ID
fi

if [[ -n "${EIP_ALLOC}" && "${EIP_ALLOC}" != "None" ]]; then
    echo "== Liberando la Elastic IP =="
    aws ec2 release-address --region "$REGION" --allocation-id $EIP_ALLOC
fi

if [[ -n "${SG_ID}" && "${SG_ID}" != "None" ]]; then
    echo "== Borrando el security group =="
    # Puede fallar si la instancia aún lo referencia: reintentamos.
    for _ in 1 2 3 4 5; do
        if aws ec2 delete-security-group --region "$REGION" --group-id $SG_ID 2>/dev/null; then
            break
        fi
        echo "   el SG sigue en uso, reintentando en 15s..."
        sleep 15
    done
fi

echo "== Borrando el repo ECR =="
aws ecr delete-repository --region "$REGION" --repository-name "$PREFIX" --force 2>/dev/null || true

echo "== Borrando los roles IAM =="
aws iam detach-role-policy --role-name "${PREFIX}-ec2-ecr-role" \
    --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly 2>/dev/null || true
aws iam detach-role-policy --role-name "${PREFIX}-ec2-ecr-role" \
    --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore 2>/dev/null || true
aws iam remove-role-from-instance-profile --instance-profile-name "${PREFIX}-ec2-ecr-role" 2>/dev/null || true
aws iam delete-instance-profile --instance-profile-name "${PREFIX}-ec2-ecr-role" 2>/dev/null || true
aws iam delete-role --role-name "${PREFIX}-ec2-ecr-role" 2>/dev/null || true

aws iam delete-role-policy --role-name "${PREFIX}-gha-deploy" --policy-name deploy 2>/dev/null || true
aws iam delete-role --role-name "${PREFIX}-gha-deploy" 2>/dev/null || true

echo "== Borrando el proveedor OIDC =="
OIDC_ARN=$(aws iam list-open-id-connect-providers --query \
  "OpenIDConnectProviderList[?contains(Arn, 'token.actions.githubusercontent.com')].Arn | [0]" \
  --output text 2>/dev/null || true)
if [[ -n "${OIDC_ARN}" && "${OIDC_ARN}" != "None" ]]; then
    aws iam delete-open-id-connect-provider --open-id-connect-provider-arn "$OIDC_ARN" 2>/dev/null || true
fi

echo ""
echo "== Listo. Se han borrado los recursos de la demo en $REGION (cuenta $ACCOUNT) =="
