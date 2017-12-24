package com.justtap.comp.models.frags;

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
 * =
 */

public class LeaderboardsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Create and returns our leaderboards menu fragment
        View view = inflater.inflate(
                R.layout.menu_leaderboards, container, false);


        //Get label (Fingers crossed this works)
        TextView label = (TextView) view.findViewById(R.id.MENU_leaderboards_label);
        ImageButton button = (ImageButton) view.findViewById(R.id.MENU_leaderboards_button);
        //Style the label by the settings configuration
        Settings settings = SettingsManager.get(view.getContext());
        assert settings != null;
        if (settings.getFont((Settings.Property.Font_PrimaryLabel)) != null)
            label.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
        label.setTextColor(settings.primaryLabelColor);
        label.setTextSize(Settings.dpUnit, Settings.primaryLabelFontSize);


        //done?

        //Animations
        GraphicsHandler.floatView(1000, .00f, 0.1f, true, label, view.getContext());
        GraphicsHandler.floatView(1000, .05f, 0, true, button, view.getContext());


        return view;

    }
}
