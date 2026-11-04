export interface BestSeller {
  productId: string;
  name: string;
  units: number;
  revenue: number;
}

export interface RevenueByDay {
  date: string;
  revenue: number;
}

export interface SellerSalesStats {
  revenue: number;
  unitsSold: number;
  ordersCount: number;
  bestSellers: BestSeller[];
  revenueByDay: RevenueByDay[];
}

export interface SellerSalesStatsResponse {
  success: boolean;
  message: string;
  data: SellerSalesStats;
}
