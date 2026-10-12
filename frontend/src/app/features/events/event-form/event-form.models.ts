import { Category } from '../../../core/models/user.models';

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';

export const EVENT_CATEGORY_LABELS: Record<Category, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
};

export interface EventLocationRequest {
  street1: string;
  street2: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface EventRequest {
  title: string;
  description: string;
  location: EventLocationRequest;
  category: Category;
  startDateTime: string;
  endDateTime: string;
  affiliatePrice: number;
  nonAffiliatePrice: number;
  maxCapacity: number;
  idClub: number;
  status?: EventStatus;
}

export interface EventSaved {
  id: number;
  status: EventStatus;
}
