export interface ProductDto {
  productId: string;
  name: string;
  price: number;
  quantity: number;
  category: 'FOOD' | 'HOUSEHOLD';
  categoryName: string;
  expiryDate?: string | null;
  brand?: string | null;
  expired: boolean;
  expiringSoon: boolean;
  lowStock: boolean;
  version?: number;
}

export interface ProductCreatePayload {
  productId: string;
  name: string;
  price: number;
  quantity: number;
  category: 'FOOD' | 'HOUSEHOLD';
  expiryDate?: string | null;
  brand?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DiscountPolicyDto {
  code: string;
  name: string;
  description: string;
}

export interface SaleItemInput {
  productId: string;
  quantity: number;
}

export interface PreviewLineItem {
  productId: string;
  productName: string;
  category: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface SalePreviewResponse {
  items: PreviewLineItem[];
  subtotal: number;
  discountPolicy: string;
  discount: number;
  totalAmount: number;
}

export interface InvoiceItemDto {
  detailId: number;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface InvoiceDto {
  invoiceId: string;
  invoiceDate: string;
  status: 'COMPLETED' | 'CANCELLED';
  subtotal: number;
  discount: number;
  totalAmount: number;
  amountPaid: number;
  changeAmount: number;
  items: InvoiceItemDto[];
}

export interface StatsSummaryDto {
  totalRevenue: number;
  completedInvoices: number;
  cancelledInvoices: number;
  averageOrderValue: number;
  totalDiscount: number;
  lowStockProductsCount: number;
  expiringSoonProductsCount: number;
}

export interface DailyRevenueDto {
  date: string;
  revenue: number;
  invoiceCount: number;
}

export interface CategoryRevenueDto {
  categoryCode: string;
  categoryName: string;
  revenue: number;
  quantitySold: number;
  percentage: number;
}

export interface TopProductDto {
  productId: string;
  productName: string;
  categoryName: string;
  quantitySold: number;
  revenue: number;
}

export interface ExternalProductDto {
  externalId: string;
  suggestedProductId: string;
  title: string;
  category: string;
  mappedCategoryCode: 'FOOD' | 'HOUSEHOLD';
  mappedCategoryName: string;
  brand: string;
  priceUsd: number;
  priceVnd: number;
  stock: number;
  thumbnail: string;
}

export interface FieldValidationError {
  field: string;
  message: string;
}
