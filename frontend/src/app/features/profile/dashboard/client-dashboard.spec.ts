import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ClientDashboard } from './client-dashboard';
import { UserPurchaseStatsService } from '../services/user-purchase-stats.service';
import { UserPurchaseStats } from '../models/user-purchase-stats.model';

vi.mock('chart.js', () => {
  class ChartMock {
    static register = vi.fn();
    destroy = vi.fn();
  }

  return {
    ArcElement: {},
    BarController: {},
    BarElement: {},
    CategoryScale: {},
    Chart: ChartMock,
    DoughnutController: {},
    Legend: {},
    LinearScale: {},
    Tooltip: {},
  };
});

describe('ClientDashboard', () => {
  let fixture: ComponentFixture<ClientDashboard>;
  let component: ClientDashboard;
  let statsService: { getStats: ReturnType<typeof vi.fn> };

  const mockStats: UserPurchaseStats = {
    totalSpent: 125.5,
    ordersCount: 3,
    mostBought: [
      { productId: 'product-1', name: 'Coffee maker', units: 4, amount: 79.96 },
      { productId: 'product-2', name: 'Tea set', units: 2, amount: 45.54 },
    ],
    topCategories: [{ category: 'HOME', units: 6, amount: 125.5 }],
  };

  function createDashboard(): void {
    fixture = TestBed.createComponent(ClientDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    statsService = {
      getStats: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'Statistics retrieved',
          data: mockStats,
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ClientDashboard],
      providers: [
        provideRouter([]),
        { provide: UserPurchaseStatsService, useValue: statsService },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows totals and accessible chart data from the stats response', () => {
    createDashboard();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('$125.50');
    expect(text).toContain('Orders');
    expect(text).toContain('3');
    expect(text).toContain('Coffee maker');
    expect(text).toContain('HOME');
    expect(fixture.nativeElement.querySelector('canvas[aria-hidden="true"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Most bought products by units');
    expect(fixture.nativeElement.textContent).toContain('Top categories by units');
  });

  it('shows the no-orders state and a link to products when stats are empty', () => {
    statsService.getStats.mockReturnValue(
      of({
        success: true,
        message: 'Statistics retrieved',
        data: { totalSpent: 0, ordersCount: 0, mostBought: [], topCategories: [] },
      }),
    );
    createDashboard();

    expect(fixture.nativeElement.textContent).toContain('No orders yet');
    expect(fixture.nativeElement.querySelector('a[routerLink="/search"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('canvas')).toBeNull();
  });

  it('requests the selected date range and retries after a request error', () => {
    vi.setSystemTime(new Date('2026-11-04T13:00:00Z'));
    createDashboard();

    component.dateRange.setValue('90');
    expect(statsService.getStats).toHaveBeenLastCalledWith('2026-08-07', '2026-11-04');

    statsService.getStats.mockReturnValue(throwError(() => new Error('offline')));
    component.retryLoad();
    fixture.detectChanges();

    expect(component.error()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('couldn’t load your purchase statistics');
  });
});
