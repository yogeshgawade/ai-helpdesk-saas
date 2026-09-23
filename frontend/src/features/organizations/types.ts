export type OrganizationRole = 'OWNER' | 'ADMIN' | 'AGENT' | 'CUSTOMER'

export interface Organization {
  id: string
  name: string
  slug: string
  role: OrganizationRole
}

export interface OrganizationMember {
  membershipId: string
  userId: string
  name: string
  email: string
  organizationId: string
  role: OrganizationRole
  createdAt: string
}
