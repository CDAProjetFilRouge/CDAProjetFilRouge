export type InscriptionStatus = 'CONFIRMED' | 'WAITING_LIST' | 'CANCELED';

export interface InscriptionModel {
  id: number;
  event: {
    id: number;
    title: string;
    startDateTime: string;
    endDateTime: string;
  };
  inscriptionDate: string;
  status: InscriptionStatus;
  price: number;
}

export const INSCRIPTION_STATUS_LABELS: Record<InscriptionStatus, string> = {
  CONFIRMED: 'Confirmée',
  WAITING_LIST: "Liste d'attente",
  CANCELED: 'Annulée',
};
