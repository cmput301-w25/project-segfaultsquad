/**
 * Classname: FollowRequestsAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This adapter is responsible for displaying a list of follow requests in a RecyclerView.
 * It binds follow request data to the UI components and handles user interactions for
 * accepting or denying follow requests.
 *
 * Outstanding Issues: None
 */
package com.example.segfaultsquadapplication.display.following;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;

import java.util.List;

/**
 * Classname: FollowRequestsAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This adapter is responsible for displaying a list of follow requests in a
 * RecyclerView.
 * It binds follow request data to the UI components and handles user
 * interactions for
 * accepting or denying follow requests.
 *
 * Outstanding Issues: None
 */
public class FollowRequestsAdapter extends RecyclerView.Adapter<FollowRequestsAdapter.RequestViewHolder> {
    private List<User> requests;
    private FollowRequestListener listener;

    /**
     * Interface for handling follow request actions.
     */
    public interface FollowRequestListener {
        /**
         * Called when a follow request is accepted or denied.
         *
         * @param user   The user associated with the follow request.
         * @param accept True if the request is accepted, false if denied.
         */
        void onFollowRequest(User user, boolean accept);
    }

    /**
     * adapter class for the follow requests
     * 
     * @param requests
     *                 list of user objects who have sent current user requests
     * @param listener
     *                 listener to update array list
     */
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

    /**
     * notify of array of changes
     * 
     * @param newRequests
     *                    the new requests
     */
    public void updateRequests(List<User> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        private TextView username;
        private Button acceptButton;
        private Button denyButton;

        /**
         * Constructor for the RequestViewHolder.
         *
         * @param itemView The view for the item.
         *                 view to hold requests
         */
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);

            acceptButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    acceptButton.setText("Following");
                    listener.onFollowRequest(requests.get(position), true);
                }
            });

            denyButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    denyButton.setText("Denied");
                    listener.onFollowRequest(requests.get(position), false);
                }
            });
        }

        /**
         * Binds the user data to the UI components.
         *
         * @param user
         *             The user to bind.
         */
        public void bind(User user) {
            username.setText(user.getUsername());
        }
    }
}