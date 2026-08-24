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

App version `1.2.1`. Active branch is `main`.

### Place in the Latent suite

Latent Library is one of three sibling apps that share the **Latent Design System**
(`Latent-Design-System/`, vendored into `assets/css/latent/tokens/`); the others are Latent Model
Organizer and Latent Tools. Only the tokens and the two logo SVGs are shared — there is no shared
component library, build, or release process, and the three diverge on stack (LL and LMO are Vue,
LT is vanilla TS; LL is on PrimeVue 3, LMO on PrimeVue 4). Treat sibling repos as reference, not
as a dependency: nothing here breaks if they change. What *is* worth keeping in step is the token
files and the vendored skills — re-vendor rather than edit in place.

As of `081f9fe`, `tokens/colors.css` and `tokens/effects.css` are byte-identical (modulo line
endings/formatting) across the DS source and all three apps, including the `--color-text-on-danger`
token and the removal of the stray `.dropzone-hint` component rule that had leaked into
`effects.css`. Fix token gaps upstream in the DS repo first, then re-vendor into each app — don't
patch a local copy, the next re-vendor silently reverts it. The DS repo isn't checked out inside
this one; ask where it lives locally if you need to touch it.

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

### SonarQube (local, self-hosted)

SonarQube is run locally rather than via SonarQube Cloud, since this repo is private and the
Cloud free tier caps out on private-repo LOC — Community Edition self-hosted has no such cap.
Server runs as a plain Docker container, not through `sonar run mcp`'s own container management:

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

Console at `http://localhost:9000` (default `admin`/`admin`, forced change on first login). The
CLI (`sonarqube-cli`, installed to `~/AppData/Local/sonarqube-cli/bin/sonar.exe` on this machine)
is authenticated separately from the web console login — `sonar auth login -s http://localhost:9000`
opens its own device-auth flow and must be run per machine/user, independent of the browser
session. `sonar integrate claude --global` wired the MCP server and secret-scanning hooks into
the global Claude Code config (`~/.claude.json`, `~/.claude/settings.json`), not this repo's
`.claude/settings.json` — this was a deliberate choice to cover every project on this machine, not
just this one.

No `sonar-project.properties` exists in this repo yet (`sonar integrate claude` reported
"Config source: none detected"), so the MCP tools have no default project key — pass one
explicitly, or add the properties file, before relying on the project-scoped skills
(`sonar-quality-gate`, `sonar-list-issues`, etc.).

The MCP server itself needs Docker running to start (`sonar run mcp` launches it in a container),
and any new Claude Code session started before Docker was running will not see the
`mcp__sonarqube__*` tools even after the integrate step succeeded — restart the session once
Docker is confirmed up. The `sonar-analyze` skill's CLI fallback (`sonar analyze agentic`) needs
a Vortex-eligible organization, which self-hosted Community Edition is not; the working path for
this setup is the `mcp__sonarqube__*` tools once loaded, not the CLI Vortex fallback.

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

### `app.getPath('exe')` lies about the app's location inside an AppImage

An AppImage self-mounts its contents to a temporary, read-only FUSE mountpoint (something like
`/tmp/.mount_XXXXXX/`) at launch. Inside that mount, `app.getPath('exe')` resolves to the
Electron binary *inside the mount*, not to the real `.AppImage` file's location on disk. Every
Linux user hit this on first launch: `getBackendPaths()` used `path.dirname(app.getPath('exe'))`
as the fallback for `appDataDir`, so it tried to `mkdir` a `data/` folder inside that read-only,
ephemeral mount and failed with `ENOENT` before the backend could even start.

`electron/main.js`'s `resolveAppDataDir()` now checks `process.env.APPIMAGE` (set by the AppImage
runtime to the real file path) before falling back to `app.getPath('exe')`, the same way it
already special-cased `PORTABLE_EXECUTABLE_DIR` for the Windows portable build. **Any code that
needs "the directory the app is running from" must go through `resolveAppDataDir()`**, never
`app.getPath('exe')` directly — the uncaught-exception handler's `startup_error.log` path had the
identical bug and was fixed the same way.

### logback-spring.xml had its own, unrelated hardcoded relative path

Fixing `resolveAppDataDir()` (above) was not sufficient — Linux users still hit a crash
immediately after a working `App Data Dir` was logged and the data folder was created
([#86](https://github.com/erroralex/Latent-Library/issues/86)). `logback-spring.xml` initializes
**before** the Spring context is up, so its `LOG_PATH` property (`data/logs`, unqualified) never
saw `app.data.dir` — it resolved against the JVM's actual working directory instead, which for a
packaged app is `workingDir` (`resourcesPath/runtime/app`), not the writable dir Electron computed.
Inside an AppImage that's the read-only FUSE mount, so `RollingFileAppender` failed to create
`data/logs` and the whole boot aborted with a `Logback configuration error` before any endpoint
came up. Every *other* consumer of the data directory (`PortFileWriter`, `FileSystemService`,
`SystemController`, …) reads `app.data.dir` via `@Value("${app.data.dir:.}")` — logback alone had
its own untied copy of the same concept.

Fixed with `<springProperty scope="context" name="APP_DATA_DIR" source="app.data.dir"
defaultValue="."/>`, which Spring Boot's `LoggingApplicationListener` can resolve from the
environment at the point logback initializes (command-line args like `--app.data.dir=...` are
already present then). **Any new logback property that needs to point inside the app's data
directory must go through this same `APP_DATA_DIR`, never a bare relative path** — a relative path
here is silently correct in dev (cwd == data dir) and silently broken in every packaged build whose
working directory differs from its data directory, which is every platform, not just Linux.

No automated regression test covers this: logback finishes initializing during
`ApplicationEnvironmentPreparedEvent`, which fires before `@TestPropertySource`/`@SpringBootTest`
property sources are attached, so a JVM-internal test can't easily observe where the file appender
actually pointed. Verified manually instead — packaged jar run with `cwd` set to a directory
distinct from `--app.data.dir`, confirming `app.log` is written under the latter, not the former.

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

### Thumbnailator silently renames files whose extension doesn't match the output format

`FileImageSink.write()` (in `net.coobird.thumbnailator`, see its sources jar) checks whether the
destination file's own extension matches the configured output format; if it doesn't, it appends
the correct extension instead of failing. `ThumbnailService` wrote to `<hash>.tmp` with
`.outputFormat("jpg")`, so every generated file actually landed at `<hash>.tmp.jpg` — and the
atomic `Files.move(tempFile, destination, ...)` that followed always threw `NoSuchFileException`,
since `<hash>.tmp` itself never existed. This failed **every** thumbnail generation,
deterministically, not as a race; the WARN-level catch (above) swallowed it into a full-image
fallback, so nothing looked obviously broken, and each failed attempt leaked an orphaned
`*.tmp.jpg`.

Fixed by naming the temp file `<hash>.tmp.jpg` so its own extension already matches the output
format and Thumbnailator's rename never fires. **Anything writing through Thumbnailator's
`toFile()` must give the destination an extension matching `.outputFormat(...)` up front** — it
will not fail loudly if you don't.

### `/library/scan` doesn't wait for the indexing it kicks off

`IndexingService.indexFolder()` is fire-and-forget by design (`executor.submit`) so large folders
don't block the request, but `LibraryController.scanFolder` queries the database for the folder's
contents in the *same* request, before that background job has written anything. A folder opened
for the first time can therefore return an empty page while indexing is still catching up.
Nothing closed the loop: the periodic auto-refresh (`browser.js`'s `refreshCurrentFolder`) only
acts once `files.length > 0`, so it could never recover from an empty list — the grid stayed
blank until the folder was reopened manually. The indexing-status poll now detects `isIndexing`
going `true -> false` while the current view is still empty and re-fetches once. Any other UI
action that reads from the database right after triggering indexing needs the same follow-up.

### An undefined custom property fails silently — always write a fallback

`background: var(--nope)` is not an error. The declaration is invalid at computed-value time, so
the property falls back to its initial value: no console message, no build failure, just a
transparent background. This is how Latent Model Organizer shipped a whole broken theme. The same
trap sat undetected here too: `base.css`'s `.glass-panel::after` gloss overlay referenced
`--app-glass-gloss`, a token defined nowhere, and rendered nothing for the rule's entire lifetime
— nobody noticed because `.glass-panel` itself turned out to be applied nowhere either. Deleted
once a repo-wide grep confirmed it was unused, rather than given a fallback it didn't need.

The rule everywhere in `assets/css/`: `var(--token, sensible-fallback)`. The fallback must be the
token's *current* value — a stale one (the old `var(--sidebar-width, 200px)` against a 224px
token) documents a dimension the system doesn't use and is worse than none.

### A renamed store action can fail silently inside a `try/catch`

`CollectionsView.vue`'s `saveCollection`/`confirmDeleteCollection` called `store.triggerNavRefresh()`
after creating/editing/deleting a collection, but `browser.js` only ever defined `refreshNav()` — no
action named `triggerNavRefresh` existed. Both call sites wrap the whole operation in a bare
`try { ... } catch (error) { console.error(...) }`, so the `TypeError` from calling an undefined
method was swallowed after the real work (the API call, the toast, the grid refetch) had already
succeeded. Nothing looked broken from `CollectionsView`: the grid updated, the toast fired. The only
symptom was `FolderNav.vue`'s sidebar tree, which watches `store.navRefreshKey` and never saw it
increment, so newly created or deleted collections never appeared there until a full reload.

Fixed by renaming the call sites to `store.refreshNav()`. The general lesson: a `catch` that only
logs is enough to hide a plain typo indefinitely, because the visible parts of the operation had
already committed before the throw. When a Pinia action is renamed, grep the whole `views/`/
`components/` tree for the old name — nothing enforces the store's public method names at
compile time here (no TypeScript).

### The splash screen has its own copy of the brand mark

`electron/splash.html` does not reference `frontend/src/assets/latent-mark.svg` — it has its own
hand-written `<svg>` inline in the HTML, which still had the old single-letter "L" mark after the
titlebar/icon assets were updated to the new "LL" monogram. Nothing wires the splash screen to the
shared asset, so a brand-mark change that only touches `latent-mark.svg` and `icon.png` ships with
stale branding for however long the splash window is visible. If the mark changes again, grep the
repo for the SVG path data rather than assuming the shared asset files cover every surface.

The version number had the identical problem: `splash.html`'s `.version` div was a hardcoded
`v1.1.1` string, never bumped alongside `electron/package.json`'s `version` field, so it silently
lagged a release behind ([user report](https://github.com/erroralex/Latent-Library/issues) after
`v1.2.1`). Fixed by giving the div an `id="version"` and having `main.js`'s `createSplashWindow()`
inject `app.getVersion()` via `webContents.executeJavaScript` on `did-finish-load` — there's no
preload script (`contextIsolation: true`, no `nodeIntegration`), so this was the lowest-friction
way to get a Node value into the splash renderer. Any other static text in `splash.html` that
mirrors data available in the main process (version, build date, etc.) should be wired the same
way rather than hand-typed.

### Re-releasing an existing tag means deleting it first, and CI double-published on top of it

Moving `v1.2.0` to include the icon fixes required deleting the tag both locally and on `origin`
and re-pushing it at the new commit — `git tag -f` alone does not move a tag that's already been
pushed. GitHub's existing Release object survives a tag delete/recreate and just re-associates
with whatever commit the tag now points to, but its attached assets don't update themselves; only
a fresh CI run (`.github/workflows/build.yml` triggers on `push: tags: v*`) replaces them.

That run exposed a latent bug: `.github/workflows/build.yml` published to the release **twice** —
once via `electron-builder`'s own `publish` config (`electron/package.json`, active whenever
`GH_TOKEN` is set on the `npm run dist` step) and again via a separate `softprops/action-gh-release`
step uploading the same `electron/dist/*` globs. The first upload always wins; the second always
422s with `already_exists`, permanently marking the Linux job "failed" even on a fully successful
release. Removed the redundant `softprops/action-gh-release` step — `electron-builder` alone is
sufficient since `publish` is already configured. Don't re-add a second upload step without
removing the `GH_TOKEN` from the `Build Electron App` step first, or the same conflict returns.

Separately, a mid-release GitHub API partial outage caused two prior attempts to fail on transient
`503`s during asset overwrite — unrelated to the double-publish bug, but easy to conflate with it
when triaging a failed run. Check `githubstatus.com` before assuming a CI failure is code-related.

### Every tagged release lands as a GitHub draft and needs a manual publish

`electron-builder`'s `publish` config (no explicit `draft` key) defaults to `draft: true`, and
nothing in `build.yml` un-drafts it. A fresh tag push (confirmed again on `v1.2.1`) creates the
Release object in a draft state at an ugly auto-generated URL
(`/releases/tag/untagged-<hex>`) rather than `/releases/tag/vX.Y.Z`, even though `gh release view
vX.Y.Z` finds it correctly by tag. It only gets the clean tag URL and becomes publicly visible once
published — `gh release edit vX.Y.Z --draft=false` (or the "Publish release" button in the GitHub
UI). Do this (and add release notes) as a normal last step of every release, not just when
something looks broken.

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

- **The frontend has no test tooling at all.** No Vitest, no Vue Test Utils, no spec files, no
  `test` script. Every UI change is verified by eye against a rebuilt jar. That is the binding
  constraint on any component refactor — size the work accordingly, and consider adding Vitest
  before a large one rather than after.
- **Nothing mechanically enforces design-system adherence.** The DS ships
  `_adherence.oxlintrc.json`, which lints exactly the raw-hex and magic-px violations that keep
  reappearing, but no npm script runs it. Wiring it into `npm run lint` is the cheapest way to
  stop the drift recurring.
- **`primevue-overrides.css` is the weak point of the CSS layer.** 385 lines and 212
  `!important` declarations. The three accent/on-danger hexes at `:248`, `:268` and (via
  `buttons.css:45`) the close-button hover are now tokenized. The slider-handle's `#FFFFFF`
  border at `:249` stays raw and documented: it's a decorative contrast ring, not text/icon
  content on a semantic background, and the DS's own reference `Slider.jsx` is a bare native
  `<input type="range">` with no custom thumb styling to adopt a token from either.
  `!important` and line count are unchanged; fixing those needs the component swap, not a token
  swap. It exists because PrimeVue's theme is imported after our CSS. The durable fix is to
  shrink it: `components/ds/` already has `LSelect`, `LSwitch`, `LInput` and `LSlider` at parity
  with the PrimeVue components those rules target, and each swap deletes a block of overrides.
  `Dialog`, `Card` and `Button` are *not* at parity — `LDialog` has no focus trap or scroll lock —
  so those need DS work first.
- ~~README screenshots are pre-redesign.~~ Recaptured and re-vendored: `hero.jpg`, `gallery.jpg`,
  `sorter.jpg`, `comparator.jpg`, `collections.png`, `duplicate.jpg`, `scrubber.png`, `settings.png`
  now live in `frontend/src/assets/screenshots/` and `README.md` points at all eight (Gallery,
  Scrubber and Settings were previously undocumented entirely). The README's other copy was also
  fact-checked against the code in the same pass: the license line was corrected to MIT + Commons
  Clause (the actual `LICENSE` content — plain "MIT License" was wrong), the macOS "standard user
  data location" claim was removed (data always sits next to the app bundle, no
  `~/Library/Application Support` branch in `electron/main.js`), and the WSL claim was reworded
  since there's no WSL-specific parsing anywhere in `PathService` — it works only because folder
  access is fully generic.
- ~~Comparator star ratings are unverified visually.~~ Confirmed rendering correctly in the new
  `comparator.jpg` screenshot (star row under each side's metadata panel).

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
