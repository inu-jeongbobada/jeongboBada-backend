#!/usr/bin/env bash
# CLAUDE.md의 "설정값(application.yml) 추가/변경 시 체크리스트"를 자동으로 검사한다.
# application.yml.example의 jwt.* 키가 docker-compose.yml / .env.example / .env에도
# 같은 이름(JWT_* 환경변수)으로 존재하는지 비교해서, 빠진 게 있으면 경고한다.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
[ -z "$repo_root" ] && exit 0
cd "$repo_root"

yml_example="src/main/resources/application.yml.example"
compose="docker-compose.yml"
env_example=".env.example"
env_file=".env"

[ -f "$yml_example" ] || exit 0
[ -f "$compose" ] || exit 0

# application.yml.example의 jwt: 블록 아래 키들을 JWT_* 환경변수명으로 변환
# (Spring relaxed binding 규칙: kebab-case -> SCREAMING_SNAKE_CASE, jwt.refresh-expiration -> JWT_REFRESH_EXPIRATION)
yml_keys=$(awk '
  /^jwt:/ { in_jwt=1; next }
  in_jwt && /^[^ ]/ { exit }
  in_jwt && /^  [a-zA-Z_-]+:/ {
    sub(/^  /, ""); sub(/:.*/, ""); print
  }
' "$yml_example" | sed 's/-/_/g' | tr '[:lower:]' '[:upper:]' | sed 's/^/JWT_/')

[ -z "$yml_keys" ] && exit 0

compose_keys=$(grep -oE 'JWT_[A-Z_]+' "$compose" | sort -u || true)
env_example_keys=$(grep -oE '^JWT_[A-Z_]+' "$env_example" 2>/dev/null | sort -u || true)

missing=0
report=""

for key in $yml_keys; do
  if ! printf '%s\n' "$compose_keys" | grep -qx "$key"; then
    report+="  - $key: docker-compose.yml (backend.environment)에 없음\n"
    missing=1
  fi
  if ! printf '%s\n' "$env_example_keys" | grep -qx "$key"; then
    report+="  - $key: .env.example에 없음\n"
    missing=1
  fi
  if [ -f "$env_file" ] && ! grep -qE "^${key}=" "$env_file"; then
    report+="  - $key: .env에 없음 (로컬 실행 시 기본값으로만 동작하니 참고)\n"
    missing=1
  fi
done

if [ "$missing" -eq 1 ]; then
  {
    echo "⚠️  jwt.* 설정값이 application.yml.example / docker-compose.yml / .env(.example)에 안 맞습니다:"
    printf "%b" "$report"
    echo "CLAUDE.md의 '설정값 추가/변경 시 체크리스트' 참고."
  } >&2
  exit 2
fi

exit 0
