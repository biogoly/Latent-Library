# Handover Document - Latent Library

## Project Summary

**Latent Library** is a high-performance, local-first desktop asset manager designed for the AI image generation ecosystem. It features SQLite FTS5 search, dynamic Smart Collections, live hot-folder monitoring, and an ONNX-powered AI auto-tagger.

- **Backend:** Java 21 / Spring Boot 3.3 (`backend/`)
- **Frontend:** Vue 3 + Vite + PrimeVue + Pinia (`frontend/`)
- **Desktop Packaging:** Electron 31 (`electron/`)
- **Database:** SQLite with Flyway migrations (`data/`)
- **Active Branch:** `main` — the UI redesign (previously on `feature/ui-redesign`) is fully merged; that branch has been deleted after its content was confirmed identical to `main`.

---

## AI Assistant Setup & Configuration

The project is configured using the **Drop-in Brain** system:

- **Rulebook (`AGENTS.md` / `GEMINI.md`):** Contains universal engineering guidelines, git rules, testing contracts, and technology-specific directives (Spring Boot 3.x, SQLite/Flyway, Vue 3, Modular Monolith).
- **Assistant Shims:**
  - `CLAUDE.md` — Imports `@AGENTS.md` for Claude Code compatibility.
  - `GEMINI.md` — Full identical copy of `AGENTS.md` for Gemini CLI compatibility.
  - `.claude/settings.json` — Permission allowlist for Maven, npm, and git operations.
- **Skills Directory (`.agents/skills/`):**
  - `ai-setup-doctor` — Diagnostic self-check tool for instruction loading and link integrity.
  - `sql-migrations` — Flyway conventions, schema changes, and error recovery.
  - `spring-security` — Spring Boot 3 security filter chain & CORS patterns.
  - `api-design` — REST conventions, `ProblemDetail` error responses, and pagination.

---

## UI Redesign Implementation Status

The application has undergone a complete UI redesign based on the official **Latent Design System** specifications (`Latent-Design-System`).

### 1. Design System Primitive Library (`frontend/src/components/ds/`)

Built 15 reusable Vue 3 `<script setup>` SFC primitives wrapped with type-safe `lucide-vue-next` icons:
- `Titlebar.vue` — Frameless titlebar with window IPC controls and engine status indicator.
- `StatusPill.vue` — Engine health indicator (`online`, `starting`, `offline`).
- `LButton.vue` & `LIconButton.vue` — Standardized button components (`primary`, `secondary`, `danger`, `ghost`).
- `LInput.vue` & `LSelect.vue` — Form input and select controls.
- `LCheckbox.vue`, `LSwitch.vue`, `LSlider.vue` — Checkbox, toggle switch, and range slider controls.
- `LBadge.vue` & `LProgressBar.vue` — Status badge pill and progress bar indicators.
- `LCard.vue` & `LDialog.vue` — Card containers and modal dialog windows.
- `SegmentedControl.vue` & `NavItem.vue` — View toggle and navigation item primitives.

### 2. App Shell & Navigation Layout

- **Frameless Titlebar (`Titlebar.vue`)**:
  - Displays the official Latent vector mark (`latent-mark.svg`), app title ("Latent Library"), and `StatusPill` right next to the title text.
  - Exposes minimize, maximize, and close window controls via Electron IPC (`window.windowAPI` / `window.electronAPI`).
- **Single Stacked Left Sidebar Layout**:
  - `Sidebar.vue` (240px fixed width): Consolidated single left navigation sidebar hosting top view links (`Gallery`, `Collections`, `Comparator`, `Scrubber`, `Speed Sorter`, `Duplicates`), embedded scrollable `FolderNav` tree view (`Collections`, `Pinned`, `This PC`), `Settings` modal toggle button, and developer credit logo link (`alx_logo.png`).
  - `FolderNav.vue`: Embedded directly inside `Sidebar.vue` between navigation links and settings with seamless transparent container background.
- **LoRA Pills Formatting, Click-to-Copy & Tooltips**:
  - `MetadataSidebar.vue` & `layout.css`: Formatted pill labels as full `<lora:name:weight>` tag strings (e.g. `<lora:ZxY_Krea2_v4:1>`).
  - Added click-to-copy handler on `.lora-chip` that copies the full tag string (`<lora:Pony_QualityV4.0:1>`) to clipboard with a 1.5s info toast message.

  - Added native `title="Click to copy LoRA name"` hover tooltip and `cursor: pointer` hover indicator.
  - Removed legacy neon gradient glow pseudo-elements (`.lora-chip::before` / `::after`) in `layout.css`.
  - Restyled `.lora-chip` using Latent Design System tokens: `#23252F` surface background, `#9B7EF5` accent text, `JetBrains Mono` font, 6px border radius, and soft subtle borders.
- **Canvas Zoom-Out Double Background Fix**:
  - `SingleImageViewer.vue`: Removed `shadow-8` from `<img class="absolute inset-0 z-1">` so the image DOM element scales down transparently when zooming out without leaving a floating dark box-shadow rectangle on the outer canvas.
- **Single Native Splash Screen Experience**:
  - `App.vue`: Removed duplicate inline Vue splash screen overlay (`loading-overlay-ds`). Electron's native splash window (`electron/splash.html`) handles startup during backend boot, and the app shell reveals instantly with no double splash flashing.



- **Electron Native Splash Screen (`electron/splash.html`)**:
  - Restyled with `#0A0A0D` canvas background, ambient radial cyan/violet glow, official SVG mark, brand gradient text, and animated loading bar.

### 3. Feature Views & Toolbars

- **`MetadataSidebar.vue` & `TaggerSidebar.vue`**:
  - Restyled metadata inspection sidebar and WD14 AI Auto-Tagger with `#0A0A0D` canvas, `#14151B` surface level, `#23252F` inputs, confidence slider, and monospace LoRA badges (`#9B7EF5`).
- **`BrowserToolbar.vue`**:
  - Refactored toolbar containing search input, AI tag toggle, view mode switcher (`Gallery` / `Browser`), and metadata dropdown filters (`Model`, `Sampler`, `LoRA`, `Stars`).
- **`ComparisonMetadataPanel.vue` & `ComparatorView.vue`**:
  - Side-by-side comparison panels with brand gradient headers (`linear-gradient(90deg, #67E0D8, #9B7EF5)`), surface card containers, and synchronized split-viewer integration.
- **`ScrubView.vue`**:
  - Metadata scrubber featuring a drag-and-drop file zone, shield icon, and clean export copy button.
- **`SpeedSorterView.vue`**:
  - High-efficiency keyboard-driven triage view (`1-5`, `X`/`DEL`, `Ctrl+Z`, `SPACE`) with fixed container bounds preventing taskbar clipping.
- **`DuplicateDetectiveView.vue`**:
  - Perceptual dHash and cryptographic SHA-256 duplicate pair inspector with batch resolve dialog.

### 4. System Stability & Global Overrides

- **Global Response Error Interceptor (`api.js`)**:
  - Updated to ignore global error toast popups on standard `404 Not Found` responses, allowing components to handle missing metadata/thumbnails gracefully.
- **PrimeVue Overrides (`primevue-overrides.css` & `buttons.css`)**:
  - Removed all legacy pseudo-element blur glow rules (`filter: blur(4px)`, `--grad-hover`). Buttons, sliders, context menus, and dropdown panels now feature clean Latent DS surface levels and borders.

### 5. Civitai Resources & Metadata Cache Refresh

- **Civitai Resources Parsing (`CommonStrategy.java`)**:
  - Enhanced `CommonStrategy.java` to parse embedded `Civitai resources:` JSON arrays (handling escaped Unicode sequences like `OB\u534A\u5199...`), extracting `Model = "FLUX (Dev)"` and LoRAs into tags (`<lora:OB半写实肖像画...:0.55>`).
- **Metadata Cache Auto-Refresh (`ImageMetadataService.java`)**:
  - Upgraded `ImageMetadataService.getCachedMetadata` to detect stale/incomplete cached metadata entries in SQLite (where `Model` is missing or `-`) and automatically re-extract parameters from the file, refreshing the database cache.
- **Unit Tests**:
  - Added unit test cases in `CommonStrategyTest.java` and `ImageMetadataServiceTest.java` covering Civitai Unicode resource parsing and stale cache auto-refresh.

### 6. Design System Deep-Dive Audit & Token Cleanup

Follow-up audit pass to remove remaining legacy (pre-redesign) CSS variables and non-DS
component wrappers that survived the initial redesign:

- **`ImageSplitViewer.vue`**: `--accent-primary` → `--color-accent-primary`; removed
  non-existent `--grad-hover`; "Left"/"Right" badges converted from PrimeFlex alpha
  classes to a DS surface-overlay badge style.
- **`SingleImageViewer.vue`**: `--status-danger`/`--text-primary` → `--color-danger`/
  `--color-text-primary`; nav arrow hover states converted from PrimeFlex alpha
  classes to DS surface hover styles.
- **`VirtualGallery.vue`**: selection outline/glow switched from legacy PrimeVue
  `--primary-color`/`--primary-color-rgb` to `--color-accent-primary`/`--glow-primary`;
  removed custom webkit scrollbar rules (now inherits the global DS scrollbar from
  `base.css`).
- **`MetadataSidebar.vue`**: removed a stale, duplicated `<style scoped>` block
  referencing obsolete tokens (`--bg-sidebar-right`, `--glass-blur`, `--border-light`,
  `--bg-input`, `--border-input`, `--grad-hover`, `--grad-text`).
- **`SystemError.vue`**: switched to DS tokens (`--color-bg-canvas`,
  `--color-surface-1`, `--color-border-strong`), replaced hardcoded `#ff4d4d` with
  `--color-danger`, and swapped the raw PrimeVue `<Button>` for `<LButton variant="primary">`.
- **`ImageBrowserView.vue`**: rename-modal style overrides moved to DS tokens; footer
  actions swapped from raw PrimeVue `<Button>` to `<LButton variant="secondary">` /
  `<LButton variant="primary">`.
- **`LButton.vue`**: added an `icon` slot alias (alongside the existing `icon-left`/
  `icon-right`) so the `<template #icon>` convention already used at ~20 call sites
  across the app (SystemError, ImageBrowserView, TaggerSidebar, CollectionsView,
  ComparatorView, DuplicateDetectiveView, ScrubView, SpeedSorterView,
  ComparisonMetadataPanel) actually renders — previously those icons were silently
  dropped because no slot named `icon` existed.
- **Toast notifications (`primevue-overrides.css`)**: added a full `.p-toast` override
  block (message card, summary/detail text, severity accent borders for
  info/success/warn/error, close icon) using DS tokens throughout. Verified every
  fallback hex against `Latent-Design-System/tokens/colors.css` and `effects.css` —
  exact match (`--color-accent-primary #4FD8D0`, `--color-success #3DD68C`,
  `--color-warning #F5B84E`, `--color-danger #F2665B`, `--radius-lg 12px`,
  `--shadow-panel`, `--duration-fast 120ms`).

**Known pre-existing, out-of-scope issue**: the legacy multi-theme system
(`themes/neon.css`, `themes/gold.css`, `themes/light.css`, `themes/fanfriction*.css`)
and `components/layout.css` still define/consume the old token namespace
(`--bg-app`, `--accent-primary`, `--grad-hover`, `--status-danger`, etc.). These
predate the Latent DS redesign and are a separate theming feature — left untouched
since no component in this audit depends on them anymore (all fixed components now
use `--color-*` DS tokens with hardcoded fallbacks).

Verified via `cd frontend && npm run build` (clean) and `cd backend && ./mvnw test`
(153 tests, 0 failures).

### 7. Nav-Tree Root Selection Fix & Cross-View Header Consistency

- **`FolderNav.vue`**: the `Collections` / `Pinned` / `This PC` root nodes now carry
  `selectable: false`. Previously clicking one of these group headers applied
  PrimeVue's `p-highlight` selection background (visible accent tint) even though
  `navigateToNode` already no-ops for `type: 'root'` — the row looked "selected" but
  did nothing. `selectable: false` disables PrimeVue's click-to-select path entirely
  for those nodes (verified directly against `primevue/tree` source —
  `onNodeClick`/`handleSelectionWith(out)MetaKey` gate on `node.selectable !== false`),
  so clicking a root row now only expands/collapses via the toggler, matching how a
  tree section header should behave.
- **Shared view header typography**: audited all tool views (Scrubber, Comparator,
  Duplicate Detective, Collections, Speed Sorter) for visual consistency. Found the
  "hero" title/subtitle pattern had drifted independently per-file — 28px in three
  views but 24px in Duplicate Detective (fighting a stray PrimeFlex `text-3xl`
  class), and Comparator's subtitle had no explicit font-size at all (fell back to
  browser default instead of Scrub's deliberate 14px). Also found `.text-white`
  reimplemented identically (as a PrimeFlex-white → DS-token override) in 7 separate
  files.
  - Extracted `.view-title-hero`, `.view-subtitle`, and a global `.text-white` into
    `base.css` and pointed Scrub/Comparator/Collections/Duplicate Detective at them,
    deleting the now-redundant local copies (net −45 lines).
  - Speed Sorter's compact toolbar header was deliberately left alone — it's a
    denser, keyboard-driven layout, not a page hero, so forcing the same treatment
    there would hurt usability rather than help consistency.
- **Verification**: `npm run build` clean, backend tests 153/153. Live-verified by
  launching `electron/` standalone (`npm start`) and screenshotting the running app —
  confirmed the Pinned/Test Suite nav-tree behavior visually (root row unhighlighted,
  child row correctly highlighted) and that the window renders normally end-to-end.
  Note for future dev-mode browser testing: the Vite proxy (`vite.config.js`) targets
  a fixed `localhost:8080`, but the backend binds a random port per launch by design
  (see `SecurityConfig`'s handshake token, normally supplied to the renderer via
  Electron IPC) — plain-browser testing against `npm run dev` needs the backend
  started with `--server.port=8080` and the handshake token manually injected, or use
  the packaged Electron app instead.

### 8. ScrubView Vertical Centering Fix (found from real screenshots)

The horizontal-centering check in section 7 passed, but missed a real bug: unlike
Comparator/Collections/Duplicate Detective (title pinned near the top via
`mb-4`/`mb-5`, no `justify-content-center` on the outer flex container), `ScrubView`'s
root element had `justify-content-center` on the *whole* content block (title +
card), vertically centering the entire group mid-viewport instead of anchoring the
title to the top like every sibling view. Full-window ShareX screenshots from the
user made the mismatch obvious — title sat around y≈355px in a 1350px-tall window
vs. y≈117px for Collections/Comparator.

- **Fix**: `ScrubView.vue` root now matches the Comparator/Collections structure —
  outer container is `flex flex-column h-full p-4 overflow-hidden` (no
  `justify-content-center`), title wrapper is `flex flex-column align-items-center
  mb-4 flex-shrink-0`, and the `LCard` drop-zone is wrapped in a new
  `flex-grow-1 flex align-items-center justify-content-center` div so it still
  centers vertically *within the remaining space* below the anchored title, mirroring
  how Comparator centers its dropzones.
- **Process note — stale JAR trap**: verifying this fix live initially gave a false
  negative. Electron (standalone `npm start`, not via IntelliJ) spawns
  `java -jar backend/target/backend.jar`, which bundles a *packaged snapshot* of
  `frontend`'s build output taken at the JAR's last `mvn package`. Running
  `cd frontend && npm run build` alone updates `backend/src/main/resources/static/`
  on disk but does **not** repackage `backend/target/backend.jar` — so Electron kept
  serving the pre-fix UI until `cd backend && ./mvnw clean package -DskipTests` was
  run to rebuild the jar. Any time a frontend change needs to be checked in the
  standalone Electron shell (as opposed to IntelliJ's "Full Stack Dev", which runs
  `BackendApplication` directly from `target/classes` and reflects source changes
  after a Maven build automatically), rebuild the backend jar first.
- **Verification**: rebuilt frontend + backend jar, relaunched Electron, screenshotted
  Scrubber and Comparator back-to-back in the same session — titles now align at the
  identical y-position. Backend tests 153/153.

### 9. Post-Redesign Regression Fixes (found via code review)

A medium-depth code review of the redesign branch against pre-redesign behavior
turned up several places where the DS refactor silently changed request contracts
or dropped handlers. All fixed and re-verified:

- **`SpeedSorterView.vue` — delete and undo were completely broken**: the delete
  request sent `params: { source }` but `SpeedSorterController.deleteFile` expects
  `@RequestParam("path")`; every Delete/X press 400'd. Separately, undo posted the
  history entry as a JSON body while `SpeedSorterController.undoMove` expects
  `@RequestParam("source")`/`@RequestParam("original")` query params, so every
  Ctrl+Z 400'd too. The pre-redesign "a Recycle Bin delete can't be undone" guard
  (checking `lastAction.isDelete`) had also been dropped, so undo would try — and
  fail — to restore deleted files while optimistically showing them as restored in
  the UI. Restored the original param shapes and the isDelete guard.
- **`stores/browser.js` — `loadMoreImages()` filter-detection was inconsistent**:
  the branch that routes to `/images/search` checked `searchQuery`/`selectedModel`/
  `selectedRating` but omitted `selectedSampler`/`selectedLora`, so paginating with
  only a Sampler or LoRA filter active fell through to the unfiltered `/library/scan`
  branch. The response-shape check below it had the same omission, so paginating
  with only Model or Rating active read `response.data.content` on a plain array
  response and threw (silently swallowed), stopping infinite scroll. Both checks now
  share one `hasActiveFilter` condition covering all five filter fields.
- **`CollectionsView.vue` — "Dynamic Auto-Population" label stopped toggling the
  switch on click**: `LSwitch`'s root element is itself a `<label>`, so a sibling
  `<label for="isSmartCollection">` no longer forwards click-to-toggle (browsers only
  auto-forward from a `for`-label to native form controls, not to another `<label>`).
  Moved the label text into `LSwitch`'s own default slot instead of a separate
  element.
- **`ImageBrowserView.vue` — Escape no longer exited single-image Browser mode**:
  the rewritten keydown switch dropped the `case 'Escape': if (store.viewMode ===
  'browser') store.setViewMode('gallery')` handler present pre-redesign. Restored it.
- **`components/ds/Titlebar.vue` / `StatusPill.vue` — status text stopped reflecting
  backend health**: `Titlebar` passes a static `label="Backend"` prop, and
  `StatusPill`'s `statusText` computed returned `props.label` verbatim before
  reaching the status switch, so the pill always read literally "Backend" regardless
  of online/starting/offline — only the dot color communicated status. `label` is now
  used as a prefix (`"Backend: Online"`, etc.) combined with the live status.

**Verification**: `npm run build` clean, backend tests 153/153.

### 10. Docs/Packaging Alignment and Git History Cleanup

Two follow-up passes after the redesign merged, unrelated to UI code:

- **Docs vs. actual app audit**: README, CONTRIBUTING.md, BUILDING.md, and packaging metadata
  (`pom.xml`, `frontend`/`electron` `package.json`) had drifted from reality. Fixed:
  - `pom.xml` had entirely empty Spring-Initializr-default `<licenses>`/`<developers>`/`<scm>`/`<url>`
    tags; filled in with real project metadata.
  - The real project license is **MIT with a Commons Clause condition** (prohibits selling/hosting
    the software for a fee) — not plain MIT. `pom.xml`'s license name and the `package.json` `license`
    fields (`SEE LICENSE IN LICENSE`, the correct SPDX convention for a non-standard license) were
    corrected to match, and README's license line reads "Free for personal use" (not "and commercial").
  - README and BUILDING.md both described the Windows build as `Latent Library Setup X.X.X.exe`,
    implying an NSIS installer, but electron-builder's `win.target` is `"portable"` — no installer,
    contradicting README's own "no installer required" claim elsewhere. Fixed the naming in both docs.
  - README's "Multi-Theme System" feature bullet and a "Custom Themes" screenshot described a theme
    picker (Deep Neon/Light/Gold) that no longer exists — `SettingsModal.vue` now shows a static
    theme-info box for the single unified Latent Design System dark theme. Bullet and screenshot removed.
  - CONTRIBUTING.md referenced a versioned jar (`backend-1.0.1.jar`) that doesn't match the actual
    unversioned `finalName` (`backend.jar`); switched to `./mvnw`, added the test command and the
    stale-JAR-in-standalone-Electron trap from section 8 above.
  - **Known gap, not fixed**: all 6 README screenshots still show the pre-redesign app under the old
    "AI Toolbox" branding with the old top-nav layout, not the current Latent Library stacked-sidebar
    UI. Needs a real running app + curated sample images to recapture — a content decision, not a
    docs-text fix.
- **Git history cleanup**: two commits merged into `main` (originally via PR #82, `EnragedAntelope/
  claude/security-review-Yfv4J`) had their author *and* committer identity literally set to
  `Claude <noreply@anthropic.com>`, causing "Claude" to show up in GitHub's contributor graph — a
  direct violation of this repo's own git rule ("no AI attribution anywhere in git"). Reauthored both
  commits (`82d34c8` "Fix overly broad filename validation in renameFile", `faea92f` "Fix security
  vulnerabilities found in review") to the human author on `main` and `development` via
  `git filter-branch --env-filter`, verified byte-identical content before and after (tree diff empty,
  patch-id match), then force-pushed both branches. Both branches had GitHub repository rulesets
  blocking force-push/delete that had to be temporarily disabled per-branch before the push would go
  through. `feature/ui-redesign` was deleted (locally and requested on the remote) after confirming its
  tip tree was identical to `main`'s.

### 11. Cross-App Design Alignment with Latent Model Organizer

Latent Model Organizer was designated the reference standard for shared Latent DS chrome
across the three sibling apps (Latent Library, Latent Model Organizer, Latent Tools). This
pass brought Library's sidebar, titlebar, and iconography in line with it:

- **`Sidebar.vue`**: `.sidebar-ds` background changed from opaque `var(--color-surface-1,
  #14151B)` to translucent `rgba(14, 15, 19, 0.6)` with `backdrop-filter: var(--blur-glass,
  blur(20px))` added; hardcoded `width: 240px; min-width: 240px;` replaced with
  `var(--sidebar-width, 200px)` (the `--sidebar-width` token already existed in
  `tokens/spacing.css` at `224px`, matching Organizer — no token changes needed). The
  `FolderNav` scroll region below the nav items was untouched.
- **`components/ds/Titlebar.vue`**: `.brand-title` switched from a flat `color:
  var(--color-text-primary)` at 13px to Organizer's treatment — `font-size:
  var(--text-body-lg, 16px)`, `font-weight: var(--weight-bold, 700)`, gradient text via
  `background: var(--gradient-brand-text)` + `-webkit-background-clip: text` +
  `-webkit-text-fill-color: transparent`, `letter-spacing: var(--tracking-tight, -0.01em)`.
  All tokens already existed in `tokens/colors.css`/`typography.css`.
- **PrimeIcons → Lucide migration**: the app previously mixed `lucide-vue-next` (main nav)
  with PrimeIcons (`pi pi-*`) everywhere else — 12 files
  (`BrowserToolbar.vue`, `CustomContextSubMenu.vue`, `FolderNav.vue`, `ImageSplitViewer.vue`,
  `MetadataSidebar.vue`, `SettingsModal.vue`, `SingleImageViewer.vue`, `SystemError.vue`,
  `VirtualGallery.vue`, `main.js`, `CollectionsView.vue`, `ImageBrowserView.vue`). All
  `pi pi-*` usages replaced with `lucide-vue-next` components; the `primeicons` npm
  dependency and its stylesheet import (`main.js`) were removed entirely.
  - PrimeVue `<Button icon="pi pi-x">` string-prop usages were converted to the `#icon`
    slot (`<Button><template #icon><IconComp :size="16"/></template></Button>`) since
    PrimeVue's `icon` prop only accepts CSS class strings, not components.
  - PrimeVue `<Tree>` node icons (`FolderNav.vue`) needed a `#nodeicon` slot added on the
    `<Tree>` element — PrimeVue only supports component-based node icons through that
    extension point, otherwise it renders `node.icon` as a raw class string.
  - The custom recursive `CustomContextSubMenu`/context-menu item schema switched from
    `icon: 'pi pi-x'` strings to actual component references rendered via
    `<component :is="item.icon">`, with a separate `iconFilled` boolean added for the one
    filled-vs-outline case (`pi-bookmark` / `pi-bookmark-fill` → `Bookmark` with
    `:fill="'currentColor'"` toggled by the flag).
  - `ConfirmDialog`'s icon (previously passed as a `pi pi-exclamation-triangle` string to
    `confirm.require()`) moved to a `#icon` slot on `<ConfirmDialog>` in `App.vue`, since
    there was only one call site.
  - Added a `.icon-spin` keyframe utility (`animation: spin 1s linear infinite`) for the
    two `Loader2` replacements of `pi-spin pi-spinner` — no spin utility existed previously.
  - **Verification**: `grep -rn "pi pi-\|primeicons\|pi-spin" frontend/src` returns zero
    results; `npm run build` clean (1945 modules, no errors). The build output's
    `primeicons-*.{svg,woff,eot,woff2,ttf}` font assets are gone, confirming no runtime
    PrimeIcons dependency remains.
  - **Not yet done**: `npm install` has not been run to actually remove `primeicons` from
    `node_modules`/lockfile — only `package.json` was edited.

**Companion changes in sibling repos** (same session, tracked in their own HANDOVER files):
Latent Model Organizer got the equivalent PrimeIcons → Lucide migration
(`lucide-vue-next` added as a dependency, matching Library's version). Latent Tools —
a bundler-less Electron renderer — got its hand-rolled inline SVG icons replaced with
real Lucide glyphs via the `lucide` UMD bundle loaded as a static script (an ES import
would break at runtime under its plain-`tsc`/`contextIsolation` setup, so the standard
`import { createIcons } from 'lucide'` approach doesn't apply there).

### 12. Post-Migration Visual Fixes (found via user screenshots)

Follow-up pass after section 11 landed — the user flagged two visual regressions/
inconsistencies from a live screenshot comparison against Organizer:

- **Toolbar icons rendered too small**: `BrowserToolbar.vue`'s icon-only nav buttons
  (Network/Zap/LayoutGrid/Image, the AI-tagger toggle, the metadata-panel toggle) and
  `MetadataSidebar.vue`'s icon-only action buttons stayed at `:size="16"` after the
  Lucide migration. PrimeVue forces icon-only buttons into a fixed 2.5rem (40px)
  square box (`.p-button-icon-only`) regardless of icon size — that box size didn't
  change, but Lucide's stroke-style glyphs carry more internal whitespace inside their
  24×24 viewBox than PrimeIcons' font glyphs did at the same nominal size, so visually
  the icons read noticeably smaller than before even though the migration didn't
  shrink anything numerically. Bumped `BrowserToolbar.vue`'s six icon-only buttons to
  `:size="20"` and `MetadataSidebar.vue`'s six icon-only buttons to `:size="18"`.
  Left the buttons inside explicit `.w-2rem.h-2rem` (32px) boxes (the Copy buttons,
  star-rating buttons) unchanged — their icon-to-box fill ratio was already
  proportionate.
- **Dev-credit logo sized inconsistently across apps**: `.dev-logo-img` here used
  `max-width: 120px; max-height: 44px`, while both Organizer and Tools used a plain
  `width: 64px`. Initially standardized on `width: 64px` to match the other two —
  but the user preferred the original 120/44 sizing (64px read as too tiny), so
  the standard flipped the other way: Library's original `max-width: 120px;
  height: auto; max-height: 44px; object-fit: contain;` is now the shared value,
  and Organizer/Tools were updated to match it instead (see their own HANDOVER
  entries).

**Verification**: `cd frontend && npm run build` clean after each change.

### 13. Ctrl+Scroll UI Zoom (ported from Latent Tools)

Latent Tools already shipped a Ctrl/Cmd+Mouse Wheel app-scale zoom (50%–250%, 5%
steps, Ctrl+0 to reset) via Electron's `webFrame.setZoomFactor`/`getZoomFactor`. Ported
the same mechanism here for consistency across all three apps:

- **`electron/preload.js`**: `windowAPI` gained `getZoomFactor()`/`setZoomFactor(factor)`,
  calling `webFrame` directly (no IPC round-trip needed — `webFrame` is safe to call from
  the preload context).
- **`frontend/src/composables/useUiZoom.js`** (new): a `wheel` listener gated on
  `ctrlKey || metaKey` adjusts zoom by 0.05 per notch, clamped `[0.5, 2.5]`; a `keydown`
  listener resets to `1.0` on Ctrl/Cmd+0 (skipped while a `TEXTAREA`/`INPUT` is
  focused, matching Tools' guard). Mounted once from `App.vue` via `useUiZoom()`.
- **Conflict fix**: `SingleImageViewer.vue`'s `onWheel` and `ImageSplitViewer.vue`'s
  `handleWheel` (both plain-scroll image zoom, not gated on any modifier key) now bail
  out early when `ctrlKey`/`metaKey` is held, so Ctrl+wheel over an open image zooms the
  *app* rather than double-firing both the image zoom and the UI zoom at once — the same
  guard Latent Tools already has on its own image-preview wheel handler.
- Zoom is not persisted across restarts, matching Tools' scope (resets to 100% each
  launch).

**Verification**: `cd frontend && npm run build` clean.

### 14. Settings Modal Broken by Sidebar's `backdrop-filter`, Titlebar Hover Colors

Two more fixes from user screenshots:

- **Settings modal rendered as a small panel pinned to the sidebar instead of a
  centered fullscreen dialog**: `SettingsModal.vue` is declared inside
  `Sidebar.vue`'s `<aside class="sidebar-ds">`, and that element gained
  `backdrop-filter: var(--blur-glass)` in section 11 above. Per the CSS spec,
  `backdrop-filter` (like `filter`/`transform`) establishes a new containing block
  for descendant `position: fixed` elements — so the modal's `inset: 0` resolved
  relative to the 200px-wide sidebar instead of the viewport, even though the modal's
  own CSS was otherwise correct. Organizer doesn't have this bug because its
  equivalent modal is declared in `App.vue`, a sibling of `Sidebar`, never a
  descendant of the blurred element. Fixed by wrapping `SettingsModal.vue`'s root in
  `<Teleport to="body">`, which moves it out of the sidebar's DOM subtree at render
  time regardless of where it's declared in the component tree — the standard Vue 3
  fix for this class of bug, and no prop/emit restructuring needed.
- **Titlebar window-control hover colors**: `Titlebar.vue`'s `.win-btn` only had a
  distinct hover color for `.close` (danger red); `.min`/`.max` fell back to the
  generic gray hover. Added `.win-btn.min:hover` (warning/amber) and
  `.win-btn.max:hover` (success/green), matching Latent Tools' `#win-min:hover`/
  `#win-max:hover` treatment — all three apps now use the same three-color scheme
### 15. Comprehensive Code Review & Lockfile Maintenance (August 5, 2026)

Conducted a full-system architectural and code quality audit across `backend/`, `frontend/`, `electron/`, and `data/` against `AGENTS.md` guidelines and recent handover milestones:

- **Formal Code Review Report**: Generated and published [`docs/code-review-2026-08-05.md`](docs/code-review-2026-08-05.md) covering backend architecture (Spring Boot 3.3, constructor injection, ArchUnit guardrails), frontend state management & DS tokens, SQLite FTS5 search, and Electron IPC safety.
- **Dependency & Lockfile Prune**: Ran `cd frontend && npm install` to prune unreferenced `primeicons` dependencies from `frontend/package-lock.json` following the `lucide-vue-next` migration.
- **BrowserToolbar Filter Clear Icon Overlay, Click Propagation & Label Flex Fill Fix**: Resolved the root cause of prematurely cropped filter labels in `BrowserToolbar.vue`. PrimeVue applies `.p-inputtext` to `.p-dropdown-label`, which caused global `.p-inputtext` rules to inject a nested inner box (`border`, `background: #23252F`, `padding: 6px 12px`) inside `.p-dropdown`, while legacy `width: 1%` constrained label text calculation to ~40px. Updated `primevue-overrides.css` and `BrowserToolbar.vue` to override `.p-dropdown-label` with `background: transparent; border: none; flex: 1 1 0%; width: 100%; min-width: 0; display: block;`, eliminating the nested box and allowing label text to fill 100% of the available dropdown width cleanly up to `padding-right: 34px`.
- **Verification**: Executed backend unit suite (`cd backend && ./mvnw test` — 154/154 tests passed) and frontend production build (`cd frontend && npm run build` — 1,946 modules transformed, built clean in 2.06s).

### 16. Collection View Lockup, Disconnected Drives & Terminal Log Streaming Fixes (August 5, 2026)

Addressed end-to-end fixes for Smart Collections, disconnected external drive paths, state synchronization, and developer tooling:

- **Dropdown Clear Icon & Text Truncation (`BrowserToolbar.vue` & `primevue-overrides.css`)**:
  - Removed wrapping `w-full h-full` span from `#clearicon` slot in `BrowserToolbar.vue` to prevent `.p-dropdown-clear-icon` from expanding to 100% width and centering the clear icon in the middle of dropdown boxes.
  - Set `.p-dropdown-clear-icon` in `primevue-overrides.css` to `display: inline-flex; width: 14px; height: 14px; right: 20px;`.
  - Applied `flex: 1 1 0% !important; min-width: 0 !important; width: 100% !important; padding-right: 34px !important;` to `.p-dropdown-label`, allowing long text filter selections to fill the dropdown width cleanly up to `34px`.
- **Non-Existent File Filtering & SQL Missing Constraints (`UserDataManager.java`, `SearchRepository.java`, `CollectionRepository.java`)**:
  - Added explicit `if (file == null || !file.exists()) return null;` checks to `UserDataManager.findFilesWithFilters()` and `MetadataService.java`, preventing disk I/O timeouts and thread pool starvation when loading collections containing missing/unmounted drive paths (`D:\...`).
  - Added `COALESCE(i.is_missing, 0) = 0` to `SearchRepository.findPaths()` and `CollectionRepository.getFilePaths()`, excluding disconnected files (`is_missing = 1`) from search results and Smart Collection auto-population.
  - Updated `CollectionService.getPreviewPaths()` for Smart Collections to query `SearchRepository.findPaths()` directly (`limit 4`), rendering 4 clean preview thumbnails dynamically without database locking.
- **Smart Collection Search Population & Navigation State Reset (`UserDataManager.java`, `Sidebar.vue`, `CollectionsView.vue`)**:
  - Added explicit `collectionService.getFilePathsFromCollection(collectionName)` invocation in `UserDataManager.findFilesWithFilters()` before calling `SearchRepository.findPaths()`, populating `collection_images` dynamically when querying Smart Collections via API.
  - Fixed `navigateToPath('/')` in `Sidebar.vue` to reset `store.activeCollection = null` and clear search queries whenever **Gallery** (`/`) is clicked from any view, eliminating state locking across view transitions.
  - Updated `navigateToCollection` in `CollectionsView.vue` to call `store.loadCollection(name)` before routing.
- **Electron Live Log Pipelining (`electron/main.js` & `logback-spring.xml`)**:
  - Added `process.stdout.write(`[Backend]: ${msg}`);` to `backendProcess.stdout.on('data')` in `electron/main.js`, streaming real-time Spring Boot SLF4J logs directly into IntelliJ's **3. Run Electron App** terminal console window.
  - Changed `<logger name="com.nilsson" level="INFO"/>` in `logback-spring.xml` to reduce debug log noise.
- **Verification**: Ran full backend test suite (`cd backend && ./mvnw test` — 154/154 tests passed), rebuilt backend archive (`cd backend && ./mvnw clean package -DskipTests`), and built frontend production bundle (`cd frontend && npm run build` — clean build in 1.81s).

### 17. Collections View Blanked by Unreadable Files — Root Cause & Fix (August 5, 2026)

Reported symptom: clicking a pinned folder containing missing/disconnected files left the
Collections view empty (no cards at all), and the app felt frozen. The earlier §16 work had
guessed at the backend collection queries; the actual cause was elsewhere and is now fixed.

- **Root cause — `File.exists()` was treated as proof a file is readable.**
  `ImageController.getImageContent` and `getThumbnail` guarded only on `!file.exists()`.
  `exists()` is a stat; it does not prove the bytes can be read. For entries where the stat
  succeeds but the open fails — directories, permission-denied files, and the "ghost" entries
  seen on a secondary drive in the reporter's library (where `File.exists()` and `File.length()`
  return `true` and a plausible size while `Files.newInputStream` throws `FileNotFoundException`
  and `Files.list` throws `NoSuchFileException`) — Spring committed a `200 OK` with a `Content-Length` from
  `file.length()` **before** streaming the body. The body then never arrived; the response was
  already committed so no error could be sent, and the connection hung until the client gave up.
  Measured live: `time_starttransfer` 3 ms, `time_total` **60.4 s**, `size_download` **0**.
- **Why Collections went blank.** Browsers cap concurrent connections per origin (6 in Chrome).
  A folder of 26 such files saturated all of them with permanently-pending
  `/api/images/thumbnail` requests, and the `indexing-status` poller queued behind them. Every
  later XHR was starved — a network trace across the repro shows **`GET /api/collections` is
  never issued at all**. `CollectionsView.fetchCollections()` therefore never resolved and
  `collections` stayed `[]`, rendering an empty grid. The backend was healthy the whole time
  (`/api/collections` answered 200 in 0.2 s from curl at every stage); this was never a backend
  collections bug.
- **Fix (`ImageController`)**: new private `requireReadableFile(File, String)` replaces the
  `!file.exists()` guards on both `/content` and `/thumbnail`. It requires `isFile()` and then
  actually opens the file (`Files.newInputStream`, the same syscall the body write would make),
  throwing `ResourceNotFoundException` on failure — so the request 404s in milliseconds instead
  of committing a 200 it cannot fulfil. Verified live: the ghost-file thumbnail went from
  **60.4 s / 200 / 0 bytes → 7 ms / 404**, `/content` → 3 ms / 404, and a healthy `C:` thumbnail
  still returns 200 with 95,025 bytes in 107 ms.
- **Regression tests** (`ImageControllerTest`, +3): a `@TempDir` directory reproduces the same
  stat-ok/open-fails shape portably and deterministically (confirmed to hang identically against
  the live server before the fix). Both endpoints now assert 404, plus a positive test that a
  genuinely readable file still returns 200. Both new 404 tests fail with `expected:<404> but
  was:<200>` before the fix and pass after.
- **Verification**: `./mvnw test` — **157/157 passed** (was 154). Jar rebuilt
  (`./mvnw clean package -DskipTests`) and the full UI repro replayed in a browser against the
  running app: visiting the affected folder now fails fast with "Failed to load image" instead of
  freezing, and Collections subsequently renders all six collections with previews.

Three further defects surfaced by the same investigation were fixed in a follow-up pass — see §18.

### 18. Watcher Leak, Tooltip Crashes & Silent Thumbnail Failures (August 5, 2026)

Follow-up to §17, fixing the three secondary bugs that investigation uncovered.

- **Leaked folder watchers (`IndexingService`)** — a thread dump showed **64
  `FileSystemWatchService` threads out of 122 total**, two per visited folder. Two independent
  causes, both fixed:
  1. `watchLoop`'s `catch (Exception e)` also caught the `InterruptedException` thrown by
     `watchService.take()` when `stopWatching()` interrupts. **`InterruptedException` clears the
     thread's interrupt flag**, so the `while (!Thread.currentThread().isInterrupted())` guard
     saw a live thread, fell through, rebuilt a fresh `WatchService` and ran forever — one leaked
     watcher (and one leaked `WatchService`) per folder navigation. The resurrected thread also
     overwrote the shared `watchService` field, so a later `stopWatching()` closed the wrong one.
     Now `InterruptedException` restores the flag and breaks, `ClosedWatchServiceException` breaks
     (it *is* the stop signal), and only genuine I/O errors take the retry path — which now logs
     at `WARN` instead of retrying silently.
  2. `startWatching`/`stopWatching` were unsynchronised while overlapping `/library/scan` calls
     invoke them concurrently (the UI reliably fires two scans per navigation — visible as the
     duplicated `Indexing folder:` log lines). Interleaved stop/start could publish two watchers
     while only the last was recorded in `watchThread`, so the other was never interrupted. Both
     methods are now `synchronized`.
  - **Regression tests** (`IndexingServiceTest`, +2): both assert *behaviourally* that no watcher
    survives `stopWatching()` — a file created afterwards must not reach the listener. Virtual
    threads are invisible to `Thread.getAllStackTraces()`, so counting threads is not an option.
    One test covers repeated sequential `startWatching`, the other 8 concurrent calls. Both fail
    before the fix and pass after.
  - **Verified live**: 6 repeated scans of one folder now leave **exactly 1** `Folder-Watcher`
    thread (total process threads 122 → 51), and watching still functions.
- **PrimeVue tooltip crashes (`BrowserToolbar.vue`)** — the renderer logged a flood of
  `TypeError: Cannot read/set properties of null (reading '$_ptooltipModifiers')` on every view
  transition, four at a time. Cause: PrimeVue's Tooltip resolves its target via
  `getTarget(el) => hasClass(el, 'p-inputwrapper') ? findSingle(el, 'input') : el`. A `Dropdown`
  root carries `p-inputwrapper`, but a **non-editable, non-filter** Dropdown renders a
  `<span class="p-dropdown-label">` and contains no `<input>` — so `findSingle` returns `null`
  and every `beforeMount`/`updated`/`unmounted` hook threw. The four errors were the four filter
  dropdowns (Model, Sampler, LoRA, Stars). Fixed by moving `v-tooltip.bottom` from each
  `<Dropdown>` onto its existing plain wrapper `<div>`; tooltips still work and the directive now
  has a real element to bind. The other `v-tooltip` call sites are on `<Button>`/`<div>`, which
  are never `p-inputwrapper`, and were left alone.
  - **Verified live**: after replaying the full navigation sequence, zero `$_ptooltipModifiers`
    errors remain in the console (the only entries left come from an unrelated browser extension).
- **`ThumbnailService` swallowed failures** — generation errors were logged at `logger.trace`,
  which is why nothing in the logs pointed at the unreadable files behind §17. Raised to `WARN`
  with the full path and exception, and the outer `future.get(15s)` no longer catches everything
  into a bare `return null`: a `TimeoutException` now logs the 15 s timeout explicitly and
  `InterruptedException` restores the interrupt flag.
  - **Verified live**: requesting a thumbnail for a deliberately corrupt PNG now logs
    `WARN ... Thumbnail generation failed for <path>: UnsupportedFormatException: No suitable
    ImageReader found`, where previously it was silent.

**Verification**: `./mvnw test` — **159/159 passed** (was 157); `npm run build` clean; jar rebuilt
and all three fixes confirmed against the running app.

### 19. "File missing" Thumbnail Placeholder (August 5, 2026)

With §17 in place, unreadable files now 404 promptly — but the UI rendered that as the browser's
own broken-image glyph plus the leaked `alt` text ("AI Image"), which reads as a rendering fault
rather than as a statement about the file.

- **New `components/ds/MissingFileThumb.vue`**: muted DS placeholder — `ImageOff` (lucide) at
  `--color-text-secondary` on `--color-surface-1`, with an optional 11px "File missing" caption.
  Deliberately *not* danger-coloured: a missing file is an expected state in a local-first library,
  not an application error. Props `iconSize` / `showLabel` / `label` let dense strips drop the
  caption.
- **Wired into three call sites** via each `<img>`'s `@error` handler (the same signal the 404
  produces), which is why a shared component was worth extracting rather than duplicating markup:
  - `ImageCard.vue` (gallery grid) — icon + caption, with the blurred backdrop `<img>` suppressed
    too so no broken-image artefact shows through. The missing flag is **reset by a `watch` on
    `file.path`**: cards are recycled by the virtualized grid, so without it one missing file would
    poison every row that later reused its DOM node.
  - `FilmstripView.vue` (bottom strip) — glyph only at 24px; tiles are too small for legible text.
    Tracked in a `reactive(Set)` keyed by path, since the strip re-slices around the selection.
  - `CollectionsView.vue` (preview stacks) — glyph only at 28px.
- `alt` text was emptied on these decorative thumbnails (the filename is already rendered as visible
  text beside them), so a failed load no longer paints stray alt text over the tile.
- The large `SingleImageViewer` canvas keeps its existing explicit red "Failed to load image" state —
  a full-viewport failure the user actively navigated to warrants a louder treatment than a grid tile.

**Verification**: `npm run build` clean, jar rebuilt, verified live — all 26 tiles of an unreadable
folder render the placeholder consistently in both grid and filmstrip, collection previews are
unaffected, and a healthy folder still loads every real thumbnail with **no false positives**.

**Known issue, not fixed here**: `ImageCard.vue` still renders its star rating with PrimeIcons
classes (`pi pi-star-fill` / `pi pi-star`). The `primeicons` package and its stylesheet were removed
in §11, so those stars currently render as nothing. Out of scope for this change but worth a
follow-up — §11's "grep returns zero results" claim missed this file.

---

## Verification & Build Commands

- **Build Frontend:**
  ```bash
  cd frontend && npm run build
  ```
- **Run Backend Unit Tests:**
  ```bash
  cd backend && ./mvnw test
  ```
- **Run Desktop App Locally:**
  ```bash
  # Terminal 1 (Backend)
  cd backend && ./mvnw spring-boot:run

  # Terminal 2 (Frontend)
  cd frontend && npm run dev

  # Terminal 3 (Electron Shell)
  cd electron && npm start
  ```

