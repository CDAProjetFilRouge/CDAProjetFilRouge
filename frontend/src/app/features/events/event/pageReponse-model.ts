export interface PageResponse<T>{
  content: T[];
  totalElements: number;
  totalPages: number;
  size:number;
  first: boolean;
  last: boolean;
  numberOfElement: number;
  empty: boolean
}
