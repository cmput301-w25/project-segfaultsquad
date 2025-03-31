package com.example.segfaultsquadapplication.display.moodaddedit;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.google.android.material.card.MaterialCardView;

/**
 * This class provides utility methods for inflating and setting up UI components in the EditMoodFragment. It includes methods for setting up the mood grid and social situation spinner.
 * Outstanding Issues: None
 */
public class EditMoodFragmentInflater {
    /**
     * helper method to setup the UI elements of the availible moods to select from.
     * includes setup of boxes, texts and emojis
     */
    public static void setupMoodGrid(Context ctx, Resources res, View.OnClickListener cardListener,
                                     GridLayout moodGrid) {
        for (int i = 0; i < MoodEvent.MoodType.values().length; i++) {
            MoodEvent.MoodType type = MoodEvent.MoodType.values()[i];
            MaterialCardView moodCard = new MaterialCardView(ctx);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 4, 1f);
            params.rowSpec = GridLayout.spec(i / 4);
            params.setMargins(8, 8, 8, 8);
            moodCard.setLayoutParams(params);
            moodCard.setRadius(8);
            moodCard.setStrokeWidth(1);

            // Set stroke color to the mood's primary color
            moodCard.setStrokeColor(type.getPrimaryColor(ctx));

            // Set card background to white
            moodCard.setCardBackgroundColor(res.getColor(android.R.color.white));

            // Create vertical layout for emoji and text
            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(8, 16, 8, 16);

            // Add emoji
            TextView emojiText = new TextView(ctx);
            emojiText.setText(type.getEmoticon());
            emojiText.setTextSize(24);
            emojiText.setGravity(Gravity.CENTER);
            emojiText.setTextColor(res.getColor(R.color.color_primary)); // Set text color

            // Add mood name
            TextView moodText = new TextView(ctx);
            moodText.setText(type.name().charAt(0) + type.name().substring(1).toLowerCase());
            moodText.setGravity(Gravity.CENTER);
            moodText.setTextSize(12);
            moodText.setPadding(0, 8, 0, 0);
            moodText.setTextColor(res.getColor(R.color.color_primary)); // Set text color

            layout.addView(emojiText);
            layout.addView(moodText);
            moodCard.addView(layout);
            moodCard.setTag(type);

            moodCard.setOnClickListener(cardListener);

            moodGrid.addView(moodCard);
        }
    }

    /**
     * helper mehtod to setup dropdown options dynamically
     *
     * @param ctx
     *                               context
     * @param socialSituationSpinner
     *                               the social spinner ui element
     */
    public static void setupSocialSituationSpinner(Context ctx, Spinner socialSituationSpinner) {
        ArrayAdapter<MoodEvent.SocialSituation> adapter = new ArrayAdapter<>(
                ctx,
                android.R.layout.simple_spinner_item,
                MoodEvent.SocialSituation.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(adapter);
    }
}
