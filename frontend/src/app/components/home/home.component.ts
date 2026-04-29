import { Component, OnInit, OnDestroy } from '@angular/core';
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
  private searchSub: Subscription | undefined;
  private categorySub: Subscription | undefined;

  constructor(
    private bookService: BookService, 
    private cartService: CartService,
    private searchService: SearchService,
    private categoryService: CategoryService
  ) { }

  ngOnInit(): void {
    // 1. Load categories first
    this.loadCategories();
    
    // 2. Load books immediately on startup
    this.loadBooks();

    // 3. Then listen for search changes with a delay
    this.searchSub = this.searchService.searchQuery$.pipe(
      debounceTime(300)
    ).subscribe(query => {
      if (this.searchQuery !== query) {
        this.searchQuery = query;
        if (query) {
          this.selectedCategory = ''; // Clear category when searching
        }
        this.fetchBooks();
      }
    });

    // 4. Listen for category changes from Navbar
    this.categorySub = this.categoryService.selectedCategory$.subscribe(category => {
      if (this.selectedCategory !== category) {
        this.selectedCategory = category;
        if (category) {
          this.searchQuery = '';
        }
        this.fetchBooks();
      }
    });

    // 5. Listen for global book refreshes (from Admin Dashboard)
    this.bookService.bookRefresh$.subscribe(() => {
      // Force refresh by clearing cache first
      this.bookService.clearCache();
      this.fetchBooks();
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
    if (this.categorySub) {
      this.categorySub.unsubscribe();
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
        // Fallback to hardcoded categories
        this.categories = CATEGORIES;
        this.categoryService.setCategories(CATEGORIES);
      }
    });
  }

  loadBooks() {
    this.fetchBooks();
  }

  filterByCategory(category: string) {
    this.categoryService.selectCategory(category);
  }

  private fetchBooks() {
    const category = this.selectedCategory;
    
    // Check cache first to avoid "reflecting back" (loading spinner)
    const cached = category 
      ? this.bookService.getCachedBooksByCategory(category)
      : this.bookService.getCachedBooks(this.searchQuery);

    if (cached) {
      this.books = cached;
      this.isLoading = false;
      // We still fetch in background to keep it fresh, but don't show spinner
    } else {
      this.isLoading = true;
      this.books = []; 
    }

    this.errorMessage = '';
    
    const observable = category 
      ? this.bookService.getBooksByCategory(category)
      : this.bookService.getBooks(this.searchQuery);

    observable.pipe(
      timeout(10000),
      catchError(err => {
        console.error('HomeComponent: Fetch failed', err);
        if (!cached) {
          this.errorMessage = 'Connection timeout. The server is taking too long to respond.';
        }
        return of([]);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    ).subscribe({
      next: (data) => {
        if (this.selectedCategory === category) {
          this.books = data;
        }
      }
    });
  }

  addToCart(book: Book) {
    this.cartService.addToCart(book);
  }
}
