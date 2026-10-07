import { ProfileUpdate } from "../../features/account/account.models";

export interface AppUserSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export type Role = 'MEMBER' | 'ORGANIZER' | 'ADMINISTRATOR';

export const ROLE_LABELS: Record<Role, string> = {
  MEMBER: 'Membre',
  ORGANIZER: 'Organisateur',
  ADMINISTRATOR: 'Administrateur',
}

export type Category = 'CULTURE' | 'SPORT' | 'HOBBIES';

export type AccountStatus = 'INACTIVE' | 'ACTIVE' | 'SUSPENDED' | 'ANONYMIZE' | 'PENDING_ACTIVATION';

export const ACCOUNT_STATUS_LABELS: Record<AccountStatus, string> = {
  INACTIVE: 'Inactif',
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  ANONYMIZE: 'Anonymisé',
  PENDING_ACTIVATION:'En attente d\'activation',
}

export interface Address {
  id: number;
  street1: string;
  street2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface ClubSummary {
  id: number;
  name: string;
  category: Category;
}

export interface AppUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  role: Role;
  status: AccountStatus;
  suspensionEndDate: string | null;
  creationDate: string;
  address: Address | null;
  clubs: ClubSummary[];
}

export interface AdminUserUpdate extends ProfileUpdate {
  role: Role;
  clubIds: number[];
}

export interface AdminUserCreate {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  clubIds?: number[];
}

