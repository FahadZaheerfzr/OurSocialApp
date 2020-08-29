

package com.nust.socialapp.main.search.posts;

import com.nust.socialapp.main.base.BaseFragmentView;
import com.nust.socialapp.model.Post;

import java.util.List;


public interface SearchPostsView extends BaseFragmentView {
    void onSearchResultsReady(List<Post> posts);
    void showLocalProgress();
    void hideLocalProgress();
    void showEmptyListLayout();
}
