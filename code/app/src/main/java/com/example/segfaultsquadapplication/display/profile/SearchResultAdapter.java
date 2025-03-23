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

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public SearchResultAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

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