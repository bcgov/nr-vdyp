import AppSpinField from './AppSpinField.vue'

describe('AppSpinField.vue', () => {
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
    cy.mountWithVuetify(AppSpinField, {
      props,
    })

    // Verify label is rendered correctly
    cy.get('.bcds-text-field-label').should('contain.text', props.label)

    // Verify initial value
    cy.get('input').should('have.value', props.modelValue)
  })

  it('increments the value when the up button is clicked', () => {
    cy.mountWithVuetify(AppSpinField, {
      props,
    })

    // Trigger mousedown and mouseup events on the increment button
    cy.get('.spin-up-arrow-button').trigger('mousedown')
    cy.get('.spin-up-arrow-button').trigger('mouseup')
    cy.get('input').should('have.value', '11.00')
  })

  it('decrements the value when the down button is clicked', () => {
    cy.mountWithVuetify(AppSpinField, {
      props,
    })

    // Trigger mousedown and mouseup events on the decrement button
    cy.get('.spin-down-arrow-button').trigger('mousedown')
    cy.get('.spin-down-arrow-button').trigger('mouseup')
    cy.get('input').should('have.value', '9.00')
  })

  it('does not exceed max limits', () => {
    cy.mountWithVuetify(AppSpinField, {
      props: {
        ...props,
        modelValue: props.max.toFixed(props.decimalAllowNumber),
      },
    })

    // Try to increment beyond the max value
    cy.get('.spin-up-arrow-button').trigger('mousedown')
    cy.get('.spin-up-arrow-button').trigger('mouseup')
    cy.get('input').should(
      'have.value',
      props.max.toFixed(props.decimalAllowNumber),
    )
  })

  it('does not exceed min limits', () => {
    cy.mountWithVuetify(AppSpinField, {
      props: {
        ...props,
        modelValue: props.min.toFixed(props.decimalAllowNumber),
      },
    })
    cy.get('.spin-down-arrow-button').trigger('mousedown')
    cy.get('.spin-down-arrow-button').trigger('mouseup')
    cy.get('input').should(
      'have.value',
      props.min.toFixed(props.decimalAllowNumber),
    )
  })

  it('disables the buttons when the component is disabled', () => {
    cy.mountWithVuetify(AppSpinField, {
      props: {
        ...props,
        disabled: true,
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

    cy.mountWithVuetify(AppSpinField, {
      props: {
        ...props,
        'onUpdate:modelValue': onUpdateSpy,
      },
    })

    // Increment and verify emitted value
    cy.get('.spin-up-arrow-button').trigger('mousedown')
    cy.get('.spin-up-arrow-button').trigger('mouseup')
    cy.get('@updateSpy').should('have.been.calledWith', '11.00')

    // Decrement and verify emitted value
    cy.get('.spin-down-arrow-button').trigger('mousedown')
    cy.get('.spin-down-arrow-button').trigger('mouseup')
    cy.get('@updateSpy').should('have.been.calledWith', '10.00')
  })
})
