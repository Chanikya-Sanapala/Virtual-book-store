import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { OrderHistoryComponent } from '../../components/order-history/order-history.component';

const routes: Routes = [
  { path: '', component: OrderHistoryComponent }
];

@NgModule({
  declarations: [
    OrderHistoryComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class OrderHistoryModule { }
