import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';

import { SellerDashboard } from './seller-dashboard';
import { Product } from '../../shared/models/product.model';
import { ProductService } from '../../core/services/product.service';
import { ToastService } from '../../core/services/toast.service';

const mockProduct: Product = {
  id: '1212',
  name: 'chair',
  description: 'A sturdy wooden chair.',
  price: 21,
  quantity: 20,
};

describe('SellerDashboard', () => {
  let productService: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let toastService: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    productService = {
      getAll: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    };

    toastService = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SellerDashboard],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();

    productService.getAll.mockReturnValue(of([mockProduct]));
  });

  function createFixture(): ComponentFixture<SellerDashboard> {
    return TestBed.createComponent(SellerDashboard);
  }

  it('should create', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  // --------------------------------------------------
  // Fetching on init
  // --------------------------------------------------

  it('should fetch products on construction and turn off loading', () => {
    const fixture = createFixture();
    fixture.detectChanges();

    expect(productService.getAll).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.products()).toEqual([mockProduct]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('should be loading before the fetch resolves', () => {
    const subject = new Subject<Product[]>();
    productService.getAll.mockReturnValue(subject.asObservable());

    const fixture = createFixture();

    expect(fixture.componentInstance.loading()).toBe(true);

    subject.next([mockProduct]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('should show an error toast and turn off loading when the fetch fails', () => {
    productService.getAll.mockReturnValue(throwError(() => new Error('network error')));

    const fixture = createFixture();
    fixture.detectChanges();

    expect(toastService.error).toHaveBeenCalledWith('Could not load your products. Try refreshing.');
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  // --------------------------------------------------
  // Create / edit focus state
  // --------------------------------------------------

  it('should clear editingProduct and bump focusTrigger on create', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.onEditClick(mockProduct); 
    const before = component.focusTrigger();

    component.onCreateClick();

    expect(component.editingProduct()).toBeNull();
    expect(component.focusTrigger()).toBeGreaterThan(before);
  });

  it('should set editingProduct and bump focusTrigger on edit', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const before = component.focusTrigger();

    component.onEditClick(mockProduct);

    expect(component.editingProduct()).toEqual(mockProduct);
    expect(component.focusTrigger()).toBeGreaterThan(before);
  });

  // --------------------------------------------------
  // Delete confirmation flow
  // --------------------------------------------------

  it('should set productPendingDelete on delete click, without calling the service yet', () => {
    const fixture = createFixture();
    fixture.detectChanges();

    fixture.componentInstance.onDeleteClick(mockProduct);

    expect(fixture.componentInstance.productPendingDelete()).toEqual(mockProduct);
    expect(productService.delete).not.toHaveBeenCalled();
  });

  it('should clear productPendingDelete on cancel, without calling the service', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.onDeleteClick(mockProduct);
    component.onCancelDelete();

    expect(component.productPendingDelete()).toBeNull();
    expect(productService.delete).not.toHaveBeenCalled();
  });

  it('should remove the product, show a success toast, and clear productPendingDelete when confirmed delete succeeds', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    productService.delete.mockReturnValue(of(undefined));

    component.onDeleteClick(mockProduct);
    component.onConfirmDelete();

    expect(productService.delete).toHaveBeenCalledWith(mockProduct.id);
    expect(component.products()).toEqual([]);
    expect(toastService.success).toHaveBeenCalledWith('Product deleted.');
    expect(component.productPendingDelete()).toBeNull();
  });

  it('should show an error toast, keep the product, and clear productPendingDelete when confirmed delete fails', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    productService.delete.mockReturnValue(throwError(() => new Error('network error')));

    component.onDeleteClick(mockProduct);
    component.onConfirmDelete();

    expect(component.products()).toEqual([mockProduct]);
    expect(toastService.error).toHaveBeenCalledWith('Could not delete this product. Try again.');
    expect(component.productPendingDelete()).toBeNull();
  });

  it('should do nothing if onConfirmDelete is somehow called with no product pending', () => {
    const fixture = createFixture();
    fixture.detectChanges();

    fixture.componentInstance.onConfirmDelete();

    expect(productService.delete).not.toHaveBeenCalled();
  });

  // --------------------------------------------------
  // Save (create / update)
  // --------------------------------------------------

  it('should call create and append the returned product when not editing', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const before = component.focusTrigger();
    const newProduct: Product = {
      id: '3',
      name: 'table',
      description: 'desc',
      price: 50,
      quantity: 5,
    };
    productService.create.mockReturnValue(of(newProduct));

    component.onFormSave({
      name: 'table',
      description: 'desc',
      price: 50,
      quantity: 5,
    });

    expect(productService.create).toHaveBeenCalledWith({
      name: 'table',
      description: 'desc',
      price: 50,
      quantity: 5,
    });
    expect(component.products()).toEqual([mockProduct, newProduct]);
    expect(toastService.success).toHaveBeenCalledWith('Product created.');
    // Form should reset after a successful save so a duplicate accidental
    // submit can't re-post the same data.
    expect(component.focusTrigger()).toBeGreaterThan(before);
  });

  it('should call update and replace the edited product when editing, then clear editingProduct and reset focusTrigger', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const updated: Product = { ...mockProduct, name: 'renamed chair', price: 99 };
    productService.update.mockReturnValue(of(updated));

    component.onEditClick(mockProduct);
    const before = component.focusTrigger();

    component.onFormSave({
      name: 'renamed chair',
      description: mockProduct.description,
      price: 99,
      quantity: mockProduct.quantity,
    });

    expect(productService.update).toHaveBeenCalledWith(mockProduct.id, {
      name: 'renamed chair',
      description: mockProduct.description,
      price: 99,
      quantity: mockProduct.quantity,
    });
    expect(component.products()).toEqual([updated]);
    expect(component.editingProduct()).toBeNull();
    expect(component.focusTrigger()).toBeGreaterThan(before);
    expect(toastService.success).toHaveBeenCalledWith('Product updated.');
  });

  it('should show an error toast when create fails, without touching the product list', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    productService.create.mockReturnValue(throwError(() => new Error('network error')));

    fixture.componentInstance.onFormSave({
      name: 'table',
      description: 'desc',
      price: 50,
      quantity: 5,
    });

    expect(fixture.componentInstance.products()).toEqual([mockProduct]);
    expect(toastService.error).toHaveBeenCalledWith('Could not create this product.');
  });

  it('should show an error toast when update fails, without clearing editingProduct', () => {
    const fixture = createFixture();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    productService.update.mockReturnValue(throwError(() => new Error('network error')));

    component.onEditClick(mockProduct);
    component.onFormSave({
      name: 'renamed chair',
      description: mockProduct.description,
      price: 99,
      quantity: mockProduct.quantity,
    });

    expect(component.editingProduct()).toEqual(mockProduct);
    expect(toastService.error).toHaveBeenCalledWith('Could not update this product.');
  });
});