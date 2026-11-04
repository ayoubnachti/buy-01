import { Component, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { AnalyticsDateRange } from './analytics-date-range';

@Component({
  selector: 'app-dashboard-date-filter',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="date-filter">
      <label class="form-label small fw-semibold mb-1" [for]="id()">Date range</label>
      <select [id]="id()" class="form-select" [formControl]="control()">
        <option value="30">Last 30 days</option>
        <option value="90">Last 90 days</option>
        <option value="all">All time</option>
      </select>
    </div>
  `,
  styles: `
    .date-filter {
      width: min(100%, 13rem);
    }
  `,
})
export class DashboardDateFilter {
  readonly control = input.required<FormControl<AnalyticsDateRange>>();
  readonly id = input.required<string>();
}
