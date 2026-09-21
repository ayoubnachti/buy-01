import { beforeEach, describe, expect, it } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SellerDashboard } from './seller-dashboard';

describe('SellerDashboard', () => {
  let fixture: ComponentFixture<SellerDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SellerDashboard],
    }).compileComponents();
    fixture = TestBed.createComponent(SellerDashboard);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should start with the mock product', () => {
    expect(fixture.componentInstance.products()).toHaveLength(1);
    expect(fixture.componentInstance.products()[0].name).toBe('chair');
  });

  it('should clear editingProduct and bump focusTrigger on create', () => {
    const component = fixture.componentInstance;
    component.onEditClick(component.products()[0]); // set some editing state first
    const before = component.focusTrigger();

    component.onCreateClick();

    expect(component.editingProduct()).toBeNull();
    expect(component.focusTrigger()).toBeGreaterThan(before);
  });

  it('should set editingProduct and bump focusTrigger on edit', () => {
    const component = fixture.componentInstance;
    const product = component.products()[0];
    const before = component.focusTrigger();

    component.onEditClick(product);

    expect(component.editingProduct()).toEqual(product);
    expect(component.focusTrigger()).toBeGreaterThan(before);
  });

  it('should remove the product on delete', () => {
    const component = fixture.componentInstance;
    const product = component.products()[0];

    component.onDeleteClick(product);

    expect(component.products()).toHaveLength(0);
  });

  it('should add a new product on save when not editing', () => {
    const component = fixture.componentInstance;

    component.onFormSave({ name: 'table', description: 'desc', price: 50, quantity: 5 });

    expect(component.products()).toHaveLength(2);
    expect(component.products()[1]).toMatchObject({ name: 'table', price: 50 });
  });

  it('should update the existing product on save when editing, and clear editingProduct', () => {
    const component = fixture.componentInstance;
    const product = component.products()[0];

    component.onEditClick(product);
    component.onFormSave({ name: 'renamed chair', description: 'desc', price: 99, quantity: 1 });

    expect(component.products()).toHaveLength(1);
    expect(component.products()[0]).toMatchObject({ id: product.id, name: 'renamed chair', price: 99 });
    expect(component.editingProduct()).toBeNull();
  });
});