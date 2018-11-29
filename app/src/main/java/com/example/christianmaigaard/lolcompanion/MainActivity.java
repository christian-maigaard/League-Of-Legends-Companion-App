package com.example.christianmaigaard.lolcompanion;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.christianmaigaard.lolcompanion.Utilities.Constants;
import com.example.christianmaigaard.lolcompanion.Utilities.SharedPrefs;

import static com.example.christianmaigaard.lolcompanion.EnterSummonerNameActivity.SUMMONER_NAME;

import static com.example.christianmaigaard.lolcompanion.Utilities.Constants.SUMMONER_LEVEL;


public class MainActivity extends AppCompatActivity {

    private static final String LOG = "MainActivity";

    // UI
    private TextView name;
    private TextView profileIcon;
    private TextView level;
    private TextView bestChamp;
    private ImageView champImage;
    private Button changeName;
    private Button getInfo;
    private Button liveGame;
    // Variables
    private String summonerName;
    private long summonerLevel;




    private CommunicationService mService;
    private boolean mBound = false;

    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.nameView);
        profileIcon = findViewById(R.id.profileIconView);
        level = findViewById(R.id.levelView);
        bestChamp = findViewById(R.id.bestChampView);
        getInfo = findViewById(R.id.getInfoButton);
        changeName = findViewById(R.id.main_activity_change_name_button);
        champImage = findViewById(R.id.champIcon);
        liveGame = findViewById(R.id.goToLive);

        // Code inspired heavily from "intentClassExample"
        Intent dataFromSummonerNameActivity = getIntent();
        summonerName = dataFromSummonerNameActivity.getStringExtra(SUMMONER_NAME);
        summonerLevel = dataFromSummonerNameActivity.getLongExtra(SUMMONER_LEVEL,0);


        // save summonor name in sharedpreferences
        SharedPrefs.storeSummonerNameInSharedPreferences(this, summonerName);
        // retrieve summonor name in sharedpreferences
        final String newName = SharedPrefs.retrieveSummonorNameFromSharedPreferences(this);
        Log.d(LOG, "got from prefs !!" + newName);

        if(mBound){
            mService.createSummonerInfoRequest(summonerName);
            mService.getBestChamp();
        }



        //Intent intent = new Intent()
        startService(new Intent(this, CommunicationService.class));


        changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPrefs.deleteSummonerName(MainActivity.this);
                finish();
            }
        });

        getInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBound){
                    mService.getBestChamp();
                }
            }
        });
        liveGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LiveGameActivity.class);
                startActivity(intent);
            }
        });

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constants.BROADCAST_BEST_CHAMPION_ACTION)){
                    String bestChampName = intent.getStringExtra(Constants.BEST_CHAMPION_EXTRA);
                    bestChamp.setText(bestChampName);
                    champImage.setImageDrawable(AssetHelper.loadChampImageFromAssets(MainActivity.this, bestChampName));
                }
                if(intent.getAction().equals(Constants.BROADCAST_SUMMONER_INFO_ACTION)){
                    long summonerLvl = intent.getLongExtra(Constants.SUMMONER_INFO_LEVEL_EXTRA,0);
                    level.setText(summonerLvl+"");
                }
            }
        };
        mFilter = new IntentFilter();
        mFilter.addAction(Constants.BROADCAST_BEST_CHAMPION_ACTION);
        mFilter.addAction(Constants.BROADCAST_SUMMONER_INFO_ACTION);
        registerReceiver(mReceiver, mFilter);
        if(mBound){
            mService.getBestChamp();
        }

        updateUI();
    }

    private void updateUI() {
        name.setText(summonerName);
        level.setText(String.valueOf(summonerLevel));
    }


    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = new Intent(this, CommunicationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop(){
        super.onStop();
        unbindService(connection);
        mBound = false;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private ServiceConnection connection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CommunicationService.LocalBinder binder = (CommunicationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

    };
}
