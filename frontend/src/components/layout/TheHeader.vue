<template>
  <header class="bcds-header">
    <div class="bcds-header--container">
      <!-- Skip links for accessibility -->
      <ul class="bcds-header--skiplinks">
        <li>
          <a href="#main" @click.prevent="skipToMain">Skip to main content</a>
        </li>
      </ul>

      <!-- Left group: Logo + Separator + Title -->
      <div class="bcds-header--left">
        <!-- BC Logo -->
        <BCLogo v-bind="logoProps" />

        <!-- Separator line -->
        <div class="bcds-header--line"></div>

        <!-- Title -->
          <HeaderTitle v-bind="titleProps" />
      </div>

      <!-- Right group: Training and Support + User Menu -->
      <div class="bcds-header--actions">
        <TrainingSupport />
        <div class="bcds-header--separator"></div>
        <UserMenu v-bind="userMenuProps" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import BCLogo from './BCLogo.vue'
import HeaderTitle from './HeaderTitle.vue'
import TrainingSupport from './TrainingSupport.vue'
import UserMenu from './UserMenu.vue'

defineProps({
  logoProps: {
    type: Object,
    default: () => ({}),
  },
  titleProps: {
    type: Object,
    default: () => ({}),
  },
  userMenuProps: {
    type: Object,
    default: () => ({}),
  },
})

const skipToMain = () => {
  const mainElement = document.getElementById('main')
  if (mainElement) {
    mainElement.focus()
    mainElement.scrollIntoView()
  }
}
</script>
<style scoped>
/* BC Gov Design System Header Styles */
.bcds-header {
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  align-items: center;
  background-color: var(--surface-color-forms-default);
  border-bottom-color: var(--surface-color-border-default);
  border-bottom-style: solid;
  border-bottom-width: var(--layout-border-width-small);
  min-height: 65px;
  padding: var(--layout-padding-none) var(--layout-padding-medium);
  width: 100%;
}

.bcds-header > .bcds-header--container {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: var(--layout-padding-medium);
  width: 100%;
}

/* Left group: Logo + Separator + Title */
.bcds-header--left {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--layout-padding-medium);
  min-width: 0;
  flex-shrink: 0;
}

.bcds-header > .bcds-header--container > ul.bcds-header--skiplinks {
  list-style: none;
  margin: 0;
  padding: 0;
  position: absolute;
}

.bcds-header > .bcds-header--container > ul.bcds-header--skiplinks li a,
.bcds-header > .bcds-header--container > ul.bcds-header--skiplinks li button {
  background-color: var(--surface-color-forms-default);
  box-sizing: border-box;
  color: var(--typography-color-link);
  font-family: var(--typography-font-families-bc-sans);
  font-weight: var(--typography-font-weights-regular);
  font-size: var(--typography-font-size-body);
  line-height: var(--typography-line-heights-xdense);
  margin-top: -17.5px; /* Center the link visually in a single line-height header */
  padding: var(--layout-padding-xsmall) var(--layout-padding-medium);
  position: absolute;
  margin-left: -100000px;
}

.bcds-header > .bcds-header--container > ul.bcds-header--skiplinks li a:focus,
.bcds-header
  > .bcds-header--container
  > ul.bcds-header--skiplinks
  li
  button:focus {
  margin-left: 0; /* Line up left edge with left edge of logo image */
  text-wrap: nowrap;
}

.bcds-header--left > .bcds-header--line {
  background-color: var(--surface-color-border-default);
  width: 1px;
  height: 32px;
}

/* Actions container (Training Support + User Menu) */
.bcds-header--actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex-shrink: 1;
  overflow: hidden;
}

.bcds-header--separator {
  background-color: var(--surface-color-border-default);
  margin-left: 10px;
  width: 1px;
  height: 22px;
}

/* Mobile responsive header */
@media (max-width: 480px) {
  .bcds-header {
    padding: 0 0.5rem;
  }

  .bcds-header > .bcds-header--container {
    gap: 0.5rem;
  }

  .bcds-header--left {
    gap: 0.5rem;
  }

  .bcds-header--actions {
    gap: 0.25rem;
  }

  .bcds-header--separator {
    margin-left: 4px;
  }
}

/* Very narrow screens (e.g. Galaxy Z Fold 5 at 344px) */
@media (max-width: 360px) {
  .bcds-header--separator {
    display: none;
  }
}
</style>
