<template>
  <button
    :class="buttonClasses"
    :disabled="isDisabled"
    :data-disabled="isDisabled || undefined"
    @click="onClick"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    :data-hovered="isHovered || undefined"
    :aria-label="ariaLabel || label"
  >
    <v-icon v-if="leftIcon" class="button-icon-left">{{ leftIcon }}</v-icon>
    <span v-if="label" class="button-label">{{ label }}</span>
    <v-icon v-if="rightIcon" class="button-icon-right">{{ rightIcon }}</v-icon>
  </button>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'link'
type ButtonSize = 'xsmall' | 'small' | 'medium' | 'large'

const props = defineProps({
  label: {
    type: String,
    default: '',
  },
  variant: {
    type: String as () => ButtonVariant,
    default: 'primary',
    validator: (value: string) =>
      ['primary', 'secondary', 'tertiary', 'link'].includes(value),
  },
  size: {
    type: String as () => ButtonSize,
    default: 'medium',
    validator: (value: string) =>
      ['xsmall', 'small', 'medium', 'large'].includes(value),
  },
  danger: {
    type: Boolean,
    default: false,
  },
  isDisabled: {
    type: Boolean,
    default: false,
  },
  leftIcon: {
    type: String,
    default: '',
  },
  rightIcon: {
    type: String,
    default: '',
  },
  ariaLabel: {
    type: String,
    default: '',
  },
})

const emit = defineEmits<(e: 'click', id: number) => void>()
const isHovered = ref(false)

const buttonClasses = computed(() => {
  const classes = ['bcds-button', props.variant, props.size]

  // Add danger class if applicable
  if (props.danger) {
    classes.push('danger')
  }

  // Add icon class if this is an icon-only button
  if ((props.leftIcon || props.rightIcon) && !props.label) {
    classes.push('icon')
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

.bcds-button.primary.danger {
  background-color: var(--surface-color-primary-danger-button-default);
}

.bcds-button.primary[data-disabled] {
  background-color: var(--surface-color-primary-danger-button-disabled);
  color: var(--typography-color-disabled);
}

.bcds-button.primary[data-hovered] {
  background-color: var(--surface-color-primary-button-hover);
}

.bcds-button.primary.danger[data-hovered] {
  background-color: var(--surface-color-primary-danger-button-hover);
}

/* Variant - Secondary */
.bcds-button.secondary {
  background-color: var(--surface-color-secondary-button-default);
  border: 1px solid var(--surface-color-border-dark);
  color: var(--typography-color-primary);
}

.bcds-button.secondary.danger {
  border-color: var(--support-border-color-danger);
  color: var(--surface-color-primary-danger-button-default);
}

.bcds-button.secondary[data-disabled] {
  background-color: var(--surface-color-secondary-button-disabled);
  border-color: var(--surface-color-border-default);
  color: var(--typography-color-disabled);
}

.bcds-button.secondary[data-hovered] {
  background-color: var(--surface-color-secondary-button-hover);
}

.bcds-button.secondary.danger[data-hovered] {
  background-color: var(--support-surface-color-danger);
}

/* Variant - Tertiary */
.bcds-button.tertiary {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-primary);
}

.bcds-button.tertiary.danger {
  color: var(--surface-color-primary-danger-button-default);
}

.bcds-button.tertiary[data-disabled] {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-disabled);
}

.bcds-button.tertiary[data-hovered] {
  background-color: var(--surface-color-tertiary-button-hover);
}

.bcds-button.tertiary.danger[data-hovered] {
  background-color: var(--support-surface-color-danger);
}

/* Variant - Link */
.bcds-button.link {
  background-color: var(--surface-color-tertiary-button-default);
  color: var(--typography-color-link);
  text-decoration: underline;
  text-underline-offset: 0.3em;
}

.bcds-button.link.danger {
  color: var(--surface-color-primary-danger-button-default);
}

.bcds-button.link[data-disabled] {
  color: var(--typography-color-disabled);
}
</style>
