import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo',
  standalone: true
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) return '';

    const date = new Date(value).getTime();

    if (Number.isNaN(date)) return '';

    const seconds = Math.floor((Date.now() - date) / 1000);

    if (seconds < 60) return "posté à l'instant";

    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) {
      return `posté il y a ${minutes} minute${minutes === 1 ? '' : 's'}`;
    }

    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return `posté il y a ${hours} heure${hours === 1 ? '' : 's'}`;
    }

    const days = Math.floor(hours / 24);
    if (days < 30) {
      return `posté il y a ${days} jour${days === 1 ? '' : 's'}`;
    }

    const months = Math.floor(days / 30);
    if (months < 12) {
      return `posté il y a ${months} mois`;
    }

    const years = Math.floor(days / 365);
    return `posté il y a ${years} année${years === 1 ? '' : 's'}`;
  }
}
