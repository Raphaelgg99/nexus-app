import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  Folder,
  LucideAngularModule,
  Power,
  UserPlus,
  WandSparkles,
} from 'lucide-angular';

import { authInterceptor } from './interceptors/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    importProvidersFrom(
      LucideAngularModule.pick({
            WandSparkles,
            Folder,
            UserPlus,
            Power,
          }),
        ),
  ],
};
