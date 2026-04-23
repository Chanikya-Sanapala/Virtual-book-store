export interface User {
  id?: string;
  username: string;
  email: string;
  roles: string[];
  token?: string;
}

export interface Book {
  id?: string;
  title: string;
  author: string;
  description: string;
  category: string;
  price: number;
  stock: number;
  imageUrl: string;
}

export interface Order {
  id?: string;
  items: OrderItem[];
  totalAmount: number;
  status?: string;
  orderDate?: Date;
}

export interface OrderItem {
  bookId: string;
  title: string;
  quantity: number;
  price: number;
}

export interface CommunityPost {
  id?: string;
  userId?: string;
  username?: string;
  title: string;
  content: string;
  createdAt?: Date;
}
