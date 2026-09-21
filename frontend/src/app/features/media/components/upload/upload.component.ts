import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';

import { ImagePreview } from '../../models/image-preview.model';
import { MediaRequest, TargetType } from '../../models/media.model';
import { UploadService } from '../../services/upload.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css',
})
export class Upload {
  // What this upload belongs to, e.g. a profile avatar or a product's gallery
  @Input({ required: true }) targetType!: TargetType;
  @Input({ required: true }) targetId!: string;

  // How many images are allowed in total (1 = single avatar-style picker, >1 = gallery picker)
  @Input() maxFiles = 1;

  // Only used by the single-image (avatar) variant, for the fallback initials
  @Input() name: string | null = null;

  @Input()
  set initialImages(value: string | string[] | null) {
    const urls = value == null ? [] : Array.isArray(value) ? value : [value];

    this.previews.set(urls.filter(Boolean).map((url) => ({ url, file: null })));
    this.originalAvatarUrl = urls[0] ?? null;
  }

  // Confirmed image URLs, emitted only once the backend has accepted the upload
  @Output() imagesSelected = new EventEmitter<string[]>();

  private readonly uploadService = inject(UploadService);

  // The last backend-confirmed avatar url, kept aside so a pending pick
  // (which replaces `previews`) doesn't lose track of what to ask the
  // backend to delete on commit.
  private originalAvatarUrl: string | null = null;

  readonly previews = signal<ImagePreview[]>([]);
  readonly uploading = signal(false);
  readonly errorMessage = signal('');

  readonly isSingle = computed(() => this.maxFiles <= 1);
  readonly canAddMore = computed(() => !this.uploading() && this.previews().length < this.maxFiles);
  readonly hasPendingChanges = computed(() => this.previews().some((preview) => preview.file));

  get avatarUrl(): string | null {
    return this.previews()[0]?.url ?? null;
  }

  onFilesPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];

    input.value = '';

    if (!files.length) {
      return;
    }

    const remaining = this.isSingle() ? 1 : this.maxFiles - this.previews().length;
    const accepted = files.slice(0, remaining);

    this.errorMessage.set(
      files.length > accepted.length ? `You can only upload up to ${this.maxFiles} image(s).` : '',
    );

    if (!accepted.length) {
      return;
    }

    const picked: ImagePreview[] = accepted.map((file) => ({
      url: URL.createObjectURL(file),
      file,
    }));

    this.previews.update((list) => {
      if (this.isSingle()) {
        list.forEach((preview) => this.revokeIfLocal(preview));
        return picked;
      }

      return [...list, ...picked];
    });
  }

  removeImage(index: number): void {
    this.previews.update((list) => {
      const next = [...list];
      const [removed] = next.splice(index, 1);

      if (removed) {
        this.revokeIfLocal(removed);
      }

      return next;
    });
  }

  // Uploads any newly picked files to the media service. The caller (e.g. a
  // profile/product form on submit) should subscribe to this before saving
  // the rest of its data, and must not proceed on error.
  commit(): Observable<string[]> {
    const pendingFiles = this.previews()
      .filter((preview) => preview.file)
      .map((preview) => preview.file as File);

    if (!pendingFiles.length) {
      return of(this.previews().map((preview) => preview.url));
    }

    const oldImagePaths = this.isSingle() && this.originalAvatarUrl ? [this.originalAvatarUrl] : null;

    const request: MediaRequest = {
      targetType: this.targetType,
      targetId: this.targetId,
      oldImagePaths,
    };

    this.uploading.set(true);
    this.errorMessage.set('');

    return this.uploadService.updateMedia(request, pendingFiles).pipe(
      map((res) => {
        const confirmed: ImagePreview[] = res.data.map((url) => ({ url, file: null }));

        this.previews()
          .filter((preview) => preview.file)
          .forEach((preview) => this.revokeIfLocal(preview));

        const stillConfirmed = this.previews().filter((preview) => !preview.file);
        const nextPreviews = this.isSingle() ? confirmed : [...stillConfirmed, ...confirmed];

        this.previews.set(nextPreviews);

        if (this.isSingle()) {
          this.originalAvatarUrl = confirmed[0]?.url ?? null;
        }

        this.uploading.set(false);

        const urls = nextPreviews.map((preview) => preview.url);
        this.imagesSelected.emit(urls);

        return urls;
      }),
      catchError((error) => {
        this.uploading.set(false);
        this.errorMessage.set(
          error?.error?.message || 'Failed to upload image(s). Please try again.',
        );

        return throwError(() => error);
      }),
    );
  }

  // Discards any picked-but-not-yet-committed files, e.g. when the parent
  // form is cancelled. Confirmed (already backend-persisted) images are kept.
  resetPending(): void {
    this.previews()
      .filter((preview) => preview.file)
      .forEach((preview) => this.revokeIfLocal(preview));

    this.previews.set(
      this.isSingle()
        ? this.originalAvatarUrl
          ? [{ url: this.originalAvatarUrl, file: null }]
          : []
        : this.previews().filter((preview) => !preview.file),
    );

    this.errorMessage.set('');
  }

  private revokeIfLocal(preview: ImagePreview): void {
    if (preview.file) {
      URL.revokeObjectURL(preview.url);
    }
  }
}
