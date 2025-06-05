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

  it('renders with default props and calculates fiscal year for May 2025', () => {
    // Mock the date to May 27, 2025
    const mockDate = new Date('2025-05-27T12:57:00')
    cy.clock(mockDate.getTime(), ['Date'])

    cy.mount(TopProjectYear)

    // Verify title
    cy.get('.top-project').should('contain.text', 'Projects')

    // Verify fiscal year (May 27, 2025 should result in 2025/2026)
    cy.get('.top-year').should('contain.text', 'Year: 2025/2026')
  })

  it('renders with custom title and calculates fiscal year for January 2025', () => {
    // Mock the date to January 15, 2025
    const mockDate = new Date('2025-01-15T12:00:00')
    cy.clock(mockDate.getTime(), ['Date'])

    const customTitle = 'Custom Projects'

    cy.mount(TopProjectYear, {
      props: {
        title: customTitle,
      },
    })

    // Verify title
    cy.get('.top-project').should('contain.text', customTitle)

    // Verify fiscal year (January 15, 2025 should result in 2024/2025)
    cy.get('.top-year').should('contain.text', 'Year: 2024/2025')
  })

  it('calculates fiscal year correctly on April 1st', () => {
    // Mock the date to April 1, 2025
    const mockDate = new Date('2025-04-01T12:00:00')
    cy.clock(mockDate.getTime(), ['Date'])

    cy.mount(TopProjectYear)

    // Verify title
    cy.get('.top-project').should('contain.text', 'Projects')

    // Verify fiscal year (April 1, 2025 should result in 2025/2026)
    cy.get('.top-year').should('contain.text', 'Year: 2025/2026')
  })
})
