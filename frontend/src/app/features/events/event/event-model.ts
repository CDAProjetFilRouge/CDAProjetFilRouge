export interface EventModel{
  id: number;
  title: string;
  description: string;
  location: {
    id: number;
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
      id: number;
      path: string;
      displayOrder: number

    }[],
  status: String,
  organizerFirstName: string,
  organizerLastName: string
}
