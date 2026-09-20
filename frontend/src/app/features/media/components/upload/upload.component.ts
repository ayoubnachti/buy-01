import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';

import { ImagePreview } from '../../models/image-preview.model';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css',
})
export class Upload {
  // How many images are allowed in total (1 = single avatar-style picker, >1 = gallery picker)
  @Input() maxFiles = 1;

  // Only used by the single-image (avatar) variant, for the fallback initials
  @Input() name: string | null = null;

  @Input()
  set initialImages(value: string | string[] | null) {
    const urls = value == null ? [] : Array.isArray(value) ? value : [value];
    this.previews.set(urls.filter(Boolean).map((url) => ({ url, file: null })));
  }

  // Current preview URLs (base64), in order
  @Output() imagesSelected = new EventEmitter<string[]>();

  // Current raw files, in order (only entries that came from a real file pick)
  @Output() filesSelected = new EventEmitter<File[]>();

  readonly previews = signal<ImagePreview[]>([]);
  readonly limitError = signal('');

  readonly isSingle = computed(() => this.maxFiles <= 1);
  readonly canAddMore = computed(() => this.previews().length < this.maxFiles);

  get avatarUrl(): string | null {
    return this.previews()[0]?.url ?? null;
  }

  onFilesPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];

    if (!files.length) {
      return;
    }

    const remaining = this.isSingle() ? 1 : this.maxFiles - this.previews().length;
    const accepted = files.slice(0, remaining);

    this.limitError.set(
      files.length > accepted.length ? `You can only upload up to ${this.maxFiles} image(s).` : '',
    );

    accepted.forEach((file) => this.readFile(file));

    input.value = '';
  }

  removeImage(index: number): void {
    this.previews.update((list) => list.filter((_, i) => i !== index));
    this.limitError.set('');
    this.emitChanges();
  }

  private readFile(file: File): void {
    const reader = new FileReader();

    reader.onload = () => {
      const url = reader.result as string;

      this.previews.update((list) => (this.isSingle() ? [{ url, file }] : [...list, { url, file }]));

      this.emitChanges();
    };

    reader.readAsDataURL(file);
  }

  private emitChanges(): void {
    const list = this.previews();

    this.imagesSelected.emit(list.map((preview) => preview.url));
    this.filesSelected.emit(
      list.filter((preview) => preview.file).map((preview) => preview.file as File),
    );
  }
}
