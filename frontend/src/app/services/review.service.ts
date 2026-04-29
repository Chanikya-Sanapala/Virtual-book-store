import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Review } from '../models/interfaces';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

const REVIEW_API = environment.apiUrl + '/reviews';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    const token = this.authService.currentUserValue.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getReviewsByBook(bookId: string): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/book/${bookId}`);
  }

  addReview(review: Review): Observable<Review> {
    return this.http.post<Review>(REVIEW_API, review, { headers: this.getHeaders() });
  }
}
