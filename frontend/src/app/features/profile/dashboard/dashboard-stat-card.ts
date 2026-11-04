import { Component, input } from '@angular/core';

@Component({
  selector: 'app-dashboard-stat-card',
  standalone: true,
  template: `
    <article class="card stat-card border-0 shadow-sm rounded-4 h-100">
      <div class="card-body p-4">
        <p class="text-body-secondary fw-semibold mb-2">{{ label() }}</p>
        <p class="stat-value mb-0">{{ value() }}</p>
      </div>
    </article>
  `,
})
export class DashboardStatCard {
  readonly label = input.required<string>();
  readonly value = input.required<string | number | null>();
}
