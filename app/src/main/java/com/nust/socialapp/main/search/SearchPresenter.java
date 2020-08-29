

package com.nust.socialapp.main.search;

import android.app.Activity;

import com.nust.socialapp.main.base.BasePresenter;



class SearchPresenter extends BasePresenter<SearchView> {

    private String currentUserId;
    private Activity activity;

    SearchPresenter(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public void search(String query) {

    }
}
