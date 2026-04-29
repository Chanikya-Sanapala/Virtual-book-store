import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { SearchService } from '../../services/search.service';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
  standalone: false
})
export class NavbarComponent {
  cartCount = 0;
  isSearchOpen = false;
  isMenuOpen = false;
  isCategoriesOpen = false;
  searchQuery = '';
  categories$;
  selectedCategory$;

  constructor(
    public authService: AuthService, 
    private cartService: CartService,
    private searchService: SearchService,
    private categoryService: CategoryService
  ) {
    this.categories$ = this.categoryService.categories$;
    this.selectedCategory$ = this.categoryService.selectedCategory$;
    
    this.cartService.getCart().subscribe(items => {
      this.cartCount = items.reduce((acc, item) => acc + item.quantity, 0);
    });
  }

  toggleSearch() {
    this.isSearchOpen = !this.isSearchOpen;
    if (this.isSearchOpen) {
      this.isMenuOpen = false;
      setTimeout(() => {
        const input = document.querySelector('.search-input-box input') as HTMLInputElement;
        if (input) input.focus();
      }, 300);
    } else {
      this.searchQuery = '';
      this.searchService.setSearchQuery('');
    }
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
    if (this.isMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'auto';
    }
  }

  closeMenu() {
    this.isMenuOpen = false;
    document.body.style.overflow = 'auto';
  }

  onSearchChange() {
    this.searchService.setSearchQuery(this.searchQuery);
    if (this.searchQuery) {
      this.categoryService.selectCategory(''); // Clear category when searching
    }
  }

  toggleCategories() {
    this.isCategoriesOpen = !this.isCategoriesOpen;
  }

  selectCategory(category: string) {
    this.categoryService.selectCategory(category);
    this.isCategoriesOpen = false;
    this.closeMenu();
  }

  logout() {
    this.authService.logout();
  }
}
