# Validation module export policy

## Review guideline (anti-barrel-growth)

- Do **not** add `export * from ...` in `index.ts`.
- Only export symbols that are consumed outside `src/lib/validation`.
- For internal cross-module usage inside this folder, import from direct module
  paths (`./schemas`, `./dynamic-validator`, `./useFormValidation.svelte`) rather
  than from `./index`.
- If a new export is added to `index.ts`, include the concrete external import
  site(s) in the PR description.
