

package com.nust.socialapp.managers.listeners;

import com.nust.socialapp.model.PostListResult;

public interface OnPostListChangedListener<Post> {

    public void onListChanged(PostListResult result);

    void onCanceled(String message);
}
