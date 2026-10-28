#!/usr/bin/env bash
# Spec validation: confere se uma spec está completa e pronta para virar código.
# Etapa 3 do fluxo descrito em docs/processo/ai-workflow.md.
#
# Uso:
#   ./scripts/validar-spec.sh docs/specs/T-23-criar-reserva.md
#   ./scripts/validar-spec.sh docs/specs/*.md        (o _TEMPLATE.md é ignorado)
#
# Sai com código 0 se todas as specs passarem e 1 se alguma falhar,
# para poder rodar também no CI.

set -u

falhas_total=0

# Remove comentários HTML (<!-- ... -->), que não fazem parte do conteúdo.
sem_comentarios() {
  awk '
    /<!--/ { dentro = 1 }
    !dentro { print }
    /-->/  { dentro = 0 }
  ' "$1"
}

# Imprime o conteúdo da seção "## N." até a próxima seção "## ".
secao() {
  awk -v n="$2" '
    $0 ~ "^## " n "\\. " { dentro = 1; next }
    /^## / { dentro = 0 }
    dentro { print }
  ' "$1"
}

# Lê o valor de um campo da tabela de cabeçalho: | Campo | Valor |
campo() {
  grep -m1 -E "^\| $2 \|" "$1" | awk -F'|' '{ gsub(/^ +| +$/, "", $3); print $3 }'
}

validar() {
  local spec="$1"
  local tmp erros=0
  tmp="$(mktemp)"
  sem_comentarios "$spec" > "$tmp"

  falha() { echo "  ✗ $1"; erros=$((erros + 1)); }
  ok()    { echo "  ✓ $1"; }

  echo ""
  echo "Validando $spec"

  # 1. Campos do cabeçalho
  local c valor
  for c in "Task" "Autor\(a\)" "Revisor\(a\)" "Status" "Regra crítica" "Requisitos" "Característica ISO 25010" "Criada em"; do
    valor="$(campo "$tmp" "$c")"
    if [ -z "$valor" ] || echo "$valor" | grep -qE "_preencher_|T-00|AAAA-MM-DD|RF00"; then
      falha "campo '${c//\\/}' vazio ou com valor do template"
    fi
  done

  # 2. Autor diferente do revisor
  local autor revisor
  autor="$(campo "$tmp" "Autor\(a\)" | tr '[:upper:]' '[:lower:]')"
  revisor="$(campo "$tmp" "Revisor\(a\)" | tr '[:upper:]' '[:lower:]')"
  if [ -n "$autor" ] && [ "$autor" = "$revisor" ]; then
    falha "autor e revisor são a mesma pessoa ($autor)"
  else
    ok "autor e revisor são pessoas diferentes"
  fi

  # 3. Status: a validação só acontece depois da aprovação do revisor
  local status
  status="$(campo "$tmp" "Status")"
  case "$status" in
    Aprovada|Validada|Liberada) ok "status '$status'" ;;
    *) falha "status '$status': a spec precisa estar Aprovada pelo revisor antes da validação" ;;
  esac

  # 4. Regra crítica preenchida com Sim ou Não
  case "$(campo "$tmp" "Regra crítica")" in
    Sim|Não) ok "campo 'Regra crítica' definido" ;;
    *) falha "campo 'Regra crítica' deve ser 'Sim' ou 'Não'" ;;
  esac

  # 5. Seções obrigatórias
  local n
  for n in 1 2 3 4 5 6 7 8 9 10 11; do
    grep -qE "^## $n\. " "$tmp" || falha "seção $n ausente"
  done

  # 6. Critérios de aceite no formato Dado / Quando / Então
  local cas ca bloco
  cas="$(grep -oE '^### CA-[0-9]{2}' "$tmp" | sed 's/^### //')"
  if [ -z "$cas" ]; then
    falha "nenhum critério de aceite (### CA-01: ...)"
  else
    ok "$(echo "$cas" | wc -l | tr -d ' ') critério(s) de aceite encontrado(s)"
    for ca in $cas; do
      bloco="$(awk -v ca="$ca" '
        $0 ~ "^### " ca { dentro = 1; next }
        /^##/ { dentro = 0 }
        dentro { print }
      ' "$tmp")"
      echo "$bloco" | grep -q '\*\*Dado\*\*'   || falha "$ca sem 'Dado'"
      echo "$bloco" | grep -q '\*\*Quando\*\*' || falha "$ca sem 'Quando'"
      echo "$bloco" | grep -q '\*\*Então\*\*'  || falha "$ca sem 'Então'"
      # 7. Todo critério tem pelo menos um caso de teste
      secao "$tmp" 6 | grep -qE "^\|.*\| *$ca *\|" || falha "$ca sem caso de teste na seção 6"
    done
  fi

  # 8. Mini-plano com pelo menos um ciclo
  secao "$tmp" 8 | grep -qE '^\| *1 *\|' && ok "mini-plano com ciclos de TDD" || falha "mini-plano (seção 8) sem ciclos"

  # 9. Parecer do revisor
  local parecer
  parecer="$(secao "$tmp" 9 | grep -m1 '\*\*Parecer:\*\*')"
  if echo "$parecer" | grep -q "Aprovada" && ! echo "$parecer" | grep -q "Ajustes"; then
    ok "parecer do revisor: Aprovada"
  else
    falha "parecer do revisor (seção 9) não é 'Aprovada'"
  fi

  # 10. Nada do template sobrando nas seções 1 a 9 (10 e 11 são preenchidas depois)
  local restos
  restos="$(awk '/^## 10\. /{ exit } { print }' "$tmp" | grep -c "_preencher_")"
  [ "$restos" -eq 0 ] && ok "nenhum trecho '_preencher_' nas seções 1 a 9" \
                      || falha "$restos trecho(s) '_preencher_' nas seções 1 a 9"

  rm -f "$tmp"

  if [ "$erros" -eq 0 ]; then
    echo "  → APROVADA na spec validation"
  else
    echo "  → REPROVADA: $erros problema(s)"
    falhas_total=$((falhas_total + 1))
  fi
}

if [ "$#" -eq 0 ]; then
  echo "Uso: $0 docs/specs/<arquivo>.md [outros...]"
  exit 2
fi

for arquivo in "$@"; do
  case "$(basename "$arquivo")" in
    _TEMPLATE.md|README.md) continue ;;
  esac
  if [ ! -f "$arquivo" ]; then
    echo "Arquivo não encontrado: $arquivo"
    falhas_total=$((falhas_total + 1))
    continue
  fi
  validar "$arquivo"
done

echo ""
[ "$falhas_total" -eq 0 ] && exit 0 || exit 1
