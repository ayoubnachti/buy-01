export interface CartResponse {
  items: Array<{
    productId: string;
    quantity: number;
    addedAt: string;
  }>;
  updatedAt: string;
}
