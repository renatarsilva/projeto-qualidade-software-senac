#!/usr/bin/env bash
# Semeia a massa da carga (T-38): 50 solicitantes fictícios e a sala disputada. Usa só a API.
# Uso: APP_ADMIN_EMAIL=... APP_ADMIN_SENHA=... ./perf/semear.sh [url-base]
set -euo pipefail
BASE="${1:-http://localhost:8080}"
SENHA_DOS_USUARIOS="carga-teste-123" # senha fictícia, só para a massa de teste
ADMIN_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$APP_ADMIN_EMAIL\",\"senha\":\"$APP_ADMIN_SENHA\"}" | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
AUTH="Authorization: Bearer $ADMIN_TOKEN"

echo "email,senha" > perf/usuarios.csv
for i in $(seq -w 1 50); do
  EMAIL="carga-0$i@exemplo.com"
  curl -s -o /dev/null -X POST "$BASE/api/usuarios" -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"nome\":\"Usuário de carga $i\",\"email\":\"$EMAIL\",\"senha\":\"$SENHA_DOS_USUARIOS\",\"perfil\":\"SOLICITANTE\"}" || true
  echo "$EMAIL,$SENHA_DOS_USUARIOS" >> perf/usuarios.csv
done

curl -s -o /dev/null -X POST "$BASE/api/salas" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"nome":"sala-199","tipoSala":"Laboratório","capacidade":30,"localizacao":"Bloco C","restrito":false}' || true
SALA_ID=$(curl -s "$BASE/api/salas" -H "$AUTH" | python3 -c 'import sys,json; print([s["id"] for s in json.load(sys.stdin) if s["nome"]=="sala-199"][0])')
echo "sala-199 tem o id $SALA_ID"
echo "$SALA_ID" > perf/sala-id.txt
