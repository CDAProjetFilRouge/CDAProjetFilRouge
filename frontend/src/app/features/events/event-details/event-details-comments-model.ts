export interface CommentAuthorModel {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CommentModel {
  id: string;
  author: CommentAuthorModel;
  content: string;
  creationDate: string;
}
