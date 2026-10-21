import { ProductCategory } from './product.model';

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: ProductCategory;
  imageUrls?: string[];
}
