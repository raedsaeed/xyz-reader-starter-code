package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private String bodyText;
    private Button seeMore;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private NestedScrollView mScrollView;
    private ColorDrawable mStatusBarColorDrawable;
    private int textoffset;
    private TextView bodyView;

    private int mTopInset;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = mRootView.findViewById(R.id.photo);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        bodyView = mRootView.findViewById(R.id.article_body);
        if (mCursor != null) {
            Toolbar mToolbar = mRootView.findViewById(R.id.toolbar);
            getActivityCast().setSupportActionBar(mToolbar);
            ActionBar actionBar = getActivityCast().getSupportActionBar();
            if (actionBar != null){
                actionBar.setDisplayHomeAsUpEnabled(true);
            }


            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                Log.d(TAG, "onResponse: adding image to it's position");
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                    });

            // from https://github.com/AbbieTurner95/XYZ_Reader_AndroidApp
            textoffset = 0;
            bodyText = mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />");
            String bodyText2 = bodyText.substring(0, (bodyText.length() <= 600 ? bodyText.length() : 1000));
            bodyView.setText(Html.fromHtml(bodyText2));

            seeMore =  mRootView.findViewById(R.id.read_more);
            seeMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textoffset += 1000;
                    if (bodyText.length() > textoffset) {
                        String bodyText3 = bodyText.substring(0, ((bodyText.length() - textoffset) <= 1000 ? bodyText.length() : textoffset + 1000));
                        bodyView.setText(Html.fromHtml(bodyText3));
                    } else {
                        seeMore.setVisibility(View.GONE);
                    }
                }
            });


        } else {
            mRootView.setVisibility(View.VISIBLE);
            bodyView.setText("Some text I don't know about !!");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

}