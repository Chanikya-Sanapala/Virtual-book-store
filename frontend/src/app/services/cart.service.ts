import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Book, OrderItem } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItems: OrderItem[] = [];
  private cartSubject = new BehaviorSubject<OrderItem[]>([]);

  constructor() {
    this.cartItems = JSON.parse(localStorage.getItem('cart') || '[]');
    this.cartSubject.next(this.cartItems);
  }

  getCart() {
    return this.cartSubject.asObservable();
  }

  addToCart(book: Book) {
    const existing = this.cartItems.find(i => i.bookId === book.id);
    if (existing) {
      existing.quantity++;
    } else {
      this.cartItems.push({
        bookId: book.id!,
        title: book.title,
        quantity: 1,
        price: book.price
      });
    }
    this.saveCart();
  }

  removeFromCart(bookId: string) {
    this.cartItems = this.cartItems.filter(i => i.bookId !== bookId);
    this.saveCart();
  }

  clearCart() {
    this.cartItems = [];
    this.saveCart();
  }

  private saveCart() {
    localStorage.setItem('cart', JSON.stringify(this.cartItems));
    this.cartSubject.next(this.cartItems);
  }

  getTotal() {
    return this.cartItems.reduce((acc, item) => acc + (item.price * item.quantity), 0);
  }
}
