import React, { useCallback, useEffect, useState } from 'react';
import { Ban, Eye, RefreshCw } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type { InvoiceDto } from '../types/api';
import { formatCurrency, formatDateTime } from '../utils/format';
import { useToast } from '../context/ToastContext';
import { ReceiptModal } from '../components/ReceiptModal';

export const InvoicesPage: React.FC = () => {
  const { showToast } = useToast();

  const [invoices, setInvoices] = useState<InvoiceDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceDto | null>(null);
  const [cancellingInvoice, setCancellingInvoice] = useState<InvoiceDto | null>(null);

  const loadInvoices = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await apiClient.getInvoices({
        from: fromDate || undefined,
        to: toDate || undefined,
        status: statusFilter,
        size: 100,
      });
      setInvoices(page.content);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách hóa đơn');
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate, statusFilter]);

  useEffect(() => {
    loadInvoices();
  }, [loadInvoices]);

  const handleConfirmCancel = async () => {
    if (!cancellingInvoice) return;
    try {
      await apiClient.cancelInvoice(cancellingInvoice.invoiceId);
      showToast(
        `Đã hủy hóa đơn ${cancellingInvoice.invoiceId} và hoàn trả tồn kho thành công`,
        'success'
      );
      setCancellingInvoice(null);
      await loadInvoices();
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : 'Hủy hóa đơn thất bại', 'error');
    }
  };

  return (
    <div className="space-y-5">
      {/* Bộ lọc ngày & trạng thái */}
      <div className="bg-surface border border-bordercol rounded-card p-4 shadow-xs flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor="inv-from" className="block text-xs font-semibold text-textmuted mb-1">
              Từ ngày
            </label>
            <input
              id="inv-from"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            />
          </div>
          <div>
            <label htmlFor="inv-to" className="block text-xs font-semibold text-textmuted mb-1">
              Đến ngày
            </label>
            <input
              id="inv-to"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            />
          </div>
          <div>
            <label htmlFor="inv-status" className="block text-xs font-semibold text-textmuted mb-1">
              Trạng thái
            </label>
            <select
              id="inv-status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="COMPLETED">Hoàn tất (COMPLETED)</option>
              <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            </select>
          </div>
          {(fromDate || toDate || statusFilter !== 'ALL') && (
            <button
              type="button"
              onClick={() => {
                setFromDate('');
                setToDate('');
                setStatusFilter('ALL');
              }}
              className="h-10 px-3 rounded-control border border-bordercol text-xs font-medium text-textmuted hover:text-textmain"
            >
              Xóa bộ lọc
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={loadInvoices}
          className="h-10 px-4 rounded-control border border-bordercol bg-surface hover:bg-appbg text-sm font-medium inline-flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Bảng danh sách hóa đơn */}
      <div className="bg-surface border border-bordercol rounded-card shadow-xs overflow-hidden">
        {loading ? (
          <div className="p-8 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-10 rounded bg-appbg animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div className="p-8 text-center space-y-3">
            <div className="text-red-600 font-medium">{error}</div>
            <button
              type="button"
              onClick={loadInvoices}
              className="h-9 px-4 rounded-control bg-primary text-white text-xs font-semibold"
            >
              Thử lại
            </button>
          </div>
        ) : invoices.length === 0 ? (
          <div className="p-12 text-center text-textmuted">
            Không có hóa đơn nào trong khoảng thời gian / trạng thái đã chọn.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="bg-appbg/60 border-b border-bordercol text-xs font-semibold text-textmuted uppercase">
                  <th className="py-3 px-4">Mã hóa đơn</th>
                  <th className="py-3 px-4">Ngày lập</th>
                  <th className="py-3 px-4 text-center">Số mặt hàng</th>
                  <th className="py-3 px-4 text-right">Tạm tính</th>
                  <th className="py-3 px-4 text-right">Giảm giá</th>
                  <th className="py-3 px-4 text-right">Tổng thanh toán</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bordercol">
                {invoices.map((inv) => (
                  <tr key={inv.invoiceId} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40">
                    <td className="py-3 px-4 font-mono font-semibold text-xs text-primary">
                      {inv.invoiceId}
                    </td>
                    <td className="py-3 px-4 text-xs text-textmuted">
                      {formatDateTime(inv.invoiceDate)}
                    </td>
                    <td className="py-3 px-4 text-center">{inv.items.length} dòng</td>
                    <td className="py-3 px-4 text-right">{formatCurrency(inv.subtotal)}</td>
                    <td className="py-3 px-4 text-right text-emerald-600">
                      -{formatCurrency(inv.discount)}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-textmain">
                      {formatCurrency(inv.totalAmount)}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`px-2.5 py-0.5 rounded-badge text-xs font-semibold ${
                          inv.status === 'COMPLETED'
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                            : 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300'
                        }`}
                      >
                        {inv.status === 'COMPLETED' ? 'Hoàn tất' : 'Đã hủy'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="inline-flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() => setSelectedInvoice(inv)}
                          className="h-8 px-2.5 rounded-control border border-bordercol text-xs font-medium text-textmain hover:bg-appbg inline-flex items-center gap-1"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Chi tiết</span>
                        </button>
                        {inv.status === 'COMPLETED' && (
                          <button
                            type="button"
                            onClick={() => setCancellingInvoice(inv)}
                            className="h-8 px-2.5 rounded-control border border-red-300 dark:border-red-900 text-xs font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/50 inline-flex items-center gap-1"
                          >
                            <Ban className="w-3.5 h-3.5" />
                            <span>Hủy HĐ</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ReceiptModal invoice={selectedInvoice} onClose={() => setSelectedInvoice(null)} />

      {cancellingInvoice && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4"
        >
          <div className="bg-surface border border-bordercol rounded-card shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-textmain">Xác nhận hủy hóa đơn & hoàn kho</h3>
            <p className="text-sm text-textmuted">
              Bạn có chắc muốn hủy hóa đơn{' '}
              <strong className="text-textmain">{cancellingInvoice.invoiceId}</strong> trị giá{' '}
              <strong className="text-textmain">{formatCurrency(cancellingInvoice.totalAmount)}</strong>?
              Số lượng hàng trong hóa đơn sẽ được hoàn trả vào tồn kho và loại khỏi thống kê doanh thu (BR7, BR8).
            </p>
            <div className="flex justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setCancellingInvoice(null)}
                className="h-10 px-4 rounded-control border border-bordercol text-sm font-medium text-textmain"
              >
                Đóng
              </button>
              <button
                type="button"
                onClick={handleConfirmCancel}
                className="h-10 px-4 rounded-control bg-red-600 hover:bg-red-700 text-white text-sm font-semibold"
              >
                Xác nhận hủy & hoàn kho
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
