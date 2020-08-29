package com.nust.socialapp.adapters.holders;

import android.view.View;

import com.nust.socialapp.main.base.BaseActivity;
import com.nust.socialapp.managers.listeners.OnPostChangedListener;
import com.nust.socialapp.model.FollowingPost;
import com.nust.socialapp.model.Post;
import com.nust.socialapp.utils.LogUtil;


public class FollowPostViewHolder extends PostViewHolder {


    public FollowPostViewHolder(View view, OnClickListener onClickListener, BaseActivity activity) {
        super(view, onClickListener, activity);
    }

    public FollowPostViewHolder(View view, OnClickListener onClickListener, BaseActivity activity, boolean isAuthorNeeded) {
        super(view, onClickListener, activity, isAuthorNeeded);
    }

    public void bindData(FollowingPost followingPost) {
        postManager.getSinglePostValue(followingPost.getPostId(), new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                bindData(obj);
            }

            @Override
            public void onError(String errorText) {
                LogUtil.logError(TAG, "bindData", new RuntimeException(errorText));
            }
        });
    }

}
