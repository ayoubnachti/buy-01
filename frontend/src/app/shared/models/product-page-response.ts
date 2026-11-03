import { Product } from './product.model';

export interface ProductPageResponse {
  items: Product[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ProductSearchPageResponse {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductSearchFacets {
  categories: Array<{
    category: Product['category'];
    count: number;
  }>;
  minPrice: number | null;
  maxPrice: number | null;
}