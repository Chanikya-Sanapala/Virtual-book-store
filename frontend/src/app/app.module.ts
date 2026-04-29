import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { CartComponent } from './components/cart/cart.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { CommunityComponent } from './components/community/community.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { OrderHistoryComponent } from './components/order-history/order-history.component';
import { AiChatbotComponent } from './components/ai-chatbot/ai-chatbot.component';
import { BookDetailComponent } from './components/book-detail/book-detail.component';
import { PaymentComponent } from './components/payment/payment.component';

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    HomeComponent,
    LoginComponent,
    RegisterComponent,
    CartComponent,
    AdminDashboardComponent,
    CommunityComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    OrderHistoryComponent,
    AiChatbotComponent,
    BookDetailComponent,
    PaymentComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
