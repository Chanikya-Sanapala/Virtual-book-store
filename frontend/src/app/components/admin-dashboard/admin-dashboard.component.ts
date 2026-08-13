import { Component, OnInit, OnDestroy } from '@angular/core';
import { BookService } from '../../services/book.service';
import { Book, ImportProgressInfo } from '../../models/interfaces';
import { NotificationService } from '../../services/notification.service';
import { AuthService } from '../../services/auth.service';
import { CATEGORIES } from '../../constants/categories';
import { timeout, catchError, of, Subscription, interval } from 'rxjs';
import { executeWithColdStartRetry } from '../../utils/cold-start';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
  standalone: false
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  books: Book[] = [];
  newBook: Book = this.resetBook();
  isEditing = false;
  editingId = '';
  userId = '';
  isUploading = false;
  isLoading = false;
  errorMessage = '';
  loadingMessage = 'Fetching your listings...';
  imagePreview: string | null = null;
  selectedCategory = '';
  categories: string[] = [];

  // Inventory pagination state
  currentPage = 0;
  pageSize = 12;
  totalPages = 1;
  totalElements = 0;
  
  isImporting = false;
  importStatus = '';
  activeImportProgress: ImportProgressInfo | null = null;

  private progressSub?: Subscription;
  private inventorySub?: Subscription;
  private lastProcessedCount = 0;

  constructor(
    private bookService: BookService, 
    private notificationService: NotificationService,
    private authService: AuthService
  ) { 
    this.userId = this.authService.currentUserValue.id;
  }

  ngOnInit(): void {
    // Load categories first
    this.loadCategories();
    
    this.loadBooks();
    
    // Listen for global refreshes to keep inventory in sync
    this.bookService.bookRefresh$.subscribe(() => {
      // Clear cache to ensure fresh data
      this.bookService.clearCache();
      this.loadBooks();
    });

    // Check for active import on page load/refresh
    this.checkActiveImport();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  private stopPolling() {
    if (this.progressSub) {
      this.progressSub.unsubscribe();
      this.progressSub = undefined;
    }
    if (this.inventorySub) {
      this.inventorySub.unsubscribe();
      this.inventorySub = undefined;
    }
  }

  loadCategories() {
    this.bookService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (err) => {
        console.error('Error loading categories:', err);
        // Fallback to hardcoded categories
        this.categories = CATEGORIES;
      }
    });
  }

  loadBooks(page: number = this.currentPage) {
    this.isLoading = true;
    this.errorMessage = '';
    this.loadingMessage = 'Fetching your listings...';
    
    // Check if user is admin
    const isAdmin = this.authService.currentUserValue.roles.includes('ROLE_ADMIN');
    
    const requestFactory = () => {
      if (this.selectedCategory) {
        return this.bookService.getBooksByCategory(this.selectedCategory, page, this.pageSize);
      } else if (!isAdmin && this.userId) {
        return this.bookService.getBooksBySeller(this.userId, page, this.pageSize);
      } else {
        return this.bookService.getBooks(page, this.pageSize);
      }
    };

    executeWithColdStartRetry(requestFactory, {
      firstTimeoutMs: 15000,
      retryTimeoutMs: 120000,
      onWakingUp: () => {
        this.loadingMessage = 'Server is waking up. Loading books...';
      }
    }).pipe(
      catchError(err => {
        console.error('AdminDashboard: Fetch failed after retry', err);
        this.errorMessage = 'Unable to load books. Please try again.';
        return of(null);
      })
    ).subscribe({
      next: (data) => {
        if (data) {
          if (Array.isArray(data)) {
            this.books = data;
            this.totalPages = 1;
            this.totalElements = data.length;
            this.currentPage = 0;
          } else {
            const bookList: Book[] = data.content || [];
            if (!isAdmin && this.userId) {
              this.books = bookList.filter(b => b.sellerId === this.userId);
            } else {
              this.books = bookList;
            }
            this.currentPage = data.number !== undefined ? data.number : page;
            this.totalPages = data.totalPages || 1;
            this.totalElements = data.totalElements || this.books.length;
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading books:', err);
        this.isLoading = false;
      }
    });
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.loadBooks(page);
    }
  }

  filterByCategory(category: string) {
    this.selectedCategory = category;
    this.currentPage = 0;
    this.loadBooks(0);
  }

  onSubmit() {
    this.isLoading = true;
    if (this.isEditing) {
      this.bookService.updateBook(this.editingId, this.newBook).pipe(
        timeout(10000),
        catchError(err => {
          this.notificationService.show('Update failed (timeout).', 'error');
          return of(null);
        })
      ).subscribe((res) => {
        if (res) {
          this.loadBooks();
          this.cancelEdit();
          this.notificationService.show('Book updated successfully!');
        }
        this.isLoading = false;
      });
    } else {
      this.bookService.addBook(this.newBook).pipe(
        timeout(10000),
        catchError(err => {
          this.notificationService.show('Failed to add book (timeout).', 'error');
          return of(null);
        })
      ).subscribe({
        next: (res) => {
          if (res) {
            this.loadBooks();
            this.newBook = this.resetBook();
            this.notificationService.show('Book added successfully!');
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error(err);
          this.notificationService.show('Failed to add book. Access Denied.', 'error');
          this.isLoading = false;
        }
      });
    }
  }

  editBook(book: Book) {
    this.isEditing = true;
    this.editingId = book.id!;
    this.newBook = { ...book };
    this.imagePreview = book.imageUrl;
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.isUploading = true;
      
      const reader = new FileReader();
      reader.onload = (e: any) => this.imagePreview = e.target.result;
      reader.readAsDataURL(file);

      this.bookService.uploadImage(file).subscribe({
        next: (res) => {
          this.newBook.imageUrl = res.url;
          this.isUploading = false;
          this.notificationService.show('Image uploaded successfully!');
        },
        error: (err) => {
          console.error(err);
          this.isUploading = false;
          this.notificationService.show('Upload failed.', 'error');
        }
      });
    }
  }

  deleteBook(id: string) {
    if (confirm('Are you sure you want to delete this book?')) {
      this.bookService.deleteBook(id).subscribe({
        next: () => {
          // If the current page will be empty after delete and it's not page 0, go to previous page
          if (this.books.length <= 1 && this.currentPage > 0) {
            this.loadBooks(this.currentPage - 1);
          } else {
            this.loadBooks(this.currentPage);
          }
          this.notificationService.show('Book deleted.');
        },
        error: () => this.notificationService.show('Delete failed.', 'error')
      });
    }
  }

  cancelEdit() {
    this.isEditing = false;
    this.editingId = '';
    this.newBook = this.resetBook();
    this.imagePreview = null;
  }

  private resetBook(): Book {
    return {
      title: '',
      author: '',
      description: '',
      category: 'Fiction',
      price: 0,
      stock: 0,
      imageUrl: ''
    };
  }

  private checkActiveImport() {
    this.bookService.getActiveImport().subscribe({
      next: (activeJob) => {
        if (activeJob && (activeJob.status === 'STARTING' || activeJob.status === 'IN_PROGRESS')) {
          this.startPolling(activeJob);
        }
      },
      error: (err) => {
        console.warn('Could not check active import:', err);
      }
    });
  }

  private startPolling(initialInfo: ImportProgressInfo) {
    this.stopPolling();
    this.isImporting = true;
    this.activeImportProgress = initialInfo;
    this.importStatus = initialInfo.message || 'Import in progress...';
    this.lastProcessedCount = initialInfo.processed || 0;

    // 1. Progress polling approximately every 1.5s
    this.progressSub = interval(1500).subscribe(() => {
      if (!this.activeImportProgress?.importId) return;

      this.bookService.getImportProgress(this.activeImportProgress.importId).subscribe({
        next: (progress) => {
          if (progress) {
            this.activeImportProgress = progress;
            this.importStatus = progress.message;

            if (progress.status === 'COMPLETED' || progress.status === 'FAILED' || progress.status === 'CANCELLED') {
              this.onImportFinished(progress);
            }
          }
        },
        error: (err) => {
          console.error('Error fetching import progress:', err);
        }
      });
    });

    // 2. Live Inventory sync approximately every 2.5s while active
    this.inventorySub = interval(2500).subscribe(() => {
      if (this.activeImportProgress && this.activeImportProgress.processed > this.lastProcessedCount) {
        this.lastProcessedCount = this.activeImportProgress.processed;
        this.bookService.clearCache();
        this.loadBooks();
      }
    });
  }

  onCsvFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    this.stopPolling();
    this.isImporting = true;
    this.lastProcessedCount = 0;

    this.bookService.startBookImport(file).subscribe({
      next: (initialInfo) => {
        this.startPolling(initialInfo);
      },
      error: (err) => {
        console.error(err);
        this.isImporting = false;
        const msg = err.error?.message || 'Bulk import failed to start.';
        this.importStatus = msg;
        this.notificationService.show(msg, 'error');
      }
    });
  }

  private onImportFinished(progress: ImportProgressInfo) {
    this.stopPolling();
    this.isImporting = false;
    this.bookService.notifyBookRefresh();

    if (progress.status === 'COMPLETED') {
      const summary = `Import completed! ${progress.processed} rows processed (${progress.added} added, ${progress.updated} updated, ${progress.failed} failed).`;
      this.notificationService.show(summary, 'success');
    } else {
      this.notificationService.show(`Import failed: ${progress.message}`, 'error');
    }
  }
}
