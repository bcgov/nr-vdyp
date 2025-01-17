import { mount } from 'cypress/vue'
import { createVuetify } from 'vuetify'
import 'vuetify/styles'
import AppSpinField from './AppSpinField.vue'

const vuetify = createVuetify()

describe('AppSpinField.vue', () => {
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

  const props = {
    label: 'Test Field',
    modelValue: '10.00',
    max: 20,
    min: 0,
    step: 1,
    interval: 200,
    decimalAllowNumber: 2,
    disabled: false,
  }

  it('renders correctly with initial props', () => {
    mount(AppSpinField, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Verify label is rendered correctly
    cy.get('label').should('contain.text', props.label)

    // Verify initial value
    cy.get('input').should('have.value', props.modelValue)
  })

  it('increments the value when the up button is clicked', () => {
    mount(AppSpinField, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Click the increment button
    cy.get('.spin-up-arrow-button').click()
    cy.get('input').should('have.value', '11.00')
  })

  it('decrements the value when the down button is clicked', () => {
    mount(AppSpinField, {
      props,
      global: {
        plugins: [vuetify],
      },
    })

    // Click the decrement button
    cy.get('.spin-down-arrow-button').click()
    cy.get('input').should('have.value', '9.00')
  })

  it('does not exceed max limits', () => {
    mount(AppSpinField, {
      props: {
        ...props,
        modelValue: props.max.toString(),
      },
      global: {
        plugins: [vuetify],
      },
    })

    // Try to increment beyond the max value
    cy.get('.spin-up-arrow-button').click()
    cy.get('input').should(
      'have.value',
      props.max.toFixed(props.decimalAllowNumber),
    )
  })

  it('does not exceed min limits', () => {
    mount(AppSpinField, {
      props: {
        ...props,
        modelValue: props.min.toString(),
      },
      global: {
        plugins: [vuetify],
      },
    })
    cy.get('.spin-down-arrow-button').click()
    cy.get('input').should(
      'have.value',
      props.min.toFixed(props.decimalAllowNumber),
    )
  })

  it('disables the buttons when the component is disabled', () => {
    mount(AppSpinField, {
      props: {
        ...props,
        disabled: true,
      },
      global: {
        plugins: [vuetify],
      },
    })

    // Verify buttons are disabled
    cy.get('.spin-up-arrow-button').should('have.class', 'disabled')
    cy.get('.spin-down-arrow-button').should('have.class', 'disabled')

    // Verify input is disabled
    cy.get('input').should('be.disabled')
  })

  it('emits the correct value when updated', () => {
    const onUpdateSpy = cy.spy().as('updateSpy')

    mount(AppSpinField, {
      props: {
        ...props,
        'onUpdate:modelValue': onUpdateSpy,
      },
      global: {
        plugins: [vuetify],
      },
    })

    // Increment and verify emitted value
    cy.get('.spin-up-arrow-button').click()
    cy.get('@updateSpy').should('have.been.calledWith', '11.00')

    // Decrement and verify emitted value
    cy.get('.spin-down-arrow-button').click()
    cy.get('@updateSpy').should('have.been.calledWith', '10.00')
  })
})
