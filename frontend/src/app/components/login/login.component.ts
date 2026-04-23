import { Component, OnInit, AfterViewInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';

declare var google: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: false
})
export class LoginComponent implements OnInit, AfterViewInit {
  form: any = {
    username: null,
    password: null
  };
  isLoggedIn = false;
  isLoginFailed = false;
  errorMessage = '';
  roles: string[] = [];

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
        this.isLoginFailed = false;
        this.isLoggedIn = true;
        this.notificationService.show('Welcome back via Google!');
        this.router.navigate(['/']);
      },
      error: err => {
        this.errorMessage = 'Google Login failed. Please try again.';
        this.isLoginFailed = true;
      }
    });
  }

  onSubmit(): void {
    const { username, password } = this.form;

    this.authService.login(username, password).subscribe({
      next: data => {
        this.isLoginFailed = false;
        this.isLoggedIn = true;
        this.notificationService.show('Welcome back!');
        this.router.navigate(['/']);
      },
      error: err => {
        this.errorMessage = err.error ? err.error.message : 'Login failed. Please check your connection.';
        this.isLoginFailed = true;
      }
    });
  }
}
