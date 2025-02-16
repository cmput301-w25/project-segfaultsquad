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

public class LoginFragment extends Fragment {

    private TextInputEditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppCompatButton loginButton;

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

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
            return rootView;
        }

        loginButton.setOnClickListener(v -> loginUser());

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
                        // Create new user document
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
        navController.navigate(R.id.navigation_my_mood_history); // Replace with your fragment's ID
    }

}
