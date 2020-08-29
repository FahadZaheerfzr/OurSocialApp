

package com.nust.socialapp.main.usersList;

import android.support.annotation.StringRes;

import com.nust.socialapp.main.base.BaseView;

import java.util.List;



public interface UsersListView extends BaseView {

    void onProfilesIdsListLoaded(List<String> list);

    void showLocalProgress();

    void hideLocalProgress();

    void setTitle(@StringRes int title);

    void showEmptyListMessage(String message);

    void hideEmptyListMessage();

    void updateSelectedItem();
}
