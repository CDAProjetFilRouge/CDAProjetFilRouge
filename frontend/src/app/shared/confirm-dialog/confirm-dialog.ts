import { Component, effect, ElementRef, inject, viewChild } from '@angular/core';
import { ConfirmService } from '../../core/ui/confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  styleUrl: './confirm-dialog.scss',
  templateUrl: './confirm-dialog.html',
})
export class ConfirmDialog {
  protected readonly confirmService = inject(ConfirmService);
  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    // La boîte native suit la demande en cours : showModal() piège le focus et rend le reste inerte.
    effect(() => {
      const el = this.dialog()?.nativeElement;
      if (!el) return;
      if (this.confirmService.request()) {
        if (!el.open) el.showModal();
      } else if (el.open) {
        el.close();
      }
    });
  }

  // Touche Échap : la boîte se ferme d'elle-même, la demande est alors un refus.
  protected onClosed(): void {
    if (this.confirmService.request()) {
      this.confirmService.answer(false);
    }
  }
}
