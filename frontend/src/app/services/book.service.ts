import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Book, ImportProgressInfo } from '../models/interfaces';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

const BASE_API = environment.apiUrl;
const BOOK_API = BASE_API + '/books';

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

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

  getBooks(page: number = 0, size: number = 12, title?: string): Observable<any> {
    const cacheKey = `all_${page}_${size}_${title || ''}`;
    let url = `${BOOK_API}?page=${page}&size=${size}`;
    if (title) url += `&title=${encodeURIComponent(title)}`;
    
    return this.http.get<any>(url).pipe(
      tap(res => {
        const books = Array.isArray(res) ? res : (res?.content || []);
        this.cache[cacheKey] = books;
      })
    );
  }

  // Helper for components to check if data is cached
  getCachedBooks(title?: string, page: number = 0, size: number = 12): Book[] | null {
    return this.cache[`all_${page}_${size}_${title || ''}`] || null;
  }

  getBook(id: string): Observable<Book> {
    return this.http.get<Book>(BOOK_API + '/' + id);
  }

  getBooksByCategory(category: string, page: number = 0, size: number = 12): Observable<any> {
    const cacheKey = `cat_${category}_${page}_${size}`;
    const url = `${BOOK_API}/category/${encodeURIComponent(category)}?page=${page}&size=${size}`;
    return this.http.get<any>(url).pipe(
      tap(res => {
        const books = Array.isArray(res) ? res : (res?.content || []);
        this.cache[cacheKey] = books;
      })
    );
  }

  getCachedBooksByCategory(category: string, page: number = 0, size: number = 12): Book[] | null {
    return this.cache[`cat_${category}_${page}_${size}`] || null;
  }

  getBooksBySeller(sellerId: string, page: number = 0, size: number = 12): Observable<any> {
    const url = `${BOOK_API}/seller/${encodeURIComponent(sellerId)}?page=${page}&size=${size}`;
    return this.http.get<any>(url);
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

  notifyBookRefresh() {
    this.clearAllCaches();
    this.bookRefresh.next();
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

  startBookImport(file: File): Observable<ImportProgressInfo> {
    const formData: FormData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportProgressInfo>(BOOK_API + '/import', formData, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${this.authService.currentUserValue.token}`
      })
    });
  }

  importBooks(file: File): Observable<ImportProgressInfo> {
    return this.startBookImport(file);
  }

  getImportProgress(importId: string): Observable<ImportProgressInfo> {
    return this.http.get<ImportProgressInfo>(`${BOOK_API}/import/progress/${encodeURIComponent(importId)}`, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${this.authService.currentUserValue.token}`
      })
    });
  }

  getActiveImport(): Observable<ImportProgressInfo | null> {
    return this.http.get<ImportProgressInfo | null>(`${BOOK_API}/import/active`, {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${this.authService.currentUserValue.token}`
      })
    });
  }
}
