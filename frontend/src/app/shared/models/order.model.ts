export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  imageUrl: string | null;
}

export interface Address {
  fullName: string;
  phone: string | null;
  street: string;
  city: string;
  postalCode: string;
  country: string;
}

export interface StatusHistoryEntry {
  status: OrderStatus;
  at: string;
  by: string;
}

export interface Order {
  id: string;
  userId: string;
  sellerId: string;
  status: OrderStatus;
  items: OrderItem[];
  shippingAddress: Address;
  paymentMethod: string;
  totalAmount: number;
  statusHistory: StatusHistoryEntry[];
  createdAt: string;
  updatedAt: string;
}
