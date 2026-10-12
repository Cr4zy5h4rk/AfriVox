"""Ligne de commande `afrivox`.

    export AFRIVOX_URL=https://afrivox.example.org
    export AFRIVOX_API_KEY=afv_XXXXXXXX_...
    afrivox purchases
    afrivox download agriculture-wolof --extract
    afrivox testsets --language wo
    afrivox template 14 -o predictions.csv
    afrivox submit 3 14 predictions.csv
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from typing import Any, Sequence

from .client import ApiError, Client, UnsafeUrlError, safe_extract, verify_archive

EXIT_OK, EXIT_API, EXIT_USAGE = 0, 1, 2


def _table(rows: Sequence[dict], columns: Sequence[tuple[str, str]]) -> str:
    """Tableau texte : `columns` = (clé, titre)."""
    if not rows:
        return "(aucun)"
    cells = [[str(row.get(key, "") if row.get(key) is not None else "") for key, _ in columns] for row in rows]
    widths = [max(len(title), *(len(c[i]) for c in cells)) for i, (_, title) in enumerate(columns)]
    lines = ["  ".join(title.ljust(widths[i]) for i, (_, title) in enumerate(columns))]
    lines.append("  ".join("-" * w for w in widths))
    lines += ["  ".join(c[i].ljust(widths[i]) for i in range(len(columns))) for c in cells]
    return "\n".join(lines)


def _human(n: int) -> str:
    size = float(n)
    for unit in ("o", "Ko", "Mo", "Go"):
        if size < 1024 or unit == "Go":
            return f"{size:.0f} {unit}" if unit == "o" else f"{size:.1f} {unit}"
        size /= 1024
    return f"{n} o"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="afrivox", description="Client en ligne de commande d'AfriVox.")
    parser.add_argument("--url", default=os.environ.get("AFRIVOX_URL"), help="adresse du site (ou AFRIVOX_URL)")
    parser.add_argument("--api-key", default=os.environ.get("AFRIVOX_API_KEY"),
                        help="clé d'API (ou AFRIVOX_API_KEY) ; préférer la variable d'environnement")
    parser.add_argument("--json", action="store_true", help="sortie JSON (pour les scripts)")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("purchases", help="lister mes achats")

    d = sub.add_parser("download", help="télécharger un dataset acheté")
    d.add_argument("purchase", help="numéro d'achat, « dataset » ou « dataset@version »")
    d.add_argument("-o", "--out", default=".", help="dossier de destination (défaut : dossier courant)")
    d.add_argument("--extract", action="store_true", help="extraire l'archive après vérification")
    d.add_argument("--no-verify", action="store_true", help="ne pas vérifier checksums.sha256")

    sub.add_parser("models", help="lister mes modèles")

    t = sub.add_parser("testsets", help="lister les jeux de test du classement")
    t.add_argument("--language", help="code de langue (ex. wo)")

    tp = sub.add_parser("template", help="écrire le CSV id,text à remplir pour un jeu de test")
    tp.add_argument("testset", type=int)
    tp.add_argument("-o", "--out", help="fichier de sortie (défaut : sortie standard)")

    s = sub.add_parser("submit", help="envoyer des prédictions au classement")
    s.add_argument("model", type=int, help="numéro de mon modèle")
    s.add_argument("testset", type=int, help="numéro du jeu de test")
    s.add_argument("csv", help="fichier id,text")

    p = sub.add_parser("publish", help="publier un modèle (métadonnées et lien, jamais les poids)")
    p.add_argument("--name", required=True)
    p.add_argument("--task", required=True, choices=["ASR", "TTS", "INTENT", "CONVERSATION", "OTHER"])
    p.add_argument("--url", dest="model_url", help="lien https du modèle")
    p.add_argument("--license", dest="model_license")
    p.add_argument("--description")
    p.add_argument("--language", dest="language_code")
    p.add_argument("--sha256", help="empreinte SHA-256 du fichier pointé par le lien")
    p.add_argument("--trained-on", type=int, dest="trained_on", help="numéro de version du dataset d'entraînement")
    p.add_argument("--usage", choices=["PROTOTYPE", "RESEARCH", "COMMERCIAL"],
                   help="usage accordé au modèle ; obligatoire avec --trained-on, plafonné par vos licences d'achat")
    return parser


def _emit(args: argparse.Namespace, data: Any, text: str) -> None:
    print(json.dumps(data, ensure_ascii=False, indent=2) if args.json else text)


def _progress(done: int, total: int | None) -> None:
    if not sys.stderr.isatty():
        return
    if total:
        sys.stderr.write(f"\r  {_human(done)} / {_human(total)} ({100 * done // total} %)")
    else:
        sys.stderr.write(f"\r  {_human(done)}")
    sys.stderr.flush()


def run(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if not args.url:
        print("Adresse du site manquante : --url ou variable AFRIVOX_URL.", file=sys.stderr)
        return EXIT_USAGE
    needs_key = args.command not in ("testsets", "template")
    if needs_key and not args.api_key:
        print("Clé d'API manquante : variable AFRIVOX_API_KEY (ou --api-key). "
              "Elle se crée sur la page « Clés d'API » du site.", file=sys.stderr)
        return EXIT_USAGE
    try:
        client = Client(args.url, args.api_key)
    except (ValueError, UnsafeUrlError) as e:
        print(f"Erreur : {e}", file=sys.stderr)
        return EXIT_USAGE

    try:
        return _dispatch(client, args)
    except ApiError as e:
        hint = {
            401: " (clé inconnue, révoquée ou expirée)",
            403: " (cette clé n'a pas la portée nécessaire)",
            429: f" (réessayer dans {e.retry_after or 60} s)",
        }.get(e.status, "")
        print(f"Erreur : {e}{hint}", file=sys.stderr)
        return EXIT_API
    except (OSError, ValueError) as e:
        print(f"Erreur : {e}", file=sys.stderr)
        return EXIT_API


def _dispatch(client: Client, args: argparse.Namespace) -> int:
    cmd = args.command
    if cmd == "purchases":
        items = client.purchases()
        _emit(args, items, _table(items, [("id", "N°"), ("datasetSlug", "Dataset"), ("versionLabel", "Version"),
                                          ("license", "Licence"), ("priceFcfa", "Prix FCFA"),
                                          ("newerVersionLabel", "Plus récente")]))
    elif cmd == "download":
        purchase = client.find_purchase(args.purchase)
        os.makedirs(args.out, exist_ok=True)
        link = client.download_link(purchase["id"])
        print(f"Téléchargement de {purchase['datasetSlug']} {purchase['versionLabel']} …", file=sys.stderr)
        path = client.download(link["url"], args.out, progress=_progress)
        sys.stderr.write("\n" if sys.stderr.isatty() else "")
        result: dict[str, Any] = {"file": path, "purchase": purchase["id"]}
        if not args.no_verify:
            checked, problems = verify_archive(path)
            result.update(verified=checked, problems=problems)
            if problems:
                _emit(args, result, "Archive INCOMPLÈTE ou altérée :\n  " + "\n  ".join(problems))
                return EXIT_API
        if args.extract:
            folder = os.path.splitext(path)[0]
            safe_extract(path, folder)
            result["extracted"] = folder
        text = f"OK : {path}"
        if "verified" in result:
            text += f" ({result['verified']} fichiers vérifiés)"
        if "extracted" in result:
            text += f"\nExtrait dans : {result['extracted']}"
        _emit(args, result, text)
    elif cmd == "models":
        items = client.models()
        _emit(args, items, _table(items, [("id", "N°"), ("name", "Nom"), ("task", "Tâche"),
                                          ("languageCode", "Langue"), ("license", "Licence")]))
    elif cmd == "testsets":
        items = client.test_sets(args.language)
        _emit(args, items, _table(items, [("versionId", "N°"), ("title", "Dataset"), ("version", "Version"),
                                          ("languageCode", "Langue"), ("sentences", "Phrases")]))
    elif cmd == "template":
        content = client.template(args.testset)
        if args.out:
            with open(args.out, "w", encoding="utf-8", newline="") as f:
                f.write(content)
            print(f"Écrit : {args.out}", file=sys.stderr)
        else:
            sys.stdout.write(content)
    elif cmd == "submit":
        r = client.submit(args.model, args.testset, args.csv)
        _emit(args, r, f"WER : {r['wer']:.2f} %   CER : {r['cer']:.2f} %   ({r['sentences']} phrases)")
    elif cmd == "publish":
        created = client.publish_model(
            name=args.name, task=args.task, url=args.model_url, license=args.model_license,
            description=args.description, languageCode=args.language_code, sha256=args.sha256,
            trainedOnVersionId=args.trained_on, usage=args.usage,
        )
        _emit(args, created, f"Modèle publié : n° {created.get('id')} ({created.get('name')})")
    return EXIT_OK


def main() -> None:
    # Console Windows : ne jamais planter sur un caractère que l'encodage local ne sait pas écrire.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(errors="replace")
    sys.exit(run())


if __name__ == "__main__":  # pragma: no cover
    main()
