import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { catchError, of, finalize } from 'rxjs';
import { executeWithColdStartRetry } from '../../utils/cold-start';

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
  loadingMessage = 'Retrieving your archives...';

  constructor(private orderService: OrderService) { }

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    this.errorMessage = '';
    this.loadingMessage = 'Retrieving your archives...';

    executeWithColdStartRetry(() => this.orderService.getMyOrders(), {
      firstTimeoutMs: 15000,
      retryTimeoutMs: 120000,
      onWakingUp: () => {
        this.loadingMessage = 'Server is waking up. Loading history...';
      }
    }).pipe(
      catchError(err => {
        console.error('OrderHistory: Fetch failed after retry', err);
        this.errorMessage = 'Unable to load history. Please try again.';
        return of([]);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    ).subscribe({
      next: (data) => {
        if (data) {
          this.orders = data.sort((a: any, b: any) => {
            const dateA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
            const dateB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
            return dateB - dateA;
          });
        }
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
