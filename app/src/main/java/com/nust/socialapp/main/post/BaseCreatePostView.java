

package com.nust.socialapp.main.post;

import android.net.Uri;

import com.nust.socialapp.main.pickImageBase.PickImageView;



public interface BaseCreatePostView extends PickImageView {
    void setDescriptionError(String error);

    void setTitleError(String error);

    String getTitleText();

    String getDescriptionText();

    void requestImageViewFocus();

    void onPostSavedSuccess();

    Uri getImageUri();
}

