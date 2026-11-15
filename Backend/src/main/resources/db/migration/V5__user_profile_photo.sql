-- Profile photo stored as a small (256x256) resized image data URL.
ALTER TABLE bank_users
    ADD COLUMN IF NOT EXISTS profile_photo TEXT;
