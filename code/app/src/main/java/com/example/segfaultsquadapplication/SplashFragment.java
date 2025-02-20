package com.example.segfaultsquadapplication;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import android.os.Handler;
import android.os.Looper;

public class SplashFragment extends Fragment {
    // attributes
    private TextView emojiText; // displayed emoji str
    // emojis: angry, sad, happy, excited, tired, scared, surprised
    private final String[] emojis = { "😡", "😭", "😀", "😆", "😴", "😱", "🤯" }; // set of emojis
    private int currentEmojiIndex = 0; // index of current display emoji
    private final Handler handler = new Handler(Looper.getMainLooper()); // loop handler

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);
        emojiText = view.findViewById(R.id.emojiText);
        startEmojiAnimation();
        return view;
    }

    /**
     * Helper method for emoji animation
     */
    private void startEmojiAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();

        // Rotate animation
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(emojiText, "rotationY", 0f, 360f);
        rotateAnimator.setDuration(1000);
        rotateAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // Scale animation
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(emojiText, "scaleX", 1f, 0.5f, 1f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(emojiText, "scaleY", 1f, 0.5f, 1f);
        scaleXAnimator.setDuration(1000);
        scaleYAnimator.setDuration(1000);

        animatorSet.playTogether(rotateAnimator, scaleXAnimator, scaleYAnimator);

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentEmojiIndex = (currentEmojiIndex + 1) % emojis.length;
                emojiText.setText(emojis[currentEmojiIndex]);

                if (currentEmojiIndex != 0) {
                    // Continue animation if we haven't shown all emojis
                    handler.postDelayed(() -> startEmojiAnimation(), 500);
                } else {
                    // Navigate to appropriate screen after showing all emojis
                    handler.postDelayed(() -> navigateToNextScreen(), 500);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animatorSet.start();
    }

    /**
     * takes user to the next screen after the initial splash/loading screen. Can
     * take to either LoginFragment or MoodHistory depending on if user has
     * previosuly logged in
     */
    private void navigateToNextScreen() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_splash_to_myMoodHistory);
        } else {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_splash_to_login);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}