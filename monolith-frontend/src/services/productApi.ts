import { apiClient } from './apiClient';
import { Product, CreateProductRequest, UpdateProductRequest } from '@/types/product';
import { PageResponse } from '@/types/api';

export const productApi = {
  getProducts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<Product>>(`/api/products?page=${page}&size=${size}`),

  getProductById: (id: number) =>
    apiClient.get<Product>(`/api/products/${id}`),

  createProduct: (request: CreateProductRequest) =>
    apiClient.post<Product>('/api/products', request),

  updateProduct: (id: number, request: UpdateProductRequest) =>
    apiClient.put<Product>(`/api/products/${id}`, request),

  deleteProduct: (id: number) =>
    apiClient.delete<void>(`/api/products/${id}`),
};
