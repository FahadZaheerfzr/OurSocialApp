

package com.nust.socialapp.main.main;

import android.view.View;

import com.nust.socialapp.main.base.BaseView;
import com.nust.socialapp.model.Post;



public interface MainView extends BaseView {
    void openCreatePostActivity();
    void hideCounterView();
    void openPostDetailsActivity(Post post, View v);
    void showFloatButtonRelatedSnackBar(int messageId);
    void openProfileActivity(String userId, View view);
    void refreshPostList();
    void removePost();
    void updatePost();
    void showCounterView(int count);
}
