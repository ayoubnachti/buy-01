import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-dashboard-content-state',
  standalone: true,
  template: `
    @if (loading()) {
      <section class="row g-3 mb-4" [attr.aria-label]="loadingLabel()">
        @for (card of skeletonCards(); track card) {
          <div class="col-12 col-sm-6 col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 p-4 placeholder-glow">
              <span class="placeholder col-5 mb-3"></span>
              <span class="placeholder col-7 placeholder-lg"></span>
            </div>
          </div>
        }
        <div class="col-12">
          <div class="card border-0 shadow-sm rounded-4 p-4 placeholder-glow chart-skeleton">
            <span class="placeholder col-4 mb-3"></span>
            <span class="placeholder col-12"></span>
          </div>
        </div>
      </section>
    } @else if (error()) {
      <div
        class="alert alert-danger d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3"
        role="alert"
      >
        <span>{{ errorMessage() }}</span>
        <button type="button" class="btn btn-outline-danger" (click)="retry.emit()">
          Try again
        </button>
      </div>
    } @else {
      <ng-content />
    }
  `,
})
export class DashboardContentState {
  readonly loading = input.required<boolean>();
  readonly error = input.required<boolean>();
  readonly loadingLabel = input.required<string>();
  readonly errorMessage = input.required<string>();
  readonly skeletonCards = input<readonly number[]>([1, 2, 3]);
  readonly retry = output<void>();
}
