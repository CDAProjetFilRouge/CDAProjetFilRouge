import { Component } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  readonly events = [
    {
      name: "Event 1",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 2",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 3",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 4",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 5",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 6",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 7",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 8",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 9",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 10",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 11",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 0,
      placeMax: 20,
      type: "loisir"
    },
        {
      name: "Event 12",
      price: 12,
      dateDebut: "xx/xx/xxxx",
      dateFin: "yy/yy/yyyy",
      address: 'xxxxxxxxxx',
      placeDispo: 1,
      placeMax: 20,
      type: "loisir"
    }
  ]

   currentPage = 1;
   eventsPerPages = 6;
  get displayEvents(){
    const start = (this.currentPage - 1) * this.eventsPerPages;
    const end = start + this.eventsPerPages;

    return this.events.slice(start, end)
  }

  get totalPages() {
    return Math.ceil(this.events.length / this.eventsPerPages);
  }

  nextPage(){
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  previousPage(){
    if (this.currentPage >1){
      this.currentPage--
    }
  }

}
