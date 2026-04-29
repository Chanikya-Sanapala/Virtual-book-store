import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Book } from '../models/interfaces';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

const BASE_API = environment.apiUrl;
const BOOK_API = BASE_API + '/books';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private bookRefresh = new Subject<void>();
  bookRefresh$ = this.bookRefresh.asObservable();
  
  private cache: { [key: string]: Book[] } = {};

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    const token = this.authService.currentUserValue.token;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getBooks(title?: string): Observable<Book[]> {
    const cacheKey = 'all_' + (title || '');
    let url = BOOK_API;
    if (title) url += `?title=${title}`;
    
    return this.http.get<Book[]>(url).pipe(
      tap(books => this.cache[cacheKey] = books)
    );
  }

  // Helper for components to check if data is cached
  getCachedBooks(title?: string): Book[] | null {
    return this.cache['all_' + (title || '')] || null;
  }

  getBook(id: string): Observable<Book> {
    return this.http.get<Book>(BOOK_API + '/' + id);
  }

  getBooksByCategory(category: string): Observable<Book[]> {
    return this.http.get<Book[]>(BOOK_API + '/category/' + category).pipe(
      tap(books => this.cache['cat_' + category] = books)
    );
  }

  getCachedBooksByCategory(category: string): Book[] | null {
    return this.cache['cat_' + category] || null;
  }

  getBooksBySeller(sellerId: string): Observable<Book[]> {
    return this.http.get<Book[]>(BOOK_API + '/seller/' + sellerId);
  }

  addBook(book: Book): Observable<Book> {
    return this.http.post<Book>(BOOK_API, book, { headers: this.getHeaders() }).pipe(
      tap(() => {
        this.clearAllCaches();
        this.bookRefresh.next();
      })
    );
  }

  updateBook(id: string, book: Book): Observable<Book> {
    return this.http.put<Book>(BOOK_API + '/' + id, book, { headers: this.getHeaders() }).pipe(
      tap(() => {
        this.clearAllCaches();
        this.bookRefresh.next();
      })
    );
  }

  deleteBook(id: string): Observable<any> {
    return this.http.delete(BOOK_API + '/' + id, { headers: this.getHeaders() }).pipe(
      tap(() => {
        this.clearAllCaches();
        this.bookRefresh.next();
      })
    );
  }

  clearCache() {
    this.clearAllCaches();
  }

  private clearAllCaches() {
    this.cache = {};
  }

  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(BOOK_API + '/categories');
  }

  uploadImage(file: File): Observable<any> {
    const formData: FormData = new FormData();
    formData.append('file', file);
    return this.http.post(BASE_API + '/files/upload', formData, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${this.authService.currentUserValue.token}`
      })
    });
  }

  importBooks(file: File): Observable<any> {
    const formData: FormData = new FormData();
    formData.append('file', file);
    return this.http.post(BOOK_API + '/import', formData, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${this.authService.currentUserValue.token}`
      }),
      responseType: 'text' as 'json' // Handle text response as any
    }).pipe(
      tap(() => {
        this.clearAllCaches();
        this.bookRefresh.next();
      })
    );
  }
}
