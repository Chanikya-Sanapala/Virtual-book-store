import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book } from '../models/interfaces';
import { AuthService } from './auth.service';

const BOOK_API = 'http://localhost:8080/api/books';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    const token = this.authService.currentUserValue.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getBooks(title?: string): Observable<Book[]> {
    let url = BOOK_API;
    if (title) url += `?title=${title}`;
    return this.http.get<Book[]>(url);
  }

  getBook(id: string): Observable<Book> {
    return this.http.get<Book>(BOOK_API + '/' + id);
  }

  getBooksByCategory(category: string): Observable<Book[]> {
    return this.http.get<Book[]>(BOOK_API + '/category/' + category);
  }

  addBook(book: Book): Observable<Book> {
    return this.http.post<Book>(BOOK_API, book, { headers: this.getHeaders() });
  }

  updateBook(id: string, book: Book): Observable<Book> {
    return this.http.put<Book>(BOOK_API + '/' + id, book, { headers: this.getHeaders() });
  }

  deleteBook(id: string): Observable<any> {
    return this.http.delete(BOOK_API + '/' + id, { headers: this.getHeaders() });
  }
}
