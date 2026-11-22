#!/usr/bin/env node
/*
 * OpuntiaPad — TextMate asset generator.
 *
 * Copies a curated set of TextMate grammars and colour themes from the
 * `tm-grammars` / `tm-themes` npm packages into the app assets, and writes:
 *
 *   app/src/main/assets/textmate/languages.json   (Sora Editor grammar registry)
 *   app/src/main/assets/textmate/catalog.json     (OpuntiaPad language catalog: names, extensions…)
 *   app/src/main/assets/textmate/config/<id>.json (VS Code style language configurations)
 *   app/src/main/assets/textmate/themes/<id>.json
 *
 * Usage:
 *   npm pack tm-grammars@<ver> tm-themes@<ver>   (or use tools/update-textmate-assets.sh)
 *   node tools/generate-textmate-assets.mjs <tm-grammars/package> <tm-themes/package>
 */
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';

const [grammarPkg, themePkg] = process.argv.slice(2);
if (!grammarPkg || !themePkg) {
  console.error('usage: generate-textmate-assets.mjs <tm-grammars dir> <tm-themes dir>');
  process.exit(1);
}

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'app/src/main/assets/textmate');

// ---------------------------------------------------------------------------
// Comment styles
const C = { line: '//', block: ['/*', '*/'] };
const HASH = { line: '#' };
const DASH = { line: '--', block: ['/*', '*/'] };
const XML = { block: ['<!--', '-->'] };
const NONE = {};

// Curated catalog. `id` must match a grammar name in tm-grammars.
// `ext` = file extensions (lower case, no dot), `files` = exact file names.
const LANGUAGES = [
  { id: 'json', name: 'JSON', ext: ['json', 'webmanifest', 'har', 'geojson', 'topojson', 'jsonld'], files: ['.babelrc', 'composer.lock'], comment: NONE },
  { id: 'jsonc', name: 'JSON with Comments', ext: ['jsonc'], files: ['tsconfig.json', 'jsconfig.json', '.eslintrc', '.eslintrc.json', 'settings.json', 'devcontainer.json'], comment: C },
  { id: 'json5', name: 'JSON5', ext: ['json5'], comment: C },
  { id: 'jsonl', name: 'JSON Lines', ext: ['jsonl', 'ndjson'], comment: NONE },
  { id: 'yaml', name: 'YAML', ext: ['yaml', 'yml', 'eyaml', 'eyml', 'cff'], files: ['.clang-format', '.clangd'], comment: HASH },
  { id: 'toml', name: 'TOML', ext: ['toml'], files: ['Cargo.lock', 'Pipfile', 'poetry.lock'], comment: HASH },
  { id: 'ini', name: 'INI / Properties', ext: ['ini', 'cfg', 'conf', 'properties', 'prefs', 'editorconfig', 'gitconfig', 'desktop', 'inf'], files: ['.editorconfig', '.gitconfig', '.npmrc'], comment: HASH },
  { id: 'dotenv', name: '.env', ext: ['env'], files: ['.env', '.env.local', '.env.example', '.env.production', '.env.development'], comment: HASH },
  { id: 'xml', name: 'XML', ext: ['xml', 'xsd', 'xsl', 'xslt', 'svg', 'plist', 'pom', 'csproj', 'fxml', 'wsdl', 'rss', 'atom', 'kml', 'gpx', 'xaml', 'resx', 'iml', 'xib', 'storyboard'], comment: XML },
  { id: 'html', name: 'HTML', ext: ['html', 'htm', 'xhtml', 'shtml'], comment: XML },
  { id: 'css', name: 'CSS', ext: ['css'], comment: { block: ['/*', '*/'] } },
  { id: 'scss', name: 'SCSS', ext: ['scss'], comment: C },
  { id: 'less', name: 'Less', ext: ['less'], comment: C },
  { id: 'markdown', name: 'Markdown', ext: ['md', 'markdown', 'mdown', 'mkd', 'mkdn'], files: ['README', 'CHANGELOG'], comment: XML },
  { id: 'sql', name: 'SQL', ext: ['sql', 'ddl', 'dml', 'hql', 'psql', 'mysql', 'pgsql'], comment: DASH },
  { id: 'plsql', name: 'PL/SQL', ext: ['pls', 'plsql', 'pck', 'pkb', 'pks', 'prc', 'fnc', 'trg'], comment: DASH },
  { id: 'javascript', name: 'JavaScript', ext: ['js', 'mjs', 'cjs'], comment: C },
  { id: 'jsx', name: 'JSX', ext: ['jsx'], comment: C },
  { id: 'typescript', name: 'TypeScript', ext: ['ts', 'mts', 'cts'], comment: C },
  { id: 'tsx', name: 'TSX', ext: ['tsx'], comment: C },
  { id: 'python', name: 'Python', ext: ['py', 'pyw', 'pyi', 'gyp', 'bzl'], files: ['SConstruct', 'SConscript', 'BUILD', 'WORKSPACE'], comment: { line: '#', block: ['"""', '"""'] } },
  { id: 'java', name: 'Java', ext: ['java', 'jav'], comment: C },
  { id: 'kotlin', name: 'Kotlin', ext: ['kt', 'kts'], comment: C },
  { id: 'groovy', name: 'Groovy / Gradle', ext: ['groovy', 'gradle', 'gvy', 'jenkinsfile'], files: ['Jenkinsfile'], comment: C },
  { id: 'scala', name: 'Scala', ext: ['scala', 'sc', 'sbt'], comment: C },
  { id: 'c', name: 'C', ext: ['c', 'h'], comment: C },
  { id: 'cpp', name: 'C++', ext: ['cpp', 'cc', 'cxx', 'c++', 'hpp', 'hh', 'hxx', 'h++', 'ino', 'ipp', 'tpp'], comment: C },
  { id: 'csharp', name: 'C#', ext: ['cs', 'csx', 'cake'], comment: C },
  { id: 'objective-c', name: 'Objective-C', ext: ['m'], comment: C },
  { id: 'go', name: 'Go', ext: ['go'], comment: C },
  { id: 'rust', name: 'Rust', ext: ['rs'], comment: C },
  { id: 'swift', name: 'Swift', ext: ['swift'], comment: C },
  { id: 'dart', name: 'Dart', ext: ['dart'], comment: C },
  { id: 'php', name: 'PHP', ext: ['php', 'phtml', 'php3', 'php4', 'php5', 'phps'], comment: C },
  { id: 'ruby', name: 'Ruby', ext: ['rb', 'rbw', 'gemspec', 'rake', 'ru', 'podspec'], files: ['Gemfile', 'Rakefile', 'Podfile', 'Vagrantfile', 'Fastfile', 'Brewfile'], comment: HASH },
  { id: 'perl', name: 'Perl', ext: ['pl', 'pm', 'pod', 't', 'psgi'], comment: HASH },
  { id: 'lua', name: 'Lua', ext: ['lua'], comment: { line: '--', block: ['--[[', ']]'] } },
  { id: 'r', name: 'R', ext: ['r', 'rhistory', 'rprofile'], comment: HASH },
  { id: 'julia', name: 'Julia', ext: ['jl'], comment: { line: '#', block: ['#=', '=#'] } },
  { id: 'haskell', name: 'Haskell', ext: ['hs', 'lhs'], comment: { line: '--', block: ['{-', '-}'] } },
  { id: 'elixir', name: 'Elixir', ext: ['ex', 'exs'], comment: HASH },
  { id: 'erlang', name: 'Erlang', ext: ['erl', 'hrl'], comment: { line: '%' } },
  { id: 'clojure', name: 'Clojure', ext: ['clj', 'cljs', 'cljc', 'edn'], comment: { line: ';' } },
  { id: 'fsharp', name: 'F#', ext: ['fs', 'fsi', 'fsx'], comment: { line: '//', block: ['(*', '*)'] } },
  { id: 'vb', name: 'Visual Basic', ext: ['vb', 'vbs', 'bas', 'vba'], comment: { line: "'" } },
  { id: 'shellscript', name: 'Shell', ext: ['sh', 'bash', 'zsh', 'ksh', 'ash', 'command'], files: ['.bashrc', '.bash_profile', '.zshrc', '.profile', 'PKGBUILD', 'gradlew', 'mvnw'], comment: HASH },
  { id: 'powershell', name: 'PowerShell', ext: ['ps1', 'psm1', 'psd1'], comment: { line: '#', block: ['<#', '#>'] } },
  { id: 'bat', name: 'Batch', ext: ['bat', 'cmd'], comment: { line: 'REM' } },
  { id: 'docker', name: 'Dockerfile', ext: ['dockerfile', 'containerfile'], files: ['Dockerfile', 'Containerfile'], comment: HASH },
  { id: 'make', name: 'Makefile', ext: ['mk', 'mak', 'make'], files: ['Makefile', 'makefile', 'GNUmakefile'], comment: HASH },
  { id: 'cmake', name: 'CMake', ext: ['cmake'], files: ['CMakeLists.txt'], comment: HASH },
  { id: 'nginx', name: 'Nginx', ext: ['nginx', 'nginxconf'], files: ['nginx.conf'], comment: HASH },
  { id: 'apache', name: 'Apache config', ext: ['htaccess', 'htpasswd'], files: ['.htaccess', 'httpd.conf', 'apache2.conf'], comment: HASH },
  { id: 'ssh-config', name: 'SSH config', ext: [], files: ['ssh_config', 'sshd_config'], comment: HASH },
  { id: 'terraform', name: 'Terraform', ext: ['tf', 'tfvars'], comment: { line: '#', block: ['/*', '*/'] } },
  { id: 'hcl', name: 'HCL', ext: ['hcl', 'nomad'], comment: { line: '#', block: ['/*', '*/'] } },
  { id: 'graphql', name: 'GraphQL', ext: ['graphql', 'gql', 'graphqls'], comment: HASH },
  { id: 'proto', name: 'Protocol Buffers', ext: ['proto'], comment: C },
  { id: 'prisma', name: 'Prisma', ext: ['prisma'], comment: { line: '//' } },
  { id: 'http', name: 'HTTP', ext: ['http', 'rest'], comment: HASH },
  { id: 'latex', name: 'LaTeX', ext: ['tex', 'ltx', 'sty', 'cls'], comment: { line: '%' } },
  { id: 'bibtex', name: 'BibTeX', ext: ['bib'], comment: { line: '%' } },
  { id: 'diff', name: 'Diff / Patch', ext: ['diff', 'patch', 'rej'], comment: NONE },
  { id: 'git-commit', name: 'Git commit', ext: [], files: ['COMMIT_EDITMSG', 'MERGE_MSG', 'TAG_EDITMSG'], comment: HASH },
  { id: 'log', name: 'Log', ext: ['log', 'out'], comment: NONE },
  { id: 'csv', name: 'CSV', ext: ['csv'], comment: NONE },
  { id: 'tsv', name: 'TSV', ext: ['tsv', 'tab'], comment: NONE },
  { id: 'asm', name: 'Assembly', ext: ['asm', 's', 'nasm'], comment: { line: ';' } },
  { id: 'matlab', name: 'MATLAB', ext: ['matlab'], comment: { line: '%', block: ['%{', '%}'] } },
  { id: 'nix', name: 'Nix', ext: ['nix'], comment: { line: '#', block: ['/*', '*/'] } },
  { id: 'vue', name: 'Vue', ext: ['vue'], comment: XML },
  { id: 'svelte', name: 'Svelte', ext: ['svelte'], comment: XML },
  { id: 'regexp', name: 'Regular Expression', ext: [], comment: NONE },
];

// Colour themes (VS Code format) offered in the app.
const THEMES = [
  { id: 'github-light', name: 'GitHub Light', dark: false },
  { id: 'light-plus', name: 'Light+ (VS Code)', dark: false },
  { id: 'one-light', name: 'One Light', dark: false },
  { id: 'solarized-light', name: 'Solarized Light', dark: false },
  { id: 'catppuccin-latte', name: 'Catppuccin Latte', dark: false },
  { id: 'github-dark', name: 'GitHub Dark', dark: true },
  { id: 'dark-plus', name: 'Dark+ (VS Code)', dark: true },
  { id: 'one-dark-pro', name: 'One Dark Pro', dark: true },
  { id: 'monokai', name: 'Monokai', dark: true },
  { id: 'dracula', name: 'Dracula', dark: true },
  { id: 'nord', name: 'Nord', dark: true },
  { id: 'solarized-dark', name: 'Solarized Dark', dark: true },
  { id: 'catppuccin-mocha', name: 'Catppuccin Mocha', dark: true },
  { id: 'tokyo-night', name: 'Tokyo Night', dark: true },
];

// ---------------------------------------------------------------------------
const { grammars: meta } = await import(pathToFileURL(path.join(grammarPkg, 'index.js')).href);
const byName = new Map(meta.map((g) => [g.name, g]));

// Resolve embedded grammars (transitively), restricted to what tm-grammars ships.
const wanted = new Set(LANGUAGES.map((l) => l.id));
const queue = [...wanted];
while (queue.length) {
  const g = byName.get(queue.pop());
  if (!g) continue;
  for (const e of g.embedded ?? []) {
    if (!wanted.has(e) && byName.has(e)) {
      wanted.add(e);
      queue.push(e);
    }
  }
}

fs.rmSync(out, { recursive: true, force: true });
for (const d of ['grammars', 'config', 'themes']) fs.mkdirSync(path.join(out, d), { recursive: true });

/** Returns the colour as #RRGGBB or #RRGGBBAA, or null when it can't be represented. */
function normalizeColor(value) {
  if (typeof value !== 'string') return null;
  const v = value.trim();
  if (/^#[0-9a-f]{6}([0-9a-f]{2})?$/i.test(v)) return v;
  if (/^#[0-9a-f]{3,4}$/i.test(v)) return '#' + [...v.slice(1)].map((ch) => ch + ch).join('');
  return null;
}

const bracketPairs = [['{', '}'], ['[', ']'], ['(', ')']];
function languageConfiguration(comment) {
  const cfg = {};
  if (comment.line || comment.block) {
    cfg.comments = {};
    if (comment.line) cfg.comments.lineComment = comment.line;
    if (comment.block) cfg.comments.blockComment = comment.block;
  }
  cfg.brackets = bracketPairs;
  cfg.autoClosingPairs = [
    ...bracketPairs.map(([open, close]) => ({ open, close })),
    { open: '"', close: '"', notIn: ['string'] },
    { open: "'", close: "'", notIn: ['string', 'comment'] },
    { open: '`', close: '`', notIn: ['string', 'comment'] },
  ];
  cfg.surroundingPairs = [...bracketPairs, ['"', '"'], ["'", "'"], ['`', '`']];
  return cfg;
}

const catalogById = new Map(LANGUAGES.map((l) => [l.id, l]));
const registry = [];
const catalog = [];
const licenses = [];
let total = 0;

for (const id of [...wanted].sort()) {
  const g = byName.get(id);
  if (!g) {
    console.warn(`! grammar "${id}" not found in tm-grammars, skipped`);
    continue;
  }
  const raw = fs.readFileSync(path.join(grammarPkg, 'grammars', `${id}.json`));
  total += raw.length;
  // Re-serialise compactly to save a few bytes.
  fs.writeFileSync(path.join(out, 'grammars', `${id}.json`), JSON.stringify(JSON.parse(raw)));

  const entry = catalogById.get(id);
  const embedded = {};
  for (const e of g.embedded ?? []) {
    const eg = byName.get(e);
    if (eg && wanted.has(e)) embedded[eg.scopeName] = e;
  }
  const reg = {
    name: id,
    scopeName: g.scopeName,
    grammar: `textmate/grammars/${id}.json`,
  };
  if (Object.keys(embedded).length) reg.embeddedLanguages = embedded;
  if (entry) {
    fs.writeFileSync(path.join(out, 'config', `${id}.json`), JSON.stringify(languageConfiguration(entry.comment), null, 1));
    reg.languageConfiguration = `textmate/config/${id}.json`;
    catalog.push({
      id,
      name: entry.name,
      scopeName: g.scopeName,
      extensions: entry.ext,
      fileNames: entry.files ?? [],
      lineComment: entry.comment.line ?? null,
      blockComment: entry.comment.block ?? null,
    });
  }
  registry.push(reg);
  licenses.push(`- ${g.displayName} (\`${id}\`) — ${g.license ?? 'see source'} — ${g.source}`);
}

const themeOut = [];
for (const t of THEMES) {
  const file = path.join(themePkg, 'themes', `${t.id}.json`);
  if (!fs.existsSync(file)) {
    console.warn(`! theme "${t.id}" not found, skipped`);
    continue;
  }
  const json = JSON.parse(fs.readFileSync(file, 'utf8'));
  // Keep only what the editor uses: name, type, colors and tokenColors.
  // Sora Editor only understands #RRGGBB / #RRGGBBAA, so shorthand colours are expanded and
  // anything else (e.g. "inherit") is dropped — otherwise the editor throws "Unknown color".
  const colors = {};
  for (const [key, value] of Object.entries(json.colors ?? {})) {
    const c = normalizeColor(value);
    if (c) colors[key] = c;
  }
  const tokenColors = (json.tokenColors ?? []).map((rule) => {
    if (!rule.settings) return rule;
    const settings = { ...rule.settings };
    for (const key of ['foreground', 'background']) {
      if (key in settings) {
        const c = normalizeColor(settings[key]);
        if (c) settings[key] = c; else delete settings[key];
      }
    }
    return { ...rule, settings };
  });
  const slim = { name: t.name, type: t.dark ? 'dark' : 'light', colors, tokenColors };
  fs.writeFileSync(path.join(out, 'themes', `${t.id}.json`), JSON.stringify(slim));
  themeOut.push({ id: t.id, name: t.name, dark: t.dark, path: `textmate/themes/${t.id}.json` });
}

catalog.sort((a, b) => a.name.localeCompare(b.name));
fs.writeFileSync(path.join(out, 'languages.json'), JSON.stringify({ languages: registry }, null, 1));
fs.writeFileSync(path.join(out, 'catalog.json'), JSON.stringify({ languages: catalog, themes: themeOut }, null, 1));

const grammarVersion = JSON.parse(fs.readFileSync(path.join(grammarPkg, 'package.json'), 'utf8')).version;
const themeVersion = JSON.parse(fs.readFileSync(path.join(themePkg, 'package.json'), 'utf8')).version;
fs.writeFileSync(
  path.join(root, 'docs/THIRD_PARTY_GRAMMARS.md'),
  `# Bundled TextMate grammars and themes\n\n` +
    `Generated by \`tools/generate-textmate-assets.mjs\` from ` +
    `[tm-grammars](https://github.com/shikijs/textmate-grammars-themes) v${grammarVersion} and tm-themes v${themeVersion} (MIT).\n` +
    `Each grammar keeps the license of its upstream project:\n\n${licenses.join('\n')}\n\n` +
    `Themes: ${themeOut.map((t) => t.name).join(', ')} — see the tm-themes package for upstream licenses.\n`,
);

console.log(`✓ ${registry.length} grammars (${(total / 1024).toFixed(0)} KB), ${catalog.length} user-facing languages, ${themeOut.length} themes`);
