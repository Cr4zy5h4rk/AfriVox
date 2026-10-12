"""Tests du client AfriVox contre un faux serveur HTTP local (aucun accès réseau externe)."""

from __future__ import annotations

import contextlib
import hashlib
import io
import json
import os
import tempfile
import threading
import unittest
import zipfile
from email.parser import BytesParser
from email import policy
from http.server import BaseHTTPRequestHandler, HTTPServer

from afrivox import ApiError, Client, UnsafeUrlError, safe_extract, verify_archive
from afrivox import cli

KEY = "afv_AAAAAAAA_" + "x" * 43

PURCHASES = [
    {"id": 5, "datasetSlug": "agriculture-wolof", "versionLabel": "1.0", "license": "RESEARCH", "priceFcfa": 60000,
     "newerVersionLabel": None},
    {"id": 6, "datasetSlug": "agriculture-wolof", "versionLabel": "2.0", "license": "RESEARCH", "priceFcfa": 70000,
     "newerVersionLabel": None},
    {"id": 7, "datasetSlug": "quotidien-wolof", "versionLabel": "1.0", "license": "PROTOTYPE", "priceFcfa": 0,
     "newerVersionLabel": "1.1"},
]


def build_zip(files: dict[str, bytes], checksums: str | None = "auto") -> bytes:
    """Zip AfriVox minimal : audio, manifeste et checksums.sha256 (format sha256sum)."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        for name, content in files.items():
            z.writestr(name, content)
        if checksums == "auto":
            lines = [f"{hashlib.sha256(c).hexdigest()}  {n}" for n, c in files.items()]
            z.writestr("checksums.sha256", "\n".join(lines) + "\n")
        elif checksums is not None:
            z.writestr("checksums.sha256", checksums)
    return buf.getvalue()


class FakeServer(BaseHTTPRequestHandler):
    """Imite les routes d'AfriVox utilisées par le client ; mémorise les requêtes reçues."""

    requests: list[dict] = []
    archive: bytes = b""

    def log_message(self, *args):  # silence
        pass

    def _send(self, status: int, body: bytes = b"", ctype: str = "application/json", headers: dict | None = None):
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        for k, v in (headers or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _json(self, status: int, data):
        self._send(status, json.dumps(data).encode("utf-8"))

    def _handle(self):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else b""
        FakeServer.requests.append({"method": self.command, "path": self.path, "headers": dict(self.headers), "body": body})
        path = self.path.split("?")[0]
        public = path.startswith("/api/public/")
        if not public and self.headers.get("X-API-Key") != KEY:
            return self._json(401, {"title": "Cle d'API invalide, revoquee ou expiree", "status": 401})

        if self.command == "GET" and path == "/api/me/purchases":
            return self._json(200, PURCHASES)
        if self.command == "POST" and path == "/api/purchases/5/download-link":
            return self._json(200, {"url": "/api/public/purchase-downloads/tok", "expiresAt": "2026-10-20T00:00:00Z",
                                    "maxDownloads": 5})
        if self.command == "GET" and path == "/api/public/purchase-downloads/tok":
            return self._send(200, FakeServer.archive, "application/zip",
                              {"Content-Disposition": 'attachment; filename="agriculture-wolof-1.0-5.zip"'})
        if self.command == "GET" and path == "/api/public/test-sets":
            return self._json(200, [{"versionId": 14, "title": "Agriculture", "version": "2.0", "languageCode": "wo",
                                     "sentences": 120}])
        if self.command == "GET" and path == "/api/public/test-sets/14/template.csv":
            return self._send(200, b"id,text\n1,\n2,\n", "text/csv; charset=UTF-8")
        if self.command == "GET" and path == "/api/me/models":
            return self._json(200, [{"id": 3, "name": "wav2vec-wo", "task": "ASR", "languageCode": "wo", "license": "MIT"}])
        if self.command == "POST" and path == "/api/me/models/3/evaluations":
            ctype = self.headers.get("Content-Type", "")
            msg = BytesParser(policy=policy.default).parsebytes(b"Content-Type: " + ctype.encode() + b"\r\n\r\n" + body)
            parts = {p.get_param("name", header="content-disposition"): p for p in msg.iter_parts()}
            file_part = parts["file"]
            return self._json(201, {"evaluationId": 9, "wer": 12.5, "cer": 4.25, "sentences": 2,
                                    "seenTestSetId": parts["testSetId"].get_content(),
                                    "seenFilename": file_part.get_filename(),
                                    "seenContent": file_part.get_payload(decode=True).decode("utf-8")})
        if self.command == "POST" and path == "/api/me/models":
            data = json.loads(body.decode("utf-8"))
            return self._json(201, {"id": 77, **data})
        if path == "/api/limited":
            return self._send(429, b'{"title":"Trop de requetes"}', headers={"Retry-After": "60"})
        if path == "/api/business-error":
            # Forme réelle d'une erreur métier JHipster : le détail est un texte technique, la cause est « message »
            return self._json(400, {"title": "Bad Request", "status": 400, "message": "error.languagenotfound",
                                    "detail": "400 BAD_REQUEST, ProblemDetailWithCause[type='x']"})
        if path == "/api/forbidden":
            return self._json(403, {"title": "Cette cle n'a pas le droit d'appeler cette ressource", "status": 403})
        return self._json(404, {"title": "introuvable", "status": 404})

    do_GET = do_POST = do_DELETE = _handle


class ClientTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = HTTPServer(("127.0.0.1", 0), FakeServer)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        FakeServer.requests = []
        FakeServer.archive = build_zip({"audio/a.wav": b"RIFFdata", "manifest.tsv": b"id\tfile\n1\taudio/a.wav\n"})
        self.client = Client(self.url, KEY)
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)

    # ---- Adresse et clé ------------------------------------------------------------------------------------------

    def test_refuses_to_send_a_key_in_clear_over_the_internet(self):
        with self.assertRaises(UnsafeUrlError):
            Client("http://afrivox.example.org", KEY)

    def test_local_http_and_https_are_accepted(self):
        Client("http://localhost:8080", KEY)
        Client("http://127.0.0.1:8080", KEY)
        Client("https://afrivox.example.org/", KEY)

    def test_invalid_address_is_refused(self):
        for bad in ("afrivox.example.org", "ftp://x.org", ""):
            with self.assertRaises(ValueError, msg=bad):
                Client(bad, KEY)

    def test_trailing_slash_is_ignored(self):
        self.assertEqual(Client("https://afrivox.example.org/", KEY).base_url, "https://afrivox.example.org")

    # ---- Appels --------------------------------------------------------------------------------------------------

    def test_key_is_sent_in_the_dedicated_header(self):
        self.client.purchases()

        # Les noms d'en-tête HTTP sont insensibles à la casse : urllib les met en forme (« X-api-key »).
        sent = {k.lower(): v for k, v in FakeServer.requests[0]["headers"].items()}
        self.assertEqual(sent.get("x-api-key"), KEY)
        self.assertNotIn("authorization", sent)
        self.assertTrue(sent.get("user-agent", "").startswith("afrivox-python/"))

    def test_purchases_are_listed(self):
        self.assertEqual([p["id"] for p in self.client.purchases()], [5, 6, 7])

    def test_invalid_key_gives_a_clear_error(self):
        with self.assertRaises(ApiError) as caught:
            Client(self.url, "afv_BBBBBBBB_" + "y" * 43).purchases()

        self.assertEqual(caught.exception.status, 401)
        self.assertIn("invalide", caught.exception.message)

    def test_rate_limit_exposes_retry_after(self):
        with self.assertRaises(ApiError) as caught:
            self.client._json("GET", "/api/limited")

        self.assertEqual(caught.exception.status, 429)
        self.assertEqual(caught.exception.retry_after, 60)

    def test_business_error_is_shown_in_plain_language_not_as_a_spring_dump(self):
        with self.assertRaises(ApiError) as caught:
            self.client._json("GET", "/api/business-error")

        self.assertEqual(caught.exception.status, 400)
        self.assertEqual(caught.exception.message, "langue inconnue sur ce serveur")
        self.assertNotIn("ProblemDetailWithCause", str(caught.exception))

    def test_unknown_business_error_keeps_its_code(self):
        from afrivox.client import _error_message

        self.assertEqual(_error_message(b'{"message":"error.quelquechose"}'), "error.quelquechose")

    def test_unreachable_server_is_an_api_error_not_a_crash(self):
        with self.assertRaises(ApiError) as caught:
            Client("http://127.0.0.1:9", KEY, timeout=2).purchases()

        self.assertEqual(caught.exception.status, 0)

    def test_public_endpoints_work_without_a_key(self):
        anonymous = Client(self.url)

        self.assertEqual(anonymous.test_sets("wo")[0]["versionId"], 14)
        self.assertIn("language=wo", FakeServer.requests[0]["path"])
        self.assertEqual(anonymous.template(14), "id,text\n1,\n2,\n")

    # ---- Retrouver un achat --------------------------------------------------------------------------------------

    def test_purchase_is_found_by_number_slug_or_slug_at_version(self):
        self.assertEqual(self.client.find_purchase("7")["datasetSlug"], "quotidien-wolof")
        self.assertEqual(self.client.find_purchase("quotidien-wolof")["id"], 7)
        self.assertEqual(self.client.find_purchase("agriculture-wolof@2.0")["id"], 6)

    def test_ambiguous_reference_is_not_guessed(self):
        with self.assertRaises(ApiError) as caught:
            self.client.find_purchase("agriculture-wolof")

        self.assertEqual(caught.exception.status, 409)
        self.assertIn("5", caught.exception.message)
        self.assertIn("6", caught.exception.message)

    def test_unknown_purchase_is_not_found(self):
        with self.assertRaises(ApiError) as caught:
            self.client.find_purchase("inconnu")

        self.assertEqual(caught.exception.status, 404)

    # ---- Téléchargement ------------------------------------------------------------------------------------------

    def test_download_link_url_is_made_absolute(self):
        link = self.client.download_link(5)

        self.assertEqual(link["url"], self.url + "/api/public/purchase-downloads/tok")
        self.assertEqual(link["maxDownloads"], 5)

    def test_download_into_a_folder_uses_the_server_filename_and_leaves_no_partial_file(self):
        link = self.client.download_link(5)
        seen: list[tuple[int, int | None]] = []

        path = self.client.download(link["url"], self.tmp.name, progress=lambda d, t: seen.append((d, t)))

        self.assertEqual(os.path.basename(path), "agriculture-wolof-1.0-5.zip")
        with open(path, "rb") as f:
            self.assertEqual(f.read(), FakeServer.archive)
        self.assertEqual(os.listdir(self.tmp.name), ["agriculture-wolof-1.0-5.zip"])
        self.assertEqual(seen[-1][0], len(FakeServer.archive))

    def test_download_refuses_a_link_to_another_server(self):
        with self.assertRaises(ValueError):
            self.client.download("http://127.0.0.1:1/api/public/purchase-downloads/tok", self.tmp.name)

    def test_failed_download_leaves_nothing(self):
        with self.assertRaises(ApiError):
            self.client.download(self.url + "/api/public/inexistant", os.path.join(self.tmp.name, "x.zip"))

        self.assertEqual(os.listdir(self.tmp.name), [])

    # ---- Vérification et extraction ------------------------------------------------------------------------------

    def _write(self, content: bytes) -> str:
        path = os.path.join(self.tmp.name, "a.zip")
        with open(path, "wb") as f:
            f.write(content)
        return path

    def test_intact_archive_is_verified(self):
        checked, problems = verify_archive(self._write(FakeServer.archive))

        self.assertEqual(checked, 2)
        self.assertEqual(problems, [])

    def test_altered_file_is_detected(self):
        good = build_zip({"audio/a.wav": b"original"})
        bad = build_zip({"audio/a.wav": b"modifie"}, checksums=zipfile.ZipFile(io.BytesIO(good)).read("checksums.sha256").decode())

        _, problems = verify_archive(self._write(bad))

        self.assertEqual(problems, ["corrompu : audio/a.wav"])

    def test_missing_and_unlisted_files_are_reported(self):
        digest = hashlib.sha256(b"x").hexdigest()
        archive = build_zip({"extra.txt": b"y"}, checksums=f"{digest}  absent.wav\n")

        _, problems = verify_archive(self._write(archive))

        self.assertIn("manquant : absent.wav", problems)
        self.assertIn("non listé dans checksums.sha256 : extra.txt", problems)

    def test_archive_without_checksums_is_reported(self):
        _, problems = verify_archive(self._write(build_zip({"a": b"1"}, checksums=None)))

        self.assertEqual(problems, ["checksums.sha256 absent de l'archive"])

    def test_extraction_refuses_paths_leaving_the_folder(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("../evil.txt", b"x")
        path = self._write(buf.getvalue())

        with self.assertRaises(ValueError):
            safe_extract(path, os.path.join(self.tmp.name, "out"))
        self.assertFalse(os.path.exists(os.path.join(self.tmp.name, "evil.txt")))

    def test_normal_extraction_works(self):
        out = os.path.join(self.tmp.name, "out")

        safe_extract(self._write(FakeServer.archive), out)

        self.assertTrue(os.path.isfile(os.path.join(out, "audio", "a.wav")))

    # ---- Classement et publication -------------------------------------------------------------------------------

    def test_predictions_are_sent_as_multipart(self):
        csv_path = os.path.join(self.tmp.name, "predictions.csv")
        # newline="\n" : sous Windows le mode texte écrirait \r\n et fausserait la comparaison
        with open(csv_path, "w", encoding="utf-8", newline="\n") as f:
            f.write("id,text\n1,nanga def\n2,jàmm rekk\n")

        result = self.client.submit(3, 14, csv_path)

        self.assertEqual(result["wer"], 12.5)
        self.assertEqual(result["seenTestSetId"].strip(), "14")
        self.assertEqual(result["seenFilename"], "predictions.csv")
        self.assertEqual(result["seenContent"], "id,text\n1,nanga def\n2,jàmm rekk\n")

    def test_publish_drops_missing_fields(self):
        result = self.client.publish_model(name="m", task="ASR", url="https://hf.co/m", license=None, description=None)

        self.assertEqual(result["id"], 77)
        sent = json.loads(FakeServer.requests[-1]["body"])
        self.assertEqual(sent, {"name": "m", "task": "ASR", "url": "https://hf.co/m"})

    def test_my_models_are_listed(self):
        self.assertEqual(self.client.models()[0]["name"], "wav2vec-wo")


class CliTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        ClientTest.setUpClass.__func__(cls)  # même faux serveur

    @classmethod
    def tearDownClass(cls):
        ClientTest.tearDownClass.__func__(cls)

    def setUp(self):
        FakeServer.requests = []
        FakeServer.archive = build_zip({"audio/a.wav": b"RIFFdata"})
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self._env = {k: os.environ.pop(k, None) for k in ("AFRIVOX_URL", "AFRIVOX_API_KEY")}
        self.addCleanup(lambda: [os.environ.__setitem__(k, v) for k, v in self._env.items() if v is not None])

    def run_cli(self, *args: str) -> tuple[int, str, str]:
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = cli.run(["--url", self.url, "--api-key", KEY, *args])
        return code, out.getvalue(), err.getvalue()

    def test_missing_url_is_a_usage_error(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            code = cli.run(["purchases"])

        self.assertEqual(code, cli.EXIT_USAGE)
        self.assertIn("AFRIVOX_URL", err.getvalue())

    def test_missing_key_is_a_usage_error_except_for_public_commands(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            code = cli.run(["--url", self.url, "purchases"])
        self.assertEqual(code, cli.EXIT_USAGE)
        self.assertIn("AFRIVOX_API_KEY", err.getvalue())

        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = cli.run(["--url", self.url, "testsets"])
        self.assertEqual(code, cli.EXIT_OK)

    def test_clear_http_address_is_refused(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            code = cli.run(["--url", "http://afrivox.example.org", "--api-key", KEY, "purchases"])

        self.assertEqual(code, cli.EXIT_USAGE)
        self.assertIn("https", err.getvalue())

    def test_purchases_table_and_json(self):
        code, out, _ = self.run_cli("purchases")
        self.assertEqual(code, cli.EXIT_OK)
        self.assertIn("agriculture-wolof", out)

        code, out, _ = self.run_cli("--json", "purchases")
        self.assertEqual([p["id"] for p in json.loads(out)], [5, 6, 7])

    def test_download_verifies_and_extracts(self):
        code, out, _ = self.run_cli("download", "5", "-o", self.tmp.name, "--extract")

        self.assertEqual(code, cli.EXIT_OK, out)
        self.assertIn("1 fichiers vérifiés", out)
        self.assertTrue(os.path.isfile(os.path.join(self.tmp.name, "agriculture-wolof-1.0-5", "audio", "a.wav")))

    def test_download_of_a_tampered_archive_fails(self):
        FakeServer.archive = build_zip({"audio/a.wav": b"x"}, checksums=f"{'0' * 64}  audio/a.wav\n")

        code, out, _ = self.run_cli("download", "5", "-o", self.tmp.name)

        self.assertEqual(code, cli.EXIT_API)
        self.assertIn("altérée", out)

    def test_submit_prints_the_scores(self):
        csv_path = os.path.join(self.tmp.name, "p.csv")
        with open(csv_path, "w", encoding="utf-8") as f:
            f.write("id,text\n1,a\n")

        code, out, _ = self.run_cli("submit", "3", "14", csv_path)

        self.assertEqual(code, cli.EXIT_OK)
        self.assertIn("WER : 12.50 %", out)
        self.assertIn("CER : 4.25 %", out)

    def test_api_errors_exit_with_a_hint_and_never_print_the_key(self):
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            code = cli.run(["--url", self.url, "--api-key", "afv_BBBBBBBB_" + "y" * 43, "purchases"])

        self.assertEqual(code, cli.EXIT_API)
        self.assertIn("révoquée ou expirée", stderr.getvalue())
        self.assertNotIn("y" * 43, stderr.getvalue())

    def test_template_goes_to_a_file(self):
        target = os.path.join(self.tmp.name, "t.csv")

        code, _, _ = self.run_cli("template", "14", "-o", target)

        self.assertEqual(code, cli.EXIT_OK)
        with open(target, encoding="utf-8", newline="") as f:
            self.assertEqual(f.read(), "id,text\n1,\n2,\n")


if __name__ == "__main__":
    unittest.main()
