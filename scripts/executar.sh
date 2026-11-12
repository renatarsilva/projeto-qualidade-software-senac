#!/usr/bin/env bash
# Sobe a aplicação com um comando (macOS e Linux). Para quem avalia o projeto.
#
#   ./scripts/executar.sh
#
# O que faz: acha um JDK 21, confere se o Docker está ligado, define as variáveis que faltarem com valores de
# DEMONSTRAÇÃO (só para uso local) e roda `./mvnw spring-boot:run`. Nada é gravado em arquivo.
# Se você já definiu APP_JWT_CHAVE, APP_ADMIN_EMAIL, APP_ADMIN_SENHA ou JAVA_HOME, os seus valores são mantidos.

set -euo pipefail
cd "$(dirname "$0")/.."

# 1. Java 21 (o projeto exige exatamente esta versão)
versao_do_java() { "$1/bin/java" -version 2>&1 | head -1 | grep -o '"[0-9]*' | tr -d '"'; }

if [ -z "${JAVA_HOME:-}" ] || [ "$(versao_do_java "$JAVA_HOME" 2>/dev/null || echo 0)" != "21" ]; then
  if [ -x /usr/libexec/java_home ] && JAVA_21="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_21"
  else
    echo "ERRO: não achei um JDK 21." >&2
    echo "Instale o Java 21 e defina JAVA_HOME, por exemplo: export JAVA_HOME=/caminho/do/jdk-21" >&2
    exit 1
  fi
fi
echo "Java: $JAVA_HOME"

# 2. Docker (o PostgreSQL do projeto sobe em contêiner)
if ! docker info >/dev/null 2>&1; then
  echo "ERRO: o Docker não está ligado. Abra o Docker Desktop, espere ele iniciar e rode de novo." >&2
  exit 1
fi

# 3. Variáveis (valores de demonstração; use os seus se já estiverem definidos)
export APP_JWT_CHAVE="${APP_JWT_CHAVE:-chave-de-demonstracao-local-com-mais-de-32-caracteres}"
export APP_ADMIN_EMAIL="${APP_ADMIN_EMAIL:-admin@exemplo.com}"
export APP_ADMIN_SENHA="${APP_ADMIN_SENHA:-senha-do-admin-123}"

echo ""
echo "Quando aparecer 'Started OrganizacaoRecursosApplication', abra: http://localhost:8080"
echo "Entre com: ${APP_ADMIN_EMAIL}  /  ${APP_ADMIN_SENHA}"
echo "Para parar: Ctrl+C"
echo ""

exec ./mvnw spring-boot:run
