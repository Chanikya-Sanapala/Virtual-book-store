import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private selectedCategorySubject = new BehaviorSubject<string>('');
  selectedCategory$ = this.selectedCategorySubject.asObservable();

  private categoriesSubject = new BehaviorSubject<string[]>(['Fiction', 'Non-Fiction', 'Science', 'Technology', 'Biography', 'Self-Help']);
  categories$ = this.categoriesSubject.asObservable();

  selectCategory(category: string) {
    this.selectedCategorySubject.next(category);
  }

  setCategories(categories: string[]) {
    this.categoriesSubject.next(categories);
  }
}
