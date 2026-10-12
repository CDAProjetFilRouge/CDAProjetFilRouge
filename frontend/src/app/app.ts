import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from './core/layout/footer/footer/footer';
import { ConfirmDialog } from './shared/confirm-dialog/confirm-dialog';
import { Header } from './core/layout/header/header/header';

@Component({
  imports: [Header, Footer, RouterOutlet, ConfirmDialog],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
}
