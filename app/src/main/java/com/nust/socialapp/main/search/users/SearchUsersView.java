package com.nust.socialapp.main.search.users;

import com.nust.socialapp.main.base.BaseFragmentView;
import com.nust.socialapp.model.Profile;

import java.util.List;


public interface SearchUsersView extends BaseFragmentView {
    void onSearchResultsReady(List<Profile> profiles);

    void showLocalProgress();

    void hideLocalProgress();

    void showEmptyListLayout();

    void updateSelectedItem();
}
