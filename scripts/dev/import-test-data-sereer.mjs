#!/usr/bin/env node
/**
 * Import de DONNEES DE TEST en local : corpus sereer (parole spontanee longue, transcrite avec Transcriber).
 * Meme principe que import-test-data.mjs (tout passe par l'API reelle) ; outils communs dans lib.mjs.
 *
 * Source : 104 enregistrements WAV d'environ 15 min (PCM 16 kHz mono 16 bits) avec leur fichier .trs. On ne garde qu'un
 * ECHANTILLON de segments : decoupe aux reperes <Sync> d'un meme tour de parole, entre 0,5 et 30 s.
 *
 * Choix et limites a connaitre :
 *   - ce sont de vraies conversations spontanees : usage LOCAL uniquement, jamais dans une demo ni un dataset publie ;
 *     les segments contenant une suite d'au moins 6 chiffres (numeros possibles) sont ecartes ;
 *   - licence et provenance inconnues (aucun fichier de licence dans le dossier) ;
 *   - le texte est la transcription sans balises de langue (" :fra"...), le reste est laisse tel quel ;
 *   - un compte contributeur par locuteur de chaque fichier (les etiquettes spk1, spk2... sont locales a un fichier :
 *     la meme personne peut apparaitre sous plusieurs comptes), ce qui rend les decoupages train/dev/test reels ;
 *   - rien n'est reecoute : les validateurs acceptent tout (donnees de test, pas une validation humaine).
 *
 * Usage (serveur local lance) :
 *   SEREER_DIR=<dossier speech_dataset> ADMIN_USER=... ADMIN_PASSWORD=... IMPORT_PASSWORD=... \
 *     node scripts/dev/import-test-data-sereer.mjs [--dry-run | --from-review | --datasets-only]
 *   --from-review  : reprend a la qualification des validateurs (apres une panne pendant l'envoi) ;
 *   --datasets-only : ne refait que les datasets.
 */
import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import {
  ADMIN_PASSWORD,
  ADMIN_USER,
  BASE_URL,
  IMPORT_PASSWORD,
  VALIDATORS,
  checkSetup,
  fail,
  loggedIn,
  publishDatasets,
  completeTestProfile,
  promoteTestContributor,
  qualifyValidators,
  register,
  reviewEverything,
} from './lib.mjs';

const SEREER_DIR = process.env.SEREER_DIR;
const DRY_RUN = process.argv.includes('--dry-run');
const LANGUAGE = { code: 'srr', name: 'Sereer', script: 'LATIN', direction: 'LTR', minValidators: 3 };
const DOMAIN = 'agriculture';
const CAMPAIGN_TITLE = 'Corpus de test (sereer)';

/** Echantillon : TARGET segments pris dans FILES fichiers (a peu pres le meme nombre dans chacun, espaces regulierement). */
const TARGET = 2000;
const FILES = 30;

const SAMPLE_RATE = 16000;
const BYTES_PER_SAMPLE = 2;
const MIN_SEC = 0.5; // limites du serveur (application.audio) : 0,5 s a 30 s
const MAX_SEC = 30;

/** Datasets a creer (tarifs = valeurs de TEST). Emboites : Starter < Plus < Pro. */
const DATASETS = [
  { slug: 'agriculture-sereer-starter', title: 'Agriculture (sereer) - Starter', domain: DOMAIN, plan: 'STARTER', max: 30 },
  { slug: 'agriculture-sereer-plus', title: 'Agriculture (sereer) - Plus', domain: DOMAIN, plan: 'PLUS', max: 300, price: 30000 },
  { slug: 'agriculture-sereer-pro', title: 'Agriculture (sereer) - Pro', domain: DOMAIN, plan: 'PRO', max: null, price: 50000 },
];

const publishAll = () =>
  publishDatasets(DATASETS, LANGUAGE.code, (d) => `Jeu de test (domaine ${d.domain}). Donnees importees localement, non publiables.`);

// ---------------------------------------------------------------------------------------------------------------

/** Attributs d'une balise XML : ` a="1" b="2"` -> { a: '1', b: '2' }. */
function attributes(tag) {
  return Object.fromEntries([...tag.matchAll(/([\w:-]+)="([^"]*)"/g)].map((m) => [m[1], m[2]]));
}

/** Texte d'un segment : balises retirees, balises de langue ( :fra, :wol...) retirees, espaces normalises. */
function cleanText(raw) {
  return raw
    .replace(/<[^>]*>/g, ' ')
    .replace(/\s*:[a-z]{3}\b/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Segments d'un fichier .trs : chaque <Sync> ouvre un segment qui dure jusqu'au <Sync> suivant du meme tour (ou la fin du
 * tour). Les tours a plusieurs locuteurs (parole superposee) sont ignores : on ne saurait pas a qui attribuer la voix.
 */
function segmentsOf(trs) {
  const out = [];
  for (const turn of trs.matchAll(/<Turn ([^>]*)>([\s\S]*?)<\/Turn>/g)) {
    const attrs = attributes(turn[1]);
    const speaker = attrs.speaker;
    if (!speaker || /\s/.test(speaker.trim())) continue;
    const turnEnd = Number(attrs.endTime);
    const parts = [...turn[2].matchAll(/<Sync time="([\d.]+)"\/>([^<]*(?:<(?!Sync)[^>]*>[^<]*)*)/g)];
    parts.forEach((p, i) => {
      const start = Number(p[1]);
      const end = i + 1 < parts.length ? Number(parts[i + 1][1]) : turnEnd;
      const text = cleanText(p[2]);
      const duration = end - start;
      if (!text || text.length < 2 || text.length > 500) return;
      if (duration < MIN_SEC || duration > MAX_SEC) return;
      if (/\d{6,}/.test(text)) return; // numero possible : ecarte
      out.push({ speaker, start, end, text });
    });
  }
  return out;
}

/** Coupe un WAV PCM 16 kHz mono 16 bits (donnees a l'octet 44) entre deux instants, avec un nouvel en-tete. */
function cutWav(wav, startSec, endSec) {
  const dataSize = wav.length - 44;
  const from = Math.min(Math.round(startSec * SAMPLE_RATE) * BYTES_PER_SAMPLE, dataSize);
  const to = Math.min(Math.round(endSec * SAMPLE_RATE) * BYTES_PER_SAMPLE, dataSize);
  const pcm = wav.subarray(44 + from, 44 + to);
  const header = Buffer.alloc(44);
  header.write('RIFF', 0, 'ascii');
  header.writeUInt32LE(36 + pcm.length, 4);
  header.write('WAVEfmt ', 8, 'ascii');
  header.writeUInt32LE(16, 16); // taille du bloc fmt
  header.writeUInt16LE(1, 20); // PCM
  header.writeUInt16LE(1, 22); // mono
  header.writeUInt32LE(SAMPLE_RATE, 24);
  header.writeUInt32LE(SAMPLE_RATE * BYTES_PER_SAMPLE, 28);
  header.writeUInt16LE(BYTES_PER_SAMPLE, 32);
  header.writeUInt16LE(16, 34);
  header.write('data', 36, 'ascii');
  header.writeUInt32LE(pcm.length, 40);
  return { audio: Buffer.concat([header, pcm]), durationMs: Math.round((pcm.length / BYTES_PER_SAMPLE / SAMPLE_RATE) * 1000) };
}

/** Verifie l'en-tete (PCM 16 kHz mono 16 bits, donnees a l'octet 44) avant de couper : sinon les octets seraient faux. */
function assertSupportedWav(wav, name) {
  const ok =
    wav.toString('ascii', 0, 4) === 'RIFF' &&
    wav.toString('ascii', 36, 40) === 'data' &&
    wav.readUInt16LE(20) === 1 &&
    wav.readUInt16LE(22) === 1 &&
    wav.readUInt32LE(24) === SAMPLE_RATE &&
    wav.readUInt16LE(34) === 16;
  if (!ok) throw new Error(`${name} : format WAV non pris en charge (PCM 16 kHz mono 16 bits attendu)`);
}

/** Prend `count` elements espaces regulierement dans la liste. */
function evenly(list, count) {
  if (list.length <= count) return list;
  return Array.from({ length: count }, (_, i) => list[Math.floor((i * list.length) / count)]);
}

/** Lit tous les .trs et choisit l'echantillon : { file, dir, segments[] } par fichier retenu. */
async function loadSample() {
  const candidates = [];
  for (const d of (await readdir(SEREER_DIR, { withFileTypes: true })).filter((e) => e.isDirectory())) {
    for (const f of (await readdir(path.join(SEREER_DIR, d.name))).filter((n) => n.endsWith('.trs')).sort()) {
      const segments = segmentsOf(await readFile(path.join(SEREER_DIR, d.name, f), 'utf8'));
      const base = f.replace(/\.trs$/, '');
      candidates.push({ base, dir: path.join(SEREER_DIR, d.name), segments, id: base.replace(/^srr_/, '') });
    }
  }
  candidates.sort((a, b) => a.base.localeCompare(b.base));
  const quota = Math.ceil(TARGET / FILES);
  const rich = candidates.filter((c) => c.segments.length >= quota);
  const chosen = evenly(rich, FILES);
  const perFile = Math.ceil(TARGET / Math.max(1, chosen.length));
  return {
    candidates,
    files: chosen.map((c) => ({ ...c, segments: evenly(c.segments, perFile) })),
  };
}

function describe(sample) {
  const all = sample.files.flatMap((f) => f.segments);
  const sec = all.reduce((n, s) => n + (s.end - s.start), 0);
  const accounts = new Set(sample.files.flatMap((f) => f.segments.map((s) => `${f.id}_${s.speaker}`)));
  console.log(`  ${sample.candidates.length} fichiers lus, ${sample.candidates.reduce((n, c) => n + c.segments.length, 0)} segments utilisables au total`);
  console.log(`  echantillon : ${all.length} segments dans ${sample.files.length} fichiers, ${(sec / 3600).toFixed(2)} h, ${accounts.size} locuteurs (comptes)`);
  if (DRY_RUN) {
    // Apercu pour verifier le nettoyage du texte (a lire sans le copier ailleurs : ce sont de vraies conversations).
    for (const s of evenly(all, 4)) console.log(`  [${(s.end - s.start).toFixed(1)} s] ${s.text.slice(0, 140)}`);
  }
}

/** Identifiant de compte d'un locuteur : imp_srr_<numero du fichier>_<etiquette>, ex. imp_srr_33210_spk1. */
const loginOf = (file, speaker) => `imp_srr_${file.id}_${speaker}`.replace(/[^A-Za-z0-9_]/g, '_').slice(0, 50);

async function ensureLanguage(admin) {
  const existing = (await admin.call('GET', '/api/languages')).json.find((l) => l.code === LANGUAGE.code);
  if (existing) return existing;
  return (
    await admin.call('POST', '/api/languages', {
      code: LANGUAGE.code,
      name: LANGUAGE.name,
      dialect: null,
      script: LANGUAGE.script,
      direction: LANGUAGE.direction,
      status: 'WAITLIST',
      minValidators: LANGUAGE.minValidators,
    })
  ).json;
}

async function main() {
  checkSetup();
  console.log(`Serveur : ${BASE_URL}${DRY_RUN ? " (--dry-run : rien n'est ecrit)" : ''}`);
  if (process.argv.includes('--datasets-only')) return publishAll();
  if (process.argv.includes('--from-review')) {
    console.log('\nQualification des validateurs');
    await qualifyValidators(LANGUAGE.code);
    console.log('\nRelecture par les pairs');
    await reviewEverything(LANGUAGE.code);
    return publishAll();
  }
  if (!SEREER_DIR) fail('Definir SEREER_DIR (dossier "speech_dataset" du corpus sereer).');

  console.log('\n1. Lecture du corpus et choix de l\'echantillon');
  const sample = await loadSample();
  describe(sample);
  if (DRY_RUN) return;

  console.log('\n2. Langue et comptes');
  const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const language = await ensureLanguage(admin);
  console.log(`  langue ${language.code} (${language.status})`);
  for (const v of VALIDATORS) await register(v, 'CONTRIBUTOR', { quiet: true });
  const logins = new Set(sample.files.flatMap((f) => f.segments.map((s) => loginOf(f, s.speaker))));
  let created = 0;
  for (const login of logins) if (await register(login, 'CONTRIBUTOR', { quiet: true })) created++;
  console.log(`  ${created} compte(s) locuteur cree(s) sur ${logins.size}`);

  console.log('\n3. Campagne et phrases (admin)');
  const campaigns = (await admin.call('GET', '/api/campaigns')).json;
  if (campaigns.some((c) => c.title === CAMPAIGN_TITLE)) {
    fail(`la campagne "${CAMPAIGN_TITLE}" existe deja : import deja fait ? Utiliser --from-review ou --datasets-only.`);
  }
  const total = sample.files.reduce((n, f) => n + f.segments.length, 0);
  const campaign = (
    await admin.call('POST', '/api/campaigns', {
      title: CAMPAIGN_TITLE,
      description: 'Donnees de test importees localement (parole spontanee). Ne pas publier.',
      domain: DOMAIN,
      kind: 'OPEN',
      status: 'DRAFT',
      targetSentences: total,
      createdAt: new Date().toISOString(),
      language: { id: language.id },
    })
  ).json;
  const promptOf = new Map(); // "<fichier>#<debut>" -> id de phrase
  let n = 0;
  for (const f of sample.files) {
    for (const s of f.segments) {
      const prompt = (await admin.call('POST', '/api/prompts', { content: s.text, campaign: { id: campaign.id } })).json;
      promptOf.set(`${f.base}#${s.start}`, prompt.id);
      if (++n % 500 === 0) console.log(`  ${n}/${total} phrases...`);
    }
  }
  await admin.call('PATCH', `/api/campaigns/${campaign.id}`, { id: campaign.id, status: 'ACTIVE' });
  console.log(`  ${total} phrases, campagne active`);

  console.log('\n4. Envoi des enregistrements (un contributeur par locuteur)');
  const sessions = new Map(); // login -> client connecte (consentement deja donne)
  let sent = 0;
  for (const f of sample.files) {
    const wav = await readFile(path.join(f.dir, `${f.base}.wav`));
    assertSupportedWav(wav, f.base);
    for (const s of f.segments) {
      const login = loginOf(f, s.speaker);
      let session = sessions.get(login);
      // Jeton d'acces de 15 min : on se reconnecte apres 10 min. Le consentement n'est donne qu'a la premiere connexion.
      if (!session || Date.now() - session.at > 10 * 60 * 1000) {
        const api = await loggedIn(login, IMPORT_PASSWORD);
        if (!session) {
          await api.call('POST', '/api/consents', { kind: 'COLLECTION' }, { allow: [400, 409] });
          await completeTestProfile(api);
          await promoteTestContributor(login);
        }
        session = { api, at: Date.now() };
        sessions.set(login, session);
      }
      const user = session.api;
      const { audio, durationMs } = cutWav(wav, s.start, s.end);
      const form = new FormData();
      form.set('promptId', String(promptOf.get(`${f.base}#${s.start}`)));
      form.set('durationMs', String(durationMs));
      form.set('file', new Blob([audio], { type: 'audio/wav' }), `${f.base}_${s.start}.wav`);
      const { status } = await user.call('POST', '/api/me/recordings', undefined, { form, allow: [409] });
      if (status !== 409) sent++;
    }
    console.log(`  ${f.base} : ${f.segments.length} segments (${sent} envoyes au total)`);
  }

  console.log('\n5. Qualification des validateurs');
  await qualifyValidators(LANGUAGE.code);

  console.log('\n6. Relecture par les pairs (circuit normal ; les validateurs acceptent tout)');
  await reviewEverything(LANGUAGE.code);

  await publishAll();
  console.log('\nTermine.');
}

main().catch((e) => fail(e.message));
