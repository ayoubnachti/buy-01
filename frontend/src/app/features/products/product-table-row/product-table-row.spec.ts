import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductTableRow } from './product-table-row';

describe('ProductTableRow', () => {
  let component: ProductTableRow;
  let fixture: ComponentFixture<ProductTableRow>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductTableRow],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductTableRow);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
