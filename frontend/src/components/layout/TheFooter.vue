<template>
  <footer class="bcds-footer">
    <!-- Land Acknowledgement Section -->
    <div v-if="!hideAcknowledgement" class="bcds-footer--acknowledgement">
      <div class="bcds-footer--acknowledgement-text">
        <slot name="acknowledgement">
          <p>
            The B.C. Public Service acknowledges the territories of First Nations around B.C. and is grateful to carry out our work on these lands. We acknowledge the rights, interests, priorities, and concerns of all Indigenous Peoples - First Nations, Métis, and Inuit - respecting and acknowledging their distinct cultures, histories, rights, laws, and governments.
          </p>
        </slot>
      </div>
    </div>

    <!-- Footer Container with Logo, Contact, and Links -->
    <div class="bcds-footer--container">
      <div class="bcds-footer--container-content">
        <div
          v-if="!hideLogoAndLinks"
          :class="[
            'bcds-footer--logo-links',
            hasCustomContent ? 'vertical' : 'horizontal'
          ]"
        >
          <slot>
            <!-- Default Logo and Contact Section -->
            <div class="bcds-footer--logo">
              <slot name="logo">
                <BCLogo id="bcgov-logo-footer" />
              </slot>
              <slot name="contact">
                <p>
                  We can help in over 220 languages and through other accessible options.
                  <a href="https://www2.gov.bc.ca/gov/content/home/get-help-with-government-services">
                    Call, email or text us
                  </a>,
                  or
                  <a href="https://www2.gov.bc.ca/gov/content/home/services-a-z">
                    find a service centre
                  </a>
                </p>
              </slot>
            </div>

            <!-- Default Links Section -->
            <slot name="links">
              <div class="bcds-footer--links more-info">
                <h2 class="bcds-footer--links-title">
                  More info
                </h2>
                <ul>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home"
                      class="footer-link"
                      target="_self"
                    >
                      Home
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home/accessible-government"
                      class="footer-link"
                      target="_self"
                    >
                      Accessibility
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/about-gov-bc-ca"
                      class="footer-link"
                      target="_self"
                    >
                      About gov.bc.ca
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home/copyright"
                      class="footer-link"
                      target="_self"
                    >
                      Copyright
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home/disclaimer"
                      class="footer-link"
                      target="_self"
                    >
                      Disclaimer
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home/get-help-with-government-services"
                      class="footer-link"
                      target="_self"
                    >
                      Contact us
                    </a>
                  </li>
                  <li>
                    <a
                      href="https://www2.gov.bc.ca/gov/content/home/privacy"
                      class="footer-link"
                      target="_self"
                    >
                      Privacy
                    </a>
                  </li>
                </ul>
              </div>
            </slot>
          </slot>
        </div>

        <!-- Copyright Section -->
        <hr v-if="!hideLogoAndLinks && !hideCopyright" />
        <p v-if="!hideCopyright" class="bcds-footer--copyright">
          <slot name="copyright">
            © {{ currentYear }} Government of British Columbia.
          </slot>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import BCLogo from './BCLogo.vue'

export interface Props {
  hideAcknowledgement?: boolean
  hideLogoAndLinks?: boolean
  hideCopyright?: boolean
}

withDefaults(defineProps<Props>(), {
  hideAcknowledgement: false,
  hideLogoAndLinks: false,
  hideCopyright: false,
})

const slots = useSlots()

const currentYear = computed(() => new Date().getUTCFullYear())

const hasCustomContent = computed(() => !!slots.default)
</script>

<style scoped>
.bcds-footer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
}

.bcds-footer > .bcds-footer--acknowledgement {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
  border-top: var(--layout-border-width-large) solid var(--theme-gold-60);
  border-bottom: var(--layout-border-width-large) solid var(--theme-gold-60);
  background-color: var(--theme-gray-110);
  padding: var(--layout-padding-xlarge);
}

.bcds-footer
  > .bcds-footer--acknowledgement
  > .bcds-footer--acknowledgement-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: stretch;
  color: var(--typography-color-primary-invert);
  font: var(--typography-regular-small-body);
  max-width: 1100px;
  width: 100%;
}

.bcds-footer
  > .bcds-footer--acknowledgement
  > .bcds-footer--acknowledgement-text
  > p {
  margin: var(--layout-margin-none);
}

.bcds-footer > .bcds-footer--container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
  background-color: var(--surface-color-background-light-gray);
  padding: var(--layout-padding-xlarge);
}

.bcds-footer > .bcds-footer--container > .bcds-footer--container-content {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--layout-padding-xlarge);
  max-width: 1100px;
  width: 100%;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: flex-start;
  column-gap: var(--layout-padding-xlarge);
  row-gap: var(--layout-padding-medium);
  justify-content: space-between;
  width: 100%;
}

/* `horizontal` class is the default, when no custom content is passed */
.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links.horizontal {
  flex-direction: row;
}

/* `vertical` class is used when custom content is passed */
.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links.vertical {
  flex-direction: column;
  align-items: stretch;
  row-gap: var(--layout-padding-xlarge);
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--logo {
  display: flex;
  flex-direction: column;
  gap: var(--layout-padding-large);
  min-width: 324px;
  width: 576px;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--logo
  > #bcgov-logo-footer {
  width: 146px;
  min-width: 146px;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--logo
  > p {
  font: var(--typography-regular-small-body);
  margin: var(--layout-margin-none);
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--logo
  > p
  > a {
  color: var(--typography-color-secondary);
  font: var(--typography-regular-small-body);
  text-decoration: underline;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--logo
  > p
  > a:hover {
  text-decoration: none;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links {
  margin: var(--layout-margin-none);
}

/*
  When Footer hasn't been passed custom content, expand the links container to take
  up 320px, as the default links are narrower.
*/
.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links.horizontal
  > .bcds-footer--links {
  min-width: 320px;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links
  > .bcds-footer--links-title {
  display: block;
  font: var(--typography-bold-small-body);
  margin: var(--layout-margin-none);
  margin-bottom: var(--layout-padding-medium);
  text-transform: capitalize;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links
  > ul {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: auto;
  grid-column-gap: var(--layout-padding-xlarge);
  grid-row-gap: var(--layout-padding-small);
  list-style: none;
  margin: var(--layout-margin-none);
  padding: var(--layout-padding-none);
}

/*
  When a Footer has been passed custom content, the logo-links container fills the
  full width of the container, so there's room for 4 columns of links.
*/
.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links.vertical
  > .bcds-footer--links
  > ul {
  grid-template-columns: repeat(4, 1fr);
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links
  > ul
  > li {
  font: var(--typography-regular-small-body);
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links
  > ul
  > li
  > a {
  color: var(--typography-color-primary);
  text-decoration: underline;
}

.bcds-footer
  > .bcds-footer--container
  > .bcds-footer--container-content
  > .bcds-footer--logo-links
  > .bcds-footer--links
  > ul
  > li
  > a:hover {
  text-decoration: none;
}

.bcds-footer > .bcds-footer--container > .bcds-footer--container-content > hr {
  background-color: var(--surface-color-border-dark);
  border: var(--layout-border-width-none);
  height: var(--layout-border-width-small);
  margin: var(--layout-margin-none);
  width: 100%;
}

p.bcds-footer--copyright {
  color: var(--typography-color-secondary);
  font: var(--typography-regular-body);
  margin: var(--layout-margin-none);
}

/* Tablet */
@media (max-width: 991px) {
  /* Links drop beneath logo in the default configuration */
  .bcds-footer
    > .bcds-footer--container
    > .bcds-footer--container-content
    > .bcds-footer--logo-links {
    flex-wrap: wrap;
  }

  .bcds-footer
    > .bcds-footer--container
    > .bcds-footer--container-content
    > .bcds-footer--logo-links
    > .bcds-footer--logo {
    max-width: 324px;
  }

  /*
    When custom content is used with multiple FooterLinks components,
    only two columns of links appear on screen at once.
  */
  .bcds-footer
    > .bcds-footer--container
    > .bcds-footer--container-content
    > .bcds-footer--logo-links.vertical
    > .bcds-footer--links
    > ul {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Extra small screens */
@media (max-width: 575px) {
  .bcds-footer > .bcds-footer--acknowledgement {
    padding: var(--layout-padding-xlarge) var(--layout-padding-medium);
  }

  .bcds-footer > .bcds-footer--container {
    padding: var(--layout-padding-medium);
  }

  .bcds-footer > .bcds-footer--container > .bcds-footer--container-content {
    gap: var(--layout-padding-medium);
  }

  .bcds-footer
    > .bcds-footer--container
    > .bcds-footer--container-content
    > .bcds-footer--logo-links
    > .bcds-footer--logo {
    gap: var(--layout-padding-medium);
  }

  /*
    When custom content is used with multiple FooterLinks components,
    only one column of links appear on screen at once.
  */
  .bcds-footer
    > .bcds-footer--container
    > .bcds-footer--container-content
    > .bcds-footer--logo-links.vertical
    > .bcds-footer--links
    > ul {
    grid-template-columns: repeat(1, 1fr);
  }
}
</style>
