import { Component, OnInit } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { timeout, catchError, of } from 'rxjs';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
  standalone: false
})
export class CartComponent implements OnInit {
  items: any[] = [];
  total = 0;
  isProcessing = false;

  constructor(
    private cartService: CartService, 
    private orderService: OrderService,
    private authService: AuthService,
    private notificationService: NotificationService,
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

    if (this.items.length === 0) {
      this.notificationService.show('Your cart is empty.', 'error');
      return;
    }

    // Instead of placing order directly, navigate to payment gateway
    this.router.navigate(['/payment']);
  }
}
