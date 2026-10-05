import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { URL_BACKEND } from '../../../core/api/api.config';
import { DocumentType } from './legal-document.models';
import { LegalDocument } from './legal-document.models';
import { Observable } from 'rxjs';

@Service()
export class LegalDocumentService {

  private readonly http = inject(HttpClient);

  getLatest(type: DocumentType): Observable<LegalDocument> {
    return this.http.get<LegalDocument>(`${URL_BACKEND}/legal-documents/${type}`)
  }

  createNewVersion(documentType: DocumentType, content: string): Observable<LegalDocument> {
    return this.http.post<LegalDocument>(`${URL_BACKEND}/legal-documents`, {documentType, content});
  }
}
