import { Component, inject, signal } from '@angular/core';
import { LegalDocumentService } from '../../admin/legal-document/legal-document.service';
import { ActivatedRoute } from '@angular/router';
import { DOCUMENT_TYPE_LABELS, DocumentType, LegalDocument } from '../../admin/legal-document/legal-document.models';
import { URL_BACKEND } from '../../../core/api/api.config';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [DatePipe],
  selector: 'app-legal-page',
  styleUrl: './legal-page.scss',
  templateUrl: './legal-page.html',
})
export class LegalPage {

  private readonly service = inject(LegalDocumentService);
  private readonly route = inject(ActivatedRoute);
  protected readonly type = this.route.snapshot.data['documentType'] as DocumentType;
  protected readonly document = signal<LegalDocument | null>(null);
  protected readonly loading = signal<boolean>(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly DOCUMENT_TYPE_LABELS = DOCUMENT_TYPE_LABELS
  protected readonly urlBackend = URL_BACKEND;


  constructor() {
    this.loading.set(true);
    this.service.getLatest(this.type).subscribe({
      next: (document) => {
        this.document.set(document);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue');
        this.loading.set(false);
      },
    });
  }

}
