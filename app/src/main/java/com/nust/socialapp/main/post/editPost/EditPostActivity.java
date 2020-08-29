

package com.nust.socialapp.main.post.editPost;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.nust.socialapp.main.main.MainActivity;
import com.nust.socialapp.main.post.BaseCreatePostActivity;
import com.nust.socialapp.managers.PostManager;
import com.nust.socialapp.model.Post;
import com.nust.socialapp.R;
import com.nust.socialapp.utils.GlideApp;

public class EditPostActivity extends BaseCreatePostActivity<EditPostView, EditPostPresenter> implements EditPostView {
    private static final String TAG = EditPostActivity.class.getSimpleName();
    public static final String POST_EXTRA_KEY = "EditPostActivity.POST_EXTRA_KEY";
    public static final int EDIT_POST_REQUEST = 33;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Post post = (Post) getIntent().getSerializableExtra(POST_EXTRA_KEY);
        presenter.setPost(post);
        showProgress();
        fillUIFields(post);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.addCheckIsPostChangedListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.closeListeners();
    }

    @NonNull
    @Override
    public EditPostPresenter createPresenter() {
        if (presenter == null) {
            return new EditPostPresenter(this);
        }
        return presenter;
    }

    @Override
    public void openMainActivity() {
        Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void fillUIFields(Post post) {
        titleEditText.setText(post.getTitle());
        descriptionEditText.setText(post.getDescription());
        loadPostDetailsImage(post.getImageTitle());
        hideProgress();
    }

    private void loadPostDetailsImage(String imageTitle) {
        PostManager.getInstance(this.getApplicationContext()).loadImageMediumSize(GlideApp.with(this),
                imageTitle,
                imageView,
                () -> progressBar.setVisibility(View.GONE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save:
                presenter.doSavePost(imageUri);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
