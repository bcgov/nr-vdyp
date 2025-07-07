<template>
  <v-navigation-drawer app v-model="drawer" :rail="isRail" permanent>
    <!-- menu -->
    <v-list density="compact" class="nv-project-menu">
      <router-link to="/" class="link-no-decoration">
        <v-tooltip v-if="isRail" :text="'Projects'" location="right">
          <template v-slot:activator="{ props }">
            <v-list-item
              v-bind="props"
              link
              prepend-icon="mdi-folder"
              class="nv-menu-item"
              value="projects"
            >
              <span class="nv-menu-item-title">Projects</span>
            </v-list-item>
          </template>
        </v-tooltip>
        <v-list-item
          v-else
          link
          prepend-icon="mdi-folder"
          class="nv-menu-item"
          value="projects"
        >
          <span class="nv-menu-item-title">Projects</span>
        </v-list-item>
      </router-link>
    </v-list>
    <!-- version info -->
    <div class="nv-drawer-footer">
      <!-- on collapsed navi -->
      <div v-if="isRail">
        <div
          class="collapsed-build-release-area"
          @click="toggleVersion"
          style="cursor: pointer"
        >
          {{ showVersion ? `v${appVersion}` : `#${buildNumber}` }}
        </div>
        <v-tooltip
          :text="isRail ? 'Expand sidebar' : 'Collapse sidebar'"
          location="right"
        >
          <template v-slot:activator="{ props }">
            <v-icon
              v-bind="props"
              @click="toggleRail"
              class="collapsed-icon-area"
              style="color: #000000"
              icon="mdi-chevron-double-right"
            />
          </template>
        </v-tooltip>
      </div>
      <!-- on expanded navi -->
      <div v-else class="expanded-footer">
        <span
          class="expanded-build-release-area"
          @click="toggleVersion"
          style="cursor: pointer"
        >
          {{ showVersion ? `Version: ${appVersion}` : `Build: ${buildNumber}` }}
        </span>
        <v-tooltip
          :text="isRail ? 'Expand sidebar' : 'Collapse sidebar'"
          location="right"
        >
          <template v-slot:activator="{ props }">
            <v-icon
              v-bind="props"
              @click="toggleRail"
              class="expanded-icon-area"
              style="color: #000000"
              icon="mdi-chevron-double-left"
            />
          </template>
        </v-tooltip>
      </div>
    </div>
  </v-navigation-drawer>

  <div
    :style="{ marginLeft: isRail ? '24px' : '0' }"
    class="main-layout-container"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { APP_VERSION } from '@/constants/appVersion'
import { BUILD_NUMBER } from '@/constants/buildNumber'

const appVersion = computed(() => {
  if (APP_VERSION) {
    const baseVersion = APP_VERSION.replace(
      /(-snapshot|-SNAPSHOT|-Snapshot)/i,
      '',
    )
    return `${baseVersion}.${BUILD_NUMBER || ''}`
  }
  return ''
})

const drawer = ref(true)
const showVersion = ref(true)
const isRail = ref(false)
const buildNumber = BUILD_NUMBER

const toggleRail = () => {
  isRail.value = !isRail.value
}
const toggleVersion = () => {
  showVersion.value = !showVersion.value
}
</script>

<style scoped>
/* v-navigation-drawer whole box */
.v-navigation-drawer {
  background-color: #f6f6f6;
}

.nv-project-menu {
  margin-top: 20px;
}

.nv-menu-item {
  background-color: #e3e0d8;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.nv-menu-item-title {
  font-size: 14px !important;
  font-weight: bold !important;
}

.main-layout-container {
  display: flex;
  flex-direction: column;
  flex: none;
  margin-bottom: 0px;
}

.nv-drawer-footer {
  text-align: center;
  font-size: 12px;
  color: #9fa2a2;
  padding: 10px 0;
  position: absolute;
  bottom: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.v-navigation-drawer--rail {
  width: 80px !important;
}

.v-navigation-drawer--rail .nv-menu-item-title {
  display: none;
}

.v-navigation-drawer--rail .nv-menu-item {
  padding: 0 0 0 48px;
  justify-content: center;
  width: 100%;
}

.collapsed-build-release-area {
  margin-bottom: 4px;
  padding: 5px;
}

.collapsed-build-release-area:hover {
  background-color: #e0e0e0;
  border-radius: 4px;
  padding: 5px;
}

.collapsed-icon-area {
  cursor: pointer;
  justify-content: center;
  padding: 20px;
}

.collapsed-icon-area:hover {
  background-color: #e0e0e0;
  border-radius: 4px;
  padding: 20px;
}

.expanded-footer {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
}

.expanded-build-release-area {
  padding: 5px 10px 5px 10px;
  margin-left: auto;
  margin-right: auto;
}

.expanded-build-release-area:hover {
  background-color: #e0e0e0;
  border-radius: 4px;
}

.expanded-icon-area {
  cursor: pointer;
  padding: 20px;
  margin-right: 10px;
}

.expanded-icon-area:hover {
  background-color: #e0e0e0;
  border-radius: 4px;
  padding: 20px;
  margin-right: 10px;
}
</style>
