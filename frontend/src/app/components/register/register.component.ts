import { Component, OnInit, AfterViewInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';

declare var google: any;

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
  standalone: false
})
export class RegisterComponent implements OnInit, AfterViewInit {
  form: any = {
    username: null,
    email: null,
    password: null,
    role: 'user'
  };
  isSuccessful = false;
  isSignUpFailed = false;
  errorMessage = '';

  constructor(
    private authService: AuthService, 
    private router: Router,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    if (typeof google !== 'undefined') {
      google.accounts.id.initialize({
        client_id: '115274603227-h9on2aqka1ufhnrnqpo5mf9a08ulbiva.apps.googleusercontent.com',
        callback: this.handleCredentialResponse.bind(this)
      });
      google.accounts.id.renderButton(
        document.getElementById('google-btn'),
        { theme: 'outline', size: 'large', text: 'continue_with' }
      );
    }
  }

  handleCredentialResponse(response: any) {
    this.authService.googleLogin(response.credential).subscribe({
      next: data => {
        this.isSuccessful = true;
        this.isSignUpFailed = false;
        this.notificationService.show('Account created via Google successfully!');
        setTimeout(() => this.router.navigate(['/']), 2000);
      },
      error: err => {
        this.errorMessage = 'Google Registration failed. Please try again.';
        this.isSignUpFailed = true;
      }
    });
  }

  onSubmit(): void {
    const { username, email, password, role } = this.form;
    const roles = [role];

    this.authService.register({ username, email, password, roles }).subscribe({
      next: data => {
        this.isSuccessful = true;
        this.isSignUpFailed = false;
        this.notificationService.show('Account created successfully!');
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: err => {
        this.errorMessage = err.error ? err.error.message : 'Registration failed. Please check your connection.';
        this.isSignUpFailed = true;
      }
    });
  }
}
