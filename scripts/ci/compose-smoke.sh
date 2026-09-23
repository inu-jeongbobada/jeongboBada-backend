#!/usr/bin/env bash
# docker compose로 MySQL + backend를 실제로 띄워서 기동되는지 확인하는 스모크 테스트 (#93).
#
# 왜 필요한가:
#   로컬 실행은 application.yml(파일)에서, docker compose는 docker-compose.yml의 환경변수에서 설정을 받는다.
#   새 설정값을 한쪽에만 추가하면 컨테이너가 기동 실패하는데, ./gradlew build는 이 경로를 전혀 검사하지 않는다.
#
# ⚠️ CI 전용이다. 끝날 때 `docker compose down -v`로 **MySQL 볼륨까지 지우고**, 확인용 계정을 가입시킨다.
#    레포 폴더에서 그냥 실행하면 로컬 개발 DB(jeongbobada-mysql)의 데이터가 사라진다.
#    로컬에서 돌려보려면 개발 DB와 겹치지 않는 별도 복사본에서(container_name·포트를 바꿔서) ALLOW_LOCAL=1로 실행할 것.
#
# 사용법 (CI, 레포 루트에서): .env 준비(JWT_SECRET 포함) 후 `bash scripts/ci/compose-smoke.sh`
# 환경변수: BASE_URL (기본 http://localhost:8080), TIMEOUT_SECONDS (기본 240)
set -euo pipefail

if [ "${CI:-}" != "true" ] && [ "${ALLOW_LOCAL:-}" != "1" ]; then
  echo "✗ CI 전용 스크립트다 — 끝날 때 MySQL 볼륨을 지워서 로컬 개발 DB가 사라진다. 스크립트 상단 주석을 볼 것." >&2
  exit 1
fi

BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-240}"

cleanup() {
  local status=$?
  if [ "$status" -ne 0 ]; then
    echo "::group::backend 로그 (실패 원인 확인용)"
    docker compose logs --no-color --tail=200 backend || true
    echo "::endgroup::"
    echo "::group::mysql 로그"
    docker compose logs --no-color --tail=50 mysql || true
    echo "::endgroup::"
  fi
  docker compose down -v --remove-orphans >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT

echo "▶ docker compose up --build"
docker compose up -d --build

echo "▶ backend 기동 대기 (최대 ${TIMEOUT_SECONDS}초)"
deadline=$((SECONDS + TIMEOUT_SECONDS))
until curl -sf -o /dev/null "$BASE_URL/v3/api-docs"; do
  # 설정 누락 등으로 컨테이너가 죽었으면 기다리지 않고 바로 실패.
  # docker-compose.yml에 restart: unless-stopped가 있어서, 죽은 backend는 exited가 아니라 restarting으로 보인다.
  state=$(docker compose ps -a --format '{{.State}}' backend 2>/dev/null || true)
  if [ -z "$state" ] || [ "$state" = "exited" ] || [ "$state" = "dead" ] || [ "$state" = "restarting" ]; then
    echo "✗ backend 컨테이너가 기동 중에 종료됨 (state=$state) — 설정값 누락 여부 확인 (AGENTS.md 설정값 체크리스트)"
    exit 1
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "✗ ${TIMEOUT_SECONDS}초 안에 backend가 응답하지 않음"
    exit 1
  fi
  sleep 3
done
echo "  기동 완료 (${SECONDS}초)"

check() {
  local name="$1" url="$2" expected_status="$3" expected_body="$4"
  local body status
  body=$(curl -s -w '\n%{http_code}' "$url")
  status="${body##*$'\n'}"
  body="${body%$'\n'*}"
  if [ "$status" != "$expected_status" ] || [[ "$body" != *"$expected_body"* ]]; then
    echo "✗ $name: $url → $status (기대 $expected_status, 본문에 '$expected_body' 포함)"
    echo "  본문: ${body:0:300}"
    exit 1
  fi
  echo "  ✓ $name ($status)"
}

echo "▶ 확인"
check "OpenAPI 문서"                  "$BASE_URL/v3/api-docs"  200 '"openapi"'
check "DB 조회 (datasource·data.sql)" "$BASE_URL/api/courses"  200 '"success":true'
check "에러 응답 형식"                "$BASE_URL/api/no-such"  404 '"code":"API_NOT_FOUND"'

# 가입 → 로그인으로 JWT가 실제로 발급되는지 (시크릿 키 등 JWT 설정 확인). 이 DB는 끝나면 down -v로 지워진다.
student_id="9$(printf '%08d' $((RANDOM * RANDOM % 100000000)))"
curl -s -o /dev/null -X POST "$BASE_URL/api/auth/signup" -H 'Content-Type: application/json' \
  -d "{\"studentId\":\"$student_id\",\"password\":\"password1\",\"nickname\":\"smoke$((RANDOM % 1000))\",\"email\":\"smoke$student_id@example.com\"}"
login_body=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"studentId\":\"$student_id\",\"password\":\"password1\"}")
if [ "${login_body##*$'\n'}" != "200" ] || [[ "$login_body" != *'"accessToken"'* ]]; then
  echo "✗ 로그인(JWT 발급) 실패: ${login_body:0:300}"
  exit 1
fi
echo "  ✓ 로그인(JWT 발급) (200)"
echo "✓ docker compose 스모크 통과"
