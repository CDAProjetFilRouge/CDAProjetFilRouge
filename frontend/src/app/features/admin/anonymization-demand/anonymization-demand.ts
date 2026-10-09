import { Component, inject, signal } from '@angular/core';
import { AnonymizationDemandService } from './anonymization-demand.service';
import { ANONYMIZATION_DEMAND_LABELS, AnonymizationDemand, AnonymizationDemandStatus } from '../../../core/models/anonymization-demand.models';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';

@Component({
  imports: [DatePipe],
  selector: 'app-anonymization-demand',
  styleUrl: './anonymization-demand.scss',
  templateUrl: './anonymization-demand.html',
})
export class AnonymizationDemandList {

  private readonly service = inject(AnonymizationDemandService);

  protected readonly demands = signal<AnonymizationDemand[]>([]);

  protected readonly status = signal<AnonymizationDemandStatus>('PENDING');

  protected readonly errorMessage = signal<string | null>(null);

  protected readonly statusLabels = ANONYMIZATION_DEMAND_LABELS;

  protected readonly loading = signal(false);


  constructor() { this.load(); }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  protected load(): void {
    this.loading.set(true);
    this.service.getDemands(this.status()).subscribe({
      next: (list) => {
        this.demands.set(list);
        this.errorMessage.set(null);
        this.loading.set(false);
      },
      error: (err) => {
        this.handleError(err);
        this.loading.set(false);
      },
    });
  }

  protected selectStatus(status: AnonymizationDemandStatus): void {
    this.status.set(status);
    this.load();
  }

  protected onValidate(id: number): void {
    if (!confirm('Voulez-vous vraiment anonymiser cet utilisateur ? Cette action est définitive !')) {
      return;
    }
    this.service.validateDemand(id).subscribe({
      next: () => {
        this.demands.update((list) => list.filter((d) => d.id !== id));
        this.service.refreshPendingCount();
        this.errorMessage.set(null);
      },
      error: (err) => this.handleError(err),
    });
  }

}
