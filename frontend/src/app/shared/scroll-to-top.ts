/** Remonte en haut de la page (à appeler lors d'un changement de page d'une pagination). */
export function scrollToTop(): void {
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
