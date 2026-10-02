export interface AppUserSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export type Role = 'MEMBER' | 'ORGANIZER' | 'ADMINISTRATOR';

export type Category = 'CULTURE' | 'SPORT' | 'HOBBIES';

export type AccountStatus = 'INACTIVE' | 'ACTIVE' | 'SUSPENDED' | 'ANONYMIZE' | 'PENDING_ACTIVATION';

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

