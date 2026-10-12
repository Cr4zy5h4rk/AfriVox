#!/usr/bin/env node
// Inventaire des licences des dependances (Maven et npm) -> LICENSES-THIRD-PARTY.md
//
// Usage (depuis la racine du depot AfriVox, JAVA_HOME defini) :
//   node scripts/licenses/generate.mjs [--online] [--frontend ../Afrivox_Frontend] [--out LICENSES-THIRD-PARTY.md]
//
// Sans dependance externe. Les licences sont LUES (poms Maven du depot local, package-lock.json et
// node_modules), jamais devinees : une licence absente ou illisible est signalee « inconnue » et doit etre
// verifiee a la main. Le classement (permissive / copyleft faible / copyleft fort) est une aide : la decision
// finale appartient a l'equipe.
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const args = process.argv.slice(2);
const flag = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
};
const online = args.includes('--online');
const root = process.cwd();
const frontendDir = resolve(root, flag('--frontend', '../Afrivox_Frontend'));
const outFile = resolve(root, flag('--out', 'LICENSES-THIRD-PARTY.md'));
const m2 = join(homedir(), '.m2', 'repository');

// ---------------------------------------------------------------------------------------------------------------
// Classement des licences
// ---------------------------------------------------------------------------------------------------------------
const PERMISSIVE = 'permissive';
const WEAK = 'copyleft faible';
const STRONG = 'copyleft fort';
const RESTRICTIVE = 'restrictive (source disponible, non open source)';
const UNKNOWN = 'inconnue';
const RANK = { [PERMISSIVE]: 0, [WEAK]: 1, [STRONG]: 2, [RESTRICTIVE]: 2, [UNKNOWN]: 3 };

/** Nom brut -> { label, category }. L'ordre compte : les cas les plus restrictifs d'abord. */
function classify(raw) {
  const s = String(raw).trim();
  const t = s.toLowerCase();
  if (!t) return { label: 'inconnue', category: UNKNOWN };
  if (/\bagpl\b|affero/.test(t)) return { label: 'AGPL', category: STRONG };
  if (/\blgpl\b|lesser general public|library general public/.test(t)) return { label: 'LGPL', category: STRONG };
  if (/classpath exception|\bcpe\b|gpl.*exception/.test(t)) return { label: 'GPL avec exception classpath', category: STRONG };
  if (/\bgpl\b|general public license/.test(t) && !/lesser|library/.test(t)) return { label: 'GPL', category: STRONG };
  if (/\bcddl\b|common development and distribution/.test(t)) return { label: 'CDDL', category: WEAK };
  if (/eclipse public|\bepl\b/.test(t)) return { label: /2\.0|v2|v\. ?2/.test(t) ? 'EPL-2.0' : 'EPL-1.0', category: WEAK };
  if (/mozilla|\bmpl\b/.test(t)) return { label: 'MPL', category: WEAK };
  // Licences « source disponible » : lisibles mais pas open source (ex. Liquibase 5.x en FSL-1.1)
  if (/\bfsl\b|functional source|\bbusl?\b|business source|\bsspl\b|elastic license|commons clause/.test(t)) {
    return { label: s, category: RESTRICTIVE };
  }
  if (/apache/.test(t)) return { label: 'Apache-2.0', category: PERMISSIVE };
  if (/eclipse distribution|\bedl\b/.test(t)) return { label: 'BSD-3-Clause (EDL)', category: PERMISSIVE };
  if (/\bmit\b|x11/.test(t)) return { label: 'MIT', category: PERMISSIVE };
  if (/bsd/.test(t)) return { label: /2[- ]clause|simplified|freebsd/.test(t) ? 'BSD-2-Clause' : 'BSD-3-Clause', category: PERMISSIVE };
  if (/\bisc\b/.test(t)) return { label: 'ISC', category: PERMISSIVE };
  if (/cc0|public domain|unlicense|wtfpl|zlib|0bsd/.test(t)) return { label: s, category: PERMISSIVE };
  if (/bouncy ?castle/.test(t)) return { label: 'MIT (Bouncy Castle)', category: PERMISSIVE };
  if (/python|psf/.test(t)) return { label: 'PSF', category: PERMISSIVE };
  if (/w3c/.test(t)) return { label: 'W3C', category: PERMISSIVE };
  if (/creative commons|cc[- ]by/.test(t)) return { label: s, category: PERMISSIVE };
  if (/postgresql license/.test(t)) return { label: 'PostgreSQL License', category: PERMISSIVE };
  return { label: s, category: UNKNOWN };
}

/** Plusieurs licences declarees = au choix de l'utilisateur : on retient la plus permissive. */
function best(options) {
  if (options.length === 0) return { label: 'inconnue', category: UNKNOWN, options: [] };
  const classified = options.map(classify);
  const chosen = classified.reduce((a, b) => (RANK[b.category] < RANK[a.category] ? b : a));
  return { label: classified.map((c) => c.label).join(' / '), category: chosen.category, options };
}

// ---------------------------------------------------------------------------------------------------------------
// Maven
// ---------------------------------------------------------------------------------------------------------------
function mavenList() {
  const dir = mkdtempSync(join(tmpdir(), 'afrivox-lic-'));
  const out = join(dir, 'deps.txt');
  // Chemin complet et entre guillemets : sous Windows la commande passe par cmd.exe (shell), qui ne cherche pas
  // dans le dossier courant.
  const wrapper = `"${join(root, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw')}"`;
  const mvnArgs = ['-B', '-q', ...(online ? [] : ['-o']), '-Pprod', 'dependency:list', `-DoutputFile=${out}`, '-Dsort=true'];
  const run = spawnSync(wrapper, mvnArgs, { cwd: root, stdio: 'inherit', shell: process.platform === 'win32' });
  if (run.status !== 0 || !existsSync(out)) {
    throw new Error('mvnw dependency:list a echoue (JAVA_HOME defini ? depots Maven locaux presents ? sinon --online)');
  }
  return readFileSync(out, 'utf8')
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => /^[\w.-]+:[\w.-]+:/.test(l))
    .map((l) => {
      const p = l.split(' -- ')[0].split(':');
      // groupe:artefact:type:version:portee  ou  groupe:artefact:type:classifier:version:portee
      const [group, artifact, type] = p;
      const scope = p[p.length - 1];
      const version = p[p.length - 2];
      return { group, artifact, type, version, scope };
    });
}

const pomCache = new Map();
function pomPath(g, a, v) {
  return join(m2, ...g.split('.'), a, v, `${a}-${v}.pom`);
}

function readPom(g, a, v) {
  const key = `${g}:${a}:${v}`;
  if (pomCache.has(key)) return pomCache.get(key);
  const file = pomPath(g, a, v);
  let result = null;
  if (existsSync(file)) {
    const xml = readFileSync(file, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
    const block = /<licenses>([\s\S]*?)<\/licenses>/.exec(xml);
    const licenses = block
      ? [...block[1].matchAll(/<license>([\s\S]*?)<\/license>/g)].map((m) => {
          const name = /<name>([\s\S]*?)<\/name>/.exec(m[1]);
          const url = /<url>([\s\S]*?)<\/url>/.exec(m[1]);
          return { name: name ? name[1].trim().replace(/\s+/g, ' ') : '', url: url ? url[1].trim() : '' };
        })
      : [];
    const parent = /<parent>([\s\S]*?)<\/parent>/.exec(xml);
    let parentRef = null;
    if (parent) {
      const get = (tag) => new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`).exec(parent[1])?.[1].trim();
      parentRef = { g: get('groupId'), a: get('artifactId'), v: get('version') };
    }
    result = { licenses, parentRef };
  }
  pomCache.set(key, result);
  return result;
}

function mavenLicenses(g, a, v, depth = 0) {
  const pom = readPom(g, a, v);
  if (!pom) return { names: [], note: 'pom absent du depot local' };
  const named = pom.licenses.filter((l) => l.name || l.url);
  if (named.length > 0) return { names: named.map((l) => l.name || l.url) };
  if (pom.parentRef && depth < 8 && pom.parentRef.v && !pom.parentRef.v.includes('${')) {
    return mavenLicenses(pom.parentRef.g, pom.parentRef.a, pom.parentRef.v, depth + 1);
  }
  return { names: [], note: 'aucune licence declaree dans le pom ni ses parents' };
}

// ---------------------------------------------------------------------------------------------------------------
// npm
// ---------------------------------------------------------------------------------------------------------------
function npmList(dir) {
  const lockFile = join(dir, 'package-lock.json');
  if (!existsSync(lockFile)) throw new Error(`package-lock.json introuvable dans ${dir}`);
  const lock = JSON.parse(readFileSync(lockFile, 'utf8'));
  const rows = [];
  for (const [path, info] of Object.entries(lock.packages ?? {})) {
    if (!path.startsWith('node_modules/')) continue;
    const name = path.slice(path.lastIndexOf('node_modules/') + 'node_modules/'.length);
    let license = info.license;
    if (!license) {
      const manifest = join(dir, path, 'package.json');
      if (existsSync(manifest)) {
        const pkg = JSON.parse(readFileSync(manifest, 'utf8'));
        license = pkg.license ?? pkg.licenses;
      }
    }
    if (license && typeof license === 'object') {
      license = Array.isArray(license) ? license.map((l) => l.type ?? l).join(' OR ') : license.type;
    }
    rows.push({
      name,
      version: info.version,
      scope: info.dev ? 'dev' : info.optional ? 'optional' : 'production',
      license: license ? String(license) : '',
    });
  }
  return rows;
}

/** Une expression SPDX « A OR B » = au choix ; « A AND B » = les deux s'appliquent (on retient la plus stricte). */
function npmClassify(expr) {
  if (!expr) return { label: 'inconnue', category: UNKNOWN };
  const cleaned = expr.replace(/[()]/g, ' ').trim();
  const ors = cleaned.split(/\s+OR\s+/i);
  const perOr = ors.map((part) => {
    const ands = part.split(/\s+AND\s+/i).map(classify);
    return ands.reduce((a, b) => (RANK[b.category] > RANK[a.category] ? b : a));
  });
  const chosen = perOr.reduce((a, b) => (RANK[b.category] < RANK[a.category] ? b : a));
  return { label: expr, category: chosen.category };
}

// ---------------------------------------------------------------------------------------------------------------
// Rapport
// ---------------------------------------------------------------------------------------------------------------
const esc = (s) => String(s).replace(/\|/g, '\\|');

function table(rows, columns) {
  const head = `| ${columns.map((c) => c.title).join(' | ')} |`;
  const sep = `|${columns.map(() => '---').join('|')}|`;
  const body = rows.map((r) => `| ${columns.map((c) => esc(c.get(r))).join(' | ')} |`);
  return [head, sep, ...body].join('\n');
}

function summary(rows) {
  const counts = {};
  for (const r of rows) counts[r.category] = (counts[r.category] ?? 0) + 1;
  return Object.entries(counts)
    .sort((a, b) => RANK[a[0]] - RANK[b[0]])
    .map(([k, n]) => `${k} : ${n}`)
    .join(' ; ');
}

// Maven
const mavenRows = mavenList().map((d) => {
  const found = mavenLicenses(d.group, d.artifact, d.version);
  const c = best(found.names);
  return { ...d, name: `${d.group}:${d.artifact}`, license: c.label, category: c.category, note: found.note ?? '' };
});
const mavenShipped = mavenRows.filter((r) => ['compile', 'runtime'].includes(r.scope));
const mavenOther = mavenRows.filter((r) => !['compile', 'runtime'].includes(r.scope));

// npm : frontend (livre) et outillage du depot backend (non livre)
const frontendRows = npmList(frontendDir).map((r) => ({ ...r, ...npmClassify(r.license) }));
const backendToolsPath = root;
const toolRows = existsSync(join(backendToolsPath, 'package-lock.json'))
  ? npmList(backendToolsPath).map((r) => ({ ...r, ...npmClassify(r.license) }))
  : [];

const flagged = (rows) => rows.filter((r) => r.category !== PERMISSIVE);
const sortRows = (rows) => [...rows].sort((a, b) => RANK[b.category] - RANK[a.category] || a.name.localeCompare(b.name));

const mvnCols = [
  { title: 'Dependance', get: (r) => r.name },
  { title: 'Version', get: (r) => r.version },
  { title: 'Portee', get: (r) => r.scope },
  { title: 'Licence', get: (r) => r.license },
  { title: 'Classement', get: (r) => r.category },
  { title: 'Remarque', get: (r) => r.note },
];
const npmCols = [
  { title: 'Paquet', get: (r) => r.name },
  { title: 'Version', get: (r) => r.version },
  { title: 'Portee', get: (r) => r.scope },
  { title: 'Licence', get: (r) => r.license || 'inconnue' },
  { title: 'Classement', get: (r) => r.category },
];

const shippedNpm = frontendRows.filter((r) => r.scope !== 'dev');
const devNpm = frontendRows.filter((r) => r.scope === 'dev');
const toFlag = [
  ...flagged(mavenShipped).map((r) => ({ ...r, ecosystem: 'Maven (livre)' })),
  ...flagged(shippedNpm).map((r) => ({ ...r, ecosystem: 'npm frontend (livre)' })),
];

const lines = [];
lines.push('# Licences des dependances tierces');
lines.push('');
lines.push(`Genere par \`scripts/licenses/generate.mjs\` le ${new Date().toISOString().slice(0, 10)}. **Ne pas editer a la main** : relancer le script apres tout ajout ou mise a jour de dependance.`);
lines.push('');
lines.push('Regle du reglement : licences permissives uniquement (MIT, Apache 2.0, BSD). GPL, AGPL et LGPL rendent le projet irrecevable ; le copyleft faible (EPL, MPL, CDDL) fait l\'objet d\'une question ecrite a cadev@systalink.com.');
lines.push('');
lines.push('Les licences sont **lues** dans les metadonnees (poms Maven, `package-lock.json`, `node_modules`), pas verifiees dans les textes. Une licence « inconnue » doit etre controlee a la main avant le rendu.');
lines.push('');
lines.push('## Resume');
lines.push('');
lines.push(`- **Backend livre** (Maven, portees compile et runtime, profil \`prod\`) : ${mavenShipped.length} dependances — ${summary(mavenShipped)}`);
lines.push(`- **Frontend livre** (npm, hors outillage de build) : ${shippedNpm.length} paquets — ${summary(shippedNpm)}`);
lines.push(`- Outillage de build et de test (non livre) : Maven ${mavenOther.length} ; npm frontend ${devNpm.length} ; npm backend ${toolRows.length}`);
lines.push('');
lines.push('## A examiner (livre, hors licence permissive)');
lines.push('');
if (toFlag.length === 0) {
  lines.push('Aucune : toutes les dependances livrees sont lues comme permissives.');
} else {
  lines.push(
    table(sortRows(toFlag), [
      { title: 'Ecosysteme', get: (r) => r.ecosystem },
      { title: 'Dependance', get: (r) => r.name },
      { title: 'Version', get: (r) => r.version },
      { title: 'Portee', get: (r) => r.scope },
      { title: 'Licence', get: (r) => r.license || 'inconnue' },
      { title: 'Classement', get: (r) => r.category },
      { title: 'Remarque', get: (r) => r.note ?? '' },
    ]),
  );
}
lines.push('');
lines.push('## Backend livre (Maven, compile et runtime)');
lines.push('');
lines.push(table(sortRows(mavenShipped), mvnCols));
lines.push('');
lines.push('## Frontend livre (npm)');
lines.push('');
lines.push(table(sortRows(shippedNpm), npmCols));
lines.push('');
lines.push('## Outillage de build et de test (non livre)');
lines.push('');
lines.push('Ces composants servent a compiler, tester et formater ; ils ne sont pas inclus dans le jar ni dans les fichiers servis. Les plugins Maven (checkstyle, modernizer, etc.) ne sont pas listes : ils ne sont pas distribues.');
lines.push('');
lines.push('### Maven (portees test et provided)');
lines.push('');
lines.push(table(sortRows(mavenOther), mvnCols));
lines.push('');
lines.push('### npm frontend (devDependencies)');
lines.push('');
lines.push(table(sortRows(devNpm), npmCols));
lines.push('');
lines.push('### npm backend (outillage JHipster, Prettier, Husky)');
lines.push('');
lines.push(toolRows.length ? table(sortRows(toolRows), npmCols) : 'Aucun `package-lock.json` trouve.');
lines.push('');

writeFileSync(outFile, lines.join('\n'), 'utf8');
console.log(`Ecrit : ${outFile}`);
console.log(`Backend livre : ${summary(mavenShipped)}`);
console.log(`Frontend livre : ${summary(shippedNpm)}`);
console.log(`A examiner : ${toFlag.length}`);
