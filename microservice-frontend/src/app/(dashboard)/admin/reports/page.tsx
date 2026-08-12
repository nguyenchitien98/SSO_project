'use client';

import React, { useEffect, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line, Bar } from 'react-chartjs-2';
import styles from '../../products/Products.module.css';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface ReportData {
  totalRevenue: number;
  totalOrders: number;
  monthlyRevenue: number[];
  categoryRevenue: number[];
}

export default function ReportsPage() {
  const [report, setReport] = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    async function loadReports() {
      try {
        const res = await fetch('/api/orders/reports');
        const data = await res.json();
        
        if (data.success && data.data) {
          setReport(data.data);
        } else {
          setErrorMsg(data.message || 'Không thể tải báo cáo từ Gateway');
        }
      } catch (e: any) {
        console.error('Lỗi khi fetch báo cáo:', e);
        setErrorMsg('Lỗi kết nối hệ thống. Vui lòng thử lại sau.');
      } finally {
        setLoading(false);
      }
    }

    loadReports();
  }, []);

  const monthlyLabels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
  const categoryLabels = ['Điện thoại', 'Máy tính', 'Phụ kiện', 'Màn hình'];

  // Fallback monthly data
  const defaultMonthlyData = [120, 150, 180, 220, 290, 310, 380, 410, 480, 520, 680, 850].map(v => v * 1000000);
  const defaultCategoryData = [420, 280, 150, 90].map(v => v * 1000000);

  const monthlyData = {
    labels: monthlyLabels,
    datasets: [
      {
        fill: true,
        label: 'Doanh thu (VND)',
        data: report?.monthlyRevenue || defaultMonthlyData,
        borderColor: 'rgb(99, 102, 241)',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        tension: 0.3,
      },
    ],
  };

  const categoryData = {
    labels: categoryLabels,
    datasets: [
      {
        label: 'Doanh thu theo danh mục (VND)',
        data: report?.categoryRevenue || defaultCategoryData,
        backgroundColor: [
          'rgba(99, 102, 241, 0.7)',
          'rgba(139, 92, 246, 0.7)',
          'rgba(59, 130, 246, 0.7)',
          'rgba(34, 197, 94, 0.7)',
        ],
        borderColor: [
          'rgb(99, 102, 241)',
          'rgb(139, 92, 246)',
          'rgb(59, 130, 246)',
          'rgb(34, 197, 94)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const lineOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      y: {
        grid: { color: 'rgba(255, 255, 255, 0.05)' },
        ticks: { color: '#a1a1aa' },
      },
      x: {
        grid: { display: false },
        ticks: { color: '#a1a1aa' },
      },
    },
  };

  const barOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      y: {
        grid: { color: 'rgba(255, 255, 255, 0.05)' },
        ticks: { color: '#a1a1aa' },
      },
      x: {
        grid: { display: false },
        ticks: { color: '#a1a1aa' },
      },
    },
  };

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Thống kê & Báo cáo doanh thu</h1>
          <p className={styles.subtitle}>
            Trực quan hóa hoạt động kinh doanh thực tế từ cơ sở dữ liệu Postgres thông qua API Gateway.
          </p>
        </div>
      </div>

      {errorMsg && (
        <div style={{ padding: '15px', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', borderRadius: '8px', marginBottom: '20px' }}>
          {errorMsg}
        </div>
      )}

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '30px' }}>
        <div className={styles.detailCard} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <span style={{ fontSize: '13px', color: '#a1a1aa', fontWeight: 500 }}>TỔNG DOANH THU THỰC TẾ</span>
          <span style={{ fontSize: '28px', fontWeight: 800, color: 'rgb(34, 197, 94)' }}>
            {loading ? 'Đang tải...' : `${(report?.totalRevenue || 0).toLocaleString()} VND`}
          </span>
        </div>
        
        <div className={styles.detailCard} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <span style={{ fontSize: '13px', color: '#a1a1aa', fontWeight: 500 }}>TỔNG SỐ ĐƠN HÀNG ĐÃ TẠO</span>
          <span style={{ fontSize: '28px', fontWeight: 800, color: 'rgb(99, 102, 241)' }}>
            {loading ? 'Đang tải...' : `${report?.totalOrders || 0} đơn hàng`}
          </span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '30px' }}>
        {/* Line Chart: Revenue growth */}
        <div className={styles.detailCard}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '20px', color: 'var(--text-primary)' }}>
            Tăng trưởng Doanh thu (Theo Tháng)
          </h3>
          <div style={{ height: '300px', position: 'relative' }}>
            <Line data={monthlyData} options={lineOptions} />
          </div>
        </div>

        {/* Bar Chart: Revenue by Category */}
        <div className={styles.detailCard}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, marginBottom: '20px', color: 'var(--text-primary)' }}>
            Doanh thu Phân loại (Theo Danh mục)
          </h3>
          <div style={{ height: '300px', position: 'relative' }}>
            <Bar data={categoryData} options={barOptions} />
          </div>
        </div>
      </div>
    </div>
  );
}
