import { Component, effect, ElementRef, input, output, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

export type InscriptionDialogState =
  | { kind: 'login' }
  | { kind: 'blocked'; reason: string }
  | { kind: 'registered'; waiting: boolean; position?: number | null }
  | { kind: 'open'; waiting: boolean };

@Component({
  imports: [RouterLink],
  selector: 'app-inscription-dialog',
  styleUrl: './inscription-dialog.scss',
  templateUrl: './inscription-dialog.html',
})
export class InscriptionDialog {
  readonly open = input.required<boolean>();
  readonly eventTitle = input.required<string>();
  readonly state = input.required<InscriptionDialogState | null>();
  readonly busy = input(false);
  readonly error = input<string | null>(null);

  readonly register = output<void>();
  readonly unregister = output<void>();
  readonly dismissed = output<void>();

  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const el = this.dialog()?.nativeElement;
      if (!el) return;
      if (this.open()) {
        if (!el.open) el.showModal();
      } else if (el.open) {
        el.close();
      }
    });
  }

  protected onClosed(): void {
    if (this.open()) {
      this.dismissed.emit();
    }
  }
}
