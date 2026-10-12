export interface CommentModel {
  id: number;
  author: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  }
  content: string;
  creationDate: Date
}
