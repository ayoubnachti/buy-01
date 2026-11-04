export interface MostBoughtProduct {
  productId: string;
  name: string;
  units: number;
  amount: number;
}

export interface TopCategory {
  category: string;
  units: number;
  amount: number;
}

export interface UserPurchaseStats {
  totalSpent: number;
  ordersCount: number;
  mostBought: MostBoughtProduct[];
  topCategories: TopCategory[];
}

export interface UserPurchaseStatsResponse {
  success: boolean;
  message: string;
  data: UserPurchaseStats;
}
