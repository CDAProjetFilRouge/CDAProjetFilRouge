import { Address, AppUserSummary } from "../../../core/models/user.models";

export type ClubCategory = 'CULTURE' | 'SPORT' | 'DIVERTISSEMENT';

export const CLUB_CATEGORY_LABELS: Record<ClubCategory, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
}

export interface Club {
  id: number;
  name: string;
  category: ClubCategory;
  email: string;
  phone: string;
  endValidityDate?: string | null;
  address: Address;
  appUsers: AppUserSummary[];
}




