import TrainingSupport from './TrainingSupport.vue'
import { RouterLinkStub } from '@vue/test-utils'

describe('TrainingSupport.vue', () => {
  const mountOptions = {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  }

  it('renders default text correctly', () => {
    cy.mount(TrainingSupport, mountOptions)

    cy.get('a')
      .should('have.class', 'bcds-header-link')
      .and('contain.text', 'Training and Support')
  })

  it('renders with custom text', () => {
    cy.mount(TrainingSupport, {
      ...mountOptions,
      props: {
        text: 'Custom Training Text',
      },
    })

    cy.get('a')
      .should('contain.text', 'Custom Training Text')
      .and('have.class', 'bcds-header-link')
  })
})
