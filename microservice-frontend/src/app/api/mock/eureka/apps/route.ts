import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({
    applications: {
      application: [
        {
          name: 'USER-SERVICE',
          instance: [
            {
              instanceId: 'user-service-1',
              hostName: 'localhost',
              app: 'USER-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8091 },
            },
          ],
        },
        {
          name: 'PRODUCT-SERVICE',
          instance: [
            {
              instanceId: 'product-service-1',
              hostName: 'localhost',
              app: 'PRODUCT-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8092 },
            },
            {
              instanceId: 'product-service-2',
              hostName: 'localhost',
              app: 'PRODUCT-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8097 },
            },
          ],
        },
        {
          name: 'ORDER-SERVICE',
          instance: [
            {
              instanceId: 'order-service-1',
              hostName: 'localhost',
              app: 'ORDER-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8093 },
            },
          ],
        },
        {
          name: 'PAYMENT-SERVICE',
          instance: [
            {
              instanceId: 'payment-service-1',
              hostName: 'localhost',
              app: 'PAYMENT-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8094 },
            },
          ],
        },
        {
          name: 'NOTIFICATION-SERVICE',
          instance: [
            {
              instanceId: 'notification-service-1',
              hostName: 'localhost',
              app: 'NOTIFICATION-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8095 },
            },
          ],
        },
        {
          name: 'FILE-SERVICE',
          instance: [
            {
              instanceId: 'file-service-1',
              hostName: 'localhost',
              app: 'FILE-SERVICE',
              ipAddr: '127.0.0.1',
              status: 'UP',
              port: { $: 8096 },
            },
          ],
        },
      ],
    },
  });
}
