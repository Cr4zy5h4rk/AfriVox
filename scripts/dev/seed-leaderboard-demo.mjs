/**
 * DONNEES DE TEST (serveur local uniquement) : de quoi voir le classement se remplir.
 *
 *   1. publie une version 2.0 de quelques datasets sereer (les anciennes versions n'ont pas de jeu de test : le texte
 *      de leur partie test est lisible dans l'archive) ;
 *   2. pour des modeles ASR sereer de test, fabrique un CSV de predictions et l'envoie par l'API reelle.
 *
 * ATTENTION : les predictions sont SIMULEES. Elles partent de la vraie transcription et y introduisent des erreurs
 * a un taux choisi par modele (voir RATES). Aucun modele n'a tourne : les scores affiches ne sont PAS des resultats de
 * modeles, ils servent uniquement a montrer l'interface. Ne jamais presenter ces chiffres comme reels.
 *
 * Usage : ADMIN_USER=... ADMIN_PASSWORD=... IMPORT_PASSWORD=x node scripts/dev/seed-leaderboard-demo.mjs
 */
import { ADMIN_PASSWORD, ADMIN_USER, BASE_URL, checkSetup, loggedIn } from './lib.mjs';

// Datasets recevant une version 2.0 (assez de locuteurs pour avoir un jeu de test).
const TO_PUBLISH = [
  { slug: 'agriculture-sereer-starter', max: null },
  { slug: 'agriculture-sereer-plus', max: null },
];

// Taux d'erreur simule, par modele (part de mots modifies, supprimes ou inseres).
const RATES = {
  'test-sereer-asr-tiny': 0.62,
  'test-sereer-asr-small': 0.41,
  'test-sereer-asr-base': 0.27,
  'test-sereer-asr-research': 0.34,
  'test-wolof-sereer-multilingual-asr': 0.48,
};

checkSetup();
const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);

console.log('1. Versions 2.0');
const datasets = (await admin.call('GET', '/api/datasets')).json;
for (const t of TO_PUBLISH) {
  const dataset = datasets.find((d) => d.slug === t.slug);
  if (!dataset) {
    console.log(`  ${t.slug} : introuvable`);
    continue;
  }
  const { status, json } = await admin.call(
    'POST',
    `/api/admin/datasets/${dataset.id}/versions`,
    { label: '2.0', license: 'RESEARCH', pricePerHourFcfa: null, maxRecordings: t.max },
    { allow: [400, 500] },
  );
  console.log(
    status === 400 || status === 500
      ? `  ${t.slug} 2.0 : ${json?.message ?? json?.detail ?? status}`
      : `  ${t.slug} 2.0 : ${json.hours?.toFixed(3)} h, ${json.speakers} locuteur(s)`,
  );
}

console.log('\n2. Jeux de test');
const testSets = (await admin.call('GET', '/api/public/test-sets?language=srr')).json;
if (testSets.length === 0) throw new Error('aucun jeu de test sereer : la version 2.0 n\'a pas de partie test.');
for (const s of testSets) console.log(`  ${s.slug} ${s.version} : ${s.sentences} phrases`);

// Transcriptions reelles par identifiant d'enregistrement (CRUD admin ; le texte de reference n'est pas expose ailleurs).
// Le CRUD des enregistrements ne donne que l'identifiant de la phrase : le texte vient de /api/prompts.
const promptText = new Map();
for (let page = 0; ; page++) {
  const { json } = await admin.call('GET', `/api/prompts?page=${page}&size=500`);
  if (!json || json.length === 0) break;
  for (const p of json) promptText.set(p.id, p.content ?? '');
  if (json.length < 500) break;
}
const transcripts = new Map();
for (let page = 0; ; page++) {
  const { json } = await admin.call('GET', `/api/recordings?page=${page}&size=500`);
  if (!json || json.length === 0) break;
  for (const r of json) transcripts.set(r.id, r.transcript ?? promptText.get(r.prompt?.id) ?? '');
  if (json.length < 500) break;
}
const withText = [...transcripts.values()].filter((t) => t.trim() !== '').length;
console.log(`  ${transcripts.size} enregistrements, ${withText} avec un texte`);
if (withText === 0) throw new Error('aucun texte de reference lu : arret (les scores seraient faux).');

/** Generateur pseudo-aleatoire deterministe (le meme CSV a chaque lancement). */
function rng(seed) {
  let x = seed >>> 0 || 1;
  return () => ((x = (Math.imul(x, 1664525) + 1013904223) >>> 0) / 2 ** 32);
}

function corrupt(text, rate, random) {
  const out = [];
  for (const word of text.split(/\s+/).filter(Boolean)) {
    const r = random();
    if (r < rate / 3) continue; // suppression
    if (r < (2 * rate) / 3) out.push(word.split('').reverse().join('') || word); // substitution
    else out.push(word);
    if (random() < rate / 6) out.push('xx'); // insertion
  }
  return out.join(' ');
}

console.log('\n3. Predictions simulees');
const models = (await admin.call('GET', '/api/public/models?size=100')).json.items;
const testSet = testSets[0];
for (const [name, rate] of Object.entries(RATES)) {
  const model = models.find((m) => m.name === name);
  if (!model) {
    console.log(`  ${name} : introuvable`);
    continue;
  }
  const template = await (await fetch(`${BASE_URL}/api/public/test-sets/${testSet.versionId}/template.csv`)).text();
  const ids = template.split('\n').slice(1).map((l) => l.split(',')[0]).filter(Boolean);
  const random = rng([...name].reduce((n, c) => n * 31 + c.charCodeAt(0), 7));
  const lines = ['id,text'];
  for (const id of ids) {
    const text = corrupt(transcripts.get(Number(id)) ?? '', rate, random).replaceAll('"', '""');
    lines.push(`${id},"${text}"`);
  }
  const form = new FormData();
  form.set('testSetId', String(testSet.versionId));
  form.set('file', new Blob([lines.join('\n')], { type: 'text/csv' }), 'predictions.csv');
  const res = await fetch(`${BASE_URL}/api/me/models/${model.id}/evaluations`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${admin.token}` },
    body: form,
  });
  const body = await res.json().catch(() => ({}));
  console.log(
    res.ok ? `  ${name} : WER ${body.wer} %, CER ${body.cer} % (simule, taux ${rate})` : `  ${name} : ${res.status} ${body.message ?? ''}`,
  );
}
