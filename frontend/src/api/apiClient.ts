import type {
  CategoryRevenueDto,
  DailyRevenueDto,
  DiscountPolicyDto,
  ExternalProductDto,
  FieldValidationError,
  InvoiceDto,
  PageResponse,
  ProductCreatePayload,
  ProductDto,
  SaleItemInput,
  SalePreviewResponse,
  StatsSummaryDto,
  TopProductDto,
} from '../types/api';

const RAW_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '');

export class ApiError extends Error {
  public readonly status: number;
  public readonly title: string;
  public readonly fieldErrors: FieldValidationError[];

  constructor(status: number, title: string, detail: string, fieldErrors: FieldValidationError[] = []) {
    super(detail || title || 'Đã xảy ra lỗi khi gọi máy chủ');
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.fieldErrors = fieldErrors;
  }
}

type LatencyListener = (latencyMs: number | null, connected: boolean) => void;
const latencyListeners = new Set<LatencyListener>();

export function subscribeApiStatus(listener: LatencyListener): () => void {
  latencyListeners.add(listener);
  return () => {
    latencyListeners.delete(listener);
  };
}

function notifyStatus(latencyMs: number | null, connected: boolean) {
  latencyListeners.forEach((cb) => cb(latencyMs, connected));
}

function resolveUrl(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${RAW_BASE_URL}${normalizedPath}`;
}

async function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export interface RequestOptions extends RequestInit {
  timeoutMs?: number;
  retries?: number;
  retryDelayMs?: number;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const {
    timeoutMs = 10000,
    retries = 2,
    retryDelayMs = 600,
    headers,
    ...rest
  } = options;

  let attempt = 0;
  while (true) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const startTime = performance.now();

    try {
      const response = await fetch(resolveUrl(path), {
        ...rest,
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...(headers || {}),
        },
        signal: controller.signal,
      });

      clearTimeout(timer);
      const latency = Math.round(performance.now() - startTime);
      notifyStatus(latency, true);

      if (!response.ok) {
        let title = 'Lỗi yêu cầu';
        let detail = `Máy chủ trả về mã lỗi ${response.status}`;
        let fieldErrors: FieldValidationError[] = [];

        try {
          const errJson = await response.json();
          if (errJson) {
            title = errJson.title || title;
            detail = errJson.detail || errJson.message || detail;
            if (Array.isArray(errJson.errors)) {
              fieldErrors = errJson.errors;
            }
          }
        } catch {
          // Bỏ qua nếu body không phải JSON
        }

        // Retry nếu máy chủ đang khởi động lại (502/503/504) và là phương thức GET
        const isIdempotent = !rest.method || rest.method.toUpperCase() === 'GET';
        if (isIdempotent && [502, 503, 504].includes(response.status) && attempt < retries) {
          attempt++;
          await sleep(retryDelayMs * Math.pow(2, attempt - 1));
          continue;
        }

        throw new ApiError(response.status, title, detail, fieldErrors);
      }

      if (response.status === 204) {
        return undefined as T;
      }

      return (await response.json()) as T;
    } catch (err: unknown) {
      clearTimeout(timer);
      if (err instanceof ApiError) {
        throw err;
      }

      const isAbort = err instanceof DOMException && err.name === 'AbortError';
      if (attempt < retries) {
        attempt++;
        await sleep(retryDelayMs * Math.pow(2, attempt - 1));
        continue;
      }

      notifyStatus(null, false);
      if (isAbort) {
        throw new ApiError(504, 'Quá thời gian chờ', 'Yêu cầu tới máy chủ bị quá thời gian chờ (Timeout). Vui lòng thử lại.');
      }
      throw new ApiError(0, 'Mất kết nối máy chủ', 'Không thể kết nối tới máy chủ Backend. Vui lòng kiểm tra kết nối mạng.');
    }
  }
}

/**
 * Đánh thức máy chủ ngủ (Render/Fly/Railway) tối đa ~60 giây (mục 3.2 prompt).
 */
export async function warmUpServer(
  onProgress?: (attempt: number, elapsedSec: number) => void,
  maxWaitMs = 60000
): Promise<boolean> {
  const start = Date.now();
  let attempt = 0;

  while (Date.now() - start < maxWaitMs) {
    attempt++;
    const elapsedSec = Math.round((Date.now() - start) / 1000);
    onProgress?.(attempt, elapsedSec);

    try {
      const res = await apiRequest<{ status: string }>('/actuator/health', {
        timeoutMs: 4500,
        retries: 0,
      });
      if (res && res.status === 'UP') {
        return true;
      }
    } catch {
      // Tiếp tục thử lại khi container đang khởi động
    }

    await sleep(Math.min(3000, 800 + attempt * 400));
  }
  return false;
}

export const apiClient = {
  getProducts(params: {
    q?: string;
    category?: string;
    lowStock?: boolean;
    expiringSoon?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  } = {}) {
    const qs = new URLSearchParams();
    if (params.q) qs.set('q', params.q);
    if (params.category && params.category !== 'ALL') qs.set('category', params.category);
    if (params.lowStock) qs.set('lowStock', 'true');
    if (params.expiringSoon) qs.set('expiringSoon', 'true');
    qs.set('page', String(params.page ?? 0));
    qs.set('size', String(params.size ?? 100));
    if (params.sort) qs.set('sort', params.sort);
    return apiRequest<PageResponse<ProductDto>>(`/api/products?${qs.toString()}`);
  },

  createProduct(payload: ProductCreatePayload) {
    return apiRequest<ProductDto>('/api/products', {
      method: 'POST',
      body: JSON.stringify(payload),
      retries: 0,
    });
  },

  updateProduct(productId: string, payload: Omit<ProductCreatePayload, 'productId'>) {
    return apiRequest<ProductDto>(`/api/products/${encodeURIComponent(productId)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      retries: 0,
    });
  },

  deleteProduct(productId: string) {
    return apiRequest<void>(`/api/products/${encodeURIComponent(productId)}`, {
      method: 'DELETE',
      retries: 0,
    });
  },

  getDiscountPolicies() {
    return apiRequest<DiscountPolicyDto[]>('/api/discount-policies');
  },

  previewSale(items: SaleItemInput[], discountPolicy: string) {
    return apiRequest<SalePreviewResponse>('/api/sales/preview', {
      method: 'POST',
      body: JSON.stringify({ items, discountPolicy }),
      retries: 0,
    });
  },

  checkout(items: SaleItemInput[], discountPolicy: string, amountPaid?: number) {
    return apiRequest<InvoiceDto>('/api/sales/checkout', {
      method: 'POST',
      body: JSON.stringify({ items, discountPolicy, amountPaid }),
      retries: 0,
    });
  },

  getInvoices(params: { from?: string; to?: string; status?: string; page?: number; size?: number } = {}) {
    const qs = new URLSearchParams();
    if (params.from) qs.set('from', params.from);
    if (params.to) qs.set('to', params.to);
    if (params.status && params.status !== 'ALL') qs.set('status', params.status);
    qs.set('page', String(params.page ?? 0));
    qs.set('size', String(params.size ?? 50));
    return apiRequest<PageResponse<InvoiceDto>>(`/api/invoices?${qs.toString()}`);
  },

  getInvoiceById(id: string) {
    return apiRequest<InvoiceDto>(`/api/invoices/${encodeURIComponent(id)}`);
  },

  cancelInvoice(id: string) {
    return apiRequest<InvoiceDto>(`/api/invoices/${encodeURIComponent(id)}/cancel`, {
      method: 'POST',
      retries: 0,
    });
  },

  getStatsSummary(from?: string, to?: string) {
    const qs = new URLSearchParams();
    if (from) qs.set('from', from);
    if (to) qs.set('to', to);
    return apiRequest<StatsSummaryDto>(`/api/stats/summary?${qs.toString()}`);
  },

  getStatsDaily(from?: string, to?: string) {
    const qs = new URLSearchParams();
    if (from) qs.set('from', from);
    if (to) qs.set('to', to);
    return apiRequest<DailyRevenueDto[]>(`/api/stats/daily?${qs.toString()}`);
  },

  getStatsByCategory(from?: string, to?: string) {
    const qs = new URLSearchParams();
    if (from) qs.set('from', from);
    if (to) qs.set('to', to);
    return apiRequest<CategoryRevenueDto[]>(`/api/stats/by-category?${qs.toString()}`);
  },

  getTopProducts(from?: string, to?: string, limit = 5) {
    const qs = new URLSearchParams();
    if (from) qs.set('from', from);
    if (to) qs.set('to', to);
    qs.set('limit', String(limit));
    return apiRequest<TopProductDto[]>(`/api/stats/top-products?${qs.toString()}`);
  },

  getExternalProducts(limit = 20, q?: string) {
    const qs = new URLSearchParams();
    qs.set('limit', String(limit));
    if (q) qs.set('q', q);
    return apiRequest<ExternalProductDto[]>(`/api/external/products?${qs.toString()}`, {
      timeoutMs: 12000,
    });
  },
};
