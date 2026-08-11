'use client';

import React from 'react';
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

export default function ReportsPage() {
  // Mock monthly revenue data
  const monthlyData = {
    labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
    datasets: [
      {
        fill: true,
        label: 'Doanh thu (VND)',
        data: [120, 150, 180, 220, 290, 310, 380, 410, 480, 520, 680, 850].map(v => v * 1000000),
        borderColor: 'rgb(99, 102, 241)',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        tension: 0.3,
      },
    ],
  };

  const categoryData = {
    labels: ['Điện thoại', 'Máy tính', 'Phụ kiện', 'Màn hình'],
    datasets: [
      {
        label: 'Doanh thu theo danh mục (VND)',
        data: [420, 280, 150, 90].map(v => v * 1000000),
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
            Trực quan hóa hoạt động kinh doanh, doanh thu bán hàng của hệ thống Microservices sử dụng Chart.js.
          </p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '30px', marginTop: '20px' }}>
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
