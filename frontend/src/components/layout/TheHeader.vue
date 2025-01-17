<template>
  <v-app-bar
    extension-height="10"
    style="height: 64px !important"
    color="#003366"
  >
    <BCLogo v-bind="logoProps" />
    <v-spacer></v-spacer>
    <HeaderTitle v-bind="titleProps" />
    <v-spacer></v-spacer>

    <v-toolbar-items class="header-right-toolbar">
      <TrainingSupport />
      <span class="header-separator">|</span>
      <UserMenu v-bind="userMenuProps" />
    </v-toolbar-items>
  </v-app-bar>
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
</script>
<style scoped>
/* v-app-bar whole box */
header.v-toolbar.v-toolbar--density-default.v-theme--defaultTheme.v-locale--is-ltr.v-app-bar {
  justify-content: center;
}

/* right-toolbar whole box */
div.v-toolbar-items.header-right-toolbar {
  padding-right: 10px;
}

.header-right-toolbar {
  padding-right: 16px;
  display: flex;
  align-items: center;
}

.header-right-toolbar span {
  padding: 0 8px;
}

span.header-separator {
  padding: 0px;
}
</style>
