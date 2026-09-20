import {
  Component,
  ElementRef,
  EventEmitter,
  Output,
  ViewChild,
  effect,
  input,
  signal,
} from '@angular/core';
import { FormField, form, required, submit, validate } from '@angular/forms/signals';
import { Product } from '../../../shared/models/product.model';
import { CreateProductRequest } from '../../../shared/models/create-product-request';

interface ProductFormModel {
  name: string;
  description: string;
  price: number;
  quantity: number;
}

const EMPTY_MODEL: ProductFormModel = { name: '', description: '', price: 0, quantity: 0 };

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [FormField],
  templateUrl: './product-form.html',
})
export class ProductForm {
  editingProduct = input<Product | null>(null);
  focusTrigger = input(0);

  @Output() save = new EventEmitter<CreateProductRequest>();

  @ViewChild('nameInput') private nameInputRef?: ElementRef<HTMLInputElement>;

  private readonly model = signal<ProductFormModel>({ ...EMPTY_MODEL });

  readonly productForm = form(this.model, (schemaPath) => {
    required(schemaPath.name, { message: 'Name is required.' });
    required(schemaPath.description, { message: 'Description is required.' });

    validate(schemaPath.price, (ctx) =>
      ctx.value() > 0 ? null : { kind: 'price', message: 'Price must be greater than 0.' }
    );

    validate(schemaPath.quantity, (ctx) =>
      ctx.value() >= 0 ? null : { kind: 'quantity', message: "Quantity can't be negative." }
    );
  });

  private isFirstRun = true;

  constructor() {
    effect(() => {
      const editing = this.editingProduct();
      this.focusTrigger();

      this.model.set(
        editing
          ? {
              name: editing.name,
              description: editing.description,
              price: editing.price,
              quantity: editing.quantity,
            }
          : { ...EMPTY_MODEL }
      );

      this.productForm().reset();

      if (!this.isFirstRun) {
        queueMicrotask(() => this.nameInputRef?.nativeElement.focus());
      }
      this.isFirstRun = false;
    });
  }

  async onSubmit(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    await submit(this.productForm, async (f) => {
      this.save.emit(f().value());
      return null;
    });
  }
}
