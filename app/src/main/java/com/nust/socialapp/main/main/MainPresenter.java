

package com.nust.socialapp.main.main;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.nust.socialapp.enums.PostStatus;
import com.nust.socialapp.main.base.BasePresenter;
import com.nust.socialapp.main.postDetails.PostDetailsActivity;
import com.nust.socialapp.managers.PostManager;
import com.nust.socialapp.model.Post;
import com.nust.socialapp.R;



class MainPresenter extends BasePresenter<MainView> {

    private PostManager postManager;

    MainPresenter(Context context) {
        super(context);
        postManager = PostManager.getInstance(context);
    }


    void onCreatePostClickAction(View anchorView) {
        if (checkInternetConnection(anchorView)) {
            if (checkAuthorization()) {
                ifViewAttached(MainView::openCreatePostActivity);
            }
        }
    }

    void onPostClicked(final Post post, final View postView) {
        postManager.isPostExistSingleValue(post.getId(), exist -> ifViewAttached(view -> {
            if (exist) {
                view.openPostDetailsActivity(post, postView);
            } else {
                view.showFloatButtonRelatedSnackBar(R.string.error_post_was_removed);
            }
        }));
    }

    void onProfileMenuActionClicked() {
        if (checkAuthorization()) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            ifViewAttached(view -> view.openProfileActivity(userId, null));
        }
    }

    void onPostCreated() {
        ifViewAttached(view -> {
            view.refreshPostList();
            view.showFloatButtonRelatedSnackBar(R.string.message_post_was_created);
        });
    }

    void onPostUpdated(Intent data) {
        if (data != null) {
            ifViewAttached(view -> {
                PostStatus postStatus = (PostStatus) data.getSerializableExtra(PostDetailsActivity.POST_STATUS_EXTRA_KEY);
                if (postStatus.equals(PostStatus.REMOVED)) {
                    view.removePost();
                    view.showFloatButtonRelatedSnackBar(R.string.message_post_was_removed);
                } else if (postStatus.equals(PostStatus.UPDATED)) {
                    view.updatePost();
                }
            });
        }
    }

    void updateNewPostCounter() {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(() -> ifViewAttached(view -> {
            int newPostsQuantity = postManager.getNewPostsCounter();
            if (newPostsQuantity > 0) {
                view.showCounterView(newPostsQuantity);
            } else {
                view.hideCounterView();
            }
        }));
    }

    public void initPostCounter() {
        postManager.setPostCounterWatcher(newValue -> updateNewPostCounter());
    }
}
