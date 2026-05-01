import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Prototype } from './prototype';

describe('Prototype', () => {
  let service: Prototype;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Prototype);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
