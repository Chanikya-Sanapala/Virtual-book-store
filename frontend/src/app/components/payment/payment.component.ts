import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css'],
  standalone: false
})
export class PaymentComponent implements OnInit {
  // Navigation
  currentStep = 1;

  // Shipping
  shippingAddress = '';

  // Payment
  cardNumber = '';
  cardName = '';
  expiryDate = '';
  cvv = '';
  
  isProcessing = false;
  isSuccess = false;
  totalAmount = 0;
  items: any[] = [];
  lastOrderId = '';

  constructor(
    private cartService: CartService,
    private orderService: OrderService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartService.getCart().subscribe(items => {
      this.items = items;
      this.totalAmount = this.cartService.getTotal();
      if (this.totalAmount <= 0) {
        this.router.navigate(['/']);
      }
    });
  }

  nextStep() {
    if (this.currentStep === 1 && this.shippingAddress.trim()) {
      this.currentStep = 2;
    }
  }

  formatCardNumber() {
    let v = this.cardNumber.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    let matches = v.match(/\d{4,16}/g);
    let match = matches && matches[0] || '';
    let parts = [];

    for (let i=0, len=match.length; i<len; i+=4) {
      parts.push(match.substring(i, i+4));
    }

    if (parts.length) {
      this.cardNumber = parts.join(' ');
    }
  }

  processPayment() {
    if (!this.cardNumber || !this.cardName || !this.expiryDate || !this.cvv) {
      this.notificationService.show('Please fill in all card details.', 'error');
      return;
    }

    this.isProcessing = true;
    
    // Simulate payment processing delay
    setTimeout(() => {
      this.placeOrder();
    }, 3000);
  }

  private placeOrder() {
    const order = {
      items: this.items.map(item => ({
        bookId: item.id,
        title: item.title,
        quantity: 1,
        price: item.price
      })),
      totalAmount: this.totalAmount,
      shippingAddress: this.shippingAddress,
      status: 'PROCESSING' // Set initial status
    };

    this.orderService.placeOrder(order).subscribe({
      next: (savedOrder: any) => {
        this.isProcessing = false;
        this.isSuccess = true;
        this.lastOrderId = savedOrder.id;
        this.cartService.clearCart();
        
        // Final success redirect after animation
        setTimeout(() => {
          this.router.navigate(['/history']);
          this.notificationService.show('Payment successful! Your books are on the way.', 'success');
        }, 4000);
      },
      error: (err) => {
        this.isProcessing = false;
        this.notificationService.show('Payment failed. Please try again.', 'error');
      }
    });
  }
}
