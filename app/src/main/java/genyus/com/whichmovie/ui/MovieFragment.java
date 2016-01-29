package genyus.com.whichmovie.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.greenfrvr.hashtagview.HashtagView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import genyus.com.whichmovie.MainActivity;
import genyus.com.whichmovie.R;
import genyus.com.whichmovie.adapter.CrewRecyclerViewAdapter;
import genyus.com.whichmovie.adapter.ImageAdapter;
import genyus.com.whichmovie.listener.OnMoviePassed;
import genyus.com.whichmovie.model.Crew;
import genyus.com.whichmovie.model.Genre;
import genyus.com.whichmovie.model.Image;
import genyus.com.whichmovie.model.Movie;
import genyus.com.whichmovie.session.GlobalVars;
import genyus.com.whichmovie.task.listener.OnMovieCrewListener;
import genyus.com.whichmovie.task.listener.OnMovieImageListener;
import genyus.com.whichmovie.task.listener.OnMovieInfoListener;
import genyus.com.whichmovie.task.manager.RequestManager;
import genyus.com.whichmovie.utils.PicassoTrustAll;
import genyus.com.whichmovie.utils.ThemeUtils;
import genyus.com.whichmovie.utils.UnitsUtils;
import genyus.com.whichmovie.view.CurrencyTextView;
import genyus.com.whichmovie.view.ExpandableHeightGridView;

/**
 * Created by genyus on 29/11/15.
 */
public class MovieFragment extends Fragment implements ObservableScrollViewCallbacks, OnMovieInfoListener, OnMovieCrewListener, OnMovieImageListener {

    private Activity activity;

    private Movie movie;
    private View view;

    private float height = 0;

    //Views
    private FloatingActionButton next;
    private View margin, overlay;
    private TextView title, vote, synopsis, productionCompanies;
    private CurrencyTextView budget, revenue;
    private ImageView poster, posterBlur;
    private HashtagView hashtags;
    private RecyclerView listCast;
    private ExpandableHeightGridView listImages;

    private LinearLayout header;
    private FrameLayout posterBlurContainer;
    private ObservableScrollView scrollView;
    private RelativeLayout ratingBarContainer;

    /**
     * @param movie
     * @return
     */
    public static MovieFragment newInstance(Movie movie) {
        MovieFragment fragmentFirst = new MovieFragment();
        Bundle args = new Bundle();
        args.putSerializable("movie", movie);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (null != bundle && bundle.containsKey("movie")) {
            movie = (Movie) bundle.getSerializable("movie");
            new Thread() {
                public void run() {
                    RequestManager.getInstance(MovieFragment.this.activity).getMovieInfos(MovieFragment.this.activity, MovieFragment.this, movie.getId());
                    RequestManager.getInstance(MovieFragment.this.activity).getMovieCrew(MovieFragment.this, movie.getId());
                    RequestManager.getInstance(MovieFragment.this.activity).getMovieImages(MovieFragment.this, movie.getId());
                }
            }.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PicassoTrustAll.getInstance(activity).cancelRequest(targetPoster);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_movie, container, false);

        next = (FloatingActionButton) view.findViewById(R.id.fab);
        margin = view.findViewById(R.id.margin);
        overlay = view.findViewById(R.id.overlay);
        budget = (CurrencyTextView) view.findViewById(R.id.budget);
        revenue = (CurrencyTextView) view.findViewById(R.id.revenue);
        poster = (ImageView) view.findViewById(R.id.poster);
        posterBlur = (ImageView) view.findViewById(R.id.posterBlur);
        vote = (TextView) view.findViewById(R.id.vote);
        title = (TextView) view.findViewById(R.id.title);
        productionCompanies = (TextView) view.findViewById(R.id.productionCompanies);
        hashtags = (HashtagView) view.findViewById(R.id.hashtags);
        synopsis = (TextView) view.findViewById(R.id.synopsis);
        listCast = (RecyclerView) view.findViewById(R.id.cast);
        listImages = (ExpandableHeightGridView) view.findViewById(R.id.images);
        posterBlurContainer = (FrameLayout) view.findViewById(R.id.posterBlurContainer);
        ratingBarContainer = (RelativeLayout) view.findViewById(R.id.ratingBarContainer);

        header = (LinearLayout) view.findViewById(R.id.header);
        scrollView = (ObservableScrollView) view.findViewById(R.id.scroll);
        scrollView.requestFocus();

        overlay.setAlpha(0);
        posterBlurContainer.setAlpha(0);

        //header image loading
        PicassoTrustAll.getInstance(getActivity()).load(GlobalVars.configuration.getBase_url() + GlobalVars.configuration.getPoster_sizes().get(GlobalVars.configuration.getPoster_sizes().size() - 2) + movie.getPoster_path()).noPlaceholder().into(targetPoster);

        title.setText(""+Html.fromHtml("<b>"+movie.getTitle()+"</b>"));
        synopsis.setText("" + movie.getOverview());

        //scroll settingup
        height = UnitsUtils.getScreenPercentHeightSize(getActivity(), 83f);
        margin.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(height)));
        scrollView.setTouchInterceptionViewGroup((ViewGroup) view.findViewById(R.id.fragment_root));
        scrollView.setScrollViewCallbacks(this);

        //rating
        View progressAlpha = new View(getActivity(), null);
        View progress = new View(getActivity(), null);

        progressAlpha.setBackgroundResource(R.drawable.round_progress_alpha);
        progress.setBackgroundResource(R.drawable.round_progress);

        float halfWidth = UnitsUtils.getScreenPercentWidthSize(getActivity(), 50.0f);
        float progressWidth = halfWidth * ((movie.getVote_average()) * 10.0f / 100f);

        RelativeLayout.LayoutParams lpAlpha = new RelativeLayout.LayoutParams(Math.round(halfWidth), ViewGroup.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams(Math.round(progressWidth), ViewGroup.LayoutParams.MATCH_PARENT);

        ratingBarContainer.addView(progress, 0, lpProgress);
        ratingBarContainer.addView(progressAlpha, 0, lpAlpha);

        vote.setText(Html.fromHtml("<strong>" + movie.getVote_average() + "</strong><small>/10</small>"));

        //tags
        hashtags.setData(movie.getGenres(), new HashtagView.DataTransform<Genre>() {
            @Override
            public CharSequence prepare(Genre genre) {
                String label = "" + genre.getName();
                return label;
            }

        });
        hashtags.addOnTagClickListener(new HashtagView.TagsClickListener() {
            @Override
            public void onItemClicked(Object item) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "Tag click = " + item.toString());
            }
        });

        //cast
        listCast.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        listCast.setLayoutManager(layoutManager);

        //next
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "Next clicked");
                if(activity instanceof MainActivity){
                    ((OnMoviePassed) activity).OnMoviePassed(MovieFragment.this);
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Nullable
    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        int minOverlayTransitionY = -Math.round(height);
        int minOverlayTransitionYTitle = -Math.round(height);
        float flexibleRange = height - UnitsUtils.actionBarSize(getActivity());

        header.setTranslationY(ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));
        title.setTranslationY(ScrollUtils.getFloat(-scrollY / 3.8f, minOverlayTransitionYTitle, 0));

        poster.setTranslationY(ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0) / 9);
        posterBlur.setTranslationY(ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0) / 9);

        overlay.setAlpha(ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));
        title.setAlpha(1 - ScrollUtils.getFloat((float) scrollY/ flexibleRange, 0, 1));
        posterBlurContainer.setAlpha(ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));
        ratingBarContainer.setAlpha(1 - ScrollUtils.getFloat((float) scrollY * 2.4f / flexibleRange, 0, 1));
        hashtags.setAlpha(1 - ScrollUtils.getFloat((float) scrollY * 2.4f / flexibleRange, 0, 1));
        ((MainActivity) getActivity()).categories.setTranslationY(ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionYTitle, 0));
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    Target targetPoster = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            poster.setImageBitmap(bitmap );
            posterBlur.setImageBitmap(blurBitmap(bitmap));

            posterBlur.setScaleX(1.2f);
            posterBlur.setScaleY(1.2f);
            poster.setScaleX(1.2f);
            poster.setScaleY(1.2f);

            Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch darkMuted = palette.getDarkMutedSwatch();
                    if (darkMuted != null) {
                        next.setBackgroundTintList(ColorStateList.valueOf(darkMuted.getRgb()));
                        ThemeUtils.ColorStatusBar(getActivity(), darkMuted.getRgb());
                    }
                }
            });
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    public Bitmap blurBitmap(Bitmap bitmap) {

        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript rs = RenderScript.create(this.activity);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        blurScript.setRadius(25.f);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outBitmap);
        //bitmap.recycle();
        rs.destroy();
        return outBitmap;
    }

    @Override
    public void OnMovieInfosGet() {
    }

    @Override
    public void OnMovieInfosFailed(String reason) {
        Log.e(genyus.com.whichmovie.classes.Log.TAG, "Error getting movie info : " + reason);
    }

    @Override
    public void OnMovieCrewGet() {
    }

    @Override
    public void OnMovieCrewFailed(String reason) {
        Log.e(genyus.com.whichmovie.classes.Log.TAG, "Error getting movie crew : " + reason);
    }

    @Override
    public void OnMovieImageGet() {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != getActivity()) {
                    //infos
                    title.setText(Html.fromHtml("<b>" + movie.getTitle() + "</b><small> - "+movie.getRuntime()+" min</small>"));
                    if(0 != movie.getBudget()){
                        budget.setText(""+movie.getBudget());
                    } else {
                        budget.setText(R.string.unknown);
                    }

                    if(0 != movie.getRevenue()){
                        revenue.setText(""+movie.getRevenue());
                    } else {
                        revenue.setText(R.string.unknown);
                    }

                    //production
                    CharSequence companies = null;
                    for(int i=0 ; i<movie.getProductionCompanies().size() ; ++i){
                        if(null != companies){
                            if(i == movie.getProductionCompanies().size()-1){
                                companies = android.text.TextUtils.concat(companies, Html.fromHtml(" & "+"<i><u>"+movie.getProductionCompanies().get(i)+"</u></i>"));
                            } else {
                                companies = android.text.TextUtils.concat(companies, Html.fromHtml(", "+"<i><u>"+movie.getProductionCompanies().get(i)+"</u></i> "));
                            }
                        } else {
                            companies = Html.fromHtml(getResources().getString(R.string.producted_by)+" "+"<i><u>"+movie.getProductionCompanies().get(i)+"</u></i> ");
                        }
                    }
                    for(int i=0 ; i<movie.getProductionCompanies().size() ; ++i){

                    }
                    productionCompanies.setText(companies);

                    //crew
                    ArrayList<Crew> listCrew = movie.getCrew();
                    if(listCrew.size() > 21){
                        listCrew = new ArrayList<>(movie.getCrew().subList(0, 20));
                    }
                    final CrewRecyclerViewAdapter castAdapter = new CrewRecyclerViewAdapter(getActivity(), listCrew);
                    castAdapter.setOnItemClickListener(new CrewRecyclerViewAdapter.OnCrewItemClickListener() {
                        @Override
                        public void onItemClick(int position, View v) {
                            if(movie.getCrew().get(position).isClicked){
                                movie.getCrew().get(position).isClicked = false;
                            } else {
                                movie.getCrew().get(position).isClicked = true;
                            }
                            castAdapter.notifyDataSetChanged();
                        }
                    });
                    listCast.setAdapter(castAdapter);

                    //images
                    ArrayList<Image> listImage =  movie.getImages();
                    if(listImage.size() > 10){
                        listImage = new ArrayList<>( movie.getImages().subList(0, 9));
                    }
                    Log.d(genyus.com.whichmovie.classes.Log.TAG, "movie image get");
                    ImageAdapter imageAdapter = new ImageAdapter(getContext(), listImage);
                    listImages.setNumColumns(2);
                    listImages.setAdapter(imageAdapter);
                    listImages.setExpanded(true);
                }
            }
        });
    }

    @Override
    public void OnMovieImageFailed(String reason) {
        Log.e(genyus.com.whichmovie.classes.Log.TAG, "Error getting images crew : " + reason);
    }
}
