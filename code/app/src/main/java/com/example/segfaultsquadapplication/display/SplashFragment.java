/**
 * Classname: SplashFragment
 * Version Info: Initial
 * Date: Feb 19, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;

/**
 * This fragment cycles through set of emojis and displays them upon start of app
 * navigates to login screen or homescreen based on user login status
 */
public class SplashFragment extends Fragment {
    // attributes
    private TextView emojiText; // displayed emoji str
    private final String[] emojis = MoodEvent.MoodType.getAllEmoticons();
    private int currentEmojiIndex = 0; // index of current display emoji
    private final Handler handler = new Handler(Looper.getMainLooper()); // loop handler

    /**
     * create view for splash screen
     * @param inflater layoutInflater object
     * @param container parent container to attached view to
     * @param savedInstanceState previous fragment state to be reconstructed
     * @return inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);
        emojiText = view.findViewById(R.id.emojiText);
        startEmojiAnimation();
        return view;
    }

    /**
     * Helper method for emoji animation, advices on how emojis should appear
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
            public void onAnimationCancel(Animator animation) { //no action when animation cancelled
            }

            @Override
            public void onAnimationRepeat(Animator animation) { //no action when animation repeats
            }
        });

        animatorSet.start();
    }

    /**
     * takes user to the next screen after the initial splash/loading screen.
     * takes to login or homepage depending on previous login status
     */
    private void navigateToNextScreen() {
        if (DbUtils.getUser() != null) {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_splash_to_myMoodHistory);
        } else {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_splash_to_login);
        }
    }

    /**
     * removes any pending navigations
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}