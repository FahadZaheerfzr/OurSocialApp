

package com.nust.socialapp.main.editProfile;

import com.nust.socialapp.main.pickImageBase.PickImageView;



public interface EditProfileView extends PickImageView {
    void setName(String username);

    void setProfilePhoto(String photoUrl);

    String getNameText();

    void setNameError(String string);
}
