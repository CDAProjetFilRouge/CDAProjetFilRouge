import { Category } from '../../../core/models/user.models';

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';

export const EVENT_STATUS_STEPS: { status: EventStatus; label: string }[] = [
  { status: 'DRAFT', label: 'Brouillon' },
  { status: 'PUBLISHED', label: 'Publié' },
  { status: 'CANCELLED', label: 'Annulé' },
  { status: 'FINISHED', label: 'Terminé' },
];

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
  idClub?: string;
  status?: EventStatus;
}

export interface EventSaved {
  id: string;
  status: EventStatus;
}

export interface EventImage {
  id: string;
  path: string;
  displayOrder: number;
}

export interface EventDetail {
  id: string;
  title: string;
  description: string;
  street1: string;
  street2: string | null;
  postalCode: string;
  city: string;
  country: string;
  category: Category;
  startDate: string;
  endDate: string;
  affiliatePrice: number;
  nonAffiliatePrice: number;
  maxCapacity: number;
  status: EventStatus;
  organizerId: string;
  imageGallery: EventImage[];
}
