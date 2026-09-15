export type UserRole = 'CLIENT' | 'SELLER';

export interface ProfileResponse {
    name: string;
    email: string;
    role: UserRole;
    avatar: string | null;
}

export interface UpdateProfileRequest {
    name: string;
    email: string;
    avatar: string | null;
}

export interface AuthResponse<T> {
    success: boolean;
    message: string;
    data: T;
}