package com.justtap.comp.models.frags;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.justtap.R;
import com.justtap.comp.LogicEngine;
import com.justtap.comp.models.Settings;
import com.justtap.comp.models.SettingsManager;

import java.util.HashMap;

/**
 * This view will be responsible for cooly displaying user stats
 */

public class GameOverStatsFragment extends Fragment {

    //Controls
    private static boolean started = false;
    //Internal Components
    Settings settings = SettingsManager.get(getActivity());
    //IMPORTANT CONSTANTS
    private long SCORE_SLEEP_CONST = 350; //This variable controls the base slowest the score meter ticks!
    //Stores all the end of game data
    private HashMap<String, Object> gameStats;
    //Internal Views
    private TextSwitcher scoreSwitcher;


    public GameOverStatsFragment() {

        /*
          Contains
          toGameOver.putExtra("score",score);
          toGameOver.putExtra("popCount",popCount);
          toGameOver.putExtra("avgPopTime",avgPopTime);
          toGameOver.putExtra("popTime",totalPopTime);
          toGameOver.putExtra("maxTime",maxPopTime);
          toGameOver.putExtra("minTime",minPopTime);
          toGameOver.putExtra("missCount",missCount);
         */

        //Now we link to all the views!


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        while (LogicEngine.State() != LogicEngine.Mode.GAMEOVER) {
            //wait for engine to close
        }

        gameStats = LogicEngine.getFinalScores();


        //We have to load the view here to not get an NPE
        final View statsView = inflater.inflate(R.layout.gameover_fragment_main, container, false);


        TextView scoreSwitcher = (TextView) statsView.findViewById(R.id.GAMEOVER_TotalScoreView);
        //Apply formatting
        scoreSwitcher.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        //Apply font
        scoreSwitcher.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
        //Apply color
        scoreSwitcher.setTextColor(settings.primaryLabelColor);
        //Apply Base size
        scoreSwitcher.setTextSize(Settings.primaryHeaderFontSizeSP);


        scoreSwitcher.setText(getActivity().getString(R.string.label_totalscore) + "\n" + gameStats.get("score") + "");


        return statsView;
    }

    @Override
    public void onStart() {
        super.onStart();
        started = true;


    }
}
