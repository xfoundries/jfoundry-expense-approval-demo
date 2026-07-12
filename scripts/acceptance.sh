#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

request() {
  local method="$1" path="$2" user="$3" role="$4" body="${5:-}"
  local args=(-sS -o "$TMP_DIR/body" -w '%{http_code}' -X "$method"
    -H "X-User-Id: $user" -H "X-User-Role: $role")
  if [[ -n "$body" ]]; then
    args+=(-H 'Content-Type: application/json' -d "$body")
  fi
  curl "${args[@]}" "$BASE_URL$path"
}

expect_status() {
  local expected="$1" actual="$2" context="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL $context: expected HTTP $expected, got $actual" >&2
    cat "$TMP_DIR/body" >&2
    exit 1
  fi
}

create_claim() {
  local user="$1" title="$2"
  local status
  status="$(curl -sS -D "$TMP_DIR/headers" -o "$TMP_DIR/body" -w '%{http_code}' \
    -X POST "$BASE_URL/api/claims" \
    -H "X-User-Id: $user" -H 'X-User-Role: EMPLOYEE' \
    -H 'Content-Type: application/json' \
    -d "{\"title\":\"$title\"}")"
  expect_status 201 "$status" "create claim"
  awk 'BEGIN {IGNORECASE=1} /^Location:/ {gsub("\\r", "", $2); sub(".*/", "", $2); print $2}' \
    "$TMP_DIR/headers"
}

add_and_submit() {
  local id="$1" user="$2" amount="$3"
  local item
  item="{\"expenseDate\":\"2026-07-10\",\"category\":\"TRAVEL\",\"amount\":$amount,\"description\":\"Taxi\",\"receiptReference\":\"receipt-1\"}"
  expect_status 204 "$(request POST "/api/claims/$id/items" "$user" EMPLOYEE "$item")" "add item"
  expect_status 204 "$(request POST "/api/claims/$id/submit" "$user" EMPLOYEE)" "submit claim"
}

assert_state() {
  local id="$1" user="$2" role="$3" expected="$4"
  expect_status 200 "$(request GET "/api/claims/$id" "$user" "$role")" "get detail"
  local actual
  actual="$(jq -r '.state' "$TMP_DIR/body")"
  [[ "$actual" == "$expected" ]] || {
    echo "FAIL expected state $expected, got $actual" >&2
    cat "$TMP_DIR/body" >&2
    exit 1
  }
}

small_user="accept-small-$$"
small_id="$(create_claim "$small_user" 'Small claim')"
add_and_submit "$small_id" "$small_user" 2000.00
expect_status 204 "$(request POST "/api/claims/$small_id/manager-approval" manager-small MANAGER)" "small manager approval"
assert_state "$small_id" "$small_user" EMPLOYEE APPROVED
echo 'PASS small claim'

high_user="accept-high-$$"
high_id="$(create_claim "$high_user" 'High claim')"
add_and_submit "$high_id" "$high_user" 2000.01
expect_status 204 "$(request POST "/api/claims/$high_id/manager-approval" manager-high MANAGER)" "high manager approval"
assert_state "$high_id" finance-high FINANCE PENDING_FINANCE
expect_status 204 "$(request POST "/api/claims/$high_id/finance-approval" finance-high FINANCE)" "finance approval"
assert_state "$high_id" "$high_user" EMPLOYEE APPROVED
echo 'PASS high claim'

reopen_user="accept-reopen-$$"
reopen_id="$(create_claim "$reopen_user" 'Reopen claim')"
add_and_submit "$reopen_id" "$reopen_user" 100.00
expect_status 204 "$(request POST "/api/claims/$reopen_id/rejection" manager-reopen MANAGER '{"reason":"Wrong category"}')" "reject claim"
expect_status 204 "$(request POST "/api/claims/$reopen_id/reopen" "$reopen_user" EMPLOYEE)" "reopen claim"
expect_status 204 "$(request POST "/api/claims/$reopen_id/submit" "$reopen_user" EMPLOYEE)" "resubmit claim"
expect_status 204 "$(request POST "/api/claims/$reopen_id/withdraw" "$reopen_user" EMPLOYEE)" "withdraw claim"
assert_state "$reopen_id" "$reopen_user" EMPLOYEE WITHDRAWN
echo 'PASS rejection, reopen and withdrawal'

self_user="accept-self-$$"
self_id="$(create_claim "$self_user" 'Self approval claim')"
add_and_submit "$self_id" "$self_user" 100.00
expect_status 422 "$(request POST "/api/claims/$self_id/manager-approval" "$self_user" MANAGER)" "self approval"
echo 'PASS self approval rejection'

echo 'All acceptance scenarios passed.'
