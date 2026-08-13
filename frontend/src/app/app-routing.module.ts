import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';

const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'cart', loadChildren: () => import('./features/cart/cart.module').then(m => m.CartModule) },
  { path: 'history', loadChildren: () => import('./features/order-history/order-history.module').then(m => m.OrderHistoryModule) },
  { path: 'admin', loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule) },
  { path: 'community', loadChildren: () => import('./features/community/community.module').then(m => m.CommunityModule) },
  { path: 'book/:id', loadChildren: () => import('./features/book-detail/book-detail.module').then(m => m.BookDetailModule) },
  { path: 'payment', loadChildren: () => import('./features/payment/payment.module').then(m => m.PaymentModule) },
  { path: '', loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule) },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
