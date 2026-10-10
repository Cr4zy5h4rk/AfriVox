#!/usr/bin/env node
/**
 * Import de DONNEES DE TEST en local, par l'API reelle (aucun acces direct a la base).
 *
 * A quoi ca sert : garnir une base de developpement avec des enregistrements deja faits (corpus wolof telecharge sur
 * Hugging Face, usage local uniquement, jamais publie ni commite : le dossier data/ est ignore par Git) pour voir
 * l'interface avec du contenu : campagnes par domaine, avis des pairs, datasets Starter/Plus/Pro.
 *
 * Ce que le script fait, dans l'ordre :
 *   1. cree un compte contributeur par source (kallama, alffa...), 4 validateurs et 1 developpeur (tous prefixes "imp_") ;
 *   2. cree une campagne par source (domaine = agriculture, quotidien, transport) dont les phrases sont les transcriptions ;
 *   3. envoie chaque fichier audio comme enregistrement du contributeur de sa source (statut SUBMITTED) ;
 *   4. fait qualifier les validateurs (acceptation par l'admin) puis relire chaque enregistrement par les pairs, par le
 *      circuit normal (2 avis concordants => VALIDATED, taches pieges comprises) ;
 *   5. cree les datasets et publie une version de chaque (les plans se distinguent par la taille : maxRecordings).
 *
 * Limites a connaitre :
 *   - le script ne reecoute rien : les validateurs acceptent tout, car les transcriptions du corpus sont considerees
 *     comme exactes. Ce n'est PAS une validation humaine ; ne jamais presenter ces chiffres comme tels ;
 *   - une source n'est pas un locuteur : les decoupages train/dev/test se font entre sources (suffisant pour tester) ;
 *   - ne fonctionne que contre un serveur local (localhost) : refus net sinon.
 *
 * Usage (serveur lance en profil dev) :
 *   ADMIN_USER=admin ADMIN_PASSWORD=... IMPORT_PASSWORD=... node scripts/dev/import-test-data.mjs [--dry-run]
 * Pour ne refaire que l'etape 7 (datasets et versions) apres une panne : ajouter --datasets-only.
 *
 * Aucune dependance npm (Node 20+ : fetch, FormData, Blob integres).
 */
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';
const ADMIN_USER = process.env.ADMIN_USER;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
const IMPORT_PASSWORD = process.env.IMPORT_PASSWORD; // mot de passe commun des comptes "imp_"
const DRY_RUN = process.argv.includes('--dry-run');
const LANGUAGE = 'wo';
const CONSENT_VERSION = process.env.CONSENT_VERSION ?? '2026-10-v1';

const here = path.dirname(fileURLToPath(import.meta.url));
const DATA = path.resolve(here, '../../data');
// Les fichiers urban_bus (MP3 deguises) et fleurs (WAV float32) ont ete convertis dans le second dossier.
const ORIGINAL_DIR = path.join(DATA, 'wolof_sample_400');
const CONVERTED_DIR = path.join(DATA, 'wolof_sample_400_wav');

/** Une source du corpus -> un domaine de campagne et un compte contributeur. */
const SOURCES = {
  kallama: { domain: 'agriculture', dir: ORIGINAL_DIR },
  alffa: { domain: 'quotidien', dir: ORIGINAL_DIR },
  fleurs: { domain: 'quotidien', dir: CONVERTED_DIR },
  urban_bus: { domain: 'transport', dir: CONVERTED_DIR },
};

const VALIDATORS = ['imp_val1', 'imp_val2', 'imp_val3', 'imp_val4'];
const DEVELOPER = 'imp_dev';

/**
 * Datasets a creer. Les plans se distinguent par la taille (emboitee : Starter < Plus < Pro) ; un tarif n'est
 * necessaire que pour les plans payants. Tarifs = VALEURS DE TEST, a valider avant toute utilisation reelle.
 */
const DATASETS = [
  { slug: 'agriculture-wolof-starter', title: 'Agriculture (wolof) - Starter', domain: 'agriculture', plan: 'STARTER', max: 20 },
  { slug: 'agriculture-wolof-pro', title: 'Agriculture (wolof) - Pro', domain: 'agriculture', plan: 'PRO', max: null, price: 50000 },
  { slug: 'transport-wolof-starter', title: 'Transport (wolof) - Starter', domain: 'transport', plan: 'STARTER', max: 15 },
  { slug: 'transport-wolof-plus', title: 'Transport (wolof) - Plus', domain: 'transport', plan: 'PLUS', max: 40, price: 30000 },
  { slug: 'transport-wolof-pro', title: 'Transport (wolof) - Pro', domain: 'transport', plan: 'PRO', max: null, price: 50000 },
  { slug: 'quotidien-wolof-starter', title: 'Vie quotidienne (wolof) - Starter', domain: 'quotidien', plan: 'STARTER', max: 30 },
  { slug: 'quotidien-wolof-plus', title: 'Vie quotidienne (wolof) - Plus', domain: 'quotidien', plan: 'PLUS', max: 100, price: 30000 },
  { slug: 'quotidien-wolof-pro', title: 'Vie quotidienne (wolof) - Pro', domain: 'quotidien', plan: 'PRO', max: null, price: 50000 },
];

// ---------------------------------------------------------------------------------------------------------------

function fail(message) {
  console.error(`\nERREUR : ${message}`);
  process.exit(1);
}

function checkSetup() {
  const host = new URL(BASE_URL).hostname;
  if (!['localhost', '127.0.0.1', '::1', '[::1]'].includes(host)) {
    fail(`BASE_URL (${BASE_URL}) n'est pas local : ce script n'est fait que pour le developpement.`);
  }
  if (!ADMIN_USER || !ADMIN_PASSWORD || !IMPORT_PASSWORD) {
    fail('Definir ADMIN_USER, ADMIN_PASSWORD et IMPORT_PASSWORD (mot de passe commun des comptes imp_*, 4 caracteres min.).');
  }
}

/** Lecteur CSV minimal : champs entre guillemets, "" pour un guillemet, retours a la ligne dans un champ. */
function parseCsv(text) {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') {
        field += '"';
        i++;
      } else if (c === '"') {
        quoted = false;
      } else {
        field += c;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ',') {
      row.push(field);
      field = '';
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(field);
      field = '';
      if (row.some((v) => v !== '')) rows.push(row);
      row = [];
    } else {
      field += c;
    }
  }
  row.push(field);
  if (row.some((v) => v !== '')) rows.push(row);
  const [header, ...body] = rows;
  return body.map((r) => Object.fromEntries(header.map((h, i) => [h.trim(), (r[i] ?? '').trim()])));
}

/** Duree en ms d'un WAV PCM (lue dans l'en-tete) ; null si ce n'est pas un WAV PCM 16 bits lisible. */
function wavDurationMs(buf) {
  if (buf.length < 44 || buf.toString('ascii', 0, 4) !== 'RIFF' || buf.toString('ascii', 8, 12) !== 'WAVE') return null;
  let offset = 12;
  let byteRate = 0;
  while (offset + 8 <= buf.length) {
    const id = buf.toString('ascii', offset, offset + 4);
    const size = buf.readUInt32LE(offset + 4);
    if (id === 'fmt ') byteRate = buf.readUInt32LE(offset + 16);
    if (id === 'data') return byteRate > 0 ? Math.round((Math.min(size, buf.length - offset - 8) / byteRate) * 1000) : null;
    offset += 8 + size + (size % 2);
  }
  return null;
}

class Api {
  token = null;

  async call(method, url, body, { form, allow = [] } = {}) {
    const headers = {};
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    let payload;
    if (form) {
      payload = form;
    } else if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      payload = JSON.stringify(body);
    }
    const res = await fetch(BASE_URL + url, { method, headers, body: payload });
    const text = await res.text();
    if (!res.ok && !allow.includes(res.status)) {
      throw new Error(`${method} ${url} -> ${res.status} ${text.slice(0, 300)}`);
    }
    let json = null;
    try {
      json = text ? JSON.parse(text) : null;
    } catch {
      /* corps non JSON */
    }
    return { status: res.status, json };
  }

  async login(username, password) {
    const { json } = await this.call('POST', '/api/authenticate', { username, password });
    this.token = json.id_token;
  }
}

/** Un compte = un client avec son propre jeton (les jetons durent 15 min : on se reconnecte par phase). */
async function loggedIn(username, password) {
  const api = new Api();
  await api.login(username, password);
  return api;
}

async function register(login, role) {
  const { status } = await new Api().call(
    'POST',
    '/api/register',
    { login, email: `${login}@example.test`, password: IMPORT_PASSWORD, langKey: 'fr', role, consentVersion: CONSENT_VERSION },
    { allow: [400] },
  );
  console.log(`  compte ${login} (${role}) : ${status === 400 ? 'existe deja' : 'cree'}`);
}

// ---------------------------------------------------------------------------------------------------------------

async function loadRows() {
  const csv = parseCsv(await readFile(path.join(ORIGINAL_DIR, 'metadata.csv'), 'utf8'));
  const rows = [];
  for (const r of csv) {
    const source = SOURCES[r.source];
    if (!source) {
      console.warn(`  source inconnue ignoree : ${r.source} (${r.filename})`);
      continue;
    }
    const audio = await readFile(path.join(source.dir, r.filename)).catch(() => null);
    const durationMs = audio ? wavDurationMs(audio) : null;
    if (!audio || durationMs === null || durationMs < 500 || durationMs > 30000 || !r.sentence || r.sentence.length > 500) {
      console.warn(`  ignore (audio/phrase inacceptable) : ${r.filename}`);
      continue;
    }
    rows.push({ ...r, audio, durationMs });
  }
  return rows;
}

/**
 * Etape 7, relancable seule (--datasets-only) : cree les datasets manquants puis publie la version 1.0 de ceux qui n'en
 * ont pas encore (une version existante n'est jamais refaite : elle est immuable).
 */
async function publishDatasets() {
  console.log('\n7. Datasets et versions (admin)');
  const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const language = (await admin.call('GET', '/api/languages')).json.find((l) => l.code === LANGUAGE);
  if (!language) fail(`langue "${LANGUAGE}" introuvable en base.`);
  const existing = (await admin.call('GET', '/api/datasets')).json;
  for (const d of DATASETS) {
    let dataset = existing.find((x) => x.slug === d.slug);
    if (!dataset) {
      dataset = (
        await admin.call('POST', '/api/datasets', {
          slug: d.slug,
          title: d.title,
          description: `Jeu de test (domaine ${d.domain}). Donnees importees localement, non publiables.`,
          domain: d.domain,
          plan: d.plan,
          language: { id: language.id },
        })
      ).json;
    }
    const { status, json } = await admin.call(
      'POST',
      `/api/admin/datasets/${dataset.id}/versions`,
      { label: '1.0', license: 'RESEARCH', pricePerHourFcfa: d.price ?? null, maxRecordings: d.max },
      { allow: [400, 500] },
    );
    if (status === 400) {
      console.log(`  ${d.slug} : version 1.0 deja publiee (ou refusee : ${json?.message ?? json?.detail ?? '?'})`);
    } else if (status === 500) {
      console.log(`  ${d.slug} : ECHEC de publication : ${json?.detail ?? 'erreur serveur'}`);
    } else {
      console.log(`  ${d.slug} (${d.plan}) : version 1.0, ${json.hours?.toFixed(3)} h, ${json.speakers} locuteur(s)`);
    }
  }
}

async function main() {
  checkSetup();
  console.log(`Serveur : ${BASE_URL}${DRY_RUN ? ' (--dry-run : rien n\'est ecrit)' : ''}`);
  if (process.argv.includes('--datasets-only')) {
    await publishDatasets();
    return;
  }

  console.log('\n1. Lecture du corpus');
  const rows = await loadRows();
  const bySource = Object.groupBy(rows, (r) => r.source);
  for (const [s, list] of Object.entries(bySource)) {
    const sec = list.reduce((n, r) => n + r.durationMs, 0) / 1000;
    console.log(`  ${s}: ${list.length} fichiers, ${sec.toFixed(0)} s -> domaine ${SOURCES[s].domain}`);
  }
  if (DRY_RUN) return;

  console.log('\n2. Comptes');
  for (const s of Object.keys(bySource)) await register(`imp_${s}`, 'CONTRIBUTOR');
  for (const v of VALIDATORS) await register(v, 'CONTRIBUTOR');
  await register(DEVELOPER, 'DEVELOPER');

  console.log('\n3. Campagnes et phrases (admin)');
  const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const languages = (await admin.call('GET', '/api/languages')).json;
  const language = languages.find((l) => l.code === LANGUAGE);
  if (!language) fail(`langue "${LANGUAGE}" introuvable en base.`);
  const existing = (await admin.call('GET', '/api/campaigns')).json;
  const promptIds = new Map(); // nom de fichier -> id de phrase
  for (const [source, list] of Object.entries(bySource)) {
    const title = `Corpus de test (${source})`;
    if (existing.some((c) => c.title === title)) fail(`la campagne "${title}" existe deja : import deja fait ? (rien n'a ete envoye pour cette source)`);
    const campaign = (
      await admin.call('POST', '/api/campaigns', {
        title,
        description: `Donnees de test importees localement (source ${source}). Ne pas publier.`,
        domain: SOURCES[source].domain,
        kind: 'OPEN',
        status: 'DRAFT',
        targetSentences: list.length,
        createdAt: new Date().toISOString(),
        language: { id: language.id },
      })
    ).json;
    for (const r of list) {
      const prompt = (await admin.call('POST', '/api/prompts', { content: r.sentence, campaign: { id: campaign.id } })).json;
      promptIds.set(r.filename, prompt.id);
    }
    await admin.call('PATCH', `/api/campaigns/${campaign.id}`, { id: campaign.id, status: 'ACTIVE' });
    console.log(`  ${title} : ${list.length} phrases, campagne active`);
  }

  console.log('\n4. Envoi des enregistrements (un contributeur par source)');
  for (const [source, list] of Object.entries(bySource)) {
    const user = await loggedIn(`imp_${source}`, IMPORT_PASSWORD);
    await user.call('POST', '/api/consents', { kind: 'COLLECTION' }, { allow: [400, 409] });
    let sent = 0;
    for (const r of list) {
      const form = new FormData();
      form.set('promptId', String(promptIds.get(r.filename)));
      form.set('durationMs', String(r.durationMs));
      form.set('file', new Blob([r.audio], { type: 'audio/wav' }), r.filename);
      const { status } = await user.call('POST', '/api/me/recordings', undefined, { form, allow: [409] });
      if (status !== 409) sent++;
    }
    console.log(`  imp_${source} : ${sent}/${list.length} enregistrements envoyes`);
  }

  console.log('\n5. Qualification des validateurs');
  for (const v of VALIDATORS) {
    const api = await loggedIn(v, IMPORT_PASSWORD);
    await api.call('POST', '/api/me/validation/qualifications', { languageCode: LANGUAGE }, { allow: [400, 409] });
  }
  const adminAgain = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const pending = (await adminAgain.call('GET', '/api/admin/validation/qualifications')).json;
  // Seulement nos 4 validateurs (le nom affiche d'un profil est son login) : jamais les demandes des autres comptes.
  const ours = pending.filter((p) => p.languageCode === LANGUAGE && VALIDATORS.includes(p.displayName));
  for (const q of ours) {
    await adminAgain.call('POST', `/api/admin/validation/qualifications/${q.id}/decision`, { approve: true });
  }
  console.log(`  ${ours.length} qualification(s) acceptee(s) sur ${pending.length} en attente`);

  console.log('\n6. Relecture par les pairs (circuit normal ; les validateurs acceptent tout)');
  let reviews = 0;
  const clients = await Promise.all(VALIDATORS.map((v) => loggedIn(v, IMPORT_PASSWORD)));
  let active = clients.map((api, i) => ({ api, name: VALIDATORS[i] }));
  while (active.length > 0) {
    const stillActive = [];
    for (const v of active) {
      const next = await v.api.call('GET', `/api/me/validation/next?language=${LANGUAGE}`);
      if (next.status === 204 || !next.json) continue; // plus rien a relire pour ce validateur
      await v.api.call('POST', '/api/me/validation/reviews', {
        recordingId: next.json.recordingId,
        verdict: 'ACCEPT',
        audioOk: true,
        pronunciationOk: true,
        transcriptOk: true,
      });
      reviews++;
      stillActive.push(v);
    }
    active = stillActive;
  }
  console.log(`  ${reviews} avis enregistres`);

  await publishDatasets();

  console.log('\nTermine. Compte developpeur pour tester les modeles : imp_dev (mot de passe = IMPORT_PASSWORD).');
}

main().catch((e) => fail(e.message));
