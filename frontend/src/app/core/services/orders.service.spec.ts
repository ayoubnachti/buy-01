import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { OrdersService } from './orders.service';
import { environment } from '../../../environments/environment.prod';
import { Order } from '../../shared/models/order.model';
import { OrderPageResponse } from '../../shared/models/order-page-response';
import { CreateOrderRequest } from '../../shared/models/create-order-request';

describe('OrdersService', () => {
  let service: OrdersService;
  let http: HttpTestingController;

  const url = `${environment.apiUrl}/orders`;
  const order = { id: 'o1', status: 'PENDING' } as Order;
  const page = {
    content: [order],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
  } as unknown as OrderPageResponse;

  /** The backend wraps every success in ResponseData. */
  const wrapped = <T>(data: T) => ({ success: true, message: 'ok', data });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrdersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- create (checkout)

  it('create: POST /orders with the request, returns one order per seller', () => {
    const request: CreateOrderRequest = {
      shippingAddress: {
        fullName: 'A',
        phone: '1',
        street: 's',
        city: 'c',
        postalCode: '1',
        country: 'M',
      },
      items: [{ productId: 'p1', quantity: 1 }],
    };
    let result: Order[] | undefined;
    service.create(request).subscribe((orders) => (result = orders));

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(wrapped([order, { ...order, id: 'o2' }]), { status: 201, statusText: 'Created' });

    expect(result?.map((o) => o.id)).toEqual(['o1', 'o2']);
  });

  it('create: passes errors on (for example 409 out of stock)', () => {
    let status: number | undefined;
    service
      .create({ shippingAddress: {} as never, items: [] })
      .subscribe({ error: (e) => (status = e.status) });

    http
      .expectOne(url)
      .flush(
        { success: false, message: 'Only 1 left', data: null },
        { status: 409, statusText: 'Conflict' },
      );

    expect(status).toBe(409);
  });

  // ---- lists

  it('getMy: GET /orders/my with page and size', () => {
    let result: OrderPageResponse | undefined;
    service.getMy(2, 10).subscribe((p) => (result = p));

    const req = http.expectOne((r) => r.url === `${url}/my`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(wrapped(page));

    expect(result).toEqual(page);
  });

  it('getSellerOrders: GET /orders/seller with page and size', () => {
    service.getSellerOrders(0, 20).subscribe();

    const req = http.expectOne((r) => r.url === `${url}/seller`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(wrapped(page));
  });

  it('getMy: adds status/from/to/q when given, omits them otherwise', () => {
    service.getMy(0, 10, { status: 'SHIPPED', from: '2026-01-01', to: '2026-02-01', q: 'shoes' }).subscribe();

    const req = http.expectOne((r) => r.url === `${url}/my`);
    expect(req.request.params.get('status')).toBe('SHIPPED');
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.get('to')).toBe('2026-02-01');
    expect(req.request.params.get('q')).toBe('shoes');
    req.flush(wrapped(page));

    service.getMy(0, 10).subscribe();
    const noFilters = http.expectOne((r) => r.url === `${url}/my`);
    expect(noFilters.request.params.has('status')).toBe(false);
    expect(noFilters.request.params.has('from')).toBe(false);
    expect(noFilters.request.params.has('to')).toBe(false);
    expect(noFilters.request.params.has('q')).toBe(false);
    noFilters.flush(wrapped(page));
  });

  it('getSellerOrders: adds status/from/to/q when given', () => {
    service.getSellerOrders(0, 10, { status: 'PENDING', q: 'mug' }).subscribe();

    const req = http.expectOne((r) => r.url === `${url}/seller`);
    expect(req.request.params.get('status')).toBe('PENDING');
    expect(req.request.params.get('q')).toBe('mug');
    expect(req.request.params.has('from')).toBe(false);
    req.flush(wrapped(page));
  });

  // ---- one order

  it('getOne: GET /orders/{id}', () => {
    let result: Order | undefined;
    service.getOne('o1').subscribe((o) => (result = o));

    const req = http.expectOne(`${url}/o1`);
    expect(req.request.method).toBe('GET');
    req.flush(wrapped(order));

    expect(result).toEqual(order);
  });

  it('cancel: POST /orders/{id}/cancel with an empty body', () => {
    service.cancel('o1').subscribe();

    const req = http.expectOne(`${url}/o1/cancel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(wrapped({ ...order, status: 'CANCELLED' }));
  });

  it('updateStatus: PATCH /orders/{id}/status with the new status', () => {
    service.updateStatus('o1', 'CONFIRMED').subscribe();

    const req = http.expectOne(`${url}/o1/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'CONFIRMED' });
    req.flush(wrapped({ ...order, status: 'CONFIRMED' }));
  });
});
