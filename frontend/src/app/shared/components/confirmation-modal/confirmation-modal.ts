import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  templateUrl: './confirmation-modal.html',
})
export class ConfirmationModal {
  title = input.required<string>();
  message = input.required<string>();
  confirmLabel = input('Confirm');
  cancelLabel = input('Cancel');
  busy = input(false);

  confirmed = output<void>();
  cancelled = output<void>();

  onConfirm(): void {
    if (!this.busy()) {
      this.confirmed.emit();
    }
  }

  onDismiss(): void {
    if (!this.busy()) {
      this.cancelled.emit();
    }
  }
}