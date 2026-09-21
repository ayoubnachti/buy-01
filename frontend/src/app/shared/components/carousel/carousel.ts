import { Component, computed, input, signal } from '@angular/core';

// Placeholder images until the real product image lookup is wired up.
// Pass `[images]` once that's ready — these are only the default.
const PLACEHOLDER_IMAGES: string[] = [
  'https://res.cloudinary.com/dagr8zpdx/image/upload/v1790009452/products/6ab16066c860e7c0eda2c065/en78vi6p7nszdidml30g.jpg',
  'https://res.cloudinary.com/dagr8zpdx/image/upload/v1790009450/products/6ab16066c860e7c0eda2c065/ib9mcwvovbni3wysv2yv.png',
  'https://res.cloudinary.com/dagr8zpdx/image/upload/v1777812326/step2_mphxkn.png',
];

@Component({
  selector: 'app-carousel',
  standalone: true,
  templateUrl: './carousel.html',
  styleUrl: './carousel.css',
})
export class Carousel {
  images = input<string[]>(PLACEHOLDER_IMAGES);
  alt = input('Product image');
  height = input('180px');

  readonly currentIndex = signal(0);

  readonly slides = computed(() => (this.images().length ? this.images() : PLACEHOLDER_IMAGES));
  readonly hasMultiple = computed(() => this.slides().length > 1);

  readonly activeImage = computed(() => {
    const slides = this.slides();

    if (!slides.length) {
      return null;
    }

    const index = ((this.currentIndex() % slides.length) + slides.length) % slides.length;
    return slides[index];
  });

  next(): void {
    const total = this.slides().length;
    this.currentIndex.update((i) => (i + 1) % total);
  }

  prev(): void {
    const total = this.slides().length;
    this.currentIndex.update((i) => (i - 1 + total) % total);
  }

  goTo(index: number): void {
    this.currentIndex.set(index);
  }
}
