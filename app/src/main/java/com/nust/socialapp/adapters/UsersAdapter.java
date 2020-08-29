

package com.nust.socialapp.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.nust.socialapp.adapters.holders.UserViewHolder;
import com.nust.socialapp.R;

import java.util.ArrayList;
import java.util.List;



public class UsersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = UsersAdapter.class.getSimpleName();

    private List<String> itemsList = new ArrayList<>();

    private UserViewHolder.Callback callback;
    private Activity activity;

    public UsersAdapter(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(UserViewHolder.Callback callback) {
        this.callback = callback;
    }

    @Override
    public int getItemCount() {
        return itemsList.size();

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new UserViewHolder(inflater.inflate(R.layout.user_item_list_view, parent, false),
                callback, activity);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((UserViewHolder) holder).bindData(itemsList.get(position));
    }

    public void setList(List<String> list) {
        itemsList.clear();
        itemsList.addAll(list);
        notifyDataSetChanged();
    }

    public void updateItem(int position) {
        notifyItemChanged(position);
    }

    public String getItemByPosition(int position) {
        return itemsList.get(position);
    }
}
