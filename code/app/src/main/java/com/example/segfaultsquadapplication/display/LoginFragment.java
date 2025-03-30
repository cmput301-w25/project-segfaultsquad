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
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicReference;

public class LoginFragment extends Fragment {

    // attributes
    TextInputEditText emailEditText;
    TextInputEditText passwordEditText;
    private AppCompatButton loginButton;
    private CheckBox rememberMeCheckbox;
    private TextView forgotPasswordText;
    private TextView signUpText;
    private MaterialButton googleLoginButton;
    private MaterialButton appleLoginButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.login_fragment, container, false);

        // Initialize views
        emailEditText = rootView.findViewById(R.id.editTextEmail);
        passwordEditText = rootView.findViewById(R.id.editTextPassword);
        loginButton = rootView.findViewById(R.id.buttonLogin);
        rememberMeCheckbox = rootView.findViewById(R.id.checkboxRememberMe);
        forgotPasswordText = rootView.findViewById(R.id.textForgotPassword);
        signUpText = rootView.findViewById(R.id.textSignUp);
        googleLoginButton = rootView.findViewById(R.id.buttonGoogleLogin);
        appleLoginButton = rootView.findViewById(R.id.buttonAppleLogin);

        // Check if user is already logged in
        if (DbUtils.getUser() != null) {
            navigateToHome();
            return rootView;
        }

        // Set click listeners
        forgotPasswordText.setOnClickListener(v -> { // TODO: ask TA what to do in this situation
            // TODO: Implement forgot password functionality
        });

        signUpText.setOnClickListener(v -> { // TODO: ask TA if this is necessary
            // TODO: Navigate to sign up screen
        });

        googleLoginButton.setOnClickListener(v -> { // TODO: ask TA if this is actually even needed
            // TODO: Implement Google sign-in
        });

        appleLoginButton.setOnClickListener(v -> { // TODO: ask TA if this is actually even needed
            // TODO: Implement Apple sign-in
        });

        loginButton.setOnClickListener(v -> loginUser()); // login button listener

        return rootView;
    }

    /**
     * method ot log usir into the system/app
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
     * method to navigate to MyMoodHistoryFragment (or whatever the homescreen is)
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