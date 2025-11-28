#!/usr/bin/env bash

# Menú interactivo para consumir Casero API con curl.
# Configura BASE y TOKEN; se puede escribir durante la ejecución.

BASE="${BASE:-http://localhost:8080}"
TOKEN="${TOKEN:-}"

set -u

prompt() {
  local label="$1" default="${2:-}"
  if [[ -n "$default" ]]; then
    read -r -p "$label [$default]: " val
    echo "${val:-$default}"
  else
    read -r -p "$label: " val
    echo "$val"
  fi
}

require_token() {
  if [[ -z "$TOKEN" ]]; then
    echo "TOKEN no definido. Ejecuta opción de login o export TOKEN=..."
    return 1
  fi
}

login() {
  local pin
  pin=$(prompt "PIN (4-12 dígitos)")
  local resp
  resp=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"pin\":\"$pin\"}")
  echo "$resp"
  # Intentar capturar token automáticamente
  local parsed
  parsed=$(printf '%s' "$resp" | python3 -c 'import sys,json; import os
try:
    data=json.load(sys.stdin)
    print(data.get("token",""))
except Exception:
    pass' 2>/dev/null)
  if [[ -n "$parsed" ]]; then
    TOKEN="$parsed"
    echo "TOKEN seteado desde respuesta."
  else
    echo "No se pudo leer token automáticamente; define TOKEN manualmente en el menú (opción t)."
  fi
}

me() {
  require_token || return
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/auth/me"
  echo
}

customers_search() {
  require_token || return
  local q size page
  q=$(prompt "Buscar (q)" "")
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/customers?q=$q&page=$page&size=$size"
  echo
}

customer_create() {
  require_token || return
  local name sector address
  name=$(prompt "Nombre")
  sector=$(prompt "Sector ID")
  address=$(prompt "Dirección")
  curl -sS -X POST "$BASE/api/v1/customers" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$name\",\"sectorId\":$sector,\"address\":\"$address\"}"
  echo
}

customer_detail() {
  require_token || return
  local id
  id=$(prompt "Customer ID")
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/customers/$id"
  echo
}

customer_transactions() {
  require_token || return
  local id page size asc
  id=$(prompt "Customer ID")
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  asc=$(prompt "Ascendente? (true/false)" "false")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/customers/$id/transactions?page=$page&size=$size&ascending=$asc"
  echo
}

customer_report() {
  require_token || return
  local id range months type
  id=$(prompt "Customer ID")
  range=$(prompt "Rango (ALL/MONTHS)" "ALL")
  months=$(prompt "Meses (si MONTHS)" "12")
  type=$(prompt "Tipo (ALL/SALE/PAYMENT/REFUND/FAULT_DISCOUNT/DEBT_FORGIVENESS)" "ALL")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/customers/$id/transactions/report?range=$range&months=$months&type=$type" -o "reporte-$id.pdf"
  echo "Descargado reporte-$id.pdf"
}

customer_action() {
  require_token || return
  local id path body
  id=$(prompt "Customer ID")
  path="$1"
  body="$2"
  curl -sS -X POST "$BASE/api/v1/customers/$id/$path" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$body"
  echo
}

customer_delete_tx() {
  require_token || return
  local tx
  tx=$(prompt "Transaction ID")
  curl -sS -X DELETE "$BASE/api/v1/customers/transactions/$tx" \
    -H "Authorization: Bearer $TOKEN"
  echo
}

ranking() {
  require_token || return
  local size dir page
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "50")
  dir=$(prompt "Dirección (asc/desc)" "desc")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/customers/ranking?page=$page&size=$size&direction=$dir"
  echo
}

overdue() {
  require_token || return
  local months page size
  months=$(prompt "Meses" "1")
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/customers/overdue?months=$months&page=$page&size=$size"
  echo
}

transactions_all() {
  require_token || return
  local page size type
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  type=$(prompt "Tipo (vacío/SALE/PAYMENT/...)" "")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/transactions?page=$page&size=$size&type=$type"
  echo
}

transactions_delete() {
  require_token || return
  local tx
  tx=$(prompt "Transaction ID")
  curl -sS -X DELETE "$BASE/api/v1/transactions/$tx" \
    -H "Authorization: Bearer $TOKEN"
  echo
}

transactions_monthly_stats() {
  require_token || return
  local start end
  start=$(prompt "startDate (YYYY-MM-DD, opcional)" "")
  end=$(prompt "endDate (YYYY-MM-DD, opcional)" "")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/transactions/monthly-stats?startDate=$start&endDate=$end"
  echo
}

stats_summary() {
  require_token || return
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/statistics/summary"
  echo
}

stats_monthly() {
  require_token || return
  local start end
  start=$(prompt "start (YYYY-MM-DD, opcional)" "")
  end=$(prompt "end (YYYY-MM-DD, opcional)" "")
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/statistics/monthly?start=$start&end=$end"
  echo
}

stats_debtors() {
  require_token || return
  local page size
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/statistics/debtors?page=$page&size=$size"
  echo
}

stats_best() {
  require_token || return
  local page size
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/statistics/best-customers?page=$page&size=$size"
  echo
}

stats_sectors() {
  require_token || return
  local page size
  page=$(prompt "Página" "0")
  size=$(prompt "Tamaño" "10")
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/statistics/sectors?page=$page&size=$size"
  echo
}

sectors_list() {
  require_token || return
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/sectors"
  echo
}

admin_users() {
  require_token || return
  curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/admin/users"
  echo
}

admin_create_user() {
  require_token || return
  local name role pin
  name=$(prompt "Nombre")
  role=$(prompt "Rol (ADMIN/NORMAL)" "NORMAL")
  pin=$(prompt "PIN (4-12 dígitos)")
  curl -sS -X POST "$BASE/api/v1/admin/users" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$name\",\"role\":\"$role\",\"pin\":\"$pin\"}"
  echo
}

admin_update_pin() {
  require_token || return
  local id pin
  id=$(prompt "User ID")
  pin=$(prompt "Nuevo PIN (4-12 dígitos)")
  curl -sS -X PUT "$BASE/api/v1/admin/users/$id/pin" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"pin\":\"$pin\"}"
  echo
}

admin_export_customer_tx() {
  require_token || return
  local id
  id=$(prompt "Customer ID")
  curl -sS -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/v1/admin/customers/$id/transactions/export"
  echo
}

customer_mutations_menu() {
  echo "1) Venta  2) Pago  3) Devolución  4) Descuento falla  5) Condonar"
  read -r -p "Elige: " op
  case "$op" in
    1) customer_action "sales" "{\"detail\":\"Venta\",\"date\":\"2024-01-01\",\"itemsCount\":1,\"amount\":1000}" ;;
    2) customer_action "payments" "{\"date\":\"2024-01-02\",\"amount\":500}" ;;
    3) customer_action "refunds" "{\"detail\":\"Devolución\",\"date\":\"2024-01-03\",\"amount\":100}" ;;
    4) customer_action "fault-discounts" "{\"detail\":\"Falla\",\"date\":\"2024-01-04\",\"amount\":50}" ;;
    5) customer_action "forgiveness" "{\"detail\":\"Condonación\",\"date\":\"2024-01-05\"}" ;;
    *) echo "Opción inválida" ;;
  esac
}

main_menu() {
  while true; do
    echo "== Casero API Menu =="
    echo "Base actual: $BASE"
    echo "1) Login"
    echo "2) Me"
    echo "3) Buscar clientes"
    echo "4) Crear cliente"
    echo "5) Detalle cliente"
    echo "6) Transacciones cliente"
    echo "7) Reporte PDF cliente"
    echo "8) Acciones cliente (venta/pago/refund/falla/condonar)"
    echo "9) Actualizar nombre/dirección/sector"
    echo "10) Eliminar transacción cliente"
    echo "11) Ranking"
    echo "12) Morosos"
    echo "13) Transacciones globales"
    echo "14) Eliminar transacción global"
    echo "15) Stats mensuales transacciones"
    echo "16) Resumen global"
    echo "17) Stats mensuales"
    echo "18) Top deudores"
    echo "19) Mejores clientes"
    echo "20) Clientes por sector"
    echo "21) Listar sectores"
    echo "22) Admin: listar usuarios"
    echo "23) Admin: crear usuario"
    echo "24) Admin: actualizar PIN"
    echo "25) Admin: exportar transacciones cliente"
    echo "b) Cambiar BASE"
    echo "t) Definir TOKEN"
    echo "q) Salir"
    read -r -p "> " choice
    case "$choice" in
      1) login ;;
      2) me ;;
      3) customers_search ;;
      4) customer_create ;;
      5) customer_detail ;;
      6) customer_transactions ;;
      7) customer_report ;;
      8) customer_mutations_menu ;;
      9)
         echo "a) Nombre  b) Dirección  c) Sector"
         read -r -p "Elige: " sub
         case "$sub" in
           a) customer_action "name" "{\"newName\":\"Nombre Actualizado\"}" ;;
           b) customer_action "address" "{\"newAddress\":\"Nueva dirección\"}" ;;
           c) customer_action "sector" "{\"sectorId\":1}" ;;
           *) echo "Opción inválida" ;;
         esac
         ;;
      10) customer_delete_tx ;;
      11) ranking ;;
      12) overdue ;;
      13) transactions_all ;;
      14) transactions_delete ;;
      15) transactions_monthly_stats ;;
      16) stats_summary ;;
      17) stats_monthly ;;
      18) stats_debtors ;;
      19) stats_best ;;
      20) stats_sectors ;;
      21) sectors_list ;;
      22) admin_users ;;
      23) admin_create_user ;;
      24) admin_update_pin ;;
      25) admin_export_customer_tx ;;
      b) BASE=$(prompt "Nuevo BASE" "$BASE") ;;
      t) TOKEN=$(prompt "Nuevo TOKEN" "$TOKEN") ;;
      q) exit 0 ;;
      *) echo "Opción inválida" ;;
    esac
    echo
  done
}

main_menu
