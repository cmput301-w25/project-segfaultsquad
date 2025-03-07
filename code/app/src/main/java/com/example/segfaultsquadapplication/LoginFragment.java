/**
 * Classname: LoginFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import java.util.HashMap;
import java.util.Map;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class LoginFragment extends Fragment {

    private TextInputEditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
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

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
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

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

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

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user document exists in Firestore
                            checkAndCreateUserDocument(user);
                        }
                    } else {
                        Toast.makeText(getActivity(),
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUserDocument(FirebaseUser firebaseUser) {
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create new user document (probably shouldnt have to do this, but anyways)
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", firebaseUser.getEmail().split("@")[0]);

                        db.collection("users")
                                .document(firebaseUser.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> navigateToHome())
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getActivity(),
                                            "Error creating user profile",
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // user document was already there, navigate to MyMoodFragment
                        navigateToHome();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(),
                            "Error checking user profile",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHome() {
        // Navigate to the Home screen (MyMoodHistoryFragment) using the Navigation
        // component
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_my_mood_history);
    }

}