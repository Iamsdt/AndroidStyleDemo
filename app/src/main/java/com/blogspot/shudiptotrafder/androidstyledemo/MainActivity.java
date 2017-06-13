package com.blogspot.shudiptotrafder.androidstyledemo;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.blogspot.shudiptotrafder.androidstyledemo.adapter.CustomCursorAdapter;
import com.blogspot.shudiptotrafder.androidstyledemo.data.MainWordDBContract;
import com.blogspot.shudiptotrafder.androidstyledemo.utilities.ConstantUtills;
import com.blogspot.shudiptotrafder.androidstyledemo.utilities.Utility;

import java.util.ArrayList;
import java.util.Locale;

import br.com.mauker.materialsearchview.MaterialSearchView;

public class MainActivity extends AppCompatActivity
        implements
        CustomCursorAdapter.ClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    //loaders id that initialized that's one loader is running
    private static final int TASK_LOADER_ID = 0;


    //it's also show when data base are loading
    ProgressDialog progressDialog;

    private CustomCursorAdapter mAdapter;

    //selected column form database
    public static final String[] projection = new String[]
            {MainWordDBContract.Entry.COLUMN_WORD};

    //for words index
    public static final int INDEX_WORD = 0;

    static {
        //complete add vector drawable support
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }


    //fab
    FloatingActionButton fab;

    MaterialSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchView = (MaterialSearchView) findViewById(R.id.search_view);

        setAllSearchOption();

        //progress dialog for force wait user for database ready
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait a while");
        progressDialog.setCancelable(false);

        //assign view
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.mainRecycleView);
        final LinearLayoutManager manager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        mAdapter = new CustomCursorAdapter(this, this);
        recyclerView.setAdapter(mAdapter);

        /*
         Ensure a loader is initialized and active. If the loader doesn't already exist, one is
         created, otherwise the last created loader is re-used.
         */
        getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);

        fab = (FloatingActionButton) findViewById(R.id.main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);

                String s = mAdapter.getRandomWord();

                Uri wordUri = MainWordDBContract.Entry.buildUriWithWord(s);
                intent.setData(wordUri);
                startActivity(intent);
            }
        });

        //fab hide with recycler view scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 1)
                    fab.hide();
                else if (dy < 1)
                    fab.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (searchView.isOpen()) {
            // Close the search on the back button press.
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void setAllSearchOption(){

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                Uri uri = MainWordDBContract.Entry.buildUriWithWord(query.toUpperCase());
                Cursor cursor = getContentResolver().query(uri,
                        MainActivity.projection,null,null,null);

                if (cursor != null && cursor.getCount() > 0){
                    Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                    intent.setData(uri);
                    startActivity(intent);
                    searchView.closeSearch();
                    searchView.setCloseOnTintClick(false);
                }

                if (cursor != null){
                    cursor.close();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (newText.length() > 0){
                    String selection = MainWordDBContract.Entry.COLUMN_WORD +" like ? ";
                    //if you are try to search from any position of word
                    //then use
                    //String[] selectionArg = new String[]{"%"+newText+"%"};
                    //if you try to search from start of word the use this line
                    String[] selectionArg = new String[]{newText+"%"};

                    Cursor cursor = getContentResolver().query(MainWordDBContract.Entry.CONTENT_URI,
                            MainActivity.projection,selection,selectionArg,null);

                    if (cursor != null && cursor.getCount() > 0){
                        mAdapter.swapCursor(cursor);
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });

        searchView.setSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewOpened() {
                fab.hide();
            }

            @Override
            public void onSearchViewClosed() {
                // Do something once the view is closed.
                fab.show();
            }
        });


//        searchView.setTintAlpha(200);
        searchView.adjustTintAlpha(0.8f);


        searchView.setOnVoiceClickedListener(new MaterialSearchView.OnVoiceClickedListener() {
            @Override
            public void onVoiceClicked() {
                askSpeechInput();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //SharedPreferences for database initializing state
        // for first time value

        SharedPreferences preferences = getSharedPreferences(
                ConstantUtills.DATABASE_INIT_SP_KEY, MODE_PRIVATE);
        //sate of database is initialized or not
        boolean state = preferences.getBoolean(
                ConstantUtills.DATABASE_INIT_SP_KEY, false);

        if (!state) {
            progressDialog.show();
            Utility.initializedDatabase(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //Toast.makeText(SettingActivity.this, "change deced", Toast.LENGTH_SHORT).show();
                if (key.equals(getString(R.string.switchKey))) {
                    recreate();
                }

                if (key.equalsIgnoreCase(getString(R.string.textSizeKey))) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        searchView.activityResumed();

        // re-queries for all tasks
        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_search:
                // Open the search view on the menu item click.
                searchView.openSearch();
                return true;
            case R.id.action_nightMode:
                //Todo less SharedPreferences
                SharedPreferences preferences = getSharedPreferences("nightmode", Context.MODE_PRIVATE);
                boolean b = preferences.getBoolean("nightmode",true);
                SharedPreferences.Editor editor = preferences.edit();
                if (b){
                    //night mode code
                    Toast.makeText(this, "night mode", Toast.LENGTH_SHORT).show();
                    editor.putBoolean("nightmode",false);
                    recreate();
                } else {
                    Toast.makeText(this, "night mode off", Toast.LENGTH_SHORT).show();
                    editor.putBoolean("nightmode",true);
                }

                editor.apply();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

// Showing google speech input dialog

    private void askSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak your desire word");
        try {
            startActivityForResult(intent, MaterialSearchView.REQUEST_VOICE);
        } catch (ActivityNotFoundException a) {
            a.printStackTrace();
            slet("Activity not found", a);
            Toast.makeText(this, "Sorry Speech To Text is not " +
                    "supported in your device", Toast.LENGTH_SHORT).show();
        }
    }

    // Receiving speech input

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        switch (requestCode) {
//
//            case REQ_CODE_SPEECH_INPUT: {
//                if (resultCode == RESULT_OK && null != data) {
//
//                    ArrayList<String> result = data
//                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//
//                    boolean contain = false;
//
//                    if (contain) {
//                        Uri uri = MainWordDBContract.Entry.buildUriWithWord(result.get(0));
//
//                        Intent intent = new Intent(MainActivity.this,
//                                DetailsActivity.class);
//                        intent.setData(uri);
//
//                        startActivity(intent);
//
//                    } else {
//                        Toast.makeText(this, "Sorry word not found", Toast.LENGTH_SHORT).show();
//                    }
//
//                }
//
//                break;
//            }
//
//        }
//    }

    //for loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, MainWordDBContract.Entry.CONTENT_URI, projection, null, null,
                MainWordDBContract.Entry.COLUMN_WORD);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Update the data that the adapter uses to create ViewHolders
        Log.e("Data", String.valueOf(data.getCount()));
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * onClick listener for recycler view
     * called if click any item on recycler view
     *
     * @param word is the selected word from data base
     */
    @Override
    public void onItemClickListener(String word) {
        Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
        Uri wordUri = MainWordDBContract.Entry.buildUriWithWord(word);
        intent.setData(wordUri);
        startActivity(intent);
        searchView.closeSearch();
    }

    /**
     * log message methods that's display log only debug mode
     *
     * @param string message that to display
     */
    private static void sle(String string) {
        //show log with error message
        //if debug mode enable
        String Tag = "MainActivity";

        if (BuildConfig.DEBUG) {
            Log.e(Tag, string);
        }
    }

    /**
     * log message methods that's display log only debug mode
     *
     * @param s message that to display
     * @param t throwable that's throw if exception happen
     */
    private static void slet(String s, Throwable t) {
        //show log with error message with throwable
        //if debug mode enable
        String Tag = "MainActivity";

        if (BuildConfig.DEBUG) {
            Log.e(Tag, s, t);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWrd = matches.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {

                    //Todo more accure on settings
                    searchView.setQuery(searchWrd, false);
                    Uri uri = MainWordDBContract.Entry.buildUriWithWord(searchWrd.toUpperCase());

                    Cursor cursor = getContentResolver().query(uri,
                            MainActivity.projection,null,null,null);

                    if (cursor != null && cursor.getCount() > 0){
                        Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                        intent.setData(uri);
                        startActivity(intent);
                        searchView.closeSearch();
                        searchView.setCloseOnTintClick(false);
                    }

                    if (cursor != null){
                        cursor.close();
                    }
                }
            }

            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
