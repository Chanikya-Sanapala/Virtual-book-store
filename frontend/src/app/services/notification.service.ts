import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Notification {
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSub = new BehaviorSubject<Notification | null>(null);
  notification$ = this.notificationSub.asObservable();

  show(message: string, type: 'success' | 'error' | 'info' = 'success') {
    this.notificationSub.next({ message, type });
    setTimeout(() => this.notificationSub.next(null), 5000);
  }
}
