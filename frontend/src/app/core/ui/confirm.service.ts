import { Service, signal } from '@angular/core';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  // `danger` : action destructrice ou irréversible (bouton rouge).
  tone?: 'primary' | 'danger';
}

export interface ConfirmRequest extends Required<ConfirmOptions> {
  resolve: (confirmed: boolean) => void;
}

// Remplace window.confirm() : `await confirm.ask({...})` renvoie true si l'utilisateur confirme.
// Le composant <app-confirm-dialog> (monté dans app.html) affiche la demande en cours.
@Service()
export class ConfirmService {
  readonly request = signal<ConfirmRequest | null>(null);

  ask(options: ConfirmOptions): Promise<boolean> {
    // Une seule confirmation à la fois : une demande encore ouverte est considérée comme refusée.
    this.request()?.resolve(false);

    return new Promise<boolean>((resolve) => {
      this.request.set({
        confirmLabel: 'Confirmer',
        cancelLabel: 'Annuler',
        tone: 'primary',
        ...options,
        resolve,
      });
    });
  }

  answer(confirmed: boolean): void {
    const current = this.request();
    this.request.set(null);
    current?.resolve(confirmed);
  }
}
