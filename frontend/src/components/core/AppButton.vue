<template>
  <button
    v-bind="activatorProps"
    :class="buttonClasses"
    :disabled="isDisabled"
    :data-disabled="isDisabled || undefined"
    @click="onClick"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    :data-hovered="isHovered || undefined"
  >
    <!-- Left icon position -->
    <template v-if="iconPosition === 'left'">
      <img v-if="iconSrc" :src="iconSrc" :alt="label" class="button-icon-img button-icon-left" />
      <v-icon v-else-if="mdiName" class="button-icon-left">{{ mdiName }}</v-icon>
      <span v-if="label" class="button-label">{{ label }}</span>
    </template>
    <!-- Right icon position -->
    <template v-else-if="iconPosition === 'right'">
      <span v-if="label" class="button-label">{{ label }}</span>
      <img v-if="iconSrc" :src="iconSrc" :alt="label" class="button-icon-img button-icon-right" />
      <v-icon v-else-if="mdiName" class="button-icon-right">{{ mdiName }}</v-icon>
    </template>
    <!-- Top icon position: icon above label -->
    <template v-else-if="iconPosition === 'top'">
      <img v-if="iconSrc" :src="iconSrc" :alt="label" class="button-icon-img button-icon-top" />
      <v-icon v-else-if="mdiName" class="button-icon-top">{{ mdiName }}</v-icon>
      <span v-if="label" class="button-label">{{ label }}</span>
    </template>
    <!-- Bottom icon position: icon below label -->
    <template v-else-if="iconPosition === 'bottom'">
      <span v-if="label" class="button-label">{{ label }}</span>
      <img v-if="iconSrc" :src="iconSrc" :alt="label" class="button-icon-img button-icon-bottom" />
      <v-icon v-else-if="mdiName" class="button-icon-bottom">{{ mdiName }}</v-icon>
    </template>
  </button>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'link' | 'danger'
type ButtonSize = 'xsmall' | 'small' | 'medium' | 'large'
type IconPosition = 'left' | 'right' | 'top' | 'bottom'

const props = defineProps({
  label: {
    type: String,
    default: '',
  },
  activatorProps: {
    type: Object,
    default: () => ({}),
  },
  variant: {
    type: String as () => ButtonVariant,
    default: 'primary',
    validator: (value: string) =>
      ['primary', 'secondary', 'tertiary', 'link', 'danger'].includes(value),
  },
  size: {
    type: String as () => ButtonSize,
    default: 'medium',
    validator: (value: string) =>
      ['xsmall', 'small', 'medium', 'large'].includes(value),
  },
  isDisabled: {
    type: Boolean,
    default: false,
  },
  mdiName: {
    type: String,
    default: '',
  },
  iconSrc: {
    type: String,
    default: '',
  },
  iconPosition: {
    type: String as () => IconPosition,
    default: 'left',
    validator: (value: string) =>
      ['left', 'right', 'top', 'bottom'].includes(value),
  },
})

const emit = defineEmits<(e: 'click', id: number) => void>()
const isHovered = ref(false)

const buttonClasses = computed(() => {
  const classes = ['bcds-button', props.variant, props.size]

  // Add icon class if this is an icon-only button
  if ((props.mdiName || props.iconSrc) && !props.label) {
    classes.push('icon')
  }

  // Add icon-top/icon-bottom class for vertical icon layout
  if (props.iconPosition === 'top') {
    classes.push('icon-top')
  } else if (props.iconPosition === 'bottom') {
    classes.push('icon-bottom')
  }

  return classes.join(' ')
})

const onClick = (event: Event) => {
  if (props.isDisabled) {
    console.debug('Button is disabled, onClick event not triggered')
  } else {
    event.preventDefault()
    event.stopPropagation()
    emit('click', 1)
  }
}
</script>

<style scoped>
/*
 * Styling based on BC Gov Design Standards - Button Component
 */
.bcds-button {
  border: none;
  border-radius: var(--layout-border-radius-medium);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--layout-padding-small);
  font: var(--typography-regular-body);
}

.bcds-button[data-disabled] {
  cursor: not-allowed;
}

.bcds-button[data-focus-visible] {
  outline: solid var(--layout-border-width-medium)
    var(--surface-color-border-active);
  outline-offset: var(--layout-margin-hair);
}

/* Icon button */
.bcds-button.icon {
  align-items: center;
  justify-content: space-around;
}

/* Icon spacing */
.button-icon-left {
  margin-right: var(--layout-padding-xsmall, 4px);
}

.button-icon-right {
  margin-left: var(--layout-padding-xsmall, 4px);
}

.bcds-button.icon .button-icon-left,
.bcds-button.icon .button-icon-right {
  margin: 0;
}

/* Sizes */
.bcds-button.xsmall {
  min-height: 24px;
  padding: var(--layout-padding-hair) var(--layout-padding-small);
  font: var(--typography-regular-label);
}

.bcds-button.xsmall.icon {
  padding: unset;
  height: 24px;
  width: 24px;
  min-width: 24px;
}

.bcds-button.small {
  min-height: 32px;
  padding: var(--layout-padding-none) 0.75rem /* to be replaced with new token later */;
  font: var(--typography-regular-small-body);
}

.bcds-button.small.icon {
  padding: unset;
  height: 32px;
  width: 32px;
  min-width: 32px;
}

.bcds-button.medium {
  min-height: 40px;
  padding: var(--layout-padding-none) var(--layout-padding-medium);
}

.bcds-button.medium.icon {
  padding: unset;
  height: 40px;
  width: 40px;
  min-width: 40px;
}

.bcds-button.large, /* this doesn't work on its own */
.bcds-button.large.primary,
.bcds-button.large.secondary,
.bcds-button.large.tertiary,
.bcds-button.large.link {
  min-height: 48px;
  padding: var(--layout-padding-none) var(--layout-padding-large);
}

.bcds-button.large.icon {
  padding: unset;
  height: 48px;
  width: 48px;
  min-width: 48px;
}

/* Variant - Primary */
.bcds-button.primary {
  background: var(--surface-color-primary-button-default);
  color: var(--icons-color-primary-invert);
}

.bcds-button.primary[data-disabled] {
  background-color: var(--surface-color-primary-button-disabled);
  color: var(--typography-color-disabled);
}

.bcds-button.primary[data-hovered] {
  background-color: var(--surface-color-primary-button-hover);
}

/* Variant - Secondary */
.bcds-button.secondary {
  background-color: var(--surface-color-secondary-button-default);
  border: 1px solid var(--surface-color-border-dark);
  color: var(--typography-color-primary);
}

.bcds-button.secondary[data-disabled] {
  background-color: var(--surface-color-secondary-button-disabled);
  border-color: var(--surface-color-border-default);
  color: var(--typography-color-disabled);
}

.bcds-button.secondary[data-hovered] {
  background-color: var(--surface-color-secondary-button-hover);
}

/* Variant - Tertiary */
.bcds-button.tertiary {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-primary);
}

.bcds-button.tertiary[data-disabled] {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-disabled);
}

.bcds-button.tertiary[data-hovered] {
  background-color: var(--surface-color-tertiary-button-hover);
}

/* Variant - Link */
.bcds-button.link {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-link);
  text-decoration: underline;
  text-underline-offset: 0.3em;
}

.bcds-button.link[data-disabled] {
  color: var(--typography-color-disabled);
}

/* Variant - Danger */
.bcds-button.danger {
  background-color: var(--surface-color-primary-danger-button-default);
  color: var(--icons-color-primary-invert);
}

.bcds-button.danger[data-disabled] {
  background-color: var(--surface-color-primary-danger-button-disabled);
  color: var(--typography-color-disabled);
}

.bcds-button.danger[data-hovered] {
  background-color: var(--surface-color-primary-danger-button-hover);
}

/* Icon Top Layout - vertical icon above label */
.bcds-button.icon-top {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--layout-padding-xsmall);
  padding: var(--layout-padding-xsmall);
  min-height: auto;
  min-width: auto;
}

.bcds-button.icon-top .button-icon-top {
  margin: 0;
}

.bcds-button.icon-top .button-label {
  font: var(--typography-regular-small-body);
}

/* Image icon styles */
.button-icon-img {
  /* Use auto to preserve original image dimensions and prevent blurriness */
  width: auto;
  height: auto;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}

.button-icon-img.button-icon-left {
  margin-right: var(--layout-padding-xsmall, 4px);
}

.button-icon-img.button-icon-right {
  margin-left: var(--layout-padding-xsmall, 4px);
}

.button-icon-img.button-icon-top {
  margin: 0;
}

/* Icon Bottom Layout - vertical icon below label */
.bcds-button.icon-bottom {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--layout-padding-xsmall);
  padding: var(--layout-padding-small) var(--layout-padding-medium);
  min-height: auto;
  min-width: auto;
}

.bcds-button.icon-bottom .button-icon-bottom {
  margin: 0;
}

.bcds-button.icon-bottom .button-label {
  font: var(--typography-regular-small-body);
}

.button-icon-img.button-icon-bottom {
  margin: 0;
}
</style>
