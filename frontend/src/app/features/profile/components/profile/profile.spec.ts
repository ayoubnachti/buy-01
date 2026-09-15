import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Profile } from './profile';
import { ProfileService } from '../../services/profile.service';
import {
  ProfileResponse,
  ProfileRole,
} from '../../models/profile.model';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  let profileService: {
    getProfile: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
  };

  const mockProfile: ProfileResponse = {
    name: 'Ayoub Nachti',
    email: 'ayoub@gmail.com',
    role: 'CLIENT',
    avatar: null,
  };

  const mockProfileWithAvatar: ProfileResponse = {
    name: 'Ayoub Nachti',
    email: 'ayoub@gmail.com',
    role: 'CLIENT',
    avatar: 'https://example.com/avatar.jpg',
  };

  beforeEach(async () => {
    profileService = {
      getProfile: vi.fn(),
      updateProfile: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        {
          provide: ProfileService,
          useValue: profileService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
  });

  // =========================================================
  // COMPONENT CREATION
  // =========================================================

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  it('should load the profile on initialization', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    expect(profileService.getProfile).toHaveBeenCalledTimes(1);

    expect(component.profile()).toEqual(mockProfile);
    expect(component.role()).toBe('CLIENT');
    expect(component.avatar()).toBeNull();

    expect(component.profileForm.getRawValue()).toEqual({
      name: 'Ayoub Nachti',
      email: 'ayoub@gmail.com',
    });

    expect(component.loading()).toBe(false);
  });

  // =========================================================
  // PROFILE WITHOUT AVATAR
  // =========================================================

  it('should keep avatar as null when the profile has no avatar', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    expect(component.avatar()).toBeNull();
  });

  // =========================================================
  // PROFILE WITH AVATAR
  // =========================================================

  it('should load the avatar when the profile has an avatar', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfileWithAvatar,
      }),
    );

    fixture.detectChanges();

    expect(component.avatar()).toBe(
      'https://example.com/avatar.jpg',
    );
  });

  // =========================================================
  // ROLE
  // =========================================================

  it('should load the user role', () => {
    const sellerProfile: ProfileResponse = {
      ...mockProfile,
      role: 'SELLER',
    };

    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: sellerProfile,
      }),
    );

    fixture.detectChanges();

    expect(component.role()).toBe('SELLER');
  });

  // =========================================================
  // FORM
  // =========================================================

  it('should populate the form with profile data', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    expect(component.nameControl.value).toBe(
      'Ayoub Nachti',
    );

    expect(component.emailControl.value).toBe(
      'ayoub@gmail.com',
    );
  });

  // =========================================================
  // FORM VALIDATION
  // =========================================================

  it('should require a name', () => {
    component.profileForm.controls.name.setValue('');

    expect(component.nameControl.invalid).toBe(true);
    expect(
      component.nameControl.hasError('required'),
    ).toBe(true);
  });

  it('should reject a name shorter than 3 characters', () => {
    component.profileForm.controls.name.setValue('Ab');

    expect(component.nameControl.invalid).toBe(true);
    expect(
      component.nameControl.hasError('minlength'),
    ).toBe(true);
  });

  it('should reject a name longer than 100 characters', () => {
    component.profileForm.controls.name.setValue(
      'A'.repeat(101),
    );

    expect(component.nameControl.invalid).toBe(true);
    expect(
      component.nameControl.hasError('maxlength'),
    ).toBe(true);
  });

  it('should require an email', () => {
    component.profileForm.controls.email.setValue('');

    expect(component.emailControl.invalid).toBe(true);
    expect(
      component.emailControl.hasError('required'),
    ).toBe(true);
  });

  it('should reject an invalid email', () => {
    component.profileForm.controls.email.setValue(
      'invalid-email',
    );

    expect(component.emailControl.invalid).toBe(true);
    expect(
      component.emailControl.hasError('email'),
    ).toBe(true);
  });

  it('should accept a valid form', () => {
    component.profileForm.setValue({
      name: 'Ayoub Nachti',
      email: 'ayoub@gmail.com',
    });

    expect(component.profileForm.valid).toBe(true);
  });

  // =========================================================
  // AVATAR UPLOAD
  // =========================================================

  it('should update avatar when an image is uploaded', () => {
    component.onAvatarUploaded(
      'https://example.com/new-avatar.jpg',
    );

    expect(component.avatar()).toBe(
      'https://example.com/new-avatar.jpg',
    );

    expect(component.successMessage()).toBe(
      'Profile picture uploaded successfully.',
    );

    expect(component.errorMessage()).toBe('');
  });

  // =========================================================
  // SAVE - NO CHANGES
  // =========================================================

  it('should not update the profile when there are no changes', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    component.saveProfile();

    expect(profileService.updateProfile).not.toHaveBeenCalled();

    expect(component.errorMessage()).toBe(
      'No changes were made to your profile.',
    );
  });

  // =========================================================
  // SAVE - INVALID FORM
  // =========================================================

  it('should not update the profile when the form is invalid', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    component.profileForm.controls.name.setValue('');

    component.saveProfile();

    expect(profileService.updateProfile).not.toHaveBeenCalled();

    expect(component.errorMessage()).toBe(
      'Please fix the errors below.',
    );

    expect(component.nameControl.touched).toBe(true);
  });

  // =========================================================
  // SAVE - SUCCESS
  // =========================================================

  it('should update the profile successfully', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    const updatedProfile: ProfileResponse = {
      name: 'Ayoub Updated',
      email: 'updated@gmail.com',
      role: 'CLIENT',
      avatar: 'https://example.com/avatar.jpg',
    };

    profileService.updateProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile updated successfully',
        data: updatedProfile,
      }),
    );

    fixture.detectChanges();

    component.profileForm.setValue({
      name: 'Ayoub Updated',
      email: 'updated@gmail.com',
    });

    component.avatar.set(
      'https://example.com/avatar.jpg',
    );

    component.saveProfile();

    expect(profileService.updateProfile).toHaveBeenCalledTimes(1);

    expect(profileService.updateProfile).toHaveBeenCalledWith({
      name: 'Ayoub Updated',
      email: 'updated@gmail.com',
      avatar: 'https://example.com/avatar.jpg',
    });

    expect(component.profile()).toEqual(
      updatedProfile,
    );

    expect(component.originalProfile()).toEqual(
      updatedProfile,
    );

    expect(component.avatar()).toBe(
      'https://example.com/avatar.jpg',
    );

    expect(component.saving()).toBe(false);

    expect(component.successMessage()).toBe(
      'Your profile has been updated successfully.',
    );
  });

  // =========================================================
  // SAVE - TRIM
  // =========================================================

  it('should trim name and email before updating', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    profileService.updateProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile updated successfully',
        data: {
          ...mockProfile,
          name: 'New Name',
          email: 'new@gmail.com',
        },
      }),
    );

    fixture.detectChanges();

    component.profileForm.setValue({
      name: '  New Name  ',
      email: '  new@gmail.com  ',
    });

    component.saveProfile();

    expect(profileService.updateProfile).toHaveBeenCalledWith({
      name: 'New Name',
      email: 'new@gmail.com',
      avatar: null,
    });
  });

  // =========================================================
  // SAVE - ERROR
  // =========================================================

  it('should handle update profile errors', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    profileService.updateProfile.mockReturnValue(
      throwError(() => ({
        error: {
          message: 'Email already exists',
        },
      })),
    );

    fixture.detectChanges();

    component.profileForm.controls.name.setValue(
      'New Name',
    );

    component.saveProfile();

    expect(component.saving()).toBe(false);

    expect(component.errorMessage()).toBe(
      'Email already exists',
    );
  });

  // =========================================================
  // DOUBLE SAVE PROTECTION
  // =========================================================

  it('should not save while another save is in progress', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    component.saving.set(true);

    component.saveProfile();

    expect(profileService.updateProfile).not.toHaveBeenCalled();
  });

  // =========================================================
  // CANCEL
  // =========================================================

  it('should restore original profile values when cancel is clicked', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    // Make changes
    component.profileForm.setValue({
      name: 'Changed Name',
      email: 'changed@gmail.com',
    });

    component.avatar.set(
      'https://example.com/changed.jpg',
    );

    expect(component.profileForm.getRawValue()).toEqual({
      name: 'Changed Name',
      email: 'changed@gmail.com',
    });

    // Cancel
    component.cancelChanges();

    expect(component.profileForm.getRawValue()).toEqual({
      name: 'Ayoub Nachti',
      email: 'ayoub@gmail.com',
    });

    expect(component.avatar()).toBeNull();
    expect(component.role()).toBe('CLIENT');

    expect(component.profileForm.pristine).toBe(true);
    expect(component.profileForm.untouched).toBe(true);
  });

  // =========================================================
  // CANCEL - AVATAR
  // =========================================================

  it('should restore the original avatar when cancel is clicked', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfileWithAvatar,
      }),
    );

    fixture.detectChanges();

    component.avatar.set(
      'https://example.com/changed.jpg',
    );

    component.cancelChanges();

    expect(component.avatar()).toBe(
      'https://example.com/avatar.jpg',
    );
  });

  // =========================================================
  // CANCEL - NO CHANGES
  // =========================================================

  it('should do nothing when cancel is clicked without changes', () => {
    profileService.getProfile.mockReturnValue(
      of({
        success: true,
        message: 'Profile retrieved successfully',
        data: mockProfile,
      }),
    );

    fixture.detectChanges();

    const originalProfile = component.profile();
    const originalAvatar = component.avatar();

    component.cancelChanges();

    expect(component.profile()).toEqual(
      originalProfile,
    );

    expect(component.avatar()).toBe(
      originalAvatar,
    );

    expect(component.errorMessage()).toBe('');
    expect(component.successMessage()).toBe('');
  });

  // =========================================================
  // LOAD ERROR
  // =========================================================

  it('should handle profile loading errors', () => {
    profileService.getProfile.mockReturnValue(
      throwError(() => ({
        error: {
          message: 'Unauthorized',
        },
      })),
    );

    fixture.detectChanges();

    expect(component.loading()).toBe(false);

    expect(component.errorMessage()).toBe(
      'Unauthorized',
    );
  });

  // =========================================================
  // LOAD ERROR - DEFAULT MESSAGE
  // =========================================================

  it('should use the default message when profile loading fails without a server message', () => {
    profileService.getProfile.mockReturnValue(
      throwError(() => ({})),
    );

    fixture.detectChanges();

    expect(component.loading()).toBe(false);

    expect(component.errorMessage()).toBe(
      'Failed to load your profile. Please try again.',
    );
  });
});
