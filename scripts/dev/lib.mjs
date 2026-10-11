/**
 * Outils communs aux scripts d'import de DONNEES DE TEST (voir import-test-data.mjs et import-test-data-sereer.mjs).
 * Tout passe par l'API reelle ; aucun acces direct a la base. Aucune dependance npm (Node 20+).
 *
 * Securite : refuse de travailler contre un serveur qui n'est pas local, et ne contient aucun mot de passe : ils
 * viennent de l'environnement (ADMIN_USER, ADMIN_PASSWORD, IMPORT_PASSWORD).
 */

export const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';
export const ADMIN_USER = process.env.ADMIN_USER;
export const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
export const IMPORT_PASSWORD = process.env.IMPORT_PASSWORD; // mot de passe commun des comptes "imp_"
export const CONSENT_VERSION = process.env.CONSENT_VERSION ?? '2026-10-v1';

/** Les 4 validateurs de test, partages par tous les corpus. */
export const VALIDATORS = ['imp_val1', 'imp_val2', 'imp_val3', 'imp_val4'];

export function fail(message) {
  console.error(`\nERREUR : ${message}`);
  process.exit(1);
}

export function checkSetup() {
  const host = new URL(BASE_URL).hostname;
  if (!['localhost', '127.0.0.1', '::1', '[::1]'].includes(host)) {
    fail(`BASE_URL (${BASE_URL}) n'est pas local : ces scripts ne sont faits que pour le developpement.`);
  }
  if (!ADMIN_USER || !ADMIN_PASSWORD || !IMPORT_PASSWORD) {
    fail('Definir ADMIN_USER, ADMIN_PASSWORD et IMPORT_PASSWORD (mot de passe commun des comptes imp_*, 4 caracteres min.).');
  }
}

export class Api {
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
export async function loggedIn(username, password) {
  const api = new Api();
  await api.login(username, password);
  return api;
}

/** Cree un compte ; renvoie vrai s'il vient d'etre cree, faux s'il existait deja. */
export async function register(login, role, { quiet = false } = {}) {
  const { status } = await new Api().call(
    'POST',
    '/api/register',
    {
      login,
      email: `${login}@example.test`,
      password: IMPORT_PASSWORD,
      langKey: 'fr',
      role,
      consentVersion: CONSENT_VERSION,
      // Un contributeur confirme sa majorite et declare sa langue maternelle ; "other" convient aux comptes de test.
      adult: true,
      nativeLanguage: 'other',
    },
    { allow: [400] },
  );
  if (!quiet) console.log(`  compte ${login} (${role}) : ${status === 400 ? 'existe deja' : 'cree'}`);
  return status !== 400;
}

/**
 * Periode d'essai : un nouveau contributeur est plafonne a quelques envois tant que l'admin ne l'a pas promu. Les
 * comptes de test en envoient des centaines, on les passe donc en ACTIVE (decision de l'admin, par l'API normale).
 * A appeler une fois le profil cree (apres le consentement). Le nom affiche d'un profil est son login.
 */
let adminSession = null;
export async function promoteTestContributor(login) {
  if (!adminSession || Date.now() - adminSession.at > 10 * 60 * 1000) {
    adminSession = { api: await loggedIn(ADMIN_USER, ADMIN_PASSWORD), at: Date.now() };
  }
  const { api } = adminSession;
  for (let page = 0; ; page++) {
    const { json } = await api.call('GET', `/api/admin/contributors?status=TRIAL&size=100&page=${page}`);
    const found = json.find((c) => c.displayName === login);
    if (found) {
      await api.call('POST', `/api/admin/contributors/${found.id}/status?value=ACTIVE`);
      return;
    }
    if (json.length < 100) return; // plus de page : deja ACTIVE, ou introuvable
  }
}

/** Demande la qualification de nos validateurs pour une langue, puis l'accepte (jamais celles des autres comptes). */
export async function qualifyValidators(languageCode) {
  for (const v of VALIDATORS) {
    const api = await loggedIn(v, IMPORT_PASSWORD);
    await api.call('POST', '/api/me/validation/qualifications', { languageCode }, { allow: [400, 409] });
  }
  const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const pending = (await admin.call('GET', '/api/admin/validation/qualifications')).json;
  // Le nom affiche d'un profil est son login : on n'accepte que nos 4 validateurs.
  const ours = pending.filter((p) => p.languageCode === languageCode && VALIDATORS.includes(p.displayName));
  for (const q of ours) {
    await admin.call('POST', `/api/admin/validation/qualifications/${q.id}/decision`, { approve: true });
  }
  console.log(`  ${ours.length} qualification(s) acceptee(s) sur ${pending.length} en attente`);
}

/**
 * Relecture par les pairs, par le circuit normal : chaque validateur demande une tache puis l'accepte, jusqu'a ce
 * qu'il n'y ait plus rien a relire. ATTENTION : rien n'est reecoute, tout est accepte (donnees de test).
 */
export async function reviewEverything(languageCode) {
  const clients = await Promise.all(VALIDATORS.map((v) => loggedIn(v, IMPORT_PASSWORD)));
  let active = clients;
  let reviews = 0;
  while (active.length > 0) {
    const stillActive = [];
    for (const api of active) {
      const next = await api.call('GET', `/api/me/validation/next?language=${languageCode}`);
      if (next.status === 204 || !next.json) continue; // plus rien a relire pour ce validateur
      await api.call('POST', '/api/me/validation/reviews', {
        recordingId: next.json.recordingId,
        verdict: 'ACCEPT',
        audioOk: true,
        pronunciationOk: true,
        transcriptOk: true,
      });
      reviews++;
      stillActive.push(api);
    }
    active = stillActive;
    // Les jetons durent 15 min : sur une longue relecture on se reconnecte regulierement.
    if (reviews > 0 && reviews % 1500 < VALIDATORS.length) {
      active = await Promise.all(active.map((_, i) => loggedIn(VALIDATORS[i], IMPORT_PASSWORD)));
    }
  }
  console.log(`  ${reviews} avis enregistres`);
}

/**
 * Cree les datasets manquants puis publie la version 1.0 de ceux qui n'en ont pas encore (une version existante
 * n'est jamais refaite : elle est immuable). `datasets` : { slug, title, domain, plan, max, price? }.
 */
export async function publishDatasets(datasets, languageCode, description) {
  console.log('\nDatasets et versions (admin)');
  const admin = await loggedIn(ADMIN_USER, ADMIN_PASSWORD);
  const language = (await admin.call('GET', '/api/languages')).json.find((l) => l.code === languageCode);
  if (!language) fail(`langue "${languageCode}" introuvable en base.`);
  const existing = (await admin.call('GET', '/api/datasets')).json;
  for (const d of datasets) {
    let dataset = existing.find((x) => x.slug === d.slug);
    if (!dataset) {
      dataset = (
        await admin.call('POST', '/api/datasets', {
          slug: d.slug,
          title: d.title,
          description: description(d),
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
