import React from 'react';
import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[65vh] flex flex-col items-center justify-center text-center p-6 space-y-4">
      <div className="text-6xl font-bold text-primary">404</div>
      <h2 className="text-xl font-bold text-textmain">Không tìm thấy trang yêu cầu</h2>
      <p className="text-sm text-textmuted max-w-md">
        Đường dẫn bạn truy cập không tồn tại trong hệ thống MiniMart POS.
      </p>
      <Link
        to="/"
        className="h-10 px-5 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-semibold inline-flex items-center gap-2"
      >
        <Home className="w-4 h-4" />
        <span>Quay về Tổng quan</span>
      </Link>
    </div>
  );
};
