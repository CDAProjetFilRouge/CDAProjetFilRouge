import { AppUserSummary } from "../users/user.models";

export interface LegalDocument {
  id: number;
  documentType: DocumentType;
  content: string;
  version: number;
  updateDate: string;
  pdfPath: string;
  user: AppUserSummary;
}

export type DocumentType = 'TERM_OF_USE' | 'GDPR_POLICY';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  TERM_OF_USE: 'Conditions Générales d\'Utilisation',
  GDPR_POLICY: 'Politique RGPD'
}
