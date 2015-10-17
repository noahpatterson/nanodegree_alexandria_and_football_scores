package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
//    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private final String FOUND_BOOK_EAN_STR = "foundBookEan";
//    private static final String SCAN_FORMAT = "scanFormat";
//    private static final String SCAN_CONTENTS = "scanContents";
//
//    private String mScanFormat = "Format:";
//    private String mScanContents = "Contents:";
    private String mScannedEan = "";
    private String mFoundBookEan;



    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("add book", "in onSaveInstanceState view");
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
        if(mFoundBookEan!=null) {
            outState.putString(FOUND_BOOK_EAN_STR, mFoundBookEan);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("add book", "in oncreate view");
        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("add book", "in afterTextChanged");
                String ean =s.toString();
                //catch isbn10 numbers
                if(ean.length()==10 && !ean.startsWith("978")){
                    ean="978"+ean;
                }
                if(ean.length()<13 || ean.length()>13){
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
//                AddBook.this.restartLoader();
                mFoundBookEan = ean;

                // closes the soft keyboard
                rootView.clearFocus();
                InputMethodManager in = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator.forSupportFragment(AddBook.this).initiateScan();
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        //TODO: crash deleting book here, with no author isbn: 1412714990
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String eanStr = ean.getText().toString().length() != 0 ? ean.getText().toString() : mScannedEan;

                //catch isbn10 numbers
                if(eanStr.length()==10 && !eanStr.startsWith("978")){
                    eanStr="978"+ eanStr;
                }

                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanStr);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                mFoundBookEan = null;
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT, ""));
            ean.setHint("");
            mFoundBookEan = savedInstanceState.getString(FOUND_BOOK_EAN_STR, null);

            if (mFoundBookEan != null) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, mFoundBookEan);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                restartLoader();
            }
        }

        return rootView;
    }

    // scanner results
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d("add book", "in onActivityResult view");
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        mScannedEan = scanResult.getContents();
        if (mScannedEan != null) {
            // handle scan result
            Log.d("add book", mScannedEan);
            clearFields();
            ean.setText("");

            //catch isbn10 numbers
            if(mScannedEan.length()==10 && !mScannedEan.startsWith("978")){
                mScannedEan="978"+ mScannedEan;
            }
            if(mScannedEan.length()<13){
                clearFields();
                return;
            }
            //Once we have an ISBN, start a book intent
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, mScannedEan);
            bookIntent.setAction(BookService.FETCH_BOOK);
            getActivity().startService(bookIntent);
//            restartLoader();
            mFoundBookEan = mScannedEan;
        }
        // we could show a message when scan doesn't finish, but need to think about how to
        //      differentiate between a canceled scan and a scan error
//        else {
//            // else continue with any other code you need in the method
//            CharSequence text = "Unable to get scan. Please try again.";
//            int duration = Toast.LENGTH_SHORT;
//
//            Toast toast = Toast.makeText(getContext(), text, duration);
//            toast.show();
//        }

    }

    public void restartLoader(){
        Log.d("add book", "in restartLoader");
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d("add book", "in onCreateLoader view");
        if(ean.getText().length()==0 && mScannedEan.length() ==0 && mFoundBookEan == null){
            clearFields();
            return null;
        }
        String eanStr;
        if (ean.getText().toString().length() == 10 || ean.getText().toString().length() == 13) {
            eanStr = ean.getText().toString();
        }
        else if (mScannedEan.length() == 10 || mScannedEan.length() == 13){
            eanStr = mScannedEan;
        } else {
            eanStr = mFoundBookEan;
        }

        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.d("add book", "in onLoadFinished");
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        Log.d("add book", "in clearFields");
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
