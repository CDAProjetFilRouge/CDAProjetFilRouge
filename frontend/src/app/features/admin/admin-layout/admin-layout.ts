import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AnonymizationDemandService } from '../anonymization-demand/anonymization-demand.service';


@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-admin-layout',
  styleUrl: './admin-layout.scss',
  templateUrl: './admin-layout.html',
})
export class AdminLayout implements OnInit {

  protected demandService = inject(AnonymizationDemandService);

  // Menu déroulant, utilisé uniquement sur téléphone (sur grand écran, les liens sont toujours visibles).
  protected readonly menuOpen = signal(false);

  ngOnInit(): void {
    this.demandService.refreshPendingCount();
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  @HostListener("document:keydown.escape")
  protected closeMenu(): void {
    this.menuOpen.set(false);
  }
}
