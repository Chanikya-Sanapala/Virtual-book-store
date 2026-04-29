import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { CartComponent } from './components/cart/cart.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { CommunityComponent } from './components/community/community.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { OrderHistoryComponent } from './components/order-history/order-history.component';
import { BookDetailComponent } from './components/book-detail/book-detail.component';
import { PaymentComponent } from './components/payment/payment.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'cart', component: CartComponent },
  { path: 'history', component: OrderHistoryComponent },
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'community', component: CommunityComponent },
  { path: 'book/:id', component: BookDetailComponent },
  { path: 'payment', component: PaymentComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
