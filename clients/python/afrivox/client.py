"""Client de l'API AfriVox (bibliothèque standard uniquement).

Une clé d'API (créée sur la page « Clés d'API » du site) suffit pour : lister ses achats et télécharger un dataset,
envoyer des prédictions au classement, lister et publier ses modèles. Elle ne permet ni d'acheter, ni de gérer le
compte : ces actions restent dans le navigateur.

    from afrivox import Client
    c = Client("https://afrivox.example.org", api_key="afv_...")
    for p in c.purchases():
        print(p["datasetSlug"], p["versionLabel"])
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import secrets
import zipfile
from typing import Any, Callable
from urllib import error, parse, request

__all__ = ["Client", "ApiError", "UnsafeUrlError", "verify_archive", "safe_extract"]

USER_AGENT = "afrivox-python/0.1"
_LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}
_CHUNK = 1024 * 256


class ApiError(Exception):
    """Réponse d'erreur du serveur (`status` = code HTTP, `retry_after` = secondes pour un 429)."""

    def __init__(self, status: int, message: str, retry_after: int | None = None):
        super().__init__(f"HTTP {status} : {message}")
        self.status = status
        self.message = message
        self.retry_after = retry_after


class UnsafeUrlError(ValueError):
    """L'adresse enverrait la clé d'API en clair sur Internet."""


def _check_url(base_url: str) -> str:
    parsed = parse.urlparse(base_url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise ValueError(f"Adresse invalide : {base_url!r} (attendu https://exemple.org)")
    if parsed.scheme == "http" and parsed.hostname not in _LOCAL_HOSTS:
        # La clé voyagerait en clair : elle serait lisible par n'importe quel intermédiaire.
        raise UnsafeUrlError(
            f"Refus d'envoyer une clé d'API en clair vers {parsed.hostname} : utiliser https://"
        )
    return base_url.rstrip("/")


#: Erreurs métier du serveur (champ « message » : error.<clé>) en clair. Les autres s'affichent telles quelles.
SERVER_ERRORS = {
    "error.languagenotfound": "langue inconnue sur ce serveur",
    "error.modelnotfound": "modèle introuvable (ou qui n'est pas le vôtre)",
    "error.tasknotasr": "le classement ne mesure que la reconnaissance vocale (ASR)",
    "error.testsetnotfound": "jeu de test introuvable (version non publiée ou sans jeu de test)",
    "error.languagemismatch": "la langue du modèle diffère de celle du jeu de test",
    "error.toosoon": "un envoi toutes les 10 minutes au plus pour ce modèle et ce jeu de test",
    "error.idsmissing": "il manque des identifiants : toutes les phrases doivent avoir une réponse",
    "error.idunknown": "le CSV contient des identifiants inconnus ou trop de lignes",
    "error.idduplicate": "identifiant en double dans le CSV",
    "error.csvinvalid": "CSV invalide (en-tête id,text, UTF-8, deux colonnes)",
    "error.csvtoolarge": "fichier trop volumineux (2 Mo au plus)",
    "error.texttoolong": "une phrase dépasse 200 mots",
    "error.toomanykeys": "trop de clés actives",
    "error.alreadypurchased": "déjà acheté sous cette licence",
    "error.packagemissing": "archive indisponible sur le serveur",
}


def _error_message(raw: bytes) -> str:
    try:
        data = json.loads(raw.decode("utf-8"))
        if isinstance(data, dict):
            code = data.get("message")
            if isinstance(code, str) and code.startswith("error."):
                return SERVER_ERRORS.get(code, code)
            for key in ("detail", "title", "message", "error"):
                value = data.get(key)
                if isinstance(value, str) and value:
                    return value
    except (ValueError, UnicodeDecodeError):
        pass
    text = raw.decode("utf-8", errors="replace").strip()
    return text[:200] or "réponse vide"


class Client:
    """Accès à l'API. `timeout` s'applique à chaque échange réseau (pas à un téléchargement entier)."""

    def __init__(self, base_url: str, api_key: str | None = None, timeout: float = 60.0):
        self.base_url = _check_url(base_url)
        self.api_key = api_key
        self.timeout = timeout

    # ---- Bas niveau -------------------------------------------------------------------------------------------

    def _open(self, method: str, path: str, *, body: bytes | None = None, headers: dict[str, str] | None = None,
              params: dict[str, Any] | None = None):
        url = path if path.startswith("http") else self.base_url + path
        if path.startswith("http") and not url.startswith(self.base_url + "/"):
            raise ValueError("Adresse hors du serveur configuré : refusée")
        if params:
            url += "?" + parse.urlencode({k: v for k, v in params.items() if v is not None})
        req = request.Request(url, data=body, method=method)
        req.add_header("User-Agent", USER_AGENT)
        req.add_header("Accept", "application/json")
        if self.api_key:
            req.add_header("X-API-Key", self.api_key)
        for name, value in (headers or {}).items():
            req.add_header(name, value)
        try:
            return request.urlopen(req, timeout=self.timeout)
        except error.HTTPError as e:
            retry = e.headers.get("Retry-After") if e.headers else None
            raise ApiError(e.code, _error_message(e.read()), int(retry) if retry and retry.isdigit() else None) from None
        except error.URLError as e:
            raise ApiError(0, f"serveur injoignable : {e.reason}") from None

    def _json(self, method: str, path: str, **kwargs) -> Any:
        with self._open(method, path, **kwargs) as response:
            raw = response.read()
        return json.loads(raw.decode("utf-8")) if raw else None

    # ---- Achats et téléchargement -----------------------------------------------------------------------------

    def purchases(self) -> list[dict]:
        """Mes achats (portée DOWNLOAD)."""
        return self._json("GET", "/api/me/purchases")

    def find_purchase(self, reference: str) -> dict:
        """Retrouve un achat par numéro (`12`), par dataset (`agriculture-wolof`) ou par dataset@version."""
        items = self.purchases()
        if reference.isdigit():
            wanted = int(reference)
            matches = [p for p in items if p["id"] == wanted]
        else:
            slug, _, version = reference.partition("@")
            matches = [p for p in items if p["datasetSlug"] == slug and (not version or p["versionLabel"] == version)]
        if not matches:
            raise ApiError(404, f"Aucun achat ne correspond à « {reference} »")
        if len(matches) > 1:
            # Même dataset acheté sous plusieurs licences ou versions : ne pas deviner.
            choices = ", ".join(f"{p['id']} ({p['versionLabel']}, {p['license']})" for p in matches)
            raise ApiError(409, f"« {reference} » est ambigu, préciser le numéro d'achat : {choices}")
        return matches[0]

    def download_link(self, purchase_id: int) -> dict:
        """Lien signé et limité (expiration, nombre de téléchargements). Pas de nouveau paiement."""
        link = self._json("POST", f"/api/purchases/{int(purchase_id)}/download-link", body=b"{}",
                          headers={"Content-Type": "application/json"})
        link["url"] = self.base_url + link["url"] if link["url"].startswith("/") else link["url"]
        return link

    def download(self, url: str, target: str, progress: Callable[[int, int | None], None] | None = None) -> str:
        """Télécharge vers `target` (fichier, ou dossier : le nom envoyé par le serveur est alors utilisé).

        Écrit dans un fichier `.part` puis renomme : un téléchargement interrompu ne laisse jamais un zip tronqué
        sous le vrai nom. Renvoie le chemin final.
        """
        with self._open("GET", url, headers={"Accept": "application/zip"}) as response:
            total = response.headers.get("Content-Length")
            total_bytes = int(total) if total and total.isdigit() else None
            path = target
            if os.path.isdir(target):
                disposition = response.headers.get("Content-Disposition", "")
                found = re.search(r'filename="?([^";]+)"?', disposition)
                name = os.path.basename(found.group(1)) if found else "dataset.zip"
                path = os.path.join(target, name or "dataset.zip")
            part = path + ".part"
            done = 0
            try:
                with open(part, "wb") as out:
                    while True:
                        chunk = response.read(_CHUNK)
                        if not chunk:
                            break
                        out.write(chunk)
                        done += len(chunk)
                        if progress:
                            progress(done, total_bytes)
                os.replace(part, path)
            finally:
                if os.path.exists(part):
                    os.remove(part)
        return path

    # ---- Classement -------------------------------------------------------------------------------------------

    def test_sets(self, language: str | None = None) -> list[dict]:
        """Jeux de test publics (aucune clé nécessaire)."""
        return self._json("GET", "/api/public/test-sets", params={"language": language})

    def template(self, test_set_id: int) -> str:
        """CSV `id,text` à remplir : un identifiant par phrase du jeu de test, texte vide."""
        with self._open("GET", f"/api/public/test-sets/{int(test_set_id)}/template.csv",
                        headers={"Accept": "text/csv"}) as response:
            return response.read().decode("utf-8")

    def models(self) -> list[dict]:
        """Mes modèles (portée EVALUATE ou PUBLISH)."""
        return self._json("GET", "/api/me/models")

    def submit(self, model_id: int, test_set_id: int, csv_path: str) -> dict:
        """Envoie des prédictions `id,text` ; le serveur calcule WER et CER (portée EVALUATE)."""
        with open(csv_path, "rb") as f:
            content = f.read()
        body, content_type = _multipart(
            fields={"testSetId": str(int(test_set_id))},
            files={"file": (os.path.basename(csv_path), content, "text/csv")},
        )
        return self._json("POST", f"/api/me/models/{int(model_id)}/evaluations", body=body,
                          headers={"Content-Type": content_type})

    def publish_model(self, **fields: Any) -> dict:
        """Publie un modèle (portée PUBLISH). Champs : name, task, url, license, description, languageCode, sha256,
        trainedOnVersionId, otherVersionIds. Seules les métadonnées sont stockées, jamais les poids."""
        payload = {k: v for k, v in fields.items() if v is not None}
        return self._json("POST", "/api/me/models", body=json.dumps(payload).encode("utf-8"),
                          headers={"Content-Type": "application/json"})


def _multipart(fields: dict[str, str], files: dict[str, tuple[str, bytes, str]]) -> tuple[bytes, str]:
    boundary = "afrivox-" + secrets.token_hex(16)
    lines: list[bytes] = []
    for name, value in fields.items():
        lines += [f"--{boundary}".encode(), f'Content-Disposition: form-data; name="{name}"'.encode(), b"",
                  value.encode("utf-8")]
    for name, (filename, content, ctype) in files.items():
        safe = filename.replace('"', "_").replace("\r", "_").replace("\n", "_")
        lines += [f"--{boundary}".encode(),
                  f'Content-Disposition: form-data; name="{name}"; filename="{safe}"'.encode(),
                  f"Content-Type: {ctype}".encode(), b"", content]
    lines += [f"--{boundary}--".encode(), b""]
    return b"\r\n".join(lines), f"multipart/form-data; boundary={boundary}"


def verify_archive(zip_path: str) -> tuple[int, list[str]]:
    """Vérifie un zip AfriVox avec son `checksums.sha256` (format `sha256sum`).

    Renvoie (nombre de fichiers vérifiés, liste de problèmes). Le SHA-256 du zip lui-même n'est pas comparé à celui
    du catalogue : chaque achat reçoit une copie numérotée, donc différente ; seul `checksums.sha256` fait foi.
    """
    problems: list[str] = []
    with zipfile.ZipFile(zip_path) as z:
        names = set(z.namelist())
        if "checksums.sha256" not in names:
            return 0, ["checksums.sha256 absent de l'archive"]
        expected: dict[str, str] = {}
        for line in z.read("checksums.sha256").decode("utf-8").splitlines():
            digest, _, name = line.partition("  ")
            if digest and name:
                expected[name] = digest.lower()
        checked = 0
        for name, digest in expected.items():
            if name not in names:
                problems.append(f"manquant : {name}")
                continue
            h = hashlib.sha256()
            with z.open(name) as f:
                for chunk in iter(lambda: f.read(_CHUNK), b""):
                    h.update(chunk)
            if h.hexdigest() != digest:
                problems.append(f"corrompu : {name}")
            checked += 1
        for name in sorted(names - set(expected) - {"checksums.sha256"}):
            if not name.endswith("/"):
                problems.append(f"non listé dans checksums.sha256 : {name}")
    return checked, problems


def safe_extract(zip_path: str, destination: str) -> list[str]:
    """Extrait en refusant tout chemin qui sortirait du dossier (« ../ », chemin absolu)."""
    root = os.path.realpath(destination)
    extracted: list[str] = []
    with zipfile.ZipFile(zip_path) as z:
        for info in z.infolist():
            target = os.path.realpath(os.path.join(root, info.filename))
            if target != root and not target.startswith(root + os.sep):
                raise ValueError(f"Entrée dangereuse dans l'archive : {info.filename!r}")
        z.extractall(root)
        extracted = z.namelist()
    return extracted
