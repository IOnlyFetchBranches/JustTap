package com.justtap.act;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.justtap.R;
import com.justtap.comp.GraphicsHandler;
import com.justtap.comp.models.Settings;
import com.justtap.comp.models.SettingsManager;
import com.justtap.comp.models.adapts.GameOverPagerAdapter;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GameOver_main extends AppCompatActivity {
    //Constants
    private static int PAGE_COUNT = 2; // Amount of pages for our end game stats
    //As always load our gloabal preferences!
    private Settings settings = SettingsManager.get(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over_main);

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
        Bundle values = getIntent().getExtras();

        //Header
        TextView header = (TextView) findViewById(R.id.GAMEOVER_Header);
        //Style it!
        header.setTypeface(settings.getFont(Settings.Property.Font_Title));
        //Animate it!
        GraphicsHandler.floatView(1500, .15f, .05f, true, header, this);

        //Grab the retry button
        ConstraintLayout retryButton = (ConstraintLayout) findViewById(R.id.GAMEOVER_RetryButton);

        //Animate the retry button
        GraphicsHandler.fadeView(1500, .5f, 1f, true, retryButton, this);

        //Grab viewpager
        ViewPager endgameAnnouncments = (ViewPager) findViewById(R.id.GAMEOVER_MENUPager);

        //Set adapter /2 two pages and pulled stats
        endgameAnnouncments.setAdapter(new GameOverPagerAdapter(1, values, getSupportFragmentManager()));

        endgameAnnouncments.setCurrentItem(0);


    }


    //Our pager adapter to be used with this activity!
}
