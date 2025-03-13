package com.example.segfaultsquadapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FollowRequestsAdapter extends RecyclerView.Adapter<FollowRequestsAdapter.RequestViewHolder> {
    private List<User> requests;
    private FollowRequestListener listener;

    public interface FollowRequestListener {
        void onFollowRequest(User user, boolean accept);
    }

    public FollowRequestsAdapter(List<User> requests, FollowRequestListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        User user = requests.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<User> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        private TextView username;
        private Button acceptButton;
        private Button denyButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);

            acceptButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFollowRequest(requests.get(position), true);
                }
            });

            denyButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFollowRequest(requests.get(position), false);
                }
            });
        }

        public void bind(User user) {
            username.setText(user.getUsername());
        }
    }
}
