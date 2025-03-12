import { mount } from 'cypress/vue'
import AppTabs from './AppTabs.vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'

describe('AppTabs.vue', () => {
  const vuetify = createVuetify()

  const tabs = [
    { label: 'Tab 1', component: 'Component1', tabname: 'Tab1' },
    { label: 'Tab 2', component: 'Component2', tabname: 'Tab2' },
    { label: 'Tab 3', component: 'Component3', tabname: 'Tab3' },
  ]

  beforeEach(() => {
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.innerHTML = `
        body {
          background-color: rgb(240, 240, 240) !important;
        }
      `
      doc.head.appendChild(style)
    })
  })

  it('renders the tabs and selects the default tab', () => {
    mount(AppTabs, {
      global: {
        plugins: [vuetify],
      },
      props: {
        currentTab: 0,
        tabs,
      },
    })

    // Verify that the tabs are rendered
    cy.get('.v-tab').should('have.length', tabs.length)
    cy.get('.v-tab').first().should('contain.text', 'Tab 1')

    // Verify the default tab is selected
    cy.get('.v-tab--selected').should('contain.text', 'Tab 1')
  })

  it('switches tabs and emits the correct event', () => {
    const onUpdateCurrentTabSpy = cy.spy().as('updateCurrentTabSpy')

    mount(AppTabs, {
      global: {
        plugins: [vuetify],
      },
      props: {
        currentTab: 0,
        tabs,
        'onUpdate:currentTab': onUpdateCurrentTabSpy,
      },
    })

    // Switch to the second tab
    cy.get('.v-tab').eq(1).click()

    // Verify the second tab is selected
    cy.get('.v-tab--selected').should('contain.text', 'Tab 2')

    // Verify the emitted event
    cy.get('@updateCurrentTabSpy').should('have.been.calledWith', 1)
  })

  it('switches to the third tab and updates the tab content', () => {
    mount(AppTabs, {
      global: {
        plugins: [vuetify],
      },
      props: {
        currentTab: 0,
        tabs,
      },
    })

    // Switch to the third tab
    cy.get('.v-tab').eq(2).click()

    // Verify the third tab is selected
    cy.get('.v-tab--selected').should('contain.text', 'Tab 3')

    // Verify the component content changes
    cy.get('.v-tabs-window-item').eq(2).should('exist')
  })
})
