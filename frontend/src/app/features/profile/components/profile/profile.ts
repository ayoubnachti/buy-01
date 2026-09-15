import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ProfileResponse, ProfileRole, UpdateProfileRequest } from '../../models/profile.model';

import { ProfileService } from '../../services/profile.service';

// import { ImageUploadComponent } from '../../../shared/components/image-upload/image-upload.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    // ImageUploadComponent
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);

  // ─────────────────────────────────────────────
  // FORM
  // ─────────────────────────────────────────────

  readonly profileForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],

    email: ['', [Validators.required, Validators.email]],
  });

  // ─────────────────────────────────────────────
  // PROFILE STATE
  // ─────────────────────────────────────────────

  readonly profile = signal<ProfileResponse | null>(null);

  readonly role = signal<ProfileRole>('CLIENT');

  readonly avatar = signal<string | null>(null);

  // ─────────────────────────────────────────────
  // ORIGINAL PROFILE
  // ─────────────────────────────────────────────

  readonly originalProfile = signal<ProfileResponse | null>(null);

  // ─────────────────────────────────────────────
  // UI STATE
  // ─────────────────────────────────────────────

  readonly loading = signal(true);

  readonly saving = signal(false);

  readonly successMessage = signal('');

  readonly errorMessage = signal('');

  // ─────────────────────────────────────────────
  // FORM CONTROLS
  // ─────────────────────────────────────────────

  get nameControl() {
    return this.profileForm.controls.name;
  }

  get emailControl() {
    return this.profileForm.controls.email;
  }

  // ─────────────────────────────────────────────
  // LIFECYCLE
  // ─────────────────────────────────────────────

  ngOnInit(): void {
    console.log('🔥 PROFILE COMPONENT CREATED');

    this.loadProfile();
  }

  // ─────────────────────────────────────────────
  // LOAD PROFILE
  // ─────────────────────────────────────────────

  loadProfile(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.profileService.getProfile().subscribe({
      next: (res) => {
        this.applyProfile(res.data);

        this.loading.set(false);
      },

      error: (error) => {
        console.error('Failed to load profile:', error);

        this.errorMessage.set(
          error?.error?.message || 'Failed to load your profile. Please try again.',
        );

        this.loading.set(false);
      },
    });
  }

  // ─────────────────────────────────────────────
  // APPLY PROFILE
  // ─────────────────────────────────────────────

  private applyProfile(data: ProfileResponse): void {
    // Main profile signal
    this.profile.set(data);

    // Individual signals
    this.role.set(data.role);

    this.avatar.set(data.avatar);

    // Snapshot for cancel / change detection
    this.originalProfile.set({
      ...data,
    });

    // Form
    this.profileForm.setValue({
      name: data.name,
      email: data.email,
    });

    this.profileForm.markAsPristine();
    this.profileForm.markAsUntouched();
  }

  // ─────────────────────────────────────────────
  // AVATAR
  // ─────────────────────────────────────────────

  onAvatarUploaded(imageUrl: string): void {
    this.avatar.set(imageUrl);

    this.successMessage.set('Profile picture uploaded successfully.');

    this.errorMessage.set('');
  }

  // ─────────────────────────────────────────────
  // CHECK CHANGES
  // ─────────────────────────────────────────────

  private hasChanges(): boolean {
    const original = this.originalProfile();

    if (!original) {
      return false;
    }

    const { name, email } = this.profileForm.getRawValue();

    return name !== original.name || email !== original.email || this.avatar() !== original.avatar;
  }

  // ─────────────────────────────────────────────
  // SAVE
  // ─────────────────────────────────────────────

  saveProfile(): void {
    if (this.saving()) {
      return;
    }

    this.successMessage.set('');
    this.errorMessage.set('');

    // Validation
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();

      this.errorMessage.set('Please fix the errors below.');

      return;
    }

    // No changes
    if (!this.hasChanges()) {
      this.errorMessage.set('No changes were made to your profile.');

      return;
    }

    this.saving.set(true);

    const { name, email } = this.profileForm.getRawValue();

    const data: UpdateProfileRequest = {
      name: name.trim(),

      email: email.trim(),

      avatar: this.avatar(),
    };

    this.profileService.updateProfile(data).subscribe({
      next: (res) => {
        this.applyProfile(res.data);

        this.successMessage.set('Your profile has been updated successfully.');

        this.saving.set(false);
      },

      error: (error) => {
        console.error('Failed to update profile:', error);

        this.errorMessage.set(
          error?.error?.message || 'Failed to update your profile. Please try again.',
        );

        this.saving.set(false);
      },
    });
  }

  // ─────────────────────────────────────────────
  // CANCEL
  // ─────────────────────────────────────────────

  cancelChanges(): void {
    console.log('CANCEL CLICKED');

    const original = this.originalProfile();

    if (!original) {
      return;
    }

    if (!this.hasChanges()) {
      console.log('NO CHANGES');
      return;
    }

    console.log('RESTORING CHANGES');

    this.successMessage.set('');
    this.errorMessage.set('');

    this.profileForm.reset({
      name: original.name,
      email: original.email,
    });

    this.avatar.set(original.avatar);

    this.role.set(original.role);

    this.profileForm.markAsPristine();
    this.profileForm.markAsUntouched();
  }
}
