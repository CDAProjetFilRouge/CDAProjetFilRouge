import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { InscriptionDialog, InscriptionDialogState } from './inscription-dialog';

function render(state: InscriptionDialogState): HTMLElement {
  TestBed.configureTestingModule({ imports: [InscriptionDialog], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(InscriptionDialog);
  fixture.componentRef.setInput('open', false);
  fixture.componentRef.setInput('eventTitle', 'Tournoi de tennis');
  fixture.componentRef.setInput('state', state);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const body = (element: HTMLElement): string =>
  element.querySelector('.body')?.textContent?.replace(/\s+/g, ' ').trim() ?? '';

describe('InscriptionDialog', () => {
  it('annonce le rang dans la liste d’attente quand il est connu', () => {
    const element = render({ kind: 'registered', waiting: true, position: 3 });

    expect(body(element)).toContain('Vous êtes n°3 sur la liste d’attente.');
    expect(body(element)).toContain('Vous pouvez y renoncer.');
  });

  it('reste correct quand le rang n’est pas connu', () => {
    const element = render({ kind: 'registered', waiting: true, position: null });

    expect(body(element)).toContain('Vous êtes sur la liste d’attente.');
    expect(body(element)).not.toContain('n°');
  });

  it('ne parle pas de rang pour une inscription confirmée', () => {
    const element = render({ kind: 'registered', waiting: false, position: null });

    expect(body(element)).toContain('Vous êtes inscrit·e.');
    expect(body(element)).not.toContain('n°');
  });

  it('propose de rejoindre la liste d’attente quand l’évènement est complet', () => {
    const element = render({ kind: 'open', waiting: true });

    expect(body(element)).toContain('complet');
    expect(element.querySelector('footer .btn-primary')?.textContent).toContain('liste d’attente');
  });
});
