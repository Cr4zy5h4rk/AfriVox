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

const DRY_RUN = process.argv.includes('--dry-run');
const LANGUAGE = 'wo';

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

/** Etape 7, relancable seule (--datasets-only) : voir publishDatasets dans lib.mjs. */
const publishAll = () =>
  publishDatasets(DATASETS, LANGUAGE, (d) => `Jeu de test (domaine ${d.domain}). Donnees importees localement, non publiables.`);

async function main() {
  checkSetup();
  console.log(`Serveur : ${BASE_URL}${DRY_RUN ? ' (--dry-run : rien n\'est ecrit)' : ''}`);
  if (process.argv.includes('--datasets-only')) {
    await publishAll();
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
    const already = existing.find((c) => c.title === title);
    if (already) {
      // Reprise apres une panne : les phrases ont ete creees dans l'ordre du corpus, on les reapparie par rang.
      const prompts = (await admin.call('GET', '/api/prompts?size=2000')).json
        .filter((p) => p.campaign?.id === already.id)
        .sort((a, b) => a.id - b.id);
      if (prompts.length !== list.length) fail(`la campagne "${title}" existe deja avec ${prompts.length} phrases au lieu de ${list.length} : a corriger a la main.`);
      list.forEach((r, i) => promptIds.set(r.filename, prompts[i].id));
      console.log(`  ${title} : reprise (${list.length} phrases deja en base)`);
      continue;
    }
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
    await completeTestProfile(user);
    await promoteTestContributor(`imp_${source}`);
    let sent = 0;
    let refused = 0;
    for (const r of list) {
      const form = new FormData();
      form.set('promptId', String(promptIds.get(r.filename)));
      form.set('durationMs', String(r.durationMs));
      form.set('file', new Blob([r.audio], { type: 'audio/wav' }), r.filename);
      const { status, json } = await user.call('POST', '/api/me/recordings', undefined, { form, allow: [400, 409] });
      // Un refus des controles qualite (error.qc*) est un resultat normal : on saute le fichier et on le compte.
      if (status === 400) {
        if (!JSON.stringify(json).includes('error.qc')) throw new Error(`envoi de ${r.filename} refuse : ${JSON.stringify(json).slice(0, 200)}`);
        refused++;
      } else if (status !== 409) sent++;
    }
    console.log(`  imp_${source} : ${sent}/${list.length} enregistrements envoyes, ${refused} refuses par les controles qualite`);
  }

  console.log('\n5. Qualification des validateurs');
  await qualifyValidators(LANGUAGE);

  console.log('\n6. Relecture par les pairs (circuit normal ; les validateurs acceptent tout)');
  await reviewEverything(LANGUAGE);

  await publishAll();

  console.log('\nTermine. Compte developpeur pour tester les modeles : imp_dev (mot de passe = IMPORT_PASSWORD).');
}

main().catch((e) => fail(e.message));
