import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './guards/auth.guard';
import { ImageGenerator } from './pages/image-generator/image-generator';
import { Login } from './pages/login/login';
import { UserManagement } from './pages/user-management/user-management';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'dashboard', component: ImageGenerator, canActivate: [authGuard] },
  { path: 'admin/usuarios', component: UserManagement, canActivate: [adminGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' },
];
