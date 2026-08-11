import React from 'react';
import { userApi } from '@/lib/api/users';
import styles from '../../products/Products.module.css';

export const dynamic = 'force-dynamic';

export default async function EurekaServicesPage() {
  let services: any[] = [];
  let errorMsg = null;

  try {
    services = await userApi.getEurekaServices();
  } catch (e: any) {
    console.error(e);
    errorMsg = 'Lỗi kết nối Eureka Server. Không thể truy xuất danh sách dịch vụ đang hoạt động.';
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Eureka Service Discovery</h1>
          <p className={styles.subtitle}>
            Giám sát danh sách các instance dịch vụ đã đăng ký và tự động khám phá (Auto-discovery registry).
          </p>
        </div>
      </div>

      {errorMsg ? (
        <div className={styles.errorBox}>
          <p>{errorMsg}</p>
        </div>
      ) : services.length === 0 ? (
        <div className={styles.emptyBox}>
          <p>Không tìm thấy dịch vụ nào đăng ký trong Eureka.</p>
        </div>
      ) : (
        <div className={styles.tableCard}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Tên Dịch Vụ</th>
                <th>Mã Instance ID</th>
                <th>Host & IP Address</th>
                <th>Cổng kết nối (Port)</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {services.flatMap((app: any) =>
                app.instance.map((inst: any) => (
                  <tr key={inst.instanceId}>
                    <td style={{ fontWeight: 700, color: 'var(--color-brand)' }}>{app.name}</td>
                    <td className={styles.monoId}>{inst.instanceId}</td>
                    <td>{`${inst.hostName} (${inst.ipAddr})`}</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>{inst.port?.['$'] || 'N/A'}</td>
                    <td>
                      <span
                        className={styles.badge}
                        style={{
                          backgroundColor: inst.status === 'UP' ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                          color: inst.status === 'UP' ? 'var(--color-success)' : 'var(--color-error)',
                          padding: '2px 8px',
                          borderRadius: '9999px',
                          fontSize: '11px',
                          fontWeight: 'bold',
                        }}
                      >
                        {inst.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
