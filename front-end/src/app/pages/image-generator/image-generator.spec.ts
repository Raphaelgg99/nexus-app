import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ImageGenerator } from './image-generator';

describe('ImageGenerator', () => {
  let component: ImageGenerator;
  let fixture: ComponentFixture<ImageGenerator>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageGenerator],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImageGenerator);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
