import React from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { formatCurrency, getInitials } from '../utils/format';
import { apiRequest, ApiError } from '../api/apiClient';
import { PosPage } from '../pages/PosPage';
import { ToastProvider } from '../context/ToastContext';

describe('Định dạng tiền tệ & Avatar (format.ts)', () => {
  it('Định dạng VND chuẩn 1.234.000 đ bằng Intl.NumberFormat(vi-VN)', () => {
    expect(formatCurrency(1234000)).toBe('1.234.000 đ');
    expect(formatCurrency(60000)).toBe('60.000 đ');
    expect(formatCurrency(0)).toBe('0 đ');
  });

  it('Sinh chữ cái đầu từ tên sản phẩm cho Avatar', () => {
    expect(getInitials('Sữa tươi Vinamilk')).toBe('ST');
    expect(getInitials('Gạo')).toBe('GẠ');
  });
});

describe('Lớp apiClient (lỗi ProblemDetail / retry / timeout)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('Parse chuẩn RFC 7807 ProblemDetail khi máy chủ trả về lỗi 400/409', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({
        title: 'Xung đột dữ liệu nghiệp vụ',
        detail: "Sản phẩm 'SP01' không đủ tồn kho",
        errors: [],
      }),
    });
    vi.stubGlobal('fetch', mockFetch);

    await expect(apiRequest('/api/sales/checkout', { method: 'POST', retries: 0 })).rejects.toThrow(
      "Sản phẩm 'SP01' không đủ tồn kho"
    );
  });

  it('Tự động retry có backoff khi lần gọi đầu gặp lỗi mạng hoặc 503', async () => {
    const mockFetch = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        json: async () => ({ detail: 'Server waking up' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ status: 'UP' }),
      });
    vi.stubGlobal('fetch', mockFetch);

    const res = await apiRequest<{ status: string }>('/actuator/health', {
      retries: 2,
      retryDelayMs: 10,
    });
    expect(res.status).toBe('UP');
    expect(mockFetch).toHaveBeenCalledTimes(2);
  });
});

describe('Thành phần Giỏ hàng POS (+/−, chặn vượt tồn kho)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    const mockFetch = vi.fn(async (url: string) => {
      if (url.includes('/api/products')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            content: [
              {
                productId: 'SP01',
                name: 'Sữa tươi Vinamilk',
                price: 30000,
                quantity: 2,
                category: 'FOOD',
                categoryName: 'Thực phẩm',
                expired: false,
                expiringSoon: false,
                lowStock: true,
              },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 200,
          }),
        };
      }
      if (url.includes('/api/discount-policies')) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            { code: 'NONE', name: 'Không giảm giá', description: 'Nguyên giá' },
          ],
        };
      }
      if (url.includes('/api/sales/preview')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            items: [],
            subtotal: 60000,
            discountPolicy: 'NONE',
            discount: 0,
            totalAmount: 60000,
          }),
        };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });
    vi.stubGlobal('fetch', mockFetch);
  });

  it('Cho phép thêm vào giỏ, tăng/giảm số lượng và chặn khi vượt quá tồn kho (tồn = 2)', async () => {
    render(
      <ToastProvider>
        <PosPage />
      </ToastProvider>
    );

    const productCard = await screen.findByText('Sữa tươi Vinamilk');
    fireEvent.click(productCard);

    // Số lượng trong giỏ = 1
    expect(await screen.findByText(/Giỏ hàng \(1 SP\)/)).toBeInTheDocument();

    // Bấm nút + để tăng lên 2 (bằng tồn kho)
    const plusBtn = screen.getByLabelText('Tăng số lượng Sữa tươi Vinamilk');
    fireEvent.click(plusBtn);
    expect(await screen.findByText(/Giỏ hàng \(2 SP\)/)).toBeInTheDocument();

    // Bấm nút + lần nữa (vượt tồn kho = 2) -> Không tăng lên 3, hiện cảnh báo
    fireEvent.click(plusBtn);
    expect(await screen.findByText(/Giỏ hàng \(2 SP\)/)).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/Số lượng tối đa trong kho là 2/)).toBeInTheDocument();
    });
  });
});
