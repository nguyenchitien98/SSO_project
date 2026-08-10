/**
 * Product-related API Request and Response Types.
 */

export interface Product {
  id: number;
  name: string;
  price: number;
  stock: number;
  description: string | null;
  imageUrl: string | null;
  category: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

export interface CreateProductRequest {
  name: string;
  price: number;
  stock: number;
  description?: string;
  imageUrl?: string;
  category?: string;
}

export interface UpdateProductRequest {
  name: string;
  price: number;
  stock: number;
  description?: string;
  imageUrl?: string;
  category?: string;
  active: boolean;
}
