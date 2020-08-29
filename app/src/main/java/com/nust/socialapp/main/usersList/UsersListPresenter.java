

package com.nust.socialapp.main.usersList;

import android.app.Activity;

import com.google.firebase.auth.FirebaseAuth;
import com.nust.socialapp.main.base.BasePresenter;
import com.nust.socialapp.main.base.BaseView;
import com.nust.socialapp.managers.FollowManager;
import com.nust.socialapp.utils.LogUtil;
import com.nust.socialapp.views.FollowButton;
import com.nust.socialapp.R;



class UsersListPresenter extends BasePresenter<UsersListView> {

    private final FollowManager followManager;
    private String currentUserId;
    private Activity activity;

    UsersListPresenter(Activity activity) {
        super(activity);
        this.activity = activity;

        followManager = FollowManager.getInstance(context);
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadFollowings(String userID, boolean isRefreshing) {
        if (checkInternetConnection()) {
            if (!isRefreshing) {
                ifViewAttached(UsersListView::showLocalProgress);
            }

            FollowManager.getInstance(context).getFollowingsIdsList(userID, list -> {
                ifViewAttached(view -> {
                    view.hideLocalProgress();
                    view.onProfilesIdsListLoaded(list);
                    if (list.size() > 0) {
                        view.hideEmptyListMessage();
                    } else {
                        String message = context.getString(R.string.message_empty_list, context.getString(R.string.title_followings));
                        view.showEmptyListMessage(message);
                    }
                });
            });
        }
    }

    public void loadFollowers(String userID, boolean isRefreshing) {
        if (checkInternetConnection()) {
            if (!isRefreshing) {
                ifViewAttached(UsersListView::showLocalProgress);
            }

            FollowManager.getInstance(context).getFollowersIdsList(userID, list -> {
                ifViewAttached(view -> {
                    view.hideLocalProgress();
                    view.onProfilesIdsListLoaded(list);

                    if (list.size() > 0) {
                        view.hideEmptyListMessage();
                    } else {
                        String message = context.getString(R.string.message_empty_list, context.getString(R.string.title_followers));
                        view.showEmptyListMessage(message);
                    }

                });
            });
        }
    }

    public void onRefresh(String userId, int userListType) {
        loadUsersList(userId, userListType, true);
    }

    public void loadUsersList(String userId, int userListType, boolean isRefreshing) {
        if (userListType == UsersListType.FOLLOWERS) {
            loadFollowers(userId, isRefreshing);
        } else if (userListType == UsersListType.FOLLOWINGS) {
            loadFollowings(userId, false);
        }
    }

    public void chooseActivityTitle(int userListType) {
        ifViewAttached(view -> {
            if (userListType == UsersListType.FOLLOWERS) {
                view.setTitle(R.string.title_followers);
            } else if (userListType == UsersListType.FOLLOWINGS) {
                view.setTitle(R.string.title_followings);
            }
        });

    }

    private void followUser(String targetUserId) {
        ifViewAttached(BaseView::showProgress);
        followManager.followUser(activity, currentUserId, targetUserId, success -> {
            ifViewAttached(view -> {
                view.hideProgress();
                if (success) {
                    view.updateSelectedItem();
                } else {
                    LogUtil.logDebug(TAG, "followUser, success: " + false);
                }
            });
        });
    }

    public void unfollowUser(String targetUserId) {
        ifViewAttached(BaseView::showProgress);
        followManager.unfollowUser(activity, currentUserId, targetUserId, success ->
                ifViewAttached(view -> {
                    view.hideProgress();
                    if (success) {
                        view.updateSelectedItem();
                    } else {
                        LogUtil.logDebug(TAG, "unfollowUser, success: " + false);
                    }
                }));
    }

    public void onFollowButtonClick(int state, String targetUserId) {
        if (checkInternetConnection() && checkAuthorization()) {
            if (state == FollowButton.FOLLOW_STATE || state == FollowButton.FOLLOW_BACK_STATE) {
                followUser(targetUserId);
            } else if (state == FollowButton.FOLLOWING_STATE) {
                unfollowUser(targetUserId);
            }
        }
    }
}
