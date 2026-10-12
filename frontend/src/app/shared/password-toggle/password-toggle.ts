import { afterNextRender, Directive, ElementRef, inject, Renderer2 } from '@angular/core';

const EYE =
  '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>';
const EYE_OFF =
  '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.9 10.9 0 0 1 12 19c-6.5 0-10-7-10-7a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a18.5 18.5 0 0 1-2.16 3.19M1 1l22 22"/><path d="M14.12 14.12A3 3 0 1 1 9.88 9.88"/></svg>';

/**
 * Ajoute un bouton « œil » à un champ mot de passe pour afficher / masquer la saisie.
 * Compatible avec les formulaires réactifs : seul l'attribut `type` de l'input change.
 */
@Directive({ selector: 'input[appPasswordToggle]' })
export class PasswordToggle {
  private readonly input = inject<ElementRef<HTMLInputElement>>(ElementRef).nativeElement;
  private readonly renderer = inject(Renderer2);

  constructor() {
    afterNextRender(() => this.insertButton());
  }

  private insertButton(): void {
    const parent = this.input.parentNode;
    if (!parent) {
      return;
    }

    const wrapper = this.renderer.createElement('span');
    this.renderer.addClass(wrapper, 'password-field');
    this.renderer.insertBefore(parent, wrapper, this.input);
    this.renderer.appendChild(wrapper, this.input);

    const button = this.renderer.createElement('button') as HTMLButtonElement;
    this.renderer.addClass(button, 'password-toggle');
    this.renderer.setAttribute(button, 'type', 'button');
    this.renderer.appendChild(wrapper, button);

    const render = (visible: boolean) => {
      this.renderer.setAttribute(this.input, 'type', visible ? 'text' : 'password');
      this.renderer.setAttribute(
        button,
        'aria-label',
        visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe',
      );
      this.renderer.setAttribute(button, 'aria-pressed', String(visible));
      button.innerHTML = visible ? EYE_OFF : EYE;
    };

    let visible = false;
    render(visible);
    button.addEventListener('click', () => {
      visible = !visible;
      render(visible);
    });
  }
}
