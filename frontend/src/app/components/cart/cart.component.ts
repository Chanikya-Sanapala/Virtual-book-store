import { Component, OnInit } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
  standalone: false
})
export class CartComponent implements OnInit {
  items: any[] = [];
  total = 0;

  constructor(
    private cartService: CartService, 
    private http: HttpClient, 
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.cartService.getCart().subscribe(items => {
      this.items = items;
      this.total = this.cartService.getTotal();
    });
  }

  removeItem(id: string) {
    this.cartService.removeFromCart(id);
  }

  checkout() {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    const token = this.authService.currentUserValue.token;
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });

    const order = {
      items: this.items,
      totalAmount: this.total
    };

    this.http.post('http://localhost:8080/api/orders', order, { headers }).subscribe(() => {
      alert('Order placed successfully!');
      this.cartService.clearCart();
      this.router.navigate(['/']);
    });
  }
}
