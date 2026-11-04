export interface CartItem {
  productId: string;
  name: string | null;       // null when the product was deleted
  price: number | null;      // live price; null when deleted
  imageUrl: string | null;
  stock: number;
  quantity: number;
  available: boolean;        // false: deleted, or stock < quantity
  lineTotal: number | null;  // price × quantity; null when deleted
  addedAt: string;
}

export interface CartResponse {
  items: CartItem[];
  subtotal: number;          // available lines only
  updatedAt: string | null;  // null for a cart that was never saved
}