export interface EventDetailsModel{
  id: number;
  title: string;
  description: string;
  street1: string;
  street2: string;
  city: string;
  postalCode: string;
  country: string;
  category: string;
  startDateTime: Date;
  endDateTime: Date;
  affiliatePrice: number;
  nonAffiliatePrice: number;
  maxCapacity: number;
  remainingSpots: number;
  imageGallery: {
      id: number;
      path: string;
      displayOrder: number
    }[],
  status: string,
  clubName: string;
  organizerFirstName: string;
  organizerLastName: string;
}

