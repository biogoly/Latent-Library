<script setup>
/**
 * @file MissingFileThumb.vue
 * @description Placeholder shown in place of a thumbnail whose source file cannot be read.
 *
 * Files can disappear or become unreadable between being indexed and being displayed (an external
 * drive goes away, the file is moved outside the app, permissions change). Without this the browser
 * renders its own broken-image glyph plus the raw `alt` text, which reads as a rendering fault
 * rather than as a statement about the file.
 *
 * Deliberately muted rather than danger-coloured: a missing file is an expected state in a
 * local-first library, not an application error, so it uses secondary text on the standard surface.
 */
import { ImageOff } from 'lucide-vue-next';

defineProps({
  /** Icon size in px. Callers in dense strips pass a smaller value. */
  iconSize: {
    type: Number,
    default: 32
  },
  /** Hide the caption where the tile is too small for legible text. */
  showLabel: {
    type: Boolean,
    default: true
  },
  label: {
    type: String,
    default: 'File missing'
  }
});
</script>

<template>
  <div class="missing-file-thumb flex flex-column align-items-center justify-content-center gap-2">
    <ImageOff :size="iconSize" class="missing-file-icon" :stroke-width="1.5" aria-hidden="true" />
    <span v-if="showLabel" class="missing-file-label">{{ label }}</span>
  </div>
</template>

<style scoped>
.missing-file-thumb {
  width: 100%;
  height: 100%;
  background: var(--color-surface-1, #14151B);
  padding: var(--space-2, 8px);
  text-align: center;
}

.missing-file-icon {
  color: var(--color-text-secondary, #9294A3);
  opacity: 0.55;
}

.missing-file-label {
  font-family: var(--font-sans, Inter, sans-serif);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--color-text-secondary, #9294A3);
  opacity: 0.8;
  line-height: 1.2;
}
</style>
