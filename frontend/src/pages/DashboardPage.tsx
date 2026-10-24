import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  CalendarClock,
  DollarSign,
  Eye,
  FileCheck2,
  RefreshCw,
  ShoppingCart,
} from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type {
  CategoryRevenueDto,
  DailyRevenueDto,
  InvoiceDto,
  ProductDto,
  StatsSummaryDto,
} from '../types/api';
import { formatCurrency, formatDate, formatDateTime } from '../utils/format';
import { ReceiptModal } from '../components/ReceiptModal';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<StatsSummaryDto | null>(null);
  const [todayRevenue, setTodayRevenue] = useState<number>(0);
  const [daily, setDaily] = useState<DailyRevenueDto[]>([]);
  const [categories, setCategories] = useState<CategoryRevenueDto[]>([]);
  const [attentionProducts, setAttentionProducts] = useState<ProductDto[]>([]);
  const [recentInvoices, setRecentInvoices] = useState<InvoiceDto[]>([]);
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const today = new Date().toISOString().slice(0, 10);
      const sevenDaysAgo = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10);

      const [sumAll, sumToday, dailyData, catData, prodPage, invPage] = await Promise.all([
        apiClient.getStatsSummary(),
        apiClient.getStatsSummary(today, today),
        apiClient.getStatsDaily(sevenDaysAgo, today),
        apiClient.getStatsByCategory(),
        apiClient.getProducts({ size: 100 }),
        apiClient.getInvoices({ size: 6 }),
      ]);

      setSummary(sumAll);
      setTodayRevenue(sumToday.totalRevenue);
      setDaily(dailyData);
      setCategories(catData);
      setAttentionProducts(
        prodPage.content.filter((p) => p.lowStock || p.expiringSoon || p.expired).slice(0, 6)
      );
      setRecentInvoices(invPage.content.slice(0, 6));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu tổng quan');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 rounded-card bg-surface border border-bordercol animate-pulse p-5" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-72 rounded-card bg-surface border border-bordercol animate-pulse" />
          <div className="h-72 rounded-card bg-surface border border-bordercol animate-pulse" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-surface border border-bordercol rounded-card p-8 text-center space-y-4">
        <div className="text-red-600 font-semibold text-base">{error}</div>
        <button
          type="button"
          onClick={loadDashboard}
          className="h-10 px-4 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-medium inline-flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Thử lại</span>
        </button>
      </div>
    );
  }

  const maxDailyRevenue = Math.max(1, ...daily.map((d) => d.revenue));
  const foodPct = categories.find((c) => c.categoryCode === 'FOOD')?.percentage ?? 50;
  const householdPct = categories.find((c) => c.categoryCode === 'HOUSEHOLD')?.percentage ?? 50;

  return (
    <div className="space-y-6">
      {/* 4 Thẻ số liệu */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs flex items-start justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-textmuted">
              Doanh thu hôm nay
            </div>
            <div className="text-2xl font-bold text-textmain mt-1.5">
              {formatCurrency(todayRevenue)}
            </div>
            <div className="text-xs text-textmuted mt-1">
              Tổng lũy kế: <span className="font-semibold text-primary">{formatCurrency(summary?.totalRevenue)}</span>
            </div>
          </div>
          <div className="w-11 h-11 rounded-control bg-blue-500/10 text-primary flex items-center justify-center">
            <DollarSign className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs flex items-start justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-textmuted">
              Số hóa đơn hoàn tất
            </div>
            <div className="text-2xl font-bold text-textmain mt-1.5">
              {summary?.completedInvoices ?? 0} đơn
            </div>
            <div className="text-xs text-textmuted mt-1">
              TB/đơn: {formatCurrency(summary?.averageOrderValue)} · Đã hủy: {summary?.cancelledInvoices ?? 0}
            </div>
          </div>
          <div className="w-11 h-11 rounded-control bg-emerald-500/10 text-emerald-600 flex items-center justify-center">
            <FileCheck2 className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs flex items-start justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-textmuted">
              Sắp hết hàng (≤ 10)
            </div>
            <div className="text-2xl font-bold text-amber-600 mt-1.5">
              {summary?.lowStockProductsCount ?? 0} sản phẩm
            </div>
            <Link to="/products?lowStock=true" className="text-xs text-primary hover:underline mt-1 inline-block">
              Xem danh sách cần nhập thêm →
            </Link>
          </div>
          <div className="w-11 h-11 rounded-control bg-amber-500/10 text-amber-600 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs flex items-start justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-textmuted">
              Thực phẩm sắp/đã hết hạn
            </div>
            <div className="text-2xl font-bold text-red-600 mt-1.5">
              {summary?.expiringSoonProductsCount ?? 0} mặt hàng
            </div>
            <Link to="/products?expiringSoon=true" className="text-xs text-primary hover:underline mt-1 inline-block">
              Kiểm tra hạn sử dụng →
            </Link>
          </div>
          <div className="w-11 h-11 rounded-control bg-red-500/10 text-red-600 flex items-center justify-center">
            <CalendarClock className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Biểu đồ cột 7 ngày + Donut theo loại */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-surface border border-bordercol rounded-card p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold text-textmain">Biểu đồ doanh thu 7 ngày gần nhất</h2>
              <p className="text-xs text-textmuted">Chỉ tổng hợp từ các hóa đơn COMPLETED (loại trừ hóa đơn hủy)</p>
            </div>
            <Link
              to="/pos"
              className="h-9 px-3.5 rounded-control bg-primary hover:bg-primary-hover text-white text-xs font-semibold inline-flex items-center gap-1.5"
            >
              <ShoppingCart className="w-3.5 h-3.5" />
              <span>Mở màn hình POS (F2)</span>
            </Link>
          </div>

          <div className="h-52 flex items-end gap-3 pt-6 px-2 border-b border-bordercol">
            {daily.map((pt) => {
              const heightPct = Math.max(6, Math.round((pt.revenue / maxDailyRevenue) * 100));
              const shortDate = pt.date.slice(5).split('-').reverse().join('/');
              return (
                <div key={pt.date} className="flex-1 flex flex-col items-center gap-2 group relative">
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-9 bg-slate-900 text-white text-[11px] px-2 py-1 rounded whitespace-nowrap pointer-events-none z-10 shadow">
                    {formatCurrency(pt.revenue)} ({pt.invoiceCount} đơn)
                  </div>
                  <div className="w-full h-40 flex items-end justify-center">
                    <div
                      className="w-full max-w-[44px] bg-primary hover:bg-primary-hover rounded-t-md animate-bar-grow transition-colors"
                      style={{ height: `${heightPct}%` }}
                    />
                  </div>
                  <span className="text-xs text-textmuted font-medium pb-2">{shortDate}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Biểu đồ Donut cơ cấu doanh thu theo loại */}
        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-semibold text-textmain">Tỷ trọng doanh thu theo loại</h2>
            <p className="text-xs text-textmuted">Đa hình: Thực phẩm & Đồ gia dụng</p>
          </div>

          <div className="flex items-center justify-center my-4">
            <div
              className="w-36 h-36 rounded-full flex items-center justify-center relative"
              style={{
                background: `conic-gradient(#2563EB 0% ${foodPct}%, #0891B2 ${foodPct}% 100%)`,
              }}
            >
              <div className="w-24 h-24 rounded-full bg-surface flex flex-col items-center justify-center text-center">
                <span className="text-[11px] text-textmuted">Tổng cộng</span>
                <span className="text-xs font-bold text-textmain">{categories.length} nhóm</span>
              </div>
            </div>
          </div>

          <div className="space-y-2.5">
            {categories.map((cat) => (
              <div key={cat.categoryCode} className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <span
                    className={`w-3 h-3 rounded-xs ${
                      cat.categoryCode === 'FOOD' ? 'bg-blue-600' : 'bg-cyan-600'
                    }`}
                  />
                  <span className="font-medium text-textmain">{cat.categoryName}</span>
                  <span className="text-textmuted">({cat.quantitySold} SP)</span>
                </div>
                <div className="font-semibold text-textmain">
                  {formatCurrency(cat.revenue)} ({cat.percentage}%)
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Danh sách cần chú ý + Hóa đơn gần đây */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-textmain">Danh sách sản phẩm cần chú ý</h2>
            <Link to="/products" className="text-xs text-primary hover:underline inline-flex items-center gap-1">
              <span>Quản lý kho</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {attentionProducts.length === 0 ? (
            <div className="text-sm text-textmuted py-8 text-center">
              Tất cả mặt hàng đều đủ tồn kho và còn hạn sử dụng tốt.
            </div>
          ) : (
            <div className="divide-y divide-bordercol">
              {attentionProducts.map((p) => (
                <div key={p.productId} className="py-2.5 flex items-center justify-between gap-2 text-sm">
                  <div>
                    <div className="font-medium text-textmain">
                      {p.name} <span className="text-xs text-textmuted">({p.productId})</span>
                    </div>
                    <div className="text-xs text-textmuted">
                      Tồn kho: <strong className="text-textmain">{p.quantity}</strong>
                      {p.expiryDate ? ` · HSD: ${formatDate(p.expiryDate)}` : ''}
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5 shrink-0">
                    {p.expired && (
                      <span className="px-2.5 py-0.5 rounded-badge text-xs font-semibold bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300">
                        Đã hết hạn
                      </span>
                    )}
                    {!p.expired && p.expiringSoon && (
                      <span className="px-2.5 py-0.5 rounded-badge text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                        Sắp hết hạn
                      </span>
                    )}
                    {p.lowStock && (
                      <span className="px-2.5 py-0.5 rounded-badge text-xs font-semibold bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300">
                        Tồn thấp ({p.quantity})
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-textmain">Hóa đơn gần đây</h2>
            <Link to="/invoices" className="text-xs text-primary hover:underline inline-flex items-center gap-1">
              <span>Xem tất cả</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-bordercol text-xs text-textmuted uppercase">
                  <th className="py-2 font-semibold">Mã HĐ</th>
                  <th className="py-2 font-semibold">Thời gian</th>
                  <th className="py-2 font-semibold">Trạng thái</th>
                  <th className="py-2 font-semibold text-right">Tổng tiền</th>
                  <th className="py-2 font-semibold text-right">Xem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bordercol">
                {recentInvoices.map((inv) => (
                  <tr key={inv.invoiceId} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40">
                    <td className="py-2.5 font-mono text-xs font-semibold text-primary">{inv.invoiceId}</td>
                    <td className="py-2.5 text-xs text-textmuted">{formatDateTime(inv.invoiceDate)}</td>
                    <td className="py-2.5">
                      <span
                        className={`px-2 py-0.5 rounded-badge text-[11px] font-semibold ${
                          inv.status === 'COMPLETED'
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                            : 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300'
                        }`}
                      >
                        {inv.status === 'COMPLETED' ? 'Hoàn tất' : 'Đã hủy'}
                      </span>
                    </td>
                    <td className="py-2.5 text-right font-semibold">{formatCurrency(inv.totalAmount)}</td>
                    <td className="py-2.5 text-right">
                      <button
                        type="button"
                        onClick={() => setSelectedInvoice(inv)}
                        aria-label={`Xem hóa đơn ${inv.invoiceId}`}
                        className="p-1.5 rounded-control text-textmuted hover:text-primary"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <ReceiptModal invoice={selectedInvoice} onClose={() => setSelectedInvoice(null)} />
    </div>
  );
};
