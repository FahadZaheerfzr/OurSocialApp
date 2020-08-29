

package com.nust.socialapp.main.login;

import com.google.firebase.auth.AuthCredential;
import com.nust.socialapp.main.base.BaseView;



public interface LoginView extends BaseView {
    void startCreateProfileActivity();

    void signInWithGoogle();

    void signInWithFacebook();

    void setProfilePhotoUrl(String url);

    void firebaseAuthWithCredentials(AuthCredential credential);
}
