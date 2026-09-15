import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  ProfileResponse,
  UpdateProfileRequest
} from '../../models/profile.model';

import { ProfileService } from '../../services/profile.service';

// import { ImageUploadComponent } from '../../../shared/components/image-upload/image-upload.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    FormsModule,
    // ImageUploadComponent
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {

  private readonly profileService = inject(ProfileService);

  profile: ProfileResponse = {
    name: '',
    email: '',
    role: 'CLIENT',
    avatar: null
  };

  loading = signal(true);
  saving = false;

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadProfile();
  }

  /**
   * Load the current user's profile
   */
  loadProfile(): void {
    this.loading.set(true);
    this.errorMessage = '';

    this.profileService.getProfile().subscribe({
      next: (res) => {
        this.profile = res.data;
        this.loading.set(false);
      },

      error: (error) => {
        console.error('Failed to load profile:', error);

        this.errorMessage =
          error?.error?.message ||
          'Failed to load your profile. Please try again.';

        this.loading.set(false);
      }
    });
  }

  /**
   * Called when the image upload component
   * successfully uploads an image.
   */
  onAvatarUploaded(imageUrl: string): void {
    this.profile.avatar = imageUrl;

    this.successMessage = 'Profile picture uploaded successfully.';
    this.errorMessage = '';
  }

  /**
   * Save profile changes
   */
  saveProfile(): void {
    if (this.saving) {
      return;
    }

    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const data: UpdateProfileRequest = {
      name: this.profile.name,
      email: this.profile.email,
      avatar: this.profile.avatar
    };

    this.profileService.updateProfile(data).subscribe({
      next: (res) => {
        this.profile = res.data;

        this.successMessage =
          'Your profile has been updated successfully.';

        this.saving = false;
      },

      error: (error) => {
        console.error('Failed to update profile:', error);

        this.errorMessage =
          error?.error?.message ||
          'Failed to update your profile. Please try again.';

        this.saving = false;
      }
    });
  }

  /**
   * Reset the form to the current server data.
   *
   * For now this simply reloads the profile.
   */
  cancelChanges(): void {
    this.successMessage = '';
    this.errorMessage = '';

    this.loadProfile();
  }
}