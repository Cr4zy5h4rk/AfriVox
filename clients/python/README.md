# afrivox : client Python et ligne de commande

Télécharger un dataset acheté, envoyer des prédictions au classement et publier un modèle, depuis un script ou un
terminal, avec une **clé d'API**. Python 3.9+, **aucune dépendance** (bibliothèque standard).

## Installation

```bash
pip install ./clients/python      # depuis la racine du dépôt AfriVox
# ou, sans installer : PYTHONPATH=clients/python python -m afrivox ...
```

## Démarrage rapide

1. Sur le site, page **Clés d'API** : créer une clé (cocher les droits utiles) et la copier. Elle n'est montrée qu'une fois.
2. Dans le terminal :

```bash
export AFRIVOX_URL=https://afrivox.example.org
export AFRIVOX_API_KEY=afv_XXXXXXXX_...

afrivox purchases                          # mes achats
afrivox download agriculture-wolof         # télécharge, vérifie checksums.sha256
afrivox download 12 --extract -o data      # par numéro d'achat, puis extrait
afrivox testsets --language wo             # jeux de test du classement
afrivox template 14 -o predictions.csv     # CSV id,text à remplir
afrivox submit 3 14 predictions.csv        # WER / CER calculés par le serveur
afrivox models                             # mes modèles
afrivox publish --name mon-asr --task ASR --url https://huggingface.co/moi/mon-asr \
  --license CC-BY-NC-4.0 --language wo --trained-on 12 --usage RESEARCH
```

**Héritage de licence** : un modèle ne peut pas accorder plus de droits que les datasets qui l'ont entraîné. Avec
`--trained-on`, `--usage` est obligatoire et plafonné par la plus restrictive de vos licences d'achat
(Prototype < Recherche < Commercial) ; chaque dataset cité doit avoir été acheté (un achat gratuit suffit pour le
plan Starter). Une licence qui autorise le commercial (MIT, Apache-2.0…) est refusée si vos licences sont plus
restrictives : utiliser une licence non commerciale, par exemple `CC-BY-NC-4.0`.

`--json` donne une sortie lisible par un script. Le code de sortie est 0 (succès), 1 (erreur de l'API ou archive
altérée) ou 2 (mauvais usage : adresse ou clé manquante).

## Dans un programme

```python
from afrivox import Client

c = Client("https://afrivox.example.org", api_key="afv_...")
purchase = c.find_purchase("agriculture-wolof@1.0")
link = c.download_link(purchase["id"])
path = c.download(link["url"], "data/")          # écrit un .part puis renomme : jamais de zip tronqué
```

## Sécurité

- **La clé ne part jamais en clair** : une adresse `http://` est refusée, sauf `localhost`.
- Préférer la variable d'environnement à `--api-key` (qui reste dans l'historique du shell).
- Une clé n'ouvre que les droits cochés (téléchargement, classement, publication) : jamais l'achat, le compte, les
  autres clés ni l'administration. Elle se révoque d'un clic sur la page des clés.
- L'extraction refuse tout chemin qui sortirait du dossier de destination.
- Le zip téléchargé est une **copie numérotée** de votre achat : son SHA-256 diffère de celui du catalogue. La
  vérification repose sur `checksums.sha256` à l'intérieur de l'archive, que `afrivox download` contrôle.

## Tests

```bash
cd clients/python
python -m unittest discover -s tests -t .
```

Les tests utilisent un faux serveur local : aucun accès réseau.
