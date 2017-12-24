package com.justtap.comp.models.frags;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.justtap.R;
import com.justtap.comp.GraphicsHandler;
import com.justtap.comp.models.Settings;
import com.justtap.comp.models.SettingsManager;

/**
 *
 */

public class PlayFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //Inflates and returns a new play view!

        View play = inflater.inflate(
                R.layout.menu_play, container, false);

        ImageButton playButton = (ImageButton) play.findViewById(R.id.MENU_play_button);
        //Get label (Fingers crossed this works)
        TextView label = (TextView) play.findViewById(R.id.MENU_play_label);

        //Style the label by the settings configuration
        Settings settings = SettingsManager.get(play.getContext());
        assert settings != null;
        if (settings.getFont((Settings.Property.Font_PrimaryLabel)) != null)
            label.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
        label.setTextColor(settings.primaryLabelColor);
        label.setTextSize(Settings.dpUnit, Settings.primaryLabelFontSize);


        //done?

        //Animations
        GraphicsHandler.floatView(1000, .00f, 0.1f, true, label, play.getContext());
        GraphicsHandler.floatView(1000, .05f, 0, true, playButton, play.getContext());

        //Define play button action
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch a new Game lol
                Intent startGame = new Intent(getContext(), com.justtap.act.Gamescreen_New.class);
                startActivity(startGame);

            }
        });

        return play;
    }
}
