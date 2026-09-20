export type TargetType = 'PRODUCT' | 'PROFILE';

export interface MediaRequest {
  targetType: TargetType;
  targetId: string;
  oldImagePaths: string[] | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
