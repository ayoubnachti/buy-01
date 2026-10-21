export type ProductCategory = 'ELECTRONICS' | 'CLOTHING' | 'BOOKS' | 'HOME' | 'SPORTS' | 'OTHER';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: ProductCategory;
  userId?: string;
  imageUrls?: string[];
  createdAt?: string;
  updatedAt?: string;
}