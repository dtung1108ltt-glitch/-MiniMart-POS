import React, { useCallback, useEffect, useState } from 'react';
import { Globe, PlusCircle, RefreshCw, Search } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { ExternalProductDto } from '../types/api';
import { formatCurrency } from '../utils/format';
import { useToast } from '../context/ToastContext';

export const ExternalProductsPage: React.FC = () => {
  const { showToast } = useToast();

  const [items, setItems] = useState<ExternalProductDto[]>([]);
  const [query, setQuery] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [importingId, setImportingId] = useState<string | null>(null);

  const fetchExternal = useCallback(
    async (searchQ?: string) => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiClient.getExternalProducts(15, searchQ);
        setItems(data);
      } catch (err: unknown) {
        setError(
          err instanceof Error
            ? err.message
            : 'Không thể kết nối tới dịch vụ API ngoài (dummyjson.com)'
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    fetchExternal();
  }, [fetchExternal]);

  const handleImportToStore = async (item: ExternalProductDto) => {
    setImportingId(item.suggestedProductId);
    try {
      const defaultExpiry =
        item.mappedCategoryCode === 'FOOD'
          ? new Date(Date.now() + 90 * 86400000).toISOString().slice(0, 10)
          : null;

      await apiClient.createProduct({
        productId: item.suggestedProductId,
        name: item.title,
        price: item.priceVnd,
        quantity: Math.max(10, item.stock),
        category: item.mappedCategoryCode,
        expiryDate: defaultExpiry,
        brand: item.mappedCategoryCode === 'HOUSEHOLD' ? item.brand : null,
      });
      showToast(
        `Đã nhập '${item.title}' (${item.suggestedProductId}) vào kho cửa hàng thành công!`,
        'success'
      );
    } catch (err: unknown) {
      showToast(
        err instanceof Error ? err.message : 'Không thể thêm sản phẩm vào cửa hàng',
        'error'
      );
    } finally {
      setImportingId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="bg-surface border border-bordercol rounded-card p-4 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-sm font-semibold text-textmain">
            <Globe className="w-4 h-4 text-primary" />
            <span>Tích hợp API ngoài qua Java HttpClient (ApiService — mục 3.7 báo cáo docx)</span>
          </div>
          <p className="text-xs text-textmuted">
            Lưu ý: Dữ liệu từ <code>https://dummyjson.com/products</code> chỉ dùng để tham khảo và nhập mẫu;
            hóa đơn, tồn kho và doanh thu hoạt động độc lập trên CSDL của hệ thống.
          </p>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            fetchExternal(query);
          }}
          className="flex items-center gap-2"
        >
          <div className="relative">
            <Search className="w-4 h-4 text-textmuted absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Tìm trên dummyjson.com..."
              className="h-10 pl-9 pr-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            />
          </div>
          <button
            type="submit"
            className="h-10 px-4 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-semibold"
          >
            Truy vấn API
          </button>
        </form>
      </div>

      <div className="bg-surface border border-bordercol rounded-card shadow-xs overflow-hidden">
        {loading ? (
          <div className="p-8 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 rounded bg-appbg animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div className="p-10 text-center space-y-4">
            <div className="text-red-600 font-semibold">{error}</div>
            <p className="text-xs text-textmuted">
              Máy chủ đã bắt ngoại lệ IOException / HttpTimeoutException và trả về thông báo an toàn (Ca kiểm thử 11).
            </p>
            <button
              type="button"
              onClick={() => fetchExternal(query)}
              className="h-10 px-4 rounded-control bg-primary text-white text-sm font-semibold inline-flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Thử lại kết nối</span>
            </button>
          </div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center text-textmuted">
            Không tìm thấy sản phẩm nào từ API ngoài khớp với từ khóa.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="bg-appbg/60 border-b border-bordercol text-xs font-semibold text-textmuted uppercase">
                  <th className="py-3 px-4">Mã gợi ý</th>
                  <th className="py-3 px-4">Tên sản phẩm (API)</th>
                  <th className="py-3 px-4">Phân loại ánh xạ</th>
                  <th className="py-3 px-4 text-right">Giá gốc (USD)</th>
                  <th className="py-3 px-4 text-right">Giá quy đổi (VND)</th>
                  <th className="py-3 px-4 text-center">Tồn tham khảo</th>
                  <th className="py-3 px-4 text-right">Nhập vào kho</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bordercol">
                {items.map((item) => (
                  <tr
                    key={item.externalId}
                    className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40"
                  >
                    <td className="py-3 px-4 font-mono text-xs font-semibold text-primary">
                      {item.suggestedProductId}
                    </td>
                    <td className="py-3 px-4 font-medium text-textmain">
                      <div>{item.title}</div>
                      <div className="text-xs text-textmuted">
                        Danh mục gốc: {item.category} · Hãng: {item.brand}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2.5 py-0.5 rounded-badge text-xs font-semibold ${
                          item.mappedCategoryCode === 'FOOD'
                            ? 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300'
                            : 'bg-cyan-100 text-cyan-800 dark:bg-cyan-950 dark:text-cyan-300'
                        }`}
                      >
                        {item.mappedCategoryName}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-xs">
                      ${item.priceUsd.toFixed(2)}
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-primary">
                      {formatCurrency(item.priceVnd)}
                    </td>
                    <td className="py-3 px-4 text-center">{item.stock}</td>
                    <td className="py-3 px-4 text-right">
                      <button
                        type="button"
                        disabled={importingId === item.suggestedProductId}
                        onClick={() => handleImportToStore(item)}
                        className="h-9 px-3 rounded-control bg-primary hover:bg-primary-hover disabled:opacity-50 text-white text-xs font-semibold inline-flex items-center gap-1.5"
                      >
                        <PlusCircle className="w-3.5 h-3.5" />
                        <span>
                          {importingId === item.suggestedProductId
                            ? 'Đang thêm...'
                            : 'Thêm vào cửa hàng'}
                        </span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
