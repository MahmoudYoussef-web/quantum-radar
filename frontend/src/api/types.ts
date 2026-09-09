export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface FineSummary {
  id: number
  plateNumber: string
  totalAmount: number
  violations: number
  version: number
  createdAt: string
}

export interface ViolationSummary {
  id: number
  fineId: number
  plateNumber: string
  ruleName: string
  description: string
  fee: number
  points: number
  deviceCode: string | null
  recordedAt: string
}

export interface RuleConfig {
  code: string
  displayName: string
  enabled: boolean
  fee: number
  penaltyPoints: number
  maxSpeed: number | null
}

export interface RuleVersion {
  code: string
  version: number
  enabled: boolean
  fee: number
  penaltyPoints: number
  maxSpeed: number | null
  effectiveFrom: string
}

export interface FineTier {
  id: number
  ruleCode: string
  overFrom: number
  overTo: number | null
  fee: number
}

export interface Device {
  deviceCode: string
  name: string
  active: boolean
  health: 'ACTIVE' | 'DEGRADED' | 'OFFLINE'
  lastSeenAt: string | null
}

export interface DeviceDetail {
  deviceCode: string
  name: string
  active: boolean
  health: 'ACTIVE' | 'DEGRADED' | 'OFFLINE'
  lastSeenAt: string | null
  firmwareVersion: string | null
  lastIp: string | null
  registeredAt: string
  eventCount: number
  lastEventAt: string | null
}

export interface Driver {
  name: string
  licenseNo: string
  penaltyPoints: number
  version: number
  licenseStatus: string | null
}

export interface DriverSummary {
  driver: Driver
  vehicles: { plate: string; carType: string }[]
  totalViolations: number
  totalFines: number
  totalFineAmount: number
}

export interface VehicleHistory {
  vehicle: { plate: string; carType: string; ownerLicenseNo: string | null }
  ownerName: string | null
  ownerPoints: number | null
  fines: { id: number; totalAmount: number; violations: number }[]
}
