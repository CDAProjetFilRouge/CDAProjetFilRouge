import { Component, inject, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { LegalDocumentService } from './legal-document.service';
import { form, FormField } from '@angular/forms/signals';

@Component({
  imports: [FormField],
  selector: 'app-legal-document',
  styleUrl: './legal-document.scss',
  templateUrl: './legal-document.html',
})
export class LegalDocumentPage {

  private service = inject(LegalDocumentService);

  legalDocumentModel = signal({ content: '' });
  legalDocumentForm = form(this.legalDocumentModel);

  publish() {
    // this.service.createNewVersion(1, this.legalDocumentModel().content).subscribe();
  }
}
