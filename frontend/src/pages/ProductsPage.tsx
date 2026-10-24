import React, { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowUpDown,
  Download,
  Edit2,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { ApiError, apiClient } from '../api/apiClient';
import type { ProductCreatePayload, ProductDto } from '../types/api';
import { formatCurrency, formatDate, getAvatarColor, getInitials } from '../utils/format';
import { useToast } from '../context/ToastContext';

export const ProductsPage: React.FC = () => {
  const { showToast } = useToast();
  const [searchParams] = useSearchParams();

  const [products, setProducts] = useState<ProductDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [searchInput, setSearchInput] = useState<string>(searchParams.get('q') || '');
  const [debouncedQuery, setDebouncedQuery] = useState<string>(searchParams.get('q') || '');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [lowStockOnly, setLowStockOnly] = useState<boolean>(searchParams.get('lowStock') === 'true');
  const [expiringOnly, setExpiringOnly] = useState<boolean>(searchParams.get('expiringSoon') === 'true');
  const [sortBy, setSortBy] = useState<string>('productId,asc');

  // Modal Thêm / Sửa
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [editingProduct, setEditingProduct] = useState<ProductDto | null>(null);
  const [formId, setFormId] = useState<string>('');
  const [formName, setFormName] = useState<string>('');
  const [formPrice, setFormPrice] = useState<string>('');
  const [formQty, setFormQty] = useState<string>('');
  const [formCategory, setFormCategory] = useState<'FOOD' | 'HOUSEHOLD'>('FOOD');
  const [formExpiry, setFormExpiry] = useState<string>('');
  const [formBrand, setFormBrand] = useState<string>('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Modal Xác nhận xóa
  const [deletingProduct, setDeletingProduct] = useState<ProductDto | null>(null);

  // Debounce 250ms cho ô tìm kiếm (mục 3.3)
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(searchInput), 250);
    return () => clearTimeout(t);
  }, [searchInput]);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await apiClient.getProducts({
        q: debouncedQuery || undefined,
        category: categoryFilter,
        lowStock: lowStockOnly || undefined,
        expiringSoon: expiringOnly || undefined,
        sort: sortBy,
        size: 100,
      });
      setProducts(page.content);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách sản phẩm');
    } finally {
      setLoading(false);
    }
  }, [debouncedQuery, categoryFilter, lowStockOnly, expiringOnly, sortBy]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const openAddModal = useCallback(() => {
    setEditingProduct(null);
    setFormId('');
    setFormName('');
    setFormPrice('');
    setFormQty('10');
    setFormCategory('FOOD');
    setFormExpiry('');
    setFormBrand('');
    setFieldErrors({});
    setModalOpen(true);
  }, []);

  const openEditModal = (product: ProductDto) => {
    setEditingProduct(product);
    setFormId(product.productId);
    setFormName(product.name);
    setFormPrice(String(product.price));
    setFormQty(String(product.quantity));
    setFormCategory(product.category);
    setFormExpiry(product.expiryDate || '');
    setFormBrand(product.brand || '');
    setFieldErrors({});
    setModalOpen(true);
  };

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        openAddModal();
      } else if (e.key === 'Escape') {
        setModalOpen(false);
        setDeletingProduct(null);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [openAddModal]);

  const validateForm = (): boolean => {
    const errs: Record<string, string> = {};
    if (!editingProduct && !formId.trim()) {
      errs.productId = 'Mã sản phẩm không được để trống';
    }
    if (!formName.trim()) {
      errs.name = 'Tên sản phẩm không được để trống';
    }
    const priceNum = Number(formPrice);
    if (formPrice.trim() === '' || Number.isNaN(priceNum)) {
      errs.price = 'Vui lòng nhập đơn giá đúng định dạng số';
    } else if (priceNum < 0) {
      errs.price = 'Đơn giá không được âm';
    }
    const qtyNum = Number(formQty);
    if (formQty.trim() === '' || Number.isNaN(qtyNum) || !Number.isInteger(qtyNum)) {
      errs.quantity = 'Số lượng tồn phải là số nguyên hợp lệ';
    } else if (qtyNum < 0) {
      errs.quantity = 'Số lượng tồn kho không được âm';
    }
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    setFieldErrors({});
    try {
      const payload: ProductCreatePayload = {
        productId: formId.trim(),
        name: formName.trim(),
        price: Number(formPrice),
        quantity: Number(formQty),
        category: formCategory,
        expiryDate: formCategory === 'FOOD' && formExpiry ? formExpiry : null,
        brand: formCategory === 'HOUSEHOLD' && formBrand.trim() ? formBrand.trim() : null,
      };

      if (editingProduct) {
        await apiClient.updateProduct(editingProduct.productId, payload);
        showToast(`Đã cập nhật sản phẩm ${editingProduct.productId}`, 'success');
      } else {
        await apiClient.createProduct(payload);
        showToast(`Thêm sản phẩm ${payload.productId} thành công`, 'success');
      }
      setModalOpen(false);
      await loadProducts();
    } catch (err: unknown) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        const map: Record<string, string> = {};
        err.fieldErrors.forEach((fe) => {
          map[fe.field] = fe.message;
        });
        setFieldErrors(map);
      }
      showToast(err instanceof Error ? err.message : 'Lỗi lưu sản phẩm', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingProduct) return;
    try {
      await apiClient.deleteProduct(deletingProduct.productId);
      showToast(`Đã xóa sản phẩm ${deletingProduct.productId}`, 'success');
      setDeletingProduct(null);
      await loadProducts();
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : 'Không thể xóa sản phẩm', 'error');
    }
  };

  const handleExportCsv = () => {
    const header = ['Mã SP', 'Tên sản phẩm', 'Loại', 'Đơn giá (VND)', 'Tồn kho', 'Hạn dùng / Thương hiệu'];
    const rows = products.map((p) => [
      p.productId,
      `"${p.name.replace(/"/g, '""')}"`,
      `"${p.categoryName}"`,
      p.price,
      p.quantity,
      p.category === 'FOOD' ? p.expiryDate || '' : `"${(p.brand || '').replace(/"/g, '""')}"`,
    ]);
    const csv = [header.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `DanhSachSanPham_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Đã xuất tệp CSV danh sách sản phẩm (UTF-8 BOM)', 'success');
  };

  const toggleSort = (field: string) => {
    const [curField, curDir] = sortBy.split(',');
    if (curField === field) {
      setSortBy(`${field},${curDir === 'asc' ? 'desc' : 'asc'}`);
    } else {
      setSortBy(`${field},asc`);
    }
  };

  return (
    <div className="space-y-5">
      {/* Thanh công cụ tìm kiếm, lọc và nút thao tác */}
      <div className="bg-surface border border-bordercol rounded-card p-4 shadow-xs flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3 flex-1">
          <div className="relative flex-1 min-w-[240px]">
            <Search className="w-4 h-4 text-textmuted absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm theo mã hoặc tên sản phẩm (debounce 250ms)..."
              className="w-full h-10 pl-10 pr-8 rounded-control border border-bordercol bg-appbg text-sm text-textmain focus:outline-none focus:border-primary"
            />
            {searchInput && (
              <button
                type="button"
                onClick={() => setSearchInput('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-textmuted hover:text-textmain"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <select
            aria-label="Lọc theo loại sản phẩm"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
          >
            <option value="ALL">Tất cả loại</option>
            <option value="FOOD">Thực phẩm</option>
            <option value="HOUSEHOLD">Đồ gia dụng</option>
          </select>

          <label className="inline-flex items-center gap-2 text-xs font-medium text-textmain cursor-pointer select-none px-3 h-10 rounded-control border border-bordercol bg-appbg">
            <input
              type="checkbox"
              checked={lowStockOnly}
              onChange={(e) => setLowStockOnly(e.target.checked)}
              className="rounded text-primary"
            />
            <span>Tồn kho thấp (≤ 10)</span>
          </label>

          <label className="inline-flex items-center gap-2 text-xs font-medium text-textmain cursor-pointer select-none px-3 h-10 rounded-control border border-bordercol bg-appbg">
            <input
              type="checkbox"
              checked={expiringOnly}
              onChange={(e) => setExpiringOnly(e.target.checked)}
              className="rounded text-primary"
            />
            <span>Sắp / Đã hết hạn</span>
          </label>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={handleExportCsv}
            className="h-10 px-3.5 rounded-control border border-bordercol bg-surface hover:bg-appbg text-sm font-medium text-textmain inline-flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            <span>Xuất CSV</span>
          </button>
          <button
            type="button"
            onClick={openAddModal}
            className="h-10 px-4 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-semibold inline-flex items-center gap-2 shadow-xs"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm sản phẩm (Ctrl+N)</span>
          </button>
        </div>
      </div>

      {/* Bảng dữ liệu sản phẩm */}
      <div className="bg-surface border border-bordercol rounded-card shadow-xs overflow-hidden">
        {loading ? (
          <div className="p-8 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-10 rounded bg-appbg animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div className="p-8 text-center space-y-3">
            <div className="text-red-600 font-medium">{error}</div>
            <button
              type="button"
              onClick={loadProducts}
              className="h-9 px-4 rounded-control bg-primary text-white text-xs font-semibold inline-flex items-center gap-1.5"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center text-textmuted">
            Không có sản phẩm nào khớp với bộ lọc hiện tại.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="bg-appbg/60 border-b border-bordercol text-xs font-semibold text-textmuted uppercase">
                  <th className="py-3 px-4">
                    <button
                      type="button"
                      onClick={() => toggleSort('productId')}
                      className="inline-flex items-center gap-1 hover:text-textmain"
                    >
                      <span>Mã SP</span>
                      <ArrowUpDown className="w-3.5 h-3.5" />
                    </button>
                  </th>
                  <th className="py-3 px-4">
                    <button
                      type="button"
                      onClick={() => toggleSort('name')}
                      className="inline-flex items-center gap-1 hover:text-textmain"
                    >
                      <span>Tên sản phẩm</span>
                      <ArrowUpDown className="w-3.5 h-3.5" />
                    </button>
                  </th>
                  <th className="py-3 px-4">Loại (Đa hình)</th>
                  <th className="py-3 px-4 text-right">
                    <button
                      type="button"
                      onClick={() => toggleSort('price')}
                      className="inline-flex items-center gap-1 hover:text-textmain"
                    >
                      <span>Đơn giá</span>
                      <ArrowUpDown className="w-3.5 h-3.5" />
                    </button>
                  </th>
                  <th className="py-3 px-4 text-center">
                    <button
                      type="button"
                      onClick={() => toggleSort('quantity')}
                      className="inline-flex items-center gap-1 hover:text-textmain"
                    >
                      <span>Tồn kho</span>
                      <ArrowUpDown className="w-3.5 h-3.5" />
                    </button>
                  </th>
                  <th className="py-3 px-4">Hạn dùng / Thương hiệu</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bordercol">
                {products.map((p) => (
                  <tr key={p.productId} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40">
                    <td className="py-3 px-4 font-mono font-semibold text-xs text-primary">
                      {p.productId}
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-8 h-8 rounded-control flex items-center justify-center font-bold text-xs shrink-0 ${getAvatarColor(
                            p.name
                          )}`}
                        >
                          {getInitials(p.name)}
                        </div>
                        <span className="font-medium text-textmain">{p.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2.5 py-0.5 rounded-badge text-xs font-semibold ${
                          p.category === 'FOOD'
                            ? 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300'
                            : 'bg-cyan-100 text-cyan-800 dark:bg-cyan-950 dark:text-cyan-300'
                        }`}
                      >
                        {p.categoryName}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-textmain">
                      {formatCurrency(p.price)}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-badge text-xs font-semibold ${
                          p.quantity === 0
                            ? 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300'
                            : p.lowStock
                            ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                            : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                        }`}
                      >
                        {p.quantity} {p.lowStock && p.quantity > 0 ? '(Sắp hết)' : ''}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-xs text-textmuted">
                      {p.category === 'FOOD' ? (
                        <div className="flex items-center gap-1.5">
                          <span>HSD: {formatDate(p.expiryDate)}</span>
                          {p.expired && (
                            <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-red-100 text-red-700">
                              Đã hết hạn
                            </span>
                          )}
                          {!p.expired && p.expiringSoon && (
                            <span className="px-2 py-0.5 rounded-badge text-[10px] font-semibold bg-amber-100 text-amber-800">
                              Sắp hết hạn
                            </span>
                          )}
                        </div>
                      ) : (
                        <span>Hãng: {p.brand || '—'}</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => openEditModal(p)}
                          aria-label={`Sửa ${p.name}`}
                          className="p-1.5 rounded-control text-textmuted hover:text-primary hover:bg-appbg"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeletingProduct(p)}
                          aria-label={`Xóa ${p.name}`}
                          className="p-1.5 rounded-control text-textmuted hover:text-red-600 hover:bg-appbg"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Thêm / Sửa sản phẩm */}
      {modalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="product-modal-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4"
        >
          <div className="bg-surface border border-bordercol rounded-card shadow-xl max-w-lg w-full overflow-hidden">
            <div className="px-5 py-4 border-b border-bordercol flex items-center justify-between">
              <h3 id="product-modal-title" className="text-base font-bold text-textmain">
                {editingProduct ? `Sửa sản phẩm: ${editingProduct.productId}` : 'Thêm sản phẩm mới'}
              </h3>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="p-1 text-textmuted hover:text-textmain"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveProduct} noValidate className="p-5 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="prod-id" className="block text-xs font-semibold text-textmain mb-1">
                    Mã sản phẩm *
                  </label>
                  <input
                    id="prod-id"
                    type="text"
                    disabled={!!editingProduct}
                    value={formId}
                    onChange={(e) => setFormId(e.target.value)}
                    placeholder="VD: SP15"
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg disabled:opacity-60 text-sm text-textmain"
                  />
                  {fieldErrors.productId && (
                    <p className="text-xs text-red-600 mt-1">{fieldErrors.productId}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="prod-cat" className="block text-xs font-semibold text-textmain mb-1">
                    Loại sản phẩm *
                  </label>
                  <select
                    id="prod-cat"
                    disabled={!!editingProduct}
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as 'FOOD' | 'HOUSEHOLD')}
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg disabled:opacity-60 text-sm text-textmain"
                  >
                    <option value="FOOD">Thực phẩm (FoodProduct)</option>
                    <option value="HOUSEHOLD">Đồ gia dụng (HouseholdProduct)</option>
                  </select>
                </div>
              </div>

              <div>
                <label htmlFor="prod-name" className="block text-xs font-semibold text-textmain mb-1">
                  Tên sản phẩm *
                </label>
                <input
                  id="prod-name"
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="VD: Sữa tươi tiệt trùng Vinamilk"
                  className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
                />
                {fieldErrors.name && <p className="text-xs text-red-600 mt-1">{fieldErrors.name}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="prod-price" className="block text-xs font-semibold text-textmain mb-1">
                    Đơn giá (VND) *
                  </label>
                  <input
                    id="prod-price"
                    type="text"
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    placeholder="VD: 30000"
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
                  />
                  {fieldErrors.price && <p className="text-xs text-red-600 mt-1">{fieldErrors.price}</p>}
                </div>

                <div>
                  <label htmlFor="prod-qty" className="block text-xs font-semibold text-textmain mb-1">
                    Số lượng tồn kho *
                  </label>
                  <input
                    id="prod-qty"
                    type="text"
                    value={formQty}
                    onChange={(e) => setFormQty(e.target.value)}
                    placeholder="VD: 20"
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
                  />
                  {fieldErrors.quantity && (
                    <p className="text-xs text-red-600 mt-1">{fieldErrors.quantity}</p>
                  )}
                </div>
              </div>

              {formCategory === 'FOOD' ? (
                <div>
                  <label htmlFor="prod-expiry" className="block text-xs font-semibold text-textmain mb-1">
                    Hạn sử dụng (Thuộc tính riêng của FoodProduct)
                  </label>
                  <input
                    id="prod-expiry"
                    type="date"
                    value={formExpiry}
                    onChange={(e) => setFormExpiry(e.target.value)}
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
                  />
                </div>
              ) : (
                <div>
                  <label htmlFor="prod-brand" className="block text-xs font-semibold text-textmain mb-1">
                    Thương hiệu (Thuộc tính riêng của HouseholdProduct)
                  </label>
                  <input
                    id="prod-brand"
                    type="text"
                    value={formBrand}
                    onChange={(e) => setFormBrand(e.target.value)}
                    placeholder="VD: Sunlight, OMO, Pulppy..."
                    className="w-full h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
                  />
                </div>
              )}

              <div className="pt-3 border-t border-bordercol flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="h-10 px-4 rounded-control border border-bordercol text-sm font-medium text-textmain hover:bg-appbg"
                >
                  Hủy (Esc)
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="h-10 px-5 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-semibold"
                >
                  {submitting ? 'Đang lưu...' : editingProduct ? 'Lưu cập nhật' : 'Thêm sản phẩm'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Xác nhận xóa */}
      {deletingProduct && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4"
        >
          <div className="bg-surface border border-bordercol rounded-card shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-textmain">Xác nhận xóa sản phẩm</h3>
            <p className="text-sm text-textmuted">
              Bạn có chắc chắn muốn xóa sản phẩm{' '}
              <strong className="text-textmain">
                {deletingProduct.name} ({deletingProduct.productId})
              </strong>{' '}
              không?
            </p>
            <div className="flex justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setDeletingProduct(null)}
                className="h-10 px-4 rounded-control border border-bordercol text-sm font-medium text-textmain"
              >
                Không
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                className="h-10 px-4 rounded-control bg-red-600 hover:bg-red-700 text-white text-sm font-semibold"
              >
                Xóa sản phẩm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
