export type InscriptionStatus = 'CONFIRMED' | 'WAITING_LIST' | 'CANCELED';

export interface InscriptionModel {
  id: number;
  event: { id: number } | null;
  inscriptionDate: string;
  status: InscriptionStatus;
  price: number;
  waitingListPosition?: number | null;
}
