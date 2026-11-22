# License overrides

Some grammars in `tm-grammars` have no entry in its `NOTICE` file. Their licenses were checked by hand
at the exact upstream commit bundled, and the notice is kept here. `tools/generate-textmate-assets.mjs`
appends these files to the in-app notices and **fails if a bundled grammar has no license notice at all**.

Grammars without any upstream license (e.g. `glsl`) are excluded from the app (`UNLICENSED` in the generator).
