import { Order } from './order.model';

export interface OrderPageResponse {
  content: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
