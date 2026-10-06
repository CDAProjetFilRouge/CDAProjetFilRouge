export interface LoginResponse {
  token: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone: string | null;
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
}
