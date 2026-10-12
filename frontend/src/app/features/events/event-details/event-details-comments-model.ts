export interface CommentAuthorModel {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CommentModel {
  id: number;
  author: CommentAuthorModel;
  content: string;
  creationDate: string;
}
