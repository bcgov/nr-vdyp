/// <reference types="cypress" />

import { transformAdminProjection } from '@/services/adminService'
import { PROJECTION_STATUS, USER_TYPE_CODE } from '@/constants/constants'
import type { ProjectionModel } from '@/services/vdyp-api'

describe('adminService Unit Tests', () => {
  describe('transformAdminProjection', () => {
    it('should map a projection model to an AdminProjection', () => {
      const mockProjectionModel = {
        projectionGUID: 'guid-test',
        reportTitle: 'Test Title',
        ownerUser: {
          firstName: 'Jane',
          lastName: 'Doe',
          displayName: null,
          identityProviderCode: { code: USER_TYPE_CODE.IDIR },
        },
        startDate: '2024-01-01',
        projectionStatusCode: { code: 'RUNNING' },
        batchMapping: {
          workerCount: 2,
          completedPolygonCount: 5,
          polygonCount: 10,
        },
      } as unknown as ProjectionModel

      const result = transformAdminProjection(mockProjectionModel)

      expect(result).to.deep.equal({
        projectionGUID: 'guid-test',
        title: 'Test Title',
        ownerDisplayName: 'J. Doe',
        userType: USER_TYPE_CODE.IDIR,
        startDate: '2024-01-01',
        status: PROJECTION_STATUS.RUNNING,
        workerCount: 2,
        completedPolygonCount: 5,
        polygonCount: 10,
      })
    })

    it('should fall back to defaults when optional fields are missing', () => {
      const mockProjectionModel = {
        projectionGUID: 'guid-empty',
        reportTitle: null,
        ownerUser: null,
        startDate: null,
        projectionStatusCode: null,
        batchMapping: null,
      } as unknown as ProjectionModel

      const result = transformAdminProjection(mockProjectionModel)

      expect(result).to.deep.equal({
        projectionGUID: 'guid-empty',
        title: '',
        ownerDisplayName: '',
        userType: null,
        startDate: null,
        status: PROJECTION_STATUS.DRAFT,
        workerCount: 0,
        completedPolygonCount: 0,
        polygonCount: 0,
      })
    })
  })
})
