import { Component, inject, signal } from '@angular/core';
import { LegalDocumentService } from './legal-document.service';
import { form, FormField } from '@angular/forms/signals';
import { DOCUMENT_TYPE_LABELS, DocumentType, LegalDocument } from './legal-document.models';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  imports: [FormField, DatePipe],
  selector: 'app-legal-document',
  styleUrl: './legal-document.scss',
  templateUrl: './legal-document.html',
})
export class LegalDocumentPage {

  private service = inject(LegalDocumentService);

  private route = inject(ActivatedRoute);

  protected readonly selectedType = this.route.snapshot.data['documentType'] as DocumentType;

  protected readonly document = signal<LegalDocument | null>(null);

  protected readonly typeLabel = DOCUMENT_TYPE_LABELS;

  protected readonly loadError = signal<string | null>(null);

  protected readonly publishError = signal<string | null>(null);

  protected readonly loading = signal(false);

  legalDocumentModel = signal({ content: '' });
  legalDocumentForm = form(this.legalDocumentModel);


  constructor() {
    this.load(this.selectedType);
  }

  private handleError(err: HttpErrorResponse): string {
    return typeof err.error === 'string' ? err.error : 'Une erreur est survenue.';
  }

  protected publish(): void {
    const text = this.legalDocumentModel().content.trim();
    if (!text) {
      this.publishError.set('Le texte est obligatoire.');
      return;
    }
    if(!confirm('Publier ce texte ?')) {
      return;
    }
    this.publishError.set(null);
    this.service.createNewVersion(this.selectedType, text).subscribe({
      next: (content) => {
        this.document.set(content);
        this.legalDocumentModel.set({ content: '' });
        this.loadError.set(null);
        this.publishError.set(null);
      },
      error: (err) => this.publishError.set(this.handleError(err)),
    });
  }

  protected load(type: DocumentType): void {
    this.loading.set(true);
    this.document.set(null);
    this.service.getLatest(type).subscribe({
      next: (content) => {
        this.document.set(content);
        this.loadError.set(null);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(this.handleError(err));
        this.loading.set(false);
      },
    });
  }
}
