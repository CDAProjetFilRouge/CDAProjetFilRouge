export interface EventModel{
  id: string;
  title: string;
  description: string;
  location: {
    id: string;
    street1: string;
    street2: string;
    postalCode: string;
    city: string;
    country: string
  },
  category: string;
  startDateTime: Date;
  endDateTime: Date;
  affiliatePrice: number;
  nonAffiliatePrice: number;
  maxCapacity: number;
  remainingSpots: number;
  imageGallery: {
      id: string;
      path: string;
      displayOrder: number

    }[],
  status: string,
  clubName: string
}
