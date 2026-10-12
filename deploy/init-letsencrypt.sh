#!/usr/bin/env bash
# Obtient le premier certificat Let's Encrypt et demarre toute la pile.
# A lancer une seule fois, depuis le dossier deploy/, apres avoir rempli .env.
#   STAGING=1 ./init-letsencrypt.sh   -> certificat de test (sans limite de debit), pour verifier la chaine.
#
# Probleme de depart : nginx ne demarre pas sans certificat, et Let's Encrypt a besoin de nginx pour valider le
# domaine. On cree donc un certificat factice, on demarre nginx, on demande le vrai, puis on recharge nginx.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Fichier .env introuvable : copier .env.example en .env et le remplir." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
. ./.env
set +a
: "${DOMAIN:?DOMAIN manquant dans .env}"
: "${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL manquant dans .env}"

STAGING_ARG=""
if [ "${STAGING:-0}" = "1" ]; then
  STAGING_ARG="--staging"
  echo "### Mode test : le certificat obtenu ne sera pas reconnu par les navigateurs."
fi

if [ "${SKIP_BUILD:-0}" = "1" ]; then
  echo "### Construction ignoree (SKIP_BUILD=1) : les images doivent deja etre chargees sur le serveur"
else
  echo "### Construction des images (la premiere fois : plusieurs minutes)"
  docker compose build
fi

echo "### Certificat factice pour ${DOMAIN}"
docker compose run --rm --entrypoint sh certbot -c "
  mkdir -p /etc/letsencrypt/live/${DOMAIN} &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/${DOMAIN}/privkey.pem \
    -out /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \
    -subj '/CN=localhost'"

echo "### Demarrage de la pile (base, API, nginx)"
docker compose up -d db backend web

echo "### Suppression du certificat factice"
docker compose run --rm --entrypoint sh certbot -c "
  rm -rf /etc/letsencrypt/live/${DOMAIN} /etc/letsencrypt/archive/${DOMAIN} /etc/letsencrypt/renewal/${DOMAIN}.conf"

echo "### Demande du vrai certificat pour ${DOMAIN}"
docker compose run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  ${STAGING_ARG} -d "${DOMAIN}" --email "${LETSENCRYPT_EMAIL}" --agree-tos --no-eff-email --non-interactive

echo "### Rechargement de nginx et demarrage du renouvellement automatique"
docker compose exec web nginx -s reload
docker compose up -d certbot

echo
echo "Termine. Ouvrir https://${DOMAIN}"
