import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { BookService } from '../../services/book.service';
import { CartService } from '../../services/cart.service';
import { SearchService } from '../../services/search.service';
import { Book } from '../../models/interfaces';
import { CATEGORIES } from '../../constants/categories';
import { CategoryService } from '../../services/category.service';
import { Subscription, timeout, catchError, of } from 'rxjs';
import { debounceTime, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  standalone: false
})
export class HomeComponent implements OnInit, OnDestroy {
  books: Book[] = [];
  searchQuery = '';
  selectedCategory = '';
  categories: string[] = [];
  isLoading = false;
  errorMessage = '';

  // Pagination state
  currentPage = 0;
  pageSize = 12;
  totalPages = 1;
  totalElements = 0;

  private searchSub: Subscription | undefined;
  private categorySub: Subscription | undefined;
  private refreshSub: Subscription | undefined;

  constructor(
    private bookService: BookService, 
    private cartService: CartService,
    private searchService: SearchService,
    private categoryService: CategoryService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    // 1. Load categories
    this.loadCategories();
    
    // 2. Load books page 0 on startup
    this.fetchBooks(0);

    // 3. Listen for search changes with debounce
    this.searchSub = this.searchService.searchQuery$.pipe(
      debounceTime(300)
    ).subscribe(query => {
      if (this.searchQuery !== query) {
        this.searchQuery = query;
        if (query) {
          this.selectedCategory = ''; // Clear category when searching
        }
        this.fetchBooks(0);
      }
    });

    // 4. Listen for category changes from Navbar (skip initial if unchanged)
    this.categorySub = this.categoryService.selectedCategory$.subscribe(category => {
      if (this.selectedCategory !== category) {
        this.selectedCategory = category;
        if (category) {
          this.searchQuery = '';
        }
        this.fetchBooks(0);
      }
    });

    // 5. Listen for global book refreshes (from Admin Dashboard)
    this.refreshSub = this.bookService.bookRefresh$.subscribe(() => {
      this.bookService.clearCache();
      this.fetchBooks(this.currentPage);
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
    if (this.categorySub) {
      this.categorySub.unsubscribe();
    }
    if (this.refreshSub) {
      this.refreshSub.unsubscribe();
    }
  }

  loadCategories() {
    this.bookService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.categoryService.setCategories(categories);
      },
      error: (err) => {
        console.error('Error loading categories:', err);
        this.categories = CATEGORIES;
        this.categoryService.setCategories(CATEGORIES);
      }
    });
  }

  loadBooks() {
    this.fetchBooks(this.currentPage);
  }

  filterByCategory(category: string) {
    this.categoryService.selectCategory(category);
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.fetchBooks(page);
    }
  }

  private fetchBooks(page: number = 0) {
    const category = this.selectedCategory;
    
    // Check cache first
    const cached = category 
      ? this.bookService.getCachedBooksByCategory(category, page, this.pageSize)
      : this.bookService.getCachedBooks(this.searchQuery, page, this.pageSize);

    if (cached) {
      this.books = cached;
      this.isLoading = false;
    } else {
      this.isLoading = true;
      if (this.books.length === 0) {
        this.books = [];
      }
    }

    this.errorMessage = '';
    
    const observable = category 
      ? this.bookService.getBooksByCategory(category, page, this.pageSize)
      : this.bookService.getBooks(page, this.pageSize, this.searchQuery);

    observable.pipe(
      timeout(10000),
      catchError(err => {
        console.error('HomeComponent: Fetch failed', err);
        if (!cached) {
          this.errorMessage = 'Connection timeout. The server is taking too long to respond.';
        }
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        if (data && this.selectedCategory === category) {
          if (Array.isArray(data)) {
            this.books = data;
            this.currentPage = page;
            this.totalPages = 1;
            this.totalElements = data.length;
          } else {
            this.books = data.content || [];
            this.currentPage = data.number !== undefined ? data.number : page;
            this.totalPages = data.totalPages || 1;
            this.totalElements = data.totalElements || this.books.length;
          }
          this.cdr.detectChanges();
        }
      }
    });
  }

  addToCart(book: Book) {
    this.cartService.addToCart(book);
  }

  handleImageError(event: any) {
    const fallbackImage = 'https://placehold.co/200x300/e2e8f0/64748b?text=Cover+Not+Found';
    if (event.target.src !== fallbackImage) {
      event.target.src = fallbackImage;
    }
  }

  trackByBookId(index: number, book: Book): string {
    return book.id;
  }
}
