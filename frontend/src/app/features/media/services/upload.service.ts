import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiResponse, MediaRequest } from '../models/media.model';

@Injectable({
  providedIn: 'root',
})
export class UploadService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080';

  updateMedia(request: MediaRequest, images: File[]): Observable<ApiResponse<string[]>> {
    const formData = new FormData();

    formData.append('data', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    images.forEach((image) => formData.append('images', image));

    return this.http.put<ApiResponse<string[]>>(`${this.apiUrl}/media/images`, formData);
  }
}
