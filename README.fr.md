<div align="center">

<img src="docs/images/icon.svg" width="96" alt="Logo OpuntiaPad" />

# OpuntiaPad

**Un éditeur de code et de texte rapide et convivial pour Android — open source, sans pub, sans pistage.**

Ouvrez vos fichiers YAML, JSON, SQL, XML, Markdown, logs et plus de 70 autres formats directement depuis
votre gestionnaire de fichiers, votre messagerie ou votre application de chat, avec une vraie coloration
syntaxique, des onglets, la recherche/remplacement et le zoom à deux doigts.

[**Télécharger le dernier APK**](https://github.com/b-badis/OpuntiaPad/releases/latest) · [Version de développement](https://github.com/b-badis/OpuntiaPad/releases/tag/dev) · [English](README.md)

</div>

---

## Fonctionnalités

- 📂 **Ouverture depuis n'importe quelle application** : « Ouvrir avec… » et « Partager → OpuntiaPad » depuis les gestionnaires de fichiers, Gmail, Outlook, WhatsApp, Telegram, Drive, les téléchargements du navigateur…
- 🎨 **Coloration syntaxique** de 73 langages grâce aux grammaires TextMate de VS Code (YAML, JSON, SQL, PL/SQL, XML, HTML, CSS, Markdown, Python, Java, Kotlin, JS/TS, Go, Rust, C/C++, C#, PHP, Shell, Dockerfile, Terraform, TOML, INI, `.env`, logs, CSV…).
- 🧠 **Détection automatique du langage** par nom de fichier, extension, type MIME ou contenu ; modifiable à tout moment.
- ✏️ **Édition complète** : annuler/rétablir par fichier, poignées de sélection, indentation, auto-indentation, fermeture automatique des crochets, barre Tab & symboles au-dessus du clavier.
- 🔎 **Rechercher & remplacer** : casse, mot entier, expressions régulières, compteur d'occurrences.
- 🗂️ **Onglets** façon Notepad++, indicateur de modifications, fichiers récents.
- 🔢 **Numéros de ligne**, aller à la ligne, surlignage de la ligne courante, défilement adhérent, affichage des espaces.
- 🔍 **Zoom** à deux doigts, par menu ou `Ctrl +/-`.
- 📱 **S'adapte à tous les écrans** (téléphones, pliables, tablettes), thèmes clair/sombre, Material You, 14 thèmes de couleurs d'éditeur.
- 🌍 **Multilingue** : English, Français, العربية, Español, Deutsch (choix de la langue dans l'application).
- 🧰 **Outils** : formater/minifier le JSON, détection d'encodage (UTF-8, UTF-16, BOM, Windows-1252…), conversion LF/CRLF, raccourcis clavier physique.
- 🔒 **Respect de la vie privée** : aucune permission Internet, aucune statistique, aucune publicité.

## Installation

1. Téléchargez `opuntiapad-x.y.z.apk` depuis la [dernière version](https://github.com/b-badis/OpuntiaPad/releases/latest).
2. Ouvrez-le sur votre téléphone et autorisez l'installation depuis cette source.

Nécessite Android 8.0 ou plus récent.

## FAQ

**OpuntiaPad n'apparaît pas quand je touche un fichier JSON / YAML / SQL.**
Certains gestionnaires de fichiers ouvrent les fichiers texte dans leur propre lecteur sans demander. Utilisez
**⋮ → Ouvrir avec** (ou **Partager → OpuntiaPad**). Si une autre application a été choisie *Toujours*, réinitialisez-la
dans *Paramètres → Applications → (cette app) → Ouvrir par défaut → Effacer les valeurs par défaut*, puis choisissez
OpuntiaPad → *Toujours*.

## Compiler

```bash
git clone https://github.com/b-badis/OpuntiaPad.git
cd OpuntiaPad
./gradlew assembleDebug
```

## Publier une version

Mettez à jour `CHANGELOG.md`, puis poussez un tag : `git tag v1.0.0 && git push origin v1.0.0`.
La CI construit, signe et publie l'APK sur GitHub Releases. Configuration initiale : [docs/RELEASING.md](docs/RELEASING.md).

## Contribuer

Les rapports de bugs, traductions et pull requests sont les bienvenus : voir [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

[Apache 2.0](LICENSE) — Copyright 2026 Badis Barbouch et les contributeurs d'OpuntiaPad ([NOTICE](NOTICE)).
Composants tiers : [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) ; toutes les licences sont aussi
consultables dans l'application (*À propos → Licences open source*).
