import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginPage } from './auth/login-page';
import { TicketsPage } from './tickets/tickets-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tickets' },
  { path: 'login', component: LoginPage },
  { path: 'tickets', component: TicketsPage, canActivate: [authGuard] },
  { path: '**', redirectTo: 'tickets' },
];
