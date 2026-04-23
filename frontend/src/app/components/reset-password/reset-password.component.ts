import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
  standalone: false
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;
  newPassword: any = '';
  confirmPassword: any = '';
  isSubmitted: boolean = false;
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.errorMessage = 'Invalid password reset link.';
    }
  }

  onSubmit() {
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http.post('http://localhost:8080/api/auth/reset-password', { 
      token: this.token, 
      newPassword: this.newPassword 
    }).subscribe({
      next: (res: any) => {
        this.isSubmitted = true;
        this.isLoading = false;
        this.notificationService.show(res.message || 'Password reset successfully!');
        setTimeout(() => this.router.navigate(['/login']), 3000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to reset password. The link might be expired.';
      }
    });
  }
}
