import { Component, inject, OnInit } from '@angular/core';
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

  ngOnInit(): void {
    this.demandService.refreshPendingCount();
  }
}
