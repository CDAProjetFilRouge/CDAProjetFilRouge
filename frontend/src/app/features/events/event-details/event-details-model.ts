export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';

export interface EventDetailsModel{
  id: number;
  title: string;
  description: string;
  street1: string;
  street2: string | null;
  city: string;
  postalCode: string;
  country: string;
  category: string;
  startDate: string;
  endDate: string;
  affiliatePrice: number;
  nonAffiliatePrice: number;
  maxCapacity: number;
  remainingSpots: number;
  imageGallery: {
      id: number;
      path: string;
      displayOrder: number
    }[],
  status: EventStatus,
  clubName: string | null;
  organizerFirstName: string;
  // Le backend sérialise ce champ en « organizerLastname » (getter getOrganizerLastname).
  organizerLastname: string;
  organizerId: number;
}
