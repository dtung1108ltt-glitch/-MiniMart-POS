import React from 'react';
import { Download, Printer, X } from 'lucide-react';
import type { InvoiceDto } from '../types/api';
import { formatCurrency, formatDateTime } from '../utils/format';

interface ReceiptModalProps {
  invoice: InvoiceDto | null;
  onClose: () => void;
}

export const ReceiptModal: React.FC<ReceiptModalProps> = ({ invoice, onClose }) => {
  if (!invoice) return null;

  const generateTxtContent = (inv: InvoiceDto): string => {
    const lines: string[] = [];
    const hr = '------------------------------------------';
    lines.push('              MINIMART POS                ');
    lines.push('   HỆ THỐNG QUẢN LÝ BÁN HÀNG & SIÊU THỊ   ');
    lines.push('      ĐC: Khu Đại học, TP. Hồ Chí Minh    ');
    lines.push(hr);
    lines.push('             HÓA ĐƠN BÁN LẺ               ');
    lines.push(`Số HĐ    : ${inv.invoiceId}`);
    lines.push(`Ngày lập : ${formatDateTime(inv.invoiceDate)}`);
    lines.push(`Trạng thái: ${inv.status === 'COMPLETED' ? 'HOÀN TẤT' : 'ĐÃ HỦY'}`);
    lines.push(hr);
    lines.push('Mặt hàng                 SL    Thành tiền');
    lines.push(hr);
    for (const item of inv.items) {
      lines.push(`${item.productName} (${item.productId})`);
      const left = `  ${formatCurrency(item.unitPrice)} x ${item.quantity}`;
      const right = formatCurrency(item.subtotal);
      const pad = Math.max(1, 42 - left.length - right.length);
      lines.push(left + ' '.repeat(pad) + right);
    }
    lines.push(hr);
    lines.push(`Tạm tính         : ${formatCurrency(inv.subtotal)}`);
    lines.push(`Giảm giá         : -${formatCurrency(inv.discount)}`);
    lines.push(`TỔNG THANH TOÁN  : ${formatCurrency(inv.totalAmount)}`);
    lines.push(`Tiền khách đưa   : ${formatCurrency(inv.amountPaid)}`);
    lines.push(`Tiền thừa trả lại: ${formatCurrency(inv.changeAmount)}`);
    lines.push(hr);
    lines.push('     Cảm ơn Quý khách và hẹn gặp lại!     ');
    return lines.join('\r\n');
  };

  const handleDownloadTxt = () => {
    const content = generateTxtContent(invoice);
    const blob = new Blob(['\uFEFF' + content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `HoaDon_${invoice.invoiceId}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="receipt-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4"
    >
      <div className="bg-surface border border-bordercol rounded-card shadow-xl max-w-md w-full overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-bordercol">
          <h3 id="receipt-modal-title" className="text-base font-semibold text-textmain">
            Xem trước hóa đơn khổ 80mm
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng cửa sổ hóa đơn"
            className="p-1.5 rounded-control text-textmuted hover:text-textmain hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 overflow-y-auto bg-slate-100 dark:bg-slate-900/60 flex justify-center">
          <div
            id="printable-receipt"
            className="bg-white text-slate-900 w-[310px] p-5 rounded shadow-xs font-mono text-xs leading-relaxed border border-slate-200"
          >
            <div className="text-center border-b border-dashed border-slate-400 pb-3 mb-3">
              <div className="font-bold text-base tracking-wide">MINIMART POS</div>
              <div className="text-[11px] text-slate-600">Siêu thị Mini & Cửa hàng Tiện lợi</div>
              <div className="mt-2 font-bold text-sm">HÓA ĐƠN BÁN LẺ</div>
              <div className="text-[11px] mt-0.5">Mã HĐ: {invoice.invoiceId}</div>
              <div className="text-[11px]">Ngày: {formatDateTime(invoice.invoiceDate)}</div>
              <div className="mt-1">
                <span
                  className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                    invoice.status === 'COMPLETED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {invoice.status === 'COMPLETED' ? 'ĐÃ THANH TOÁN' : 'ĐÃ HỦY'}
                </span>
              </div>
            </div>

            <div className="space-y-2 border-b border-dashed border-slate-400 pb-3 mb-3">
              {invoice.items.map((item) => (
                <div key={item.productId} className="text-xs">
                  <div className="font-semibold">
                    {item.productName} <span className="text-slate-500">({item.productId})</span>
                  </div>
                  <div className="flex justify-between text-slate-700 mt-0.5">
                    <span>
                      {formatCurrency(item.unitPrice)} × {item.quantity}
                    </span>
                    <span className="font-semibold">{formatCurrency(item.subtotal)}</span>
                  </div>
                </div>
              ))}
            </div>

            <div className="space-y-1 text-xs border-b border-dashed border-slate-400 pb-3 mb-3">
              <div className="flex justify-between">
                <span>Tạm tính:</span>
                <span>{formatCurrency(invoice.subtotal)}</span>
              </div>
              <div className="flex justify-between text-emerald-700">
                <span>Giảm giá:</span>
                <span>-{formatCurrency(invoice.discount)}</span>
              </div>
              <div className="flex justify-between font-bold text-sm pt-1 border-t border-slate-200">
                <span>TỔNG CỘNG:</span>
                <span>{formatCurrency(invoice.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-600 pt-1">
                <span>Tiền khách đưa:</span>
                <span>{formatCurrency(invoice.amountPaid)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Tiền thừa:</span>
                <span>{formatCurrency(invoice.changeAmount)}</span>
              </div>
            </div>

            <div className="text-center text-[11px] text-slate-600">
              Cảm ơn Quý khách & Hẹn gặp lại!
            </div>
          </div>
        </div>

        <div className="px-5 py-3.5 border-t border-bordercol flex items-center justify-end gap-2.5 bg-surface">
          <button
            type="button"
            onClick={handleDownloadTxt}
            className="h-10 px-4 rounded-control border border-bordercol text-sm font-medium text-textmain hover:bg-slate-100 dark:hover:bg-slate-800 flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            <span>Tải file .txt</span>
          </button>
          <button
            type="button"
            onClick={() => window.print()}
            className="h-10 px-4 rounded-control bg-primary hover:bg-primary-hover text-white text-sm font-medium flex items-center gap-2"
          >
            <Printer className="w-4 h-4" />
            <span>In hóa đơn</span>
          </button>
        </div>
      </div>
    </div>
  );
};
