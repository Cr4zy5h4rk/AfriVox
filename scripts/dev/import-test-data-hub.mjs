#!/usr/bin/env node
/**
 * DONNEES DE TEST du hub des developpeurs : developpeurs avec profil public, projets, discussions et demandes de
 * contact. Tout passe par l'API reelle (outils communs : lib.mjs) ; refuse tout serveur non local.
 *
 * Les comptes ont le prefixe "imp_hub_" (convention des comptes de test : masquables dans la liste des utilisateurs).
 * Les textes sont fictifs, les liens pointent vers example.org : rien ici n'est a publier.
 *
 * Usage (apres avoir demarre le serveur local) :
 *   ADMIN_USER=admin ADMIN_PASSWORD=... IMPORT_PASSWORD=... node scripts/dev/import-test-data-hub.mjs
 * Peut etre relance : ce qui existe deja n'est pas refait.
 */
import { Api, IMPORT_PASSWORD, checkSetup, loggedIn, register } from './lib.mjs';

checkSetup();

const DEVELOPERS = [
  { login: 'imp_hub_awa', bio: 'Reconnaissance vocale pour les langues d’Afrique de l’Ouest. Je publie mes modèles et mes outils d’évaluation.', url: 'https://example.org/awa', open: true },
  { login: 'imp_hub_moussa', bio: 'Ingénieur mobile : applications hors ligne pour les zones à faible connexion.', url: 'https://example.org/moussa', open: true },
  { login: 'imp_hub_fatou', bio: 'Chercheuse en traitement automatique des langues, intéressée par les corpus de parole spontanée.', url: 'https://example.org/fatou', open: false },
  { login: 'imp_hub_ibrahima', bio: 'Développeur backend, je construis des API de transcription pour les radios communautaires.', url: null, open: true },
  { login: 'imp_hub_aminata', bio: '', url: null, open: false },
];
// Developpeur sans profil public : montre qu'un profil prive n'apparait nulle part.
const PRIVATE_DEVELOPER = 'imp_hub_discret';
const ORGANIZATIONS = ['imp_hub_telco', 'imp_hub_radio'];

const PROJECTS = [
  { owner: 'imp_hub_awa', title: 'Dictée vocale en wolof', summary: 'Application web de dictée : on parle, le texte s’affiche. Utilise un modèle de reconnaissance entraîné sur les datasets de la plateforme. Démonstration en ligne et code ouvert.', repo: 'https://example.org/awa/dictee-wolof', demo: 'https://example.org/awa/dictee-demo', lang: 'wo', tags: ['asr', 'wolof', 'web'], model: 'Wolof ASR base' },
  { owner: 'imp_hub_awa', title: 'Outil d’évaluation WER', summary: 'Petit utilitaire en ligne de commande qui calcule le WER et le CER d’un fichier de prédictions, avec la même normalisation que le classement.', repo: 'https://example.org/awa/wer-tool', demo: null, lang: null, tags: ['wer', 'outil', 'python'] },
  { owner: 'imp_hub_moussa', title: 'Collecteur hors ligne', summary: 'Application mobile qui enregistre des phrases sans connexion et les envoie quand le réseau revient. Pensée pour les villages à couverture irrégulière.', repo: 'https://example.org/moussa/collecteur', demo: null, lang: null, tags: ['mobile', 'hors-ligne', 'pwa'] },
  { owner: 'imp_hub_moussa', title: 'Clavier vocal pour Android', summary: 'Prototype de clavier qui transforme la voix en texte pour le sereer et le wolof. Encore expérimental : les retours sont les bienvenus.', repo: 'https://example.org/moussa/clavier', demo: null, lang: 'srr', tags: ['android', 'asr', 'sereer'] },
  { owner: 'imp_hub_fatou', title: 'Analyse de la parole spontanée', summary: 'Notebook qui compare les erreurs de reconnaissance sur de la parole lue et de la parole spontanée, avec des figures reproductibles.', repo: 'https://example.org/fatou/spontanee', demo: null, lang: null, tags: ['recherche', 'analyse', 'notebook'] },
  { owner: 'imp_hub_ibrahima', title: 'API de transcription pour radios', summary: 'Service qui découpe un flux radio en segments et les transcrit. Conçu pour tourner sur un petit serveur.', repo: 'https://example.org/ibrahima/radio-api', demo: 'https://example.org/ibrahima/radio-demo', lang: 'wo', tags: ['api', 'asr', 'radio'] },
  { owner: 'imp_hub_discret', title: 'Prototype privé de synthèse vocale', summary: 'Expérience de synthèse vocale en wolof. Le profil de l’auteur est privé : seul le nom affiché du projet apparaît ici.', repo: null, demo: null, lang: 'wo', tags: ['tts', 'wolof'] },
];

const MODELS = [
  { owner: 'imp_hub_awa', name: 'Wolof ASR base', task: 'ASR', description: 'Modèle de base de reconnaissance vocale en wolof (donnée de test).', license: 'Apache-2.0', url: 'https://example.org/awa/wolof-asr-base', lang: 'wo' },
  { owner: 'imp_hub_ibrahima', name: 'Segmenteur radio', task: 'OTHER', description: 'Découpe un flux audio en segments de parole (donnée de test).', license: 'MIT', url: 'https://example.org/ibrahima/segmenteur', lang: null },
];

/** Discussion : [auteur, projet, texte, reponse eventuelle [auteur, texte]]. */
const DISCUSSION = [
  ['imp_hub_moussa', 'Dictée vocale en wolof', 'Très bon travail ! Est-ce que ça fonctionne avec un micro de téléphone bas de gamme ?', ['imp_hub_awa', 'Oui, j’ai testé avec un téléphone d’entrée de gamme : il vaut mieux parler à 15 cm du micro.']],
  ['imp_hub_fatou', 'Dictée vocale en wolof', 'Quel jeu de test avez-vous utilisé pour le WER annoncé ?', ['imp_hub_awa', 'Celui du classement de la plateforme, version 1.0.']],
  ['imp_hub_ibrahima', 'Collecteur hors ligne', 'La synchronisation reprend bien après une coupure en plein envoi ?', null],
  ['imp_hub_awa', 'API de transcription pour radios', 'Intéressant. Quelle charge supporte le serveur le plus petit ?', ['imp_hub_ibrahima', 'Environ trois flux en parallèle sur 2 Go de mémoire, d’après mes essais.']],
  ['imp_hub_aminata', 'Outil d’évaluation WER', 'Merci, je l’utilise pour comparer mes sorties avant de soumettre.', null],
];

/** Demandes de contact : [organisation, developpeur, message, moyen de joindre, decision (null = en attente)]. */
const CONTACTS = [
  ['imp_hub_telco', 'imp_hub_awa', 'Bonjour, notre équipe développe un serveur vocal en wolof et cherche une personne expérimentée en reconnaissance vocale pour une mission de trois mois.', 'recrutement@example.org', true],
  ['imp_hub_telco', 'imp_hub_moussa', 'Bonjour, nous aimerions échanger sur votre collecteur hors ligne pour un déploiement en zone rurale.', 'recrutement@example.org', null],
  ['imp_hub_radio', 'imp_hub_ibrahima', 'Bonjour, nous diffusons sur plusieurs radios communautaires et cherchons à transcrire nos émissions. Pouvons-nous en discuter ?', 'direction@example.org', false],
];

const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const sessions = new Map();
async function as(login) {
  if (!sessions.has(login)) sessions.set(login, await loggedIn(login, IMPORT_PASSWORD));
  return sessions.get(login);
}

console.log('Comptes');
for (const d of DEVELOPERS) await register(d.login, 'DEVELOPER');
await register(PRIVATE_DEVELOPER, 'DEVELOPER');
for (const o of ORGANIZATIONS) await register(o, 'ORGANIZATION');

// Les langues demandees doivent exister ; sinon le projet est cree sans langue (et on le dit).
const anon = new Api();
const languages = (await anon.call('GET', '/api/public/languages')).json.map((l) => l.code);
const langOrNull = (code) => {
  if (code && !languages.includes(code)) {
    console.log(`  langue "${code}" absente de la base : projet cree sans langue`);
    return null;
  }
  return code;
};

console.log('\nProfils publics');
for (const d of DEVELOPERS) {
  const api = await as(d.login);
  await api.call('PUT', '/api/me/developer-profile', {
    displayName: d.login,
    bio: d.bio || null,
    profileUrl: d.url,
    publicProfile: d.bio !== '',
    openToWork: d.open,
  });
  console.log(`  ${d.login} : ${d.bio === '' ? 'privé' : 'public'}${d.open ? ', ouvert aux opportunités' : ''}`);
}
await (await as(PRIVATE_DEVELOPER)).call('GET', '/api/me/developer-profile'); // crée le profil, qui reste privé

console.log('\nModèles');
const modelIds = new Map();
for (const m of MODELS) {
  const api = await as(m.owner);
  let mine = (await api.call('GET', '/api/me/models')).json.find((x) => x.name === m.name);
  if (!mine) {
    mine = (
      await api.call('POST', '/api/me/models', {
        name: m.name, task: m.task, description: m.description, license: m.license, url: m.url,
        languageCode: langOrNull(m.lang) ?? '', sha256: '', trainedOnVersionId: null, otherVersionIds: [], usage: null,
      })
    ).json;
    console.log(`  ${m.name} publié`);
  }
  modelIds.set(m.name, mine.id);
}

console.log('\nProjets');
const projectIds = new Map();
for (const p of PROJECTS) {
  const api = await as(p.owner);
  let existing = (await api.call('GET', '/api/me/projects')).json.find((x) => x.title === p.title);
  if (!existing) {
    existing = (
      await api.call('POST', '/api/me/projects', {
        title: p.title, summary: p.summary, repoUrl: p.repo ?? '', demoUrl: p.demo ?? '',
        languageCode: langOrNull(p.lang) ?? '', tags: p.tags, modelId: p.model ? (modelIds.get(p.model) ?? null) : null,
      })
    ).json;
    console.log(`  ${p.title} (${p.owner})`);
  }
  projectIds.set(p.title, existing.id);
}

console.log('\nDiscussions');
let comments = 0;
for (const [author, title, text, reply] of DISCUSSION) {
  const projectId = projectIds.get(title);
  const first = (await anon.call('GET', `/api/public/projects/${projectId}/comments?size=50`)).json;
  if (first.items.some((c) => c.body === text)) continue;
  const posted = (await (await as(author)).call('POST', `/api/community/projects/${projectId}/comments`, { body: text })).json;
  comments++;
  if (reply) {
    await (await as(reply[0])).call('POST', `/api/community/projects/${projectId}/comments`, { body: reply[1], parentId: posted.id });
    comments++;
  }
  await pause(300); // limite de rythme : 5 commentaires par minute et par auteur
}
console.log(`  ${comments} commentaire(s) ajouté(s)`);

console.log('\nDemandes de contact');
const profileIds = new Map();
for (const d of DEVELOPERS) {
  profileIds.set(d.login, (await (await as(d.login)).call('GET', '/api/me/developer-profile')).json.id);
}
let requests = 0;
for (const [org, dev, message, contact, decision] of CONTACTS) {
  const orgApi = await as(org);
  const already = (await orgApi.call('GET', '/api/community/contact-requests/sent')).json.some((r) => r.recipientId === profileIds.get(dev));
  if (already) continue;
  await orgApi.call('POST', `/api/community/developers/${profileIds.get(dev)}/contact`, { message, contact });
  requests++;
  if (decision !== null) {
    const inbox = (await (await as(dev)).call('GET', '/api/community/contact-requests/received')).json;
    const request = inbox.find((r) => r.senderName === org && r.status === 'PENDING');
    await (await as(dev)).call('POST', `/api/community/contact-requests/${request.id}/${decision ? 'accept' : 'decline'}`);
  }
}
console.log(`  ${requests} demande(s) envoyée(s)`);

console.log(`\nTerminé. Comptes de test (mot de passe = IMPORT_PASSWORD) : ${[...DEVELOPERS.map((d) => d.login), PRIVATE_DEVELOPER, ...ORGANIZATIONS].join(', ')}`);
