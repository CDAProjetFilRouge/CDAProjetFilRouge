import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Logo } from '../../../../shared/logo/logo';

@Component({
  imports: [RouterLink, Logo],
  selector: 'app-footer',
  styleUrl: './footer.scss',
  templateUrl: './footer.html',
})
export class Footer {}
