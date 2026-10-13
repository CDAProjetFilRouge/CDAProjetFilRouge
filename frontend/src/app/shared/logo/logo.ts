import { Component } from '@angular/core';

// Logo « billet-lieu » : un billet avec un repère, suivi du nom. Le dessin est celui de
// myhubevent-logo-c.svg, recadré au plus juste (sans la marge du fichier d'origine). Le SVG est
// écrit ici, et non chargé comme image, pour que le texte utilise la police Inter de la page.
// Le texte est recentré verticalement sur le billet (ligne de base à 127 au lieu de 105).
// La hauteur se règle avec la variable CSS --logo-height (44px par défaut).
@Component({
  selector: 'app-logo',
  template: `
    <svg viewBox="20 60 540 94" role="img" aria-label="MyHubEvent">
      <g transform="translate(20 20)">
        <path
          d="M22 43h96a20 20 0 0 1 20 20v10a14 14 0 0 0 0 28v10a20 20 0 0 1-20 20H22a20 20 0 0 1-20-20v-10a14 14 0 0 0 0-28V63a20 20 0 0 1 20-20Z"
          fill="#8c2f39"
        />
        <path
          d="M70 52c-16 0-28 12-28 27 0 22 28 43 28 43s28-21 28-43c0-15-12-27-28-27Z"
          fill="#efc486"
        />
        <circle cx="70" cy="79" r="9" fill="#2a0f13" />
        <path
          d="M30 57h14M96 57h14M30 123h14M96 123h14"
          stroke="#efc486"
          stroke-linecap="round"
          stroke-width="6"
        />
      </g>
      <text x="190" y="128" style="fill: var(--logo-ink, #f1e8da)" font-size="62" font-weight="700" letter-spacing=".5">
        MyHub<tspan style="fill: var(--logo-accent, #efc486)">Event</tspan>
      </text>
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    svg {
      display: block;
      width: auto;
      height: var(--logo-height, 44px);
      font-family: 'Cormorant Garamond', Georgia, serif;
    }
  `,
})
export class Logo {}
