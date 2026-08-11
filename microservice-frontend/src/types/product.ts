export interface Product {
  id: string;
  name: string;
  price: number;
  stock: number;
  category: string;
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  name: string;
  price: number;
  stock: number;
  category: string;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  name: string;
  price: number;
  stock: number;
  category: string;
  imageUrl?: string;
}
