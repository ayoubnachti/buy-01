import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-system-design',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './system-design.html',
})
export class SystemDesign {
  colors = [
    { name: '--color-bg', hex: '#fafaf9' },
    { name: '--color-text', hex: '#1a1a18' },
    { name: '--color-accent', hex: '#0f6e56' },
    { name: '--color-accent-hover', hex: '#085041' },
    { name: '--color-border', hex: '#e5e3de' },
  ];
}