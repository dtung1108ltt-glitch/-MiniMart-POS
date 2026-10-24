import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  BarChart3,
  ChevronLeft,
  ChevronRight,
  Clock,
  FileText,
  Globe,
  LayoutDashboard,
  Moon,
  Package,
  RefreshCw,
  Search,
  ShoppingCart,
  Store,
  Sun,
  Truck,
  Users,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import { subscribeApiStatus, warmUpServer } from '../api/apiClient';

const NAV_ITEMS = [
  { to: '/', label: 'Tổng quan', icon: LayoutDashboard, shortcut: '' },
  { to: '/pos', label: 'Bán hàng (POS)', icon: ShoppingCart, shortcut: 'F2' },
  { to: '/products', label: 'Sản phẩm', icon: Package, shortcut: '' },
  { to: '/invoices', label: 'Hóa đơn', icon: FileText, shortcut: '' },
  { to: '/reports', label: 'Báo cáo', icon: BarChart3, shortcut: '' },
  { to: '/external', label: 'Sản phẩm từ API', icon: Globe, shortcut: '' },
];

const DISABLED_ITEMS = [
  { label: 'Khách hàng', icon: Users, badge: 'Sắp có' },
  { label: 'Nhà cung cấp', icon: Truck, badge: 'Sắp có' },
];

const PAGE_TITLES: Record<string, string> = {
  '/': 'Tổng quan hệ thống',
  '/pos': 'Bán hàng tại quầy (POS)',
  '/products': 'Quản lý danh mục sản phẩm & tồn kho',
  '/invoices': 'Quản lý & tra cứu hóa đơn',
  '/reports': 'Báo cáo & thống kê doanh thu',
  '/external': 'Sản phẩm tham khảo từ API ngoài (dummyjson.com)',
};

export const Layout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [dark, setDark] = useState<boolean>(() => {
    const saved = localStorage.getItem('minimart-theme');
    if (saved) return saved === 'dark';
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  });
  const [clock, setClock] = useState<string>('');
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [connected, setConnected] = useState<boolean>(true);
  const [warmingUp, setWarmingUp] = useState<boolean>(false);
  const [warmUpElapsed, setWarmUpElapsed] = useState<number>(0);
  const [warmUpFailed, setWarmUpFailed] = useState<boolean>(false);
  const [quickSearchOpen, setQuickSearchOpen] = useState<boolean>(false);
  const [quickQuery, setQuickQuery] = useState<string>('');

  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const root = document.documentElement;
    if (dark) {
      root.classList.add('dark');
      localStorage.setItem('minimart-theme', 'dark');
    } else {
      root.classList.remove('dark');
      localStorage.setItem('minimart-theme', 'light');
    }
  }, [dark]);

  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      setClock(
        now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) +
          ' · ' +
          now.toLocaleDateString('vi-VN')
      );
    };
    updateClock();
    const id = setInterval(updateClock, 1000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    return subscribeApiStatus((lat, ok) => {
      setLatencyMs(lat);
      setConnected(ok);
    });
  }, []);

  const runWarmUp = async () => {
    setWarmUpFailed(false);
    let showBannerTimer: ReturnType<typeof setTimeout> | null = setTimeout(() => {
      setWarmingUp(true);
    }, 600);

    const ok = await warmUpServer((_attempt, elapsed) => {
      setWarmUpElapsed(elapsed);
    });

    if (showBannerTimer) clearTimeout(showBannerTimer);
    setWarmingUp(false);
    if (!ok) {
      setWarmUpFailed(true);
    }
  };

  useEffect(() => {
    runWarmUp();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault();
        navigate('/pos');
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setQuickSearchOpen((v) => !v);
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'd') {
        e.preventDefault();
        setDark((d) => !d);
      } else if (e.key === 'Escape' && quickSearchOpen) {
        setQuickSearchOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [navigate, quickSearchOpen]);

  const currentTitle = PAGE_TITLES[location.pathname] || 'MiniMart POS';

  return (
    <div className="min-h-screen flex bg-appbg text-textmain">
      {/* Sidebar */}
      <aside
        className={`bg-sidebar text-sidebartext flex flex-col shrink-0 transition-all duration-150 border-r border-slate-800 ${
          collapsed ? 'w-[72px]' : 'w-[240px]'
        }`}
      >
        <div className="h-16 px-4 flex items-center justify-between border-b border-slate-800">
          <div className="flex items-center gap-3 overflow-hidden">
            <div className="w-9 h-9 rounded-control bg-blue-600 flex items-center justify-center text-white shrink-0 font-bold shadow-xs">
              <Store className="w-5 h-5" />
            </div>
            {!collapsed && (
              <div className="leading-tight truncate">
                <div className="font-bold text-white text-base tracking-tight">MiniMart POS</div>
                <div className="text-[11px] text-slate-400">Quản lý bán hàng v2.0</div>
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={() => setCollapsed((c) => !c)}
            aria-label={collapsed ? 'Mở rộng thanh bên' : 'Thu gọn thanh bên'}
            className="p-1.5 rounded-control text-slate-400 hover:text-white hover:bg-slate-800"
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>

        <nav className="flex-1 py-4 px-2.5 space-y-1 overflow-y-auto" aria-label="Điều hướng chính">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                title={collapsed ? item.label : undefined}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3 h-10 rounded-control text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white shadow-xs'
                      : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                  }`
                }
              >
                <div className="flex items-center gap-3 truncate">
                  <Icon className="w-5 h-5 shrink-0" />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                </div>
                {!collapsed && item.shortcut && (
                  <span className="text-[11px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                    {item.shortcut}
                  </span>
                )}
              </NavLink>
            );
          })}

          <div className="pt-4 mt-4 border-t border-slate-800/80">
            {!collapsed && (
              <div className="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                Mở rộng (Ngoài phạm vi)
              </div>
            )}
            {DISABLED_ITEMS.map((item) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.label}
                  title="Chức năng ngoài phạm vi tiểu luận OOP (chưa cài đặt)"
                  aria-disabled="true"
                  className="flex items-center justify-between px-3 h-10 rounded-control text-sm text-slate-500 cursor-not-allowed select-none opacity-75"
                >
                  <div className="flex items-center gap-3 truncate">
                    <Icon className="w-5 h-5 shrink-0" />
                    {!collapsed && <span className="truncate">{item.label}</span>}
                  </div>
                  {!collapsed && (
                    <span className="text-[10px] px-2 py-0.5 rounded-badge bg-slate-800 text-slate-400">
                      {item.badge}
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </nav>

        {!collapsed && (
          <div className="p-3 m-2.5 rounded-control bg-slate-900/90 border border-slate-800 text-xs text-slate-400 space-y-1">
            <div className="font-semibold text-slate-200">Phím tắt nhanh</div>
            <div>F2: Bán hàng · F3: Tìm mã</div>
            <div>F9: Thanh toán · Ctrl+K: Tìm</div>
          </div>
        )}
      </aside>

      {/* Main Content Wrapper */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Topbar */}
        <header className="h-16 bg-surface border-b border-bordercol px-6 flex items-center justify-between gap-4 sticky top-0 z-30">
          <h1 className="text-[22px] font-bold text-textmain truncate">{currentTitle}</h1>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setQuickSearchOpen(true)}
              className="h-10 px-3.5 rounded-control border border-bordercol bg-appbg hover:border-primary text-textmuted text-sm flex items-center gap-2.5 transition-colors"
            >
              <Search className="w-4 h-4" />
              <span className="hidden md:inline">Tìm nhanh sản phẩm / màn hình...</span>
              <kbd className="text-xs px-1.5 py-0.5 rounded bg-surface border border-bordercol text-textmuted font-mono">
                Ctrl+K
              </kbd>
            </button>

            <div className="hidden lg:flex items-center gap-1.5 text-xs font-medium text-textmuted bg-appbg px-3 h-10 rounded-control border border-bordercol">
              <Clock className="w-4 h-4 text-primary" />
              <span>{clock}</span>
            </div>

            <button
              type="button"
              onClick={() => setDark((d) => !d)}
              title="Đổi giao diện Sáng / Tối (Ctrl+D)"
              aria-label="Đổi giao diện Sáng / Tối"
              className="h-10 w-10 rounded-control border border-bordercol bg-surface hover:bg-appbg flex items-center justify-center text-textmain transition-colors"
            >
              {dark ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5 text-slate-700" />}
            </button>
          </div>
        </header>

        {/* Banner đánh thức máy chủ ngủ (Mục 3.2) */}
        {warmingUp && (
          <div
            role="status"
            className="bg-amber-500/15 border-b border-amber-500/30 px-6 py-2.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2"
          >
            <div className="flex items-center gap-2.5 text-sm font-medium text-amber-800 dark:text-amber-300">
              <RefreshCw className="w-4 h-4 animate-spin shrink-0" />
              <span>
                Đang đánh thức máy chủ, vui lòng chờ… ({warmUpElapsed}s / 60s — máy chủ miễn phí trên Render có thể cần vài chục giây để khởi động JVM)
              </span>
            </div>
            <div className="w-full sm:w-48 h-2 bg-amber-200 dark:bg-amber-950 rounded-full overflow-hidden">
              <div
                className="h-full bg-amber-500 transition-all duration-300"
                style={{ width: `${Math.min(100, Math.round((warmUpElapsed / 60) * 100))}%` }}
              />
            </div>
          </div>
        )}

        {warmUpFailed && !connected && (
          <div
            role="alert"
            className="bg-red-500/15 border-b border-red-500/30 px-6 py-2.5 flex items-center justify-between gap-4"
          >
            <span className="text-sm font-medium text-red-700 dark:text-red-300">
              Không thể kết nối tới máy chủ Backend sau 60 giây. Vui lòng kiểm tra máy chủ hoặc nhấn Thử lại.
            </span>
            <button
              type="button"
              onClick={runWarmUp}
              className="px-3 py-1 rounded-control bg-red-600 text-white text-xs font-semibold hover:bg-red-700"
            >
              Thử lại ngay
            </button>
          </div>
        )}

        {/* Main View */}
        <main className="flex-1 p-6 overflow-x-hidden">
          <div key={location.pathname} className="animate-page max-w-[1600px] mx-auto">
            <Outlet />
          </div>
        </main>

        {/* Bottom Status Bar */}
        <footer className="h-8 bg-surface border-t border-bordercol px-6 flex items-center justify-between text-xs text-textmuted">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5 font-medium">
              {connected ? (
                <>
                  <Wifi className="w-3.5 h-3.5 text-emerald-600" />
                  <span className="text-emerald-700 dark:text-emerald-400">Backend: Đã kết nối</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-3.5 h-3.5 text-red-600" />
                  <span className="text-red-600 dark:text-red-400">Backend: Mất kết nối</span>
                </>
              )}
            </span>
            {latencyMs !== null && <span>Phản hồi: {latencyMs} ms</span>}
          </div>
          <div className="hidden sm:block">
            MiniMart POS — Tiểu luận Lập trình Hướng đối tượng (Nhóm 8)
          </div>
        </footer>
      </div>

      {/* Quick Search Modal (Ctrl+K) */}
      {quickSearchOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Tìm kiếm nhanh"
          className="fixed inset-0 z-50 flex items-start justify-center pt-20 bg-black/50 backdrop-blur-xs p-4"
        >
          <div className="bg-surface border border-bordercol rounded-card shadow-xl max-w-lg w-full overflow-hidden">
            <div className="flex items-center gap-3 px-4 py-3 border-b border-bordercol">
              <Search className="w-5 h-5 text-primary" />
              <input
                type="text"
                autoFocus
                value={quickQuery}
                onChange={(e) => setQuickQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && quickQuery.trim()) {
                    setQuickSearchOpen(false);
                    navigate(`/products?q=${encodeURIComponent(quickQuery.trim())}`);
                    setQuickQuery('');
                  }
                }}
                placeholder="Nhập tên/mã sản phẩm rồi nhấn Enter, hoặc chọn màn hình bên dưới..."
                className="flex-1 bg-transparent text-sm text-textmain focus:outline-none"
              />
              <button
                type="button"
                onClick={() => setQuickSearchOpen(false)}
                className="p-1 text-textmuted hover:text-textmain"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="p-3 space-y-1">
              <div className="px-2 py-1 text-xs font-semibold text-textmuted uppercase">
                Đi tới màn hình
              </div>
              {NAV_ITEMS.map((item) => {
                const Icon = item.icon;
                return (
                  <button
                    key={item.to}
                    type="button"
                    onClick={() => {
                      setQuickSearchOpen(false);
                      navigate(item.to);
                    }}
                    className="w-full flex items-center justify-between px-3 py-2 rounded-control text-sm text-textmain hover:bg-primary-soft/50 transition-colors"
                  >
                    <span className="flex items-center gap-2.5">
                      <Icon className="w-4 h-4 text-primary" />
                      <span>{item.label}</span>
                    </span>
                    {item.shortcut && (
                      <kbd className="text-xs px-1.5 py-0.5 rounded border border-bordercol text-textmuted">
                        {item.shortcut}
                      </kbd>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
