

package com.nust.socialapp.main.base;

import android.support.annotation.StringRes;
import android.view.View;


public interface BaseFragmentView extends BaseView {

    void showProgress();

    void showProgress(int message);

    void hideProgress();

    void showSnackBar(String message);

    void showSnackBar(int message);

    void showSnackBar(View view, int messageId);

    void showToast(@StringRes int messageId);

    void showToast(String message);

    void showWarningDialog(int messageId);

    void showWarningDialog(String message);

    boolean hasInternetConnection();

    void startLoginActivity();
}
