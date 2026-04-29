import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

const ORDER_API = environment.apiUrl + '/orders';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    const token = this.authService.currentUserValue.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  placeOrder(order: any): Observable<any> {
    return this.http.post(ORDER_API, order, { headers: this.getHeaders() });
  }

  getMyOrders(): Observable<any[]> {
    return this.http.get<any[]>(ORDER_API + '/my-orders', { headers: this.getHeaders() });
  }

  getAllOrders(): Observable<any[]> {
    return this.http.get<any[]>(ORDER_API, { headers: this.getHeaders() });
  }
}
