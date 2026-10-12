#!/usr/bin/env bash
# Sauvegarde : base PostgreSQL (dump compresse) + audios + archives de datasets.
# A lancer depuis le dossier deploy/ ; a planifier avec cron (voir README.md). Garde les 7 dernieres sauvegardes.
set -euo pipefail
cd "$(dirname "$0")"

BACKUP_DIR="${BACKUP_DIR:-$PWD/backups}"
KEEP="${KEEP:-7}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"

echo "### Base de donnees"
docker compose exec -T db pg_dump -U afrivox -d afrivox --no-owner | gzip > "$BACKUP_DIR/db-$STAMP.sql.gz"

# Les volumes ont le prefixe du projet compose (afrivox_)
for volume in audio datasets; do
  echo "### Volume $volume"
  docker run --rm -v "afrivox_${volume}:/data:ro" -v "$BACKUP_DIR:/out" alpine:3 \
    tar czf "/out/${volume}-$STAMP.tar.gz" -C /data .
done

echo "### Nettoyage (garde les $KEEP plus recentes de chaque type)"
for prefix in db audio datasets; do
  # shellcheck disable=SC2012
  ls -1t "$BACKUP_DIR"/"$prefix"-* 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm -f --
done

echo "Sauvegarde terminee : $BACKUP_DIR (*-$STAMP.*)"
echo "Rappel : copier ce dossier HORS du serveur, une sauvegarde sur le meme disque ne protege pas d'une panne du disque."
