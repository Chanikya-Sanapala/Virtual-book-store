import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { BookDetailComponent } from '../../components/book-detail/book-detail.component';

const routes: Routes = [
  { path: '', component: BookDetailComponent }
];

@NgModule({
  declarations: [
    BookDetailComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class BookDetailModule { }
