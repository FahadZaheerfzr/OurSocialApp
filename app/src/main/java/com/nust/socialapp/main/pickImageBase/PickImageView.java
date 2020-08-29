

package com.nust.socialapp.main.pickImageBase;

import android.net.Uri;

import com.nust.socialapp.main.base.BaseView;



public interface PickImageView extends BaseView {
    void hideLocalProgress();

    void loadImageToImageView(Uri imageUri);
}
