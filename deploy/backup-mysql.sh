#!/usr/bin/env bash
#
# Backup diario de MySQL para el despliegue nativo — ver docs/08-despliegue.md,
# "Backups de MySQL". No se ejecuta a mano: lo dispara
# freestyleperu-backup.timer (systemd) todas las noches.
#
# Lee las credenciales de la base desde el mismo .env que usa el backend
# (DB_URL/DB_USERNAME/DB_PASSWORD), así que no hay nada que duplicar a mano.
set -euo pipefail

ENV_FILE="${ENV_FILE:-/opt/freestyleperu/.env}"
BACKUP_DIR="${BACKUP_DIR:-/opt/freestyleperu/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# OJO: no basta con tomar "lo último después de la última barra" — DB_URL trae
# parámetros como serverTimezone=America/Lima, que también tienen una barra.
DB_NAME=$(echo "$DB_URL" | sed -E 's#^jdbc:mysql://[^/]+/##' | cut -d'?' -f1)
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
DUMP_FILE="$BACKUP_DIR/freestyleperu-$TIMESTAMP.sql.gz"

# --single-transaction: dump consistente de tablas InnoDB sin bloquearlas
# (el sistema sigue vendiendo mientras corre el backup a las 3am).
mysqldump --single-transaction --routines --triggers --events \
    -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
    | gzip > "$DUMP_FILE"

FINAL_FILE="$DUMP_FILE"

# Encriptación opcional — el dump tiene datos de clientes (email, teléfono,
# dirección, referencias de pago), así que si el archivo va a salir del VPS
# (BACKUP_RCLONE_REMOTE) conviene encriptarlo. Sin BACKUP_ENCRYPTION_KEY,
# el backup se hace igual, solo que sin encriptar (mejor un backup sin
# encriptar que ningún backup).
if [ -n "${BACKUP_ENCRYPTION_KEY:-}" ]; then
    openssl enc -aes-256-cbc -pbkdf2 -salt \
        -in "$DUMP_FILE" -out "$DUMP_FILE.enc" -pass "pass:$BACKUP_ENCRYPTION_KEY"
    rm -f "$DUMP_FILE"
    FINAL_FILE="$DUMP_FILE.enc"
else
    echo "AVISO: BACKUP_ENCRYPTION_KEY no está configurada — este backup queda sin encriptar." >&2
fi

# Copia fuera del VPS (recomendado — si el VPS se pierde entero, el backup
# local se pierde con él). BACKUP_RCLONE_REMOTE es un remote de rclone ya
# configurado, ej. "b2:mi-bucket/freestyleperu" — ver docs/08-despliegue.md.
if [ -n "${BACKUP_RCLONE_REMOTE:-}" ]; then
    rclone copy "$FINAL_FILE" "$BACKUP_RCLONE_REMOTE/" --quiet
fi

# Rotación: borra backups (locales y remotos) más viejos que RETENTION_DAYS.
find "$BACKUP_DIR" -name "freestyleperu-*.sql.gz*" -mtime "+$RETENTION_DAYS" -delete

if [ -n "${BACKUP_RCLONE_REMOTE:-}" ]; then
    rclone delete "$BACKUP_RCLONE_REMOTE/" --min-age "${RETENTION_DAYS}d" --quiet
fi

echo "Backup completado: $FINAL_FILE"
