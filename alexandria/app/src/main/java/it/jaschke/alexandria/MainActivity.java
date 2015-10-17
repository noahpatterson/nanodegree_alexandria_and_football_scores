package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import it.jaschke.alexandria.api.Callback;

public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, Callback {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence title;
    public static boolean IS_TABLET = false;
    private BroadcastReceiver messageReciever;
    private BroadcastReceiver deleteBookReceiver;
    private BroadcastReceiver foundBookReceiver;

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";
    public static final String MESSAGE_DELETE_FROM_LIST = "MESSAGE_DELETE_FROM_LIST";
    public static final String MESSAGE_FOUND_BOOK = "FOUND_BOOK";
    private static final String LOG_TAG = "main activity";
    private static final String NAV_DRAWER_STATE = "navDrawerFragment";
    private static final String NAV_DRAWER_POS = "navDrawerPosition";
    public static final String BOOK_DETAIL_FRAG_TAG = "Book Detail";
    private static final String NAV_DRAWER_FRAG_TAG = "navigationDrawerFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "in onCreate");
        IS_TABLET = Utility.isTablet(getApplicationContext());
        if(IS_TABLET){
            setContentView(R.layout.activity_main_tablet);
        }else {
            setContentView(R.layout.activity_main);
        }

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever,filter);

        //delete book receiver to restartLoader
        deleteBookReceiver = new DeleteBookReceiver();
        IntentFilter deleteBookfilter = new IntentFilter(MESSAGE_DELETE_FROM_LIST);
        LocalBroadcastManager.getInstance(this).registerReceiver(deleteBookReceiver,deleteBookfilter);

        //restart loader after book is found
        foundBookReceiver = new FoundBookReceiver();
        IntentFilter foundBookfilter = new IntentFilter(MESSAGE_FOUND_BOOK);
        LocalBroadcastManager.getInstance(this).registerReceiver(foundBookReceiver,foundBookfilter);


        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        title = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        int id = R.id.container;
        if(findViewById(R.id.right_container) != null){
            id = R.id.right_container;
        }

        // restore the last current navigation drawer fragment
        if (savedInstanceState != null) {
            int position = savedInstanceState.getInt(NAV_DRAWER_STATE, -1);
            if (position != -1) {
                attachNavDrawerFragment(position);
            }

            // add book_detail fragment if exists
            if (savedInstanceState.getString(BookDetail.EAN_KEY) != null) {
                Bundle args = new Bundle();
                args.putString(BookDetail.EAN_KEY, savedInstanceState.getString(BookDetail.EAN_KEY));

                BookDetail fragment = new BookDetail();
                fragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .replace(id, fragment, BOOK_DETAIL_FRAG_TAG)
                        .addToBackStack(BOOK_DETAIL_FRAG_TAG)
                        .commit();

            }
        }
    }

    private void attachNavDrawerFragment(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment nextFragment;
        Bundle args = new Bundle();

        switch (position){
            default:
            case 0:
                nextFragment = new ListOfBooks();
                break;
            case 1:
                nextFragment = new AddBook();
                break;
            case 2:
                nextFragment = new About();
                break;
        }
        args.putInt(NAV_DRAWER_POS, position);
        nextFragment.setArguments(args);

        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment, NAV_DRAWER_FRAG_TAG)
                .addToBackStack((String) title)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // save book detail and navigation drawer fragments
        Fragment currDetailFragment = getSupportFragmentManager().findFragmentByTag(BOOK_DETAIL_FRAG_TAG);
        Fragment navDrawerFragment = getSupportFragmentManager().findFragmentByTag(NAV_DRAWER_FRAG_TAG);
        if (currDetailFragment != null) {
            getSupportFragmentManager().popBackStack(BOOK_DETAIL_FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            outState.putString(BookDetail.EAN_KEY, currDetailFragment.getArguments().getString(BookDetail.EAN_KEY));
        }

        if (navDrawerFragment != null) {
            outState.putInt(NAV_DRAWER_STATE, navDrawerFragment.getArguments().getInt(NAV_DRAWER_POS));
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(LOG_TAG, "in onNavigationDrawerItemSelected");
        attachNavDrawerFragment(position);
    }

    public void setTitle(int titleId) {
        title = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "in onCreateOptionsMenu");
        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "in onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

    @Override
    public void onItemSelected(String ean) {
        Log.d(LOG_TAG, "in onItemSelected");
        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);

        BookDetail fragment = new BookDetail();
        fragment.setArguments(args);

        int id = R.id.container;
        if(findViewById(R.id.right_container) != null){
            id = R.id.right_container;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(id, fragment, BOOK_DETAIL_FRAG_TAG)
                .addToBackStack(BOOK_DETAIL_FRAG_TAG)
                .commit();

    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra(MESSAGE_KEY)!=null){
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class DeleteBookReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
            if (fragment != null && fragment instanceof AddBook) {
                AddBook addBook = (AddBook) fragment;
                addBook.restartLoader();
            } else if (fragment != null && fragment instanceof ListOfBooks) {
                ListOfBooks listOfBooks = (ListOfBooks) fragment;
                listOfBooks.restartLoader();
            }
        }
    }

    private class FoundBookReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AddBook addBook = (AddBook) getSupportFragmentManager().findFragmentById(R.id.container);
            if (addBook != null) {
                addBook.restartLoader();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG, "in onBackPressed");
        if(getSupportFragmentManager().getBackStackEntryCount()<2){
            finish();
        }
        super.onBackPressed();
    }


}