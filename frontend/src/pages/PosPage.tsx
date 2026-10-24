import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Barcode,
  CheckCircle2,
  Minus,
  Plus,
  Search,
  ShoppingBag,
  Trash2,
  X,
} from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type {
  DiscountPolicyDto,
  InvoiceDto,
  ProductDto,
  SalePreviewResponse,
} from '../types/api';
import { formatCurrency, formatDate, getAvatarColor, getInitials } from '../utils/format';
import { useToast } from '../context/ToastContext';
import { ReceiptModal } from '../components/ReceiptModal';

export interface CartEntry {
  product: ProductDto;
  quantity: number;
}

export const PosPage: React.FC = () => {
  const { showToast } = useToast();
  const searchInputRef = useRef<HTMLInputElement>(null);

  const [products, setProducts] = useState<ProductDto[]>([]);
  const [policies, setPolicies] = useState<DiscountPolicyDto[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [cart, setCart] = useState<CartEntry[]>([]);
  const [selectedPolicy, setSelectedPolicy] = useState<string>('NONE');
  const [preview, setPreview] = useState<SalePreviewResponse | null>(null);
  const [amountPaidInput, setAmountPaidInput] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [checkingOut, setCheckingOut] = useState<boolean>(false);
  const [completedInvoice, setCompletedInvoice] = useState<InvoiceDto | null>(null);

  const loadPosData = useCallback(async () => {
    setLoading(true);
    try {
      const [prodPage, policyList] = await Promise.all([
        apiClient.getProducts({ size: 200, sort: 'productId,asc' }),
        apiClient.getDiscountPolicies(),
      ]);
      setProducts(prodPage.content);
      setPolicies(policyList);
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : 'Lỗi tải danh sách sản phẩm', 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadPosData();
  }, [loadPosData]);

  // Gọi /api/sales/preview mỗi khi giỏ hàng hoặc chính sách giảm giá thay đổi (không tự tính tiền ở FE - mục 3.1)
  useEffect(() => {
    if (cart.length === 0) {
      setPreview(null);
      return;
    }
    let cancelled = false;
    apiClient
      .previewSale(
        cart.map((c) => ({ productId: c.product.productId, quantity: c.quantity })),
        selectedPolicy
      )
      .then((res) => {
        if (!cancelled) setPreview(res);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          showToast(err instanceof Error ? err.message : 'Lỗi tính thử hóa đơn', 'error');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [cart, selectedPolicy, showToast]);

  const addToCart = useCallback(
    (product: ProductDto) => {
      if (product.expired) {
        showToast(`Sản phẩm '${product.name}' đã hết hạn sử dụng, không thể bán!`, 'error');
        return;
      }
      if (product.quantity <= 0) {
        showToast(`Sản phẩm '${product.name}' đã hết hàng trong kho!`, 'warning');
        return;
      }

      const existing = cart.find((item) => item.product.productId === product.productId);
      if (existing && existing.quantity + 1 > product.quantity) {
        showToast(
          `Không thể vượt quá tồn kho hiện tại (${product.quantity}) của '${product.name}'`,
          'warning'
        );
        return;
      }

      setCart((prev) => {
        const found = prev.find((item) => item.product.productId === product.productId);
        if (found) {
          return prev.map((item) =>
            item.product.productId === product.productId
              ? { ...item, quantity: Math.min(product.quantity, item.quantity + 1) }
              : item
          );
        }
        return [...prev, { product, quantity: 1 }];
      });
    },
    [cart, showToast]
  );

  const updateCartQuantity = (productId: string, delta: number) => {
    const target = cart.find((item) => item.product.productId === productId);
    if (target && target.quantity + delta > target.product.quantity) {
      showToast(`Số lượng tối đa trong kho là ${target.product.quantity}`, 'warning');
      return;
    }
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.product.productId !== productId) return item;
          const nextQty = item.quantity + delta;
          if (nextQty <= 0) return null;
          return { ...item, quantity: nextQty };
        })
        .filter((item): item is CartEntry => item !== null)
    );
  };

  const removeFromCart = (productId: string) => {
    setCart((prev) => prev.filter((item) => item.product.productId !== productId));
  };

  const handleCheckout = useCallback(async () => {
    if (cart.length === 0) {
      showToast('Giỏ hàng đang trống, vui lòng chọn sản phẩm trước khi thanh toán', 'warning');
      return;
    }
    const total = preview?.totalAmount ?? 0;
    const paid = amountPaidInput.trim() ? Number(amountPaidInput) : total;
    if (Number.isNaN(paid) || paid < total) {
      showToast('Số tiền khách đưa nhỏ hơn tổng tiền cần thanh toán', 'error');
      return;
    }

    setCheckingOut(true);
    try {
      const invoice = await apiClient.checkout(
        cart.map((c) => ({ productId: c.product.productId, quantity: c.quantity })),
        selectedPolicy,
        paid
      );
      showToast(`Lập hóa đơn ${invoice.invoiceId} thành công!`, 'success');
      setCompletedInvoice(invoice);
      setCart([]);
      setAmountPaidInput('');
      await loadPosData();
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : 'Thanh toán thất bại', 'error');
    } finally {
      setCheckingOut(false);
    }
  }, [cart, preview, amountPaidInput, selectedPolicy, showToast, loadPosData]);

  // Phím tắt F3 (ô tìm mã) và F9 (Thanh toán)
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F3') {
        e.preventDefault();
        searchInputRef.current?.focus();
      } else if (e.key === 'F9') {
        e.preventDefault();
        handleCheckout();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [handleCheckout]);

  const filteredProducts = products.filter((p) => {
    const matchesCat = categoryFilter === 'ALL' || p.category === categoryFilter;
    const q = searchQuery.trim().toLowerCase();
    const matchesQ =
      !q || p.productId.toLowerCase().includes(q) || p.name.toLowerCase().includes(q);
    return matchesCat && matchesQ;
  });

  const handleSearchEnter = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      e.preventDefault();
      const q = searchQuery.trim().toLowerCase();
      const exactMatch = products.find((p) => p.productId.toLowerCase() === q);
      const target = exactMatch || filteredProducts[0];
      if (target) {
        addToCart(target);
        setSearchQuery('');
      } else {
        showToast(`Không tìm thấy sản phẩm khớp với mã/tên '${searchQuery}'`, 'warning');
      }
    }
  };

  const totalAmount = preview?.totalAmount ?? 0;
  const paidValue = amountPaidInput.trim() ? Number(amountPaidInput) : totalAmount;
  const changeAmount = Math.max(0, (Number.isNaN(paidValue) ? 0 : paidValue) - totalAmount);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Cột trái: Tìm kiếm + Lưới thẻ sản phẩm */}
      <div className="lg:col-span-7 xl:col-span-8 space-y-4">
        <div className="bg-surface border border-bordercol rounded-card p-4 shadow-xs space-y-3">
          <div className="relative">
            <Barcode className="w-5 h-5 text-primary absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              ref={searchInputRef}
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleSearchEnter}
              placeholder="Quét mã vạch hoặc nhập mã / tên sản phẩm rồi nhấn Enter để thêm ngay (F3)..."
              className="w-full h-10 pl-11 pr-10 rounded-control border border-bordercol bg-appbg text-sm text-textmain focus:outline-none focus:border-primary"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-textmuted hover:text-textmain"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {[
              { code: 'ALL', label: 'Tất cả sản phẩm' },
              { code: 'FOOD', label: 'Thực phẩm' },
              { code: 'HOUSEHOLD', label: 'Đồ gia dụng' },
            ].map((chip) => (
              <button
                key={chip.code}
                type="button"
                onClick={() => setCategoryFilter(chip.code)}
                className={`h-8 px-3.5 rounded-badge text-xs font-semibold transition-colors ${
                  categoryFilter === chip.code
                    ? 'bg-primary text-white'
                    : 'bg-appbg text-textmuted hover:text-textmain border border-bordercol'
                }`}
              >
                {chip.label}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3.5">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <div key={i} className="h-36 rounded-card bg-surface border border-bordercol animate-pulse" />
            ))}
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="bg-surface border border-bordercol rounded-card p-12 text-center text-textmuted">
            <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <div className="font-medium">Không tìm thấy sản phẩm phù hợp</div>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3.5">
            {filteredProducts.map((p) => {
              const disabled = p.quantity <= 0 || p.expired;
              return (
                <button
                  key={p.productId}
                  type="button"
                  disabled={disabled}
                  onClick={() => addToCart(p)}
                  className={`text-left bg-surface border border-bordercol rounded-card p-3.5 shadow-xs transition-all flex flex-col justify-between min-h-[144px] ${
                    disabled
                      ? 'opacity-50 cursor-not-allowed'
                      : 'hover:border-primary hover:shadow-md active:scale-[0.99]'
                  }`}
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <div
                        className={`w-9 h-9 rounded-control flex items-center justify-center font-bold text-xs shrink-0 ${getAvatarColor(
                          p.name
                        )}`}
                      >
                        {getInitials(p.name)}
                      </div>
                      <div className="flex flex-wrap justify-end gap-1">
                        {p.expired && (
                          <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300">
                            Hết hạn
                          </span>
                        )}
                        {!p.expired && p.quantity <= 0 && (
                          <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300">
                            Hết hàng
                          </span>
                        )}
                        {!p.expired && p.quantity > 0 && p.lowStock && (
                          <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                            Sắp hết ({p.quantity})
                          </span>
                        )}
                        {!p.expired && p.expiringSoon && (
                          <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300">
                            Sắp hết hạn
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="mt-2.5 font-semibold text-sm text-textmain line-clamp-2 leading-snug">
                      {p.name}
                    </div>
                    <div className="text-[11px] text-textmuted mt-0.5 font-mono">
                      {p.productId} · {p.categoryName}
                    </div>
                  </div>

                  <div className="mt-3 pt-2 border-t border-bordercol flex items-center justify-between">
                    <span className="font-bold text-sm text-primary">{formatCurrency(p.price)}</span>
                    <span className="text-[11px] text-textmuted">Tồn: {p.quantity}</span>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Cột phải: Giỏ hàng & Thanh toán */}
      <div className="lg:col-span-5 xl:col-span-4 bg-surface border border-bordercol rounded-card shadow-xs flex flex-col sticky top-20">
        <div className="px-5 py-3.5 border-b border-bordercol flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-primary" />
            <h2 className="text-sm font-semibold text-textmain">
              Giỏ hàng ({cart.reduce((s, c) => s + c.quantity, 0)} SP)
            </h2>
          </div>
          {cart.length > 0 && (
            <button
              type="button"
              onClick={() => setCart([])}
              className="text-xs text-red-600 hover:underline flex items-center gap-1"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Xóa giỏ</span>
            </button>
          )}
        </div>

        <div className="p-4 max-h-[300px] overflow-y-auto divide-y divide-bordercol">
          {cart.length === 0 ? (
            <div className="py-10 text-center text-textmuted text-sm">
              Chưa có mặt hàng nào trong giỏ. Nhấn vào thẻ sản phẩm bên trái hoặc quét mã để thêm.
            </div>
          ) : (
            cart.map((entry) => (
              <div key={entry.product.productId} className="py-2.5 flex items-center justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium text-textmain truncate">{entry.product.name}</div>
                  <div className="text-xs text-textmuted">
                    {formatCurrency(entry.product.price)} × {entry.quantity} ={' '}
                    <strong className="text-textmain">
                      {formatCurrency(entry.product.price * entry.quantity)}
                    </strong>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    aria-label={`Giảm số lượng ${entry.product.name}`}
                    onClick={() => updateCartQuantity(entry.product.productId, -1)}
                    className="w-7 h-7 rounded-control border border-bordercol flex items-center justify-center hover:bg-appbg"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="w-8 text-center text-sm font-semibold">{entry.quantity}</span>
                  <button
                    type="button"
                    aria-label={`Tăng số lượng ${entry.product.name}`}
                    onClick={() => updateCartQuantity(entry.product.productId, 1)}
                    className="w-7 h-7 rounded-control border border-bordercol flex items-center justify-center hover:bg-appbg"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    aria-label={`Xóa ${entry.product.name} khỏi giỏ`}
                    onClick={() => removeFromCart(entry.product.productId)}
                    className="w-7 h-7 rounded-control text-textmuted hover:text-red-600 flex items-center justify-center"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Chính sách giảm giá & Tính tiền */}
        <div className="p-5 border-t border-bordercol bg-appbg/50 space-y-3.5">
          <div>
            <label htmlFor="discount-policy-select" className="block text-xs font-semibold text-textmuted mb-1">
              Chính sách giảm giá (FunctionalInterface + Lambda)
            </label>
            <select
              id="discount-policy-select"
              value={selectedPolicy}
              onChange={(e) => setSelectedPolicy(e.target.value)}
              className="w-full h-10 px-3 rounded-control border border-bordercol bg-surface text-sm text-textmain focus:outline-none focus:border-primary"
            >
              {policies.map((pol) => (
                <option key={pol.code} value={pol.code}>
                  {pol.name}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5 text-sm pt-1">
            <div className="flex justify-between text-textmuted">
              <span>Tạm tính:</span>
              <span className="font-medium text-textmain">{formatCurrency(preview?.subtotal ?? 0)}</span>
            </div>
            <div className="flex justify-between text-emerald-600">
              <span>Giảm giá:</span>
              <span className="font-semibold">-{formatCurrency(preview?.discount ?? 0)}</span>
            </div>
            <div className="pt-2 border-t border-bordercol flex items-baseline justify-between">
              <span className="text-sm font-bold uppercase text-textmain">Tổng thanh toán:</span>
              <span className="text-[32px] leading-none font-bold text-primary">
                {formatCurrency(totalAmount)}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-1">
            <div>
              <label htmlFor="amount-paid-input" className="block text-xs font-medium text-textmuted mb-1">
                Tiền khách đưa (VND)
              </label>
              <input
                id="amount-paid-input"
                type="number"
                min={0}
                step={1000}
                value={amountPaidInput}
                onChange={(e) => setAmountPaidInput(e.target.value)}
                placeholder={String(totalAmount)}
                className="w-full h-10 px-3 rounded-control border border-bordercol bg-surface text-sm text-textmain focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <span className="block text-xs font-medium text-textmuted mb-1">Tiền thừa trả khách</span>
              <div className="h-10 px-3 rounded-control border border-bordercol bg-surface flex items-center font-bold text-sm text-emerald-600">
                {formatCurrency(changeAmount)}
              </div>
            </div>
          </div>

          <button
            type="button"
            disabled={cart.length === 0 || checkingOut}
            onClick={handleCheckout}
            className="w-full h-[52px] rounded-control bg-primary hover:bg-primary-hover disabled:opacity-50 text-white font-bold text-base shadow-sm flex items-center justify-center gap-2 transition-colors"
          >
            <CheckCircle2 className="w-5 h-5" />
            <span>{checkingOut ? 'ĐANG XỬ LÝ...' : 'THANH TOÁN (F9)'}</span>
          </button>
        </div>
      </div>

      <ReceiptModal invoice={completedInvoice} onClose={() => setCompletedInvoice(null)} />
    </div>
  );
};
