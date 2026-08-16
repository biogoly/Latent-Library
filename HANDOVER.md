# Handover — Latent Library

Current state of the project, the traps that will bite you, and what is still open.

**This document is not a changelog.** Per-change narrative lives in git history (`git log`),
which is the authoritative record. What belongs here is only what a newcomer cannot derive
from reading the code: non-obvious invariants, the reasoning behind them, and open work.

---

## What this is

**Latent Library** is a high-performance, local-first desktop asset manager for the AI image
generation ecosystem: SQLite FTS5 search, dynamic Smart Collections, live hot-folder monitoring,
and an ONNX-powered WD14 auto-tagger.

| Layer | Stack | Location |
|---|---|---|
| Backend | Java 21 / Spring Boot 3.3.2 | `backend/` |
| Frontend | Vue 3 + Vite + PrimeVue 3.53 + Pinia | `frontend/` |
| Desktop shell | Electron 31 | `electron/` |
| Database | SQLite + Flyway migrations | `backend/data/` |

App version `1.1.1`. Active branch is `main`.

### Place in the Latent suite

Latent Library is one of three sibling apps that share the **Latent Design System**
(`Latent-Design-System/`, vendored into `assets/css/latent/tokens/`); the others are Latent Model
Organizer and Latent Tools. Only the tokens and the two logo SVGs are shared — there is no shared
component library, build, or release process, and the three diverge on stack (LL and LMO are Vue,
LT is vanilla TS; LL is on PrimeVue 3, LMO on PrimeVue 4). Treat sibling repos as reference, not
as a dependency: nothing here breaks if they change. What *is* worth keeping in step is the token
files and the vendored skills — re-vendor rather than edit in place.

### Repository map

- `backend/src/main/java/com/nilsson/backend/`
  - `controller/` — thin REST layer (`Image`, `Library`, `Collection`, `Folder`, `Duplicate`,
    `Scrub`, `SpeedSorter`, `System`, `Tagger`, `Spa`).
  - `service/` — business logic. The load-bearing ones are `IndexingService` (scan + hot-folder
    watch), `ThumbnailService` (striped-lock disk cache), `UserDataManager`, `CollectionService`,
    `MetadataService`, `PathService`.
  - `repository/` — JdbcClient-based; `SearchRepository` owns the FTS5 query building.
  - `strategy/` — per-generator metadata parsers behind `MetadataStrategy`: ComfyUI, Common
    (A1111-style, also handles embedded `Civitai resources:` JSON), InvokeAI, NovelAI, SwarmUI.
- `backend/src/main/resources/static/` — **the built frontend, committed to the repo.** It is
  produced by `npm run build` and served by Spring. See the stale-JAR trap below.
- `frontend/src/`
  - `components/ds/` — 16 Latent Design System primitives (`LButton`, `LInput`, `LSwitch`,
    `LDialog`, `Titlebar`, `StatusPill`, `MissingFileThumb`, …). Prefer these over raw PrimeVue.
  - `views/` — `ImageBrowserView` (gallery/browser), `CollectionsView`, `ComparatorView`,
    `ScrubView`, `SpeedSorterView`, `DuplicateDetectiveView`.
  - `stores/browser.js` — the central Pinia store; most view state lives here.
  - `assets/css/` — `latent/tokens/*` are the vendored DS tokens (the single source of colour,
    spacing and type); `components/base.css` holds global utilities, `layout.css` shared
    patterns (inputs, dialogs, scrollbars, chips), `buttons.css` window and nav controls, and
    `primevue-overrides.css` the PrimeVue theme overrides. Everything resolves against
    `--color-*` / `--radius-*` / `--duration-*`; there is no second token namespace and no
    theme switcher.
- `electron/` — `main.js` spawns `backend/target/backend.jar`, reads `data/port.txt` for the
  port + handshake token, and streams backend stdout into the terminal.

### AI assistant setup

`AGENTS.md` is the rulebook; `CLAUDE.md` imports it, `GEMINI.md` duplicates it.
`.claude/settings.json` holds the permission allowlist.

`.agents/skills/` contains all eight skills `AGENTS.md` points at: the custom `ai-setup-doctor`,
`api-design`, `spring-security`, `sql-migrations`, plus the vendored third-party
`java-springboot`, `frontend-design`, `web-design-guidelines`, `vue`. Provenance and
review-time hashes for the vendored four are recorded in `skills-lock.json`; those four are
byte-identical to the copies in the sister repos, so update them as a suite.

Claude Code reads them through the `.claude/skills` symlink (gitignored — it must be recreated
after a fresh clone; `ai-setup-doctor` checks this).

---

## Invariants and traps

These are the things that have actually caused bugs here. Each one cost real debugging time.

### Build order: the stale-JAR trap

`backend/target/backend.jar` bundles a **snapshot** of the frontend build taken at the last
`mvn package`. Running `npm run build` alone updates `backend/src/main/resources/static/` on
disk but does **not** repackage the jar, so Electron (and any `java -jar` run) keeps serving
the old UI. Any time a frontend change must be verified outside `npm run dev`:

```bash
cd frontend && npm run build          # then
cd backend  && ./mvnw clean package -DskipTests
```

`clean` fails with "Failed to delete backend.jar" if a backend is still running — stop it first,
and confirm the jar's timestamp actually moved before trusting what you see on screen.

### Testing against a real backend in a plain browser

The backend binds a **random port per launch** and requires a handshake token
(`SecurityConfig`), normally handed to the renderer over Electron IPC. `vite.config.js`'s proxy
targets a fixed `localhost:8080`, so `npm run dev` alone will not talk to it.

Two options:

- Read the port and token from `backend/data/port.txt` (format `port:token`) and pass
  `Authorization: Bearer <token>` or `?token=<token>`.
- Or launch with `--spring.profiles.active=test --server.port=8099`, which skips the handshake
  interceptor entirely (`SecurityConfig` does not register it under the `test` profile) while
  still using the real database. The bundled UI at `http://localhost:8099/` then works in an
  ordinary browser, which is the fastest way to reproduce and inspect a UI bug.

### `File.exists()` is not proof a file is readable

`exists()` is a stat. Directories, permission-denied files, and stale entries left by an
unavailable drive all stat successfully while the open fails. Because image response bodies are
streamed lazily, guarding on `exists()` alone let Spring commit a `200 OK` with a
`Content-Length` and then fail to produce the body — the response was already committed, so no
error could be sent and the connection hung until the client gave up (measured: 60s, 0 bytes).

`ImageController.requireReadableFile()` is the guard: it requires `isFile()` and then actually
opens the file, the same syscall the body write would make. **Any new endpoint that streams a
file must use it.** The general rule: never commit a response before you know the body can be
produced.

That hang was also a whole-app outage, not a cosmetic bug — browsers cap concurrent connections
per origin (6 in Chrome), so one folder of unreadable files saturated the pool and starved every
other XHR, including the one that populates the Collections view.

### Lucide icons must never shrink

Lucide renders icons as real `<svg>` elements, which are flex items with the default
`flex-shrink: 1`. Almost every icon sits in a flex container with horizontal padding (PrimeVue
buttons, DS icon buttons, toolbars), so the padding compresses them below their intrinsic width —
the PrimeIcons font glyphs they replaced were inline text and were never affected. This squashed
19 icons app-wide, worst case a 16px star rendering 2px inside a fixed 2rem button.

`base.css` pins them: `.lucide { flex: 0 0 auto; }`. Do not remove it, and be suspicious of any
icon that "looks too small" — measure the computed width against the `width` attribute rather
than bumping `:size`.

### `InterruptedException` clears the interrupt flag

`IndexingService.watchLoop` blocks in `WatchService.take()`. A `catch (Exception)` that also
swallowed `InterruptedException` left `Thread.currentThread().isInterrupted()` false, so the loop
fell through, rebuilt a fresh `WatchService` and ran forever — one leaked watcher per folder
navigation, 64 threads out of 122 in a real dump.

Interruption must restore the flag and break; `ClosedWatchServiceException` *is* the stop signal
and must also break. `startWatching`/`stopWatching` are `synchronized` because the UI fires
**two overlapping `/library/scan` calls per navigation** (visible as duplicated `Indexing folder:`
log lines), which previously let one watcher escape being recorded and therefore being stopped.

### Don't swallow failures at `trace`

`ThumbnailService` logged generation failures at `logger.trace` and returned `null`, so an
unreadable file left no trace anywhere and the caller silently fell through to a 15s timeout.
Failures that the caller cannot observe belong at `WARN` with enough context to identify the file.

### An undefined custom property fails silently — always write a fallback

`background: var(--nope)` is not an error. The declaration is invalid at computed-value time, so
the property falls back to its initial value: no console message, no build failure, just a
transparent background. This is how Latent Model Organizer shipped a whole broken theme, and how
`base.css:104`'s `.glass-panel::after` gloss overlay has never rendered — `--app-glass-gloss` has
no definition anywhere in the repo and no fallback.

The rule everywhere in `assets/css/`: `var(--token, sensible-fallback)`. The fallback must be the
token's *current* value — a stale one (the old `var(--sidebar-width, 200px)` against a 224px
token) documents a dimension the system doesn't use and is worse than none.

### PrimeVue and Vue gotchas

- **Tooltip on a `Dropdown` throws.** PrimeVue resolves a tooltip target as
  `hasClass(el, 'p-inputwrapper') ? findSingle(el, 'input') : el`. A non-editable, non-filter
  `Dropdown` renders a `<span>` label and has no `<input>`, so the target is `null` and every
  directive hook throws. Put `v-tooltip` on a plain wrapper element instead.
- **`backdrop-filter` breaks `position: fixed` descendants.** Like `filter`/`transform`, it
  establishes a new containing block, so a fixed-position modal declared inside the blurred
  sidebar resolved `inset: 0` against the sidebar. Fix with `<Teleport to="body">`.
- **Virtualized cards are recycled.** Per-item state such as "this thumbnail failed to load" must
  be reset on a `watch` of the item key, or one bad item poisons every row that reuses its node.
- **PrimeFlex loses specificity battles.** `.p-0` and `.p-button` have equal specificity and the
  PrimeVue theme is imported later, so utility classes silently lose. Verify computed style
  rather than assuming a utility applied.

---

## Open issues

- **`.glass-panel::after` is dead decoration.** `base.css:100-107` paints a gloss overlay from
  `--app-glass-gloss`, which is defined nowhere and has no fallback, so it has always rendered
  nothing. Either define the token or delete the rule.
- **`.glass-input` in `layout.css` is partly shadowed.** A scoped component rule
  (`[data-v-c4bd1c5a]`, higher specificity, also `!important`) overrides its background to
  `--color-surface-1`. Editing `layout.css` alone will not change how that input looks — check
  the component's own `<style scoped>` first.
- **The frontend has no test tooling at all.** No Vitest, no Vue Test Utils, no spec files, no
  `test` script. Every UI change is verified by eye against a rebuilt jar. That is the binding
  constraint on any component refactor — size the work accordingly, and consider adding Vitest
  before a large one rather than after.
- **Nothing mechanically enforces design-system adherence.** The DS ships
  `_adherence.oxlintrc.json`, which lints exactly the raw-hex and magic-px violations that keep
  reappearing, but no npm script runs it. Wiring it into `npm run lint` is the cheapest way to
  stop the drift recurring.
- **`primevue-overrides.css` is the weak point of the CSS layer.** 385 lines and 212
  `!important` declarations. The two accent hexes at `:248` and `:268` are now tokenized
  (`var(--color-accent-primary-hover, ...)` / `var(--color-accent-primary, ...)`); the
  `#FFFFFF` slider-handle border at `:249` stays raw and documented — same gap as the
  close-button hover below, no DS token for an on-accent white exists yet. `!important` and
  line count are unchanged; fixing those needs the component swap, not a token swap.
  It exists because PrimeVue's theme is imported after our CSS. The durable fix is to shrink it:
  `components/ds/` already has `LSelect`, `LSwitch`, `LInput` and `LSlider` at parity with the
  PrimeVue components those rules target, and each swap deletes a block of overrides. `Dialog`,
  `Card` and `Button` are *not* at parity — `LDialog` has no focus trap or scroll lock — so those
  need DS work first.
- **`LButton`'s `primary` variant diverges from the DS reference.** The spec makes `primary`
  `--color-surface-3` plus a strong border and reserves the accent fill for `cta`; ours fills
  solid accent, which dilutes the "one gradient CTA per screen" restraint.
- **`.dropzone-hint` is a component rule inside a token file.** `latent/tokens/effects.css:17`.
  It came in with the vendored DS and the DS's own guide forbids it. Fix upstream, then re-vendor
  — don't patch the local copy or the next re-vendor reverts it.
- **`#FFFFFF` on the close-button hover has no token.** `buttons.css:45`. The DS has no on-danger
  text colour (`--color-text-on-accent` is `#06101A`, for cyan). Needs either a DS token or a
  documented exemption once the adherence lint runs.
- **README screenshots are pre-redesign.** All five still show the old "AI Toolbox" branding and
  top-nav layout. `assets/screenshots/custom_themes.png` is orphaned (no longer referenced) and
  can be deleted. Recapturing needs a running app plus curated sample images.
- **Comparator star ratings are unverified visually.** `ComparisonMetadataPanel.vue` uses the same
  Lucide `Star` pattern verified elsewhere, but populating the Comparator needs a native file
  dialog, so it was never seen rendering. Worth a glance when that view is next opened.
- **`development` looks abandoned.** It is 53 commits behind `main`, contributes nothing unique,
  and was last touched on 2026-03-01. No workflow documented here uses it (unlike Latent Model
  Organizer, where releases branch from `development`). Either fast-forward it to `main` or delete
  it — leaving it stale invites someone to branch from it by mistake.

---

## Build, test, run

```bash
# Frontend production build (writes into backend/src/main/resources/static/)
cd frontend && npm run build

# Backend unit + slice tests
cd backend && ./mvnw test

# Package the jar (do this after any frontend build you need to see in Electron)
cd backend && ./mvnw clean package -DskipTests

# Packaged desktop app
cd electron && npm run dist
```

Running locally in dev mode:

```bash
cd backend  && ./mvnw spring-boot:run   # terminal 1
cd frontend && npm run dev              # terminal 2 (see the proxy caveat above)
cd electron && npm start                # terminal 3
```

Before claiming a change works: run the tests and show the output, and for UI changes verify
against a rebuilt jar rather than assuming the bundle was picked up.
