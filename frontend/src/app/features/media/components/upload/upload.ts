import { Component, EventEmitter, Input, Output, signal } from '@angular/core';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class Upload {
  @Input() avatarUrl: string | null = null;
  @Input() name: string | null = null;

  @Output() imageUploaded = new EventEmitter<string>();
  @Output() fileSelected = new EventEmitter<File>();

  private readonly previewUrl = signal<string | null>(null);

  get displayUrl(): string | null {
    return this.previewUrl() ?? this.avatarUrl;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      const result = reader.result as string;
      this.previewUrl.set(result);
      this.imageUploaded.emit(result);
    };

    reader.readAsDataURL(file);

    this.fileSelected.emit(file);

    input.value = '';
  }
}
