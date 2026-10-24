import React, { useCallback, useEffect, useState } from 'react';
import { BarChart3, Calendar, RefreshCw, TrendingUp } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import type {
  CategoryRevenueDto,
  DailyRevenueDto,
  StatsSummaryDto,
  TopProductDto,
} from '../types/api';
import { formatCurrency } from '../utils/format';

export const ReportsPage: React.FC = () => {
  const todayStr = new Date().toISOString().slice(0, 10);
  const fourteenDaysAgoStr = new Date(Date.now() - 13 * 86400000).toISOString().slice(0, 10);

  const [fromDate, setFromDate] = useState<string>(fourteenDaysAgoStr);
  const [toDate, setToDate] = useState<string>(todayStr);
  const [summary, setSummary] = useState<StatsSummaryDto | null>(null);
  const [daily, setDaily] = useState<DailyRevenueDto[]>([]);
  const [categories, setCategories] = useState<CategoryRevenueDto[]>([]);
  const [topProducts, setTopProducts] = useState<TopProductDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadReports = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [sum, dayList, catList, topList] = await Promise.all([
        apiClient.getStatsSummary(fromDate, toDate),
        apiClient.getStatsDaily(fromDate, toDate),
        apiClient.getStatsByCategory(fromDate, toDate),
        apiClient.getTopProducts(fromDate, toDate, 5),
      ]);
      setSummary(sum);
      setDaily(dayList);
      setCategories(catList);
      setTopProducts(topList);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Lỗi tải dữ liệu báo cáo');
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const applyPreset = (days: number | 'MONTH') => {
    const now = new Date();
    const end = now.toISOString().slice(0, 10);
    if (days === 'MONTH') {
      const firstOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
        .toISOString()
        .slice(0, 10);
      setFromDate(firstOfMonth);
      setToDate(end);
    } else {
      const start = new Date(Date.now() - (days - 1) * 86400000).toISOString().slice(0, 10);
      setFromDate(start);
      setToDate(end);
    }
  };

  const maxDailyRev = Math.max(1, ...daily.map((d) => d.revenue));

  return (
    <div className="space-y-6">
      {/* Bộ chọn khoảng thời gian + Nút nhanh */}
      <div className="bg-surface border border-bordercol rounded-card p-4 shadow-xs flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor="rep-from" className="block text-xs font-semibold text-textmuted mb-1">
              Từ ngày
            </label>
            <input
              id="rep-from"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            />
          </div>
          <div>
            <label htmlFor="rep-to" className="block text-xs font-semibold text-textmuted mb-1">
              Đến ngày
            </label>
            <input
              id="rep-to"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg text-sm text-textmain"
            />
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              onClick={() => applyPreset(1)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg hover:border-primary text-xs font-semibold"
            >
              Hôm nay
            </button>
            <button
              type="button"
              onClick={() => applyPreset(7)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg hover:border-primary text-xs font-semibold"
            >
              7 ngày
            </button>
            <button
              type="button"
              onClick={() => applyPreset(30)}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg hover:border-primary text-xs font-semibold"
            >
              30 ngày
            </button>
            <button
              type="button"
              onClick={() => applyPreset('MONTH')}
              className="h-10 px-3 rounded-control border border-bordercol bg-appbg hover:border-primary text-xs font-semibold"
            >
              Tháng này
            </button>
          </div>
        </div>

        <button
          type="button"
          onClick={loadReports}
          className="h-10 px-4 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-semibold inline-flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Cập nhật báo cáo</span>
        </button>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-24 rounded-card bg-surface border border-bordercol animate-pulse" />
          ))}
        </div>
      ) : error ? (
        <div className="bg-surface border border-bordercol rounded-card p-8 text-center text-red-600 font-medium">
          {error}
        </div>
      ) : (
        <>
          {/* Thẻ tổng hợp */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
              <div className="text-xs font-semibold uppercase text-textmuted">
                Tổng doanh thu (COMPLETED)
              </div>
              <div className="text-2xl font-bold text-primary mt-1.5">
                {formatCurrency(summary?.totalRevenue)}
              </div>
              <div className="text-xs text-textmuted mt-1">Đã loại trừ hóa đơn hủy (BR7)</div>
            </div>

            <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
              <div className="text-xs font-semibold uppercase text-textmuted">
                Số hóa đơn hoàn tất
              </div>
              <div className="text-2xl font-bold text-textmain mt-1.5">
                {summary?.completedInvoices ?? 0} đơn
              </div>
              <div className="text-xs text-textmuted mt-1">
                Hóa đơn đã hủy: {summary?.cancelledInvoices ?? 0}
              </div>
            </div>

            <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
              <div className="text-xs font-semibold uppercase text-textmuted">
                Giá trị trung bình / hóa đơn
              </div>
              <div className="text-2xl font-bold text-textmain mt-1.5">
                {formatCurrency(summary?.averageOrderValue)}
              </div>
              <div className="text-xs text-textmuted mt-1">Tính trên đơn hoàn tất</div>
            </div>

            <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
              <div className="text-xs font-semibold uppercase text-textmuted">
                Tổng tiền giảm giá đã áp dụng
              </div>
              <div className="text-2xl font-bold text-emerald-600 mt-1.5">
                {formatCurrency(summary?.totalDiscount)}
              </div>
              <div className="text-xs text-textmuted mt-1">Chính sách DiscountPolicy</div>
            </div>
          </div>

          {summary?.completedInvoices === 0 ? (
            <div className="bg-surface border border-bordercol rounded-card p-12 text-center space-y-2">
              <Calendar className="w-10 h-10 text-textmuted mx-auto opacity-50" />
              <div className="text-base font-semibold text-textmain">
                Không có phát sinh doanh thu trong khoảng thời gian này
              </div>
              <p className="text-xs text-textmuted">
                Hãy chọn khoảng thời gian rộng hơn (ví dụ: 30 ngày) hoặc lập thêm hóa đơn mới tại màn hình POS.
              </p>
            </div>
          ) : (
            <>
              {/* Biểu đồ doanh thu theo ngày */}
              <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
                <div className="flex items-center gap-2 mb-4">
                  <BarChart3 className="w-5 h-5 text-primary" />
                  <h2 className="text-sm font-semibold text-textmain">
                    Biểu đồ doanh thu theo ngày ({fromDate} → {toDate})
                  </h2>
                </div>

                <div className="h-56 flex items-end gap-2 pt-8 px-2 border-b border-bordercol overflow-x-auto">
                  {daily.map((d) => {
                    const heightPct = Math.max(4, Math.round((d.revenue / maxDailyRev) * 100));
                    const label = d.date.slice(5).split('-').reverse().join('/');
                    return (
                      <div
                        key={d.date}
                        className="flex-1 min-w-[36px] flex flex-col items-center gap-2 group relative"
                      >
                        <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-8 bg-slate-900 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap pointer-events-none z-10">
                          {d.date}: {formatCurrency(d.revenue)} ({d.invoiceCount} đơn)
                        </div>
                        <div className="w-full h-40 flex items-end justify-center">
                          <div
                            className="w-full max-w-[36px] bg-primary hover:bg-primary-hover rounded-t animate-bar-grow"
                            style={{ height: `${heightPct}%` }}
                          />
                        </div>
                        <span className="text-[11px] text-textmuted pb-2">{label}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Top 5 sản phẩm bán chạy + Theo loại sản phẩm */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
                  <div className="flex items-center gap-2 mb-4">
                    <TrendingUp className="w-5 h-5 text-emerald-600" />
                    <h2 className="text-sm font-semibold text-textmain">
                      Top 5 sản phẩm bán chạy nhất
                    </h2>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead>
                        <tr className="border-b border-bordercol text-xs text-textmuted uppercase">
                          <th className="py-2">Mã</th>
                          <th className="py-2">Tên sản phẩm</th>
                          <th className="py-2 text-center">SL bán</th>
                          <th className="py-2 text-right">Doanh thu</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-bordercol">
                        {topProducts.map((tp) => (
                          <tr key={tp.productId}>
                            <td className="py-2.5 font-mono text-xs font-semibold text-primary">
                              {tp.productId}
                            </td>
                            <td className="py-2.5 font-medium text-textmain">{tp.productName}</td>
                            <td className="py-2.5 text-center font-semibold">{tp.quantitySold}</td>
                            <td className="py-2.5 text-right font-semibold">
                              {formatCurrency(tp.revenue)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className="bg-surface border border-bordercol rounded-card p-5 shadow-xs">
                  <h2 className="text-sm font-semibold text-textmain mb-4">
                    Doanh thu theo phân loại sản phẩm
                  </h2>
                  <div className="space-y-4">
                    {categories.map((cat) => (
                      <div key={cat.categoryCode} className="space-y-1.5">
                        <div className="flex justify-between text-sm">
                          <span className="font-semibold text-textmain">
                            {cat.categoryName} ({cat.quantitySold} sản phẩm đã bán)
                          </span>
                          <span className="font-bold text-primary">
                            {formatCurrency(cat.revenue)} ({cat.percentage}%)
                          </span>
                        </div>
                        <div className="w-full h-3 bg-appbg rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              cat.categoryCode === 'FOOD' ? 'bg-blue-600' : 'bg-cyan-600'
                            }`}
                            style={{ width: `${Math.max(2, cat.percentage)}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
};
