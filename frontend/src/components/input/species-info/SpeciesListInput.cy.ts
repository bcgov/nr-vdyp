import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import SpeciesListInput from './SpeciesListInput.vue'
import { computed } from 'vue'
import { MAPPINGS } from '@/constants'

const vuetify = createVuetify()

describe('SpeciesListInput.vue', () => {
  beforeEach(() => {
    cy.viewport(1024, 768)

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

  const computedSpeciesOptions = computed(() =>
    (
      Object.keys(MAPPINGS.SPECIES_MAP) as Array<
        keyof typeof MAPPINGS.SPECIES_MAP
      >
    ).map((code) => ({
      label: `${code} - ${MAPPINGS.SPECIES_MAP[code]}`,
      value: code,
    })),
  )

  const props = {
    speciesList: [
      { species: 'PL', percent: '30.0' },
      { species: 'AC', percent: '30.0' },
      { species: 'H', percent: '30.0' },
      { species: 'S', percent: '10.0' },
      { species: null, percent: '0.0' },
      { species: null, percent: '0.0' },
    ],
    computedSpeciesOptions: computedSpeciesOptions.value,
    isConfirmEnabled: true,
    max: 100,
    min: 0,
    step: 5,
  }

  it('renders correctly with initial props', () => {
    mount(SpeciesListInput, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Verify that the Species list is rendered correctly
    cy.get('[data-testid="species-select"]').should(
      'have.length',
      props.speciesList.length,
    )
    cy.get('[data-testid="species-percent"]').should(
      'have.length',
      props.speciesList.length,
    )

    // Check the first Species and Percent values
    cy.get('[data-testid="species-select"] input')
      .first()
      .should('have.value', props.speciesList[0].species)
    cy.get('[data-testid="species-percent"] input')
      .first()
      .should('have.value', props.speciesList[0].percent)
  })

  it('updates species and percent values', () => {
    mount(SpeciesListInput, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Change the first Species value
    cy.get('[data-testid="species-select"]').should('exist').first().click()
    cy.contains('FD - Douglas Fir').click()
    cy.get('[data-testid="species-select"] input')
      .first()
      .should('have.value', 'FD')
    cy.get('[data-testid="species-select"]').should(
      'contain.text',
      'FD - Douglas Fir',
    )

    // Clear the input field
    cy.get('[data-testid="species-percent"]').first().find('input').clear()

    // Type the new value into the input field
    cy.get('[data-testid="species-percent"]').first().find('input').type('50.0')

    cy.get('[data-testid="species-percent"] input')
      .first()
      .should('have.value', '50.0')
  })

  it('increments and decrements percent values', () => {
    mount(SpeciesListInput, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Click the first Percent Increment button
    cy.get('.spin-up-arrow-button').should('exist').first().click()
    cy.get('[data-testid="species-percent"] input')
      .should('exist')
      .first()
      .should('have.value', '35.0')

    // Click the first Percent Decrement button
    cy.get('.spin-down-arrow-button').should('exist').first().click()
    cy.get('[data-testid="species-percent"] input')
      .should('exist')
      .first()
      .should('have.value', '30.0')
  })

  it('does not exceed max limits', () => {
    // Max Test
    mount(SpeciesListInput, {
      props: {
        ...props,
        speciesList: [{ species: 'PL - Lodgepole Pine', percent: '100.0' }],
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.spin-up-arrow-button').should('exist').first().click()

    cy.get('[data-testid="species-percent"]')
      .first()
      .find('input')
      .should('have.value', '100.0')
  })

  it('does not exceed min limits', () => {
    // Min Test
    mount(SpeciesListInput, {
      props: {
        ...props,
        speciesList: [{ species: 'PL - Lodgepole Pine', percent: '0.0' }],
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('.spin-down-arrow-button').should('exist').first().click()

    cy.get('[data-testid="species-percent"]')
      .first()
      .find('input')
      .should('have.value', '0.0')
  })

  it('disables inputs and buttons when isConfirmEnabled is false', () => {
    mount(SpeciesListInput, {
      props: {
        ...props,
        isConfirmEnabled: false,
      },
      global: {
        plugins: [vuetify],
      },
    })

    cy.get('[data-testid="species-select"]').find('input').should('be.disabled')
    cy.get('[data-testid="species-percent"]')
      .find('input')
      .should('be.disabled')

    cy.get('.spin-up-arrow-button').should('have.class', 'disabled')
    cy.get('.spin-down-arrow-button').should('have.class', 'disabled')
  })
})
