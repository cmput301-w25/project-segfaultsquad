package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;

/**
 * This file is for the searching users among the list of users in a recyclerview
 * handles user clicks into the view
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;

    /**
     * Interface to handle user click events from the search result list.
     */
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    /**
     * initializing adapter with a listener that handles user clicks
     * @param listener listener to handle user click events.
     */
    public SearchResultAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    /**
     * inflates the layout for the item view.
     * @param parent   The parent view group that the new item view will be attached to.
     * @param viewType The view type of the new item.
     * @return viewHolder that holds the item view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * bind a user to the view holder. This method sets the username and profile picture for each user.
     * @param holder ViewHolder to bind the data to.
     * @param position position of the user in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.textView.setText(user.getUsername());
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));

        List<Integer> profilePicData = user.getProfilePicUrl();
        if (profilePicData != null && !profilePicData.isEmpty()) {
            byte[] imageBytes = new byte[profilePicData.size()];
            for (int i = 0; i < profilePicData.size(); i++) {
                imageBytes[i] = profilePicData.get(i).byteValue();
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                holder.profilePic.setImageBitmap(bitmap);
            } else {
                holder.profilePic.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * adapter with a new list of users and notifies the adapter to refresh the UI
     * @param newUsers The new list of users to be displayed in the search result.
     */
    public void updateResults(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView profilePic;

        ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.username);
            profilePic = view.findViewById(R.id.profile_pic);
        }
    }
}