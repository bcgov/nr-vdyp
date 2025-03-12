import TopProjectYear from './TopProjectYear.vue'

describe('TopProjectYear.vue', () => {
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

  it('renders with default props', () => {
    cy.mount(TopProjectYear)

    // Verify title
    cy.get('.top-project').should('contain.text', 'Projects')

    // Verify year
    cy.get('.top-year').should('contain.text', 'Year: 2024/2025')
  })

  it('renders with custom title and year', () => {
    const customTitle = 'Custom Projects'
    const customYear = '2023/2024'

    cy.mount(TopProjectYear, {
      props: {
        title: customTitle,
        year: customYear,
      },
    })

    // Verify title
    cy.get('.top-project').should('contain.text', customTitle)

    // Verify year
    cy.get('.top-year').should('contain.text', `Year: ${customYear}`)
  })
})
