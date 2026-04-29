import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { timeout, catchError, of } from 'rxjs';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css'],
  standalone: false
})
export class OrderHistoryComponent implements OnInit {
  orders: any[] = [];
  isLoading = false;
  errorMessage = '';

  constructor(private orderService: OrderService) { }

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    this.errorMessage = '';
    this.orderService.getMyOrders().pipe(
      timeout(10000),
      catchError(err => {
        console.error('OrderHistory: Fetch failed', err);
        this.errorMessage = 'Connection timeout. Failed to retrieve your history.';
        return of([]);
      })
    ).subscribe({
      next: (data) => {
        this.orders = data.sort((a, b) => {
          const dateA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
          const dateB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
          return dateB - dateA;
        });
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching orders:', err);
        this.isLoading = false;
      }
    });
  }

  getStatusClass(status: string): string {
    return status?.toLowerCase() || 'pending';
  }

  getTrackingProgress(status: string): number {
    const steps: Record<string, number> = {
      'PENDING': 5,
      'PROCESSING': 35,
      'SHIPPED': 65,
      'DELIVERED': 100
    };
    return steps[status] || 5;
  }

  isStepCompleted(currentStatus: string, step: string): boolean {
    const statusOrder = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
    const currentIndex = statusOrder.indexOf(currentStatus);
    const stepIndex = statusOrder.indexOf(step);
    return stepIndex <= currentIndex;
  }
}
