import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css'],
  standalone: false
})
export class ForgotPasswordComponent {
  email: string = '';
  isSubmitted: boolean = false;
  isLoading: boolean = false;

  constructor(private http: HttpClient, private notificationService: NotificationService) {}

  onSubmit() {
    this.isLoading = true;
    this.http.post('http://localhost:8080/api/auth/forgot-password', { email: this.email })
      .subscribe({
        next: (res: any) => {
          this.isSubmitted = true;
          this.isLoading = false;
          this.notificationService.show(res.message || 'Reset link sent!');
        },
        error: (err) => {
          this.isLoading = false;
          this.notificationService.show('Failed to send reset link.');
        }
      });
  }
}
