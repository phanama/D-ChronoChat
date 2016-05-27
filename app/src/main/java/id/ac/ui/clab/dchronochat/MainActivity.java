package id.ac.ui.clab.dchronochat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private SharedPreferences mSharedPrefs;
    private Button startChat;
    private String mUserName;
    private String mScreenName;
    private String mHubPrefix;
    private String mChatRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Bundle lSavedInstanceState = savedInstanceState;
        super.onCreate(lSavedInstanceState);
        setContentView(R.layout.activity_main_page);
        startChat = (Button) findViewById(R.id.startButton);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Setup Chat
        mSharedPrefs = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
        if (!mSharedPrefs.contains(getString(R.string.usernameShared)) || !mSharedPrefs.contains(getString(R.string.screennameShared)))
        {
            Intent toLogin = new Intent(this, LoginActivity.class);
            startActivity(toLogin);
            return;
        }

        mUserName = mSharedPrefs.getString(getString(R.string.usernameShared), "");
        mHubPrefix = mSharedPrefs.getString(getString(R.string.hubprefixShared), "");
        mScreenName = mSharedPrefs.getString(getString(R.string.screennameShared), "");
        //TODO add chatroom control from user
        mChatRoom = "TestRoom";

        //Bundle extras = getIntent().getExtras();
//        if (extras != null){
//            Log.d("Main-bundle",extras.toString() + " Has Chat: " + extras.getString(Constants.CHAT_ROOM));
//            if (extras.containsKey(getString(R.string.))) this.channel = extras.getString(Constants.CHAT_ROOM);
//        }

//        startChat.setOnClickListener(new Button.OnClickListener()
//        {
//            public void onClick(View v) {
//                if (lSavedInstanceState == null) {
//                    // During initial setup, plug in the details fragment.
//                    ChatListFragment details = new ChatListFragment();
//                    details.setArguments(getIntent().getExtras());
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .add(R.id.drawer_layout, ChatListFragment.newInstance(mScreenName, mUserName, mHubPrefix, mChatRoom), "chatItemList")
//                            .commit();
//                }
//
//            }
//        });

        if (lSavedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            ChatListFragment details = new ChatListFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.drawer_layout, ChatListFragment.newInstance(mScreenName, mUserName, mHubPrefix, mChatRoom), "chatItemList")
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
