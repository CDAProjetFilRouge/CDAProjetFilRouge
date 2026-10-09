import { AppUserSummary } from "./user.models";

export type AnonymizationDemandStatus = 'PENDING' | 'VALIDATE';

export const ANONYMIZATION_DEMAND_LABELS: Record<AnonymizationDemandStatus, string> = {
  PENDING: 'En attente',
  VALIDATE: 'Validée',
};

export interface AnonymizationDemand {
  id: number;
  requestStatus: AnonymizationDemandStatus,
  demandDate: string,
  approvedDate: string | null,
  requester: AppUserSummary,
  admin: AppUserSummary | null,
}
