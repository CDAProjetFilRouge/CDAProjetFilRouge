export interface AddressUpdate {
  street1: string;
  street2: string | null;
  postalCode: string;
  city: string;
  country: string;
}

export interface ProfileUpdate {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  address: AddressUpdate | null;
}
