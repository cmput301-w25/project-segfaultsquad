/**
 * Classname: LoginFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Fragment responsible for handling the login functionality in the application
 * Implements login process with validation and feedback to the user.
 */
public class LoginFragment extends Fragment {

    // attributes
    TextInputEditText emailEditText;
    TextInputEditText passwordEditText;
    private AppCompatButton loginButton;

    /**
     * inflate the object layout view
     * @param inflater LayoutInflater object
     * @param container parent view of UI fragment
     * @param savedInstanceState previous fragment state for reconstruction
     * @return inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.login_fragment, container, false);

        // Initialize views
        emailEditText = rootView.findViewById(R.id.editTextEmail);
        passwordEditText = rootView.findViewById(R.id.editTextPassword);
        loginButton = rootView.findViewById(R.id.buttonLogin);

        // Check if user is already logged in
        if (UserManager.getCurrUser() != null) {
            navigateToHome();
            return rootView;
        }

        loginButton.setOnClickListener(v -> loginUser()); // login button listener

        return rootView;
    }

    /**
     * method to log user into the system/app
     */
    void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Disable login button while processing
        loginButton.setEnabled(false);

        UserManager.login(email, password,
                (isSuccess, failureReason) -> {
                    if (isSuccess) {
                        navigateToHome();
                    } else {
                        loginButton.setEnabled(true);
                        Toast.makeText(getActivity(), failureReason, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * method to navigate to homescreen
     */
    void navigateToHome() {
        // Navigate to the Home screen (MyMoodHistoryFragment) using the Navigation component
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_my_mood_history);

        //after navigation check for follow requests
        FirebaseUser currentUser = UserManager.getCurrUser();
        String currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        if (currentUserId != null) {
            AtomicReference<User> userHolder = new AtomicReference<>();
            UserManager.loadUserData(currentUserId, userHolder,
                    isSuccess -> {
                        if (isSuccess) {
                            User currentUserData = userHolder.get();
                            // Check if follow requests exist
                            if (currentUserData != null &&
                                    currentUserData.getFollowRequests() != null &&
                                    !currentUserData.getFollowRequests().isEmpty()) {
                                Toast.makeText(getActivity(), "There are " + currentUserData.getFollowRequestCount() + " follow requests", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Error loading follow requests", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}