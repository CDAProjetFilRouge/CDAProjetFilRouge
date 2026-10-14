export type InscriptionStatus = 'CONFIRMED' | 'WAITING_LIST' | 'CANCELED';

export interface InscriptionModel {
  id: string;
  event: { id: string } | null;
  inscriptionDate: string;
  status: InscriptionStatus;
  price: number;
  waitingListPosition?: number | null;
}
