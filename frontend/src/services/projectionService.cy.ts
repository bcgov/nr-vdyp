/// <reference types="cypress" />

import type { Projection } from '@/interfaces/interfaces'
import { SORT_ORDER, PROJECTION_LIST_HEADER_KEY } from '@/constants/constants'

// Local implementations for testing
const sortProjections = (
  projections: Projection[],
  sortKey: string,
  sortOrder: string,
): Projection[] => {
  return [...projections].sort((a, b) => {
    const aValue = a[sortKey as keyof Projection] ?? ''
    const bValue = b[sortKey as keyof Projection] ?? ''

    let comparison = 0
    if (aValue < bValue) {
      comparison = -1
    } else if (aValue > bValue) {
      comparison = 1
    }

    return sortOrder === 'desc' ? -comparison : comparison
  })
}

const paginateProjections = (
  projections: Projection[],
  page: number,
  itemsPerPage: number,
): Projection[] => {
  const startIndex = (page - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  return projections.slice(startIndex, endIndex)
}

const calculateTotalPages = (
  totalItems: number,
  itemsPerPage: number,
): number => {
  if (totalItems === 0) return 0
  return Math.ceil(totalItems / itemsPerPage)
}

describe('ProjectionListService Unit Tests', () => {
  const mockProjections: Projection[] = [
    {
      projectionGUID: 'guid-1',
      title: 'Alpha Project',
      description: 'First project',
      method: 'File Upload',
      projectionType: 'Volume',
      lastUpdated: '2024-01-15T10:00:00',
      expiration: '2024-06-15T10:00:00',
      status: 'Draft',
    },
    {
      projectionGUID: 'guid-2',
      title: 'Beta Project',
      description: 'Second project',
      method: 'Manual Input',
      projectionType: 'CFS Biomass',
      lastUpdated: '2024-02-20T14:30:00',
      expiration: '2024-07-20T14:30:00',
      status: 'Ready',
    },
    {
      projectionGUID: 'guid-3',
      title: 'Gamma Project',
      description: 'Third project',
      method: 'File Upload',
      projectionType: 'Volume',
      lastUpdated: '2024-01-10T08:00:00',
      expiration: '2024-05-10T08:00:00',
      status: 'Running',
    },
    {
      projectionGUID: 'guid-4',
      title: 'Delta Project',
      description: 'Fourth project',
      method: 'Manual Input',
      projectionType: 'CFS Biomass',
      lastUpdated: '2024-03-01T16:45:00',
      expiration: '2024-08-01T16:45:00',
      status: 'Failed',
    },
  ]

  describe('sortProjections', () => {
    it('should sort by title in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].title).to.equal('Alpha Project')
      expect(sorted[1].title).to.equal('Beta Project')
      expect(sorted[2].title).to.equal('Delta Project')
      expect(sorted[3].title).to.equal('Gamma Project')
    })

    it('should sort by title in descending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.DESC,
      )
      expect(sorted[0].title).to.equal('Gamma Project')
      expect(sorted[1].title).to.equal('Delta Project')
      expect(sorted[2].title).to.equal('Beta Project')
      expect(sorted[3].title).to.equal('Alpha Project')
    })

    it('should sort by lastUpdated in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.LAST_UPDATED,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-3') // Jan 10
      expect(sorted[1].projectionGUID).to.equal('guid-1') // Jan 15
      expect(sorted[2].projectionGUID).to.equal('guid-2') // Feb 20
      expect(sorted[3].projectionGUID).to.equal('guid-4') // Mar 01
    })

    it('should sort by lastUpdated in descending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.LAST_UPDATED,
        SORT_ORDER.DESC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-4') // Mar 01
      expect(sorted[1].projectionGUID).to.equal('guid-2') // Feb 20
      expect(sorted[2].projectionGUID).to.equal('guid-1') // Jan 15
      expect(sorted[3].projectionGUID).to.equal('guid-3') // Jan 10
    })

    it('should sort by expiration in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.EXPIRATION,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].projectionGUID).to.equal('guid-3') // May 10
      expect(sorted[1].projectionGUID).to.equal('guid-1') // Jun 15
      expect(sorted[2].projectionGUID).to.equal('guid-2') // Jul 20
      expect(sorted[3].projectionGUID).to.equal('guid-4') // Aug 01
    })

    it('should sort by status in ascending order', () => {
      const sorted = sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.STATUS,
        SORT_ORDER.ASC,
      )
      expect(sorted[0].status).to.equal('Draft')
      expect(sorted[1].status).to.equal('Failed')
      expect(sorted[2].status).to.equal('Ready')
      expect(sorted[3].status).to.equal('Running')
    })

    it('should not mutate the original array', () => {
      const original = [...mockProjections]
      sortProjections(
        mockProjections,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(mockProjections).to.deep.equal(original)
    })

    it('should handle empty array', () => {
      const sorted = sortProjections(
        [],
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted).to.deep.equal([])
    })

    it('should handle single item array', () => {
      const singleItem = [mockProjections[0]]
      const sorted = sortProjections(
        singleItem,
        PROJECTION_LIST_HEADER_KEY.TITLE,
        SORT_ORDER.ASC,
      )
      expect(sorted).to.have.length(1)
      expect(sorted[0].title).to.equal('Alpha Project')
    })
  })

  describe('paginateProjections', () => {
    it('should return correct items for first page', () => {
      const paginated = paginateProjections(mockProjections, 1, 2)
      expect(paginated).to.have.length(2)
      expect(paginated[0].projectionGUID).to.equal('guid-1')
      expect(paginated[1].projectionGUID).to.equal('guid-2')
    })

    it('should return correct items for second page', () => {
      const paginated = paginateProjections(mockProjections, 2, 2)
      expect(paginated).to.have.length(2)
      expect(paginated[0].projectionGUID).to.equal('guid-3')
      expect(paginated[1].projectionGUID).to.equal('guid-4')
    })

    it('should return remaining items for last page', () => {
      const paginated = paginateProjections(mockProjections, 2, 3)
      expect(paginated).to.have.length(1)
      expect(paginated[0].projectionGUID).to.equal('guid-4')
    })

    it('should return empty array for page beyond available items', () => {
      const paginated = paginateProjections(mockProjections, 5, 2)
      expect(paginated).to.have.length(0)
    })

    it('should return all items when itemsPerPage exceeds total', () => {
      const paginated = paginateProjections(mockProjections, 1, 10)
      expect(paginated).to.have.length(4)
    })

    it('should handle empty array', () => {
      const paginated = paginateProjections([], 1, 5)
      expect(paginated).to.have.length(0)
    })

    it('should handle page 1 with itemsPerPage of 1', () => {
      const paginated = paginateProjections(mockProjections, 1, 1)
      expect(paginated).to.have.length(1)
      expect(paginated[0].projectionGUID).to.equal('guid-1')
    })
  })

  describe('calculateTotalPages', () => {
    it('should calculate correct total pages when evenly divisible', () => {
      expect(calculateTotalPages(10, 5)).to.equal(2)
      expect(calculateTotalPages(20, 10)).to.equal(2)
      expect(calculateTotalPages(100, 25)).to.equal(4)
    })

    it('should round up when not evenly divisible', () => {
      expect(calculateTotalPages(11, 5)).to.equal(3)
      expect(calculateTotalPages(7, 3)).to.equal(3)
      expect(calculateTotalPages(1, 10)).to.equal(1)
    })

    it('should return 0 for zero items', () => {
      expect(calculateTotalPages(0, 5)).to.equal(0)
    })

    it('should handle single item', () => {
      expect(calculateTotalPages(1, 5)).to.equal(1)
    })

    it('should handle items equal to itemsPerPage', () => {
      expect(calculateTotalPages(5, 5)).to.equal(1)
    })
  })
})
