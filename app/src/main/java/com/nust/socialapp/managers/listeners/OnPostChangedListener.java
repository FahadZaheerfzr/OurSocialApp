

package com.nust.socialapp.managers.listeners;

import com.nust.socialapp.model.Post;

public interface OnPostChangedListener {
    public void onObjectChanged(Post obj);

    public void onError(String errorText);
}
