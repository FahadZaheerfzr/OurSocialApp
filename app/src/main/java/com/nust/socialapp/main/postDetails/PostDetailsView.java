

package com.nust.socialapp.main.postDetails;

import android.view.View;

import com.nust.socialapp.main.base.BaseView;
import com.nust.socialapp.model.Comment;
import com.nust.socialapp.model.Post;

import java.util.List;



public interface PostDetailsView extends BaseView {

    void onPostRemoved();

    void openImageDetailScreen(String imagePath);

    void openProfileActivity(String authorId, View authorView);

    void setTitle(String title);

    void setDescription(String description);

    void loadPostDetailImage(String imagePath);

    void loadAuthorPhoto(String photoUrl);

    void setAuthorName(String username);

    void initLikeController(Post post);

    void updateCounters(Post post);

    void initLikeButtonState(boolean exist);

    void showComplainMenuAction(boolean show);

    void showEditMenuAction(boolean show);

    void showDeleteMenuAction(boolean show);

    String getCommentText();

    void clearCommentField();

    void scrollToFirstComment();

    void openEditPostActivity(Post post);

    void showCommentProgress(boolean show);

    void showCommentsWarning(boolean show);

    void showCommentsRecyclerView(boolean show);

    void onCommentsListChanged(List<Comment> list);

    void showCommentsLabel(boolean show);
}
