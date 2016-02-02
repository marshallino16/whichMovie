package genyus.com.whichmovie.task.manager;

import android.content.Context;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import genyus.com.whichmovie.api.APIConst;
import genyus.com.whichmovie.classes.RequestReturn;
import genyus.com.whichmovie.model.serializer.CategoriesSerializer;
import genyus.com.whichmovie.model.serializer.ConfigurationSerializer;
import genyus.com.whichmovie.model.serializer.CrewSerializer;
import genyus.com.whichmovie.model.serializer.ImageSerializer;
import genyus.com.whichmovie.model.serializer.MovieInfosSerializer;
import genyus.com.whichmovie.model.serializer.MovieSerializer;
import genyus.com.whichmovie.model.serializer.VideoSerializer;
import genyus.com.whichmovie.session.GlobalVars;
import genyus.com.whichmovie.task.listener.OnCategoriesListener;
import genyus.com.whichmovie.task.listener.OnConfigurationListener;
import genyus.com.whichmovie.task.listener.OnMovieCrewListener;
import genyus.com.whichmovie.task.listener.OnMovieImageListener;
import genyus.com.whichmovie.task.listener.OnMovieInfoListener;
import genyus.com.whichmovie.task.listener.OnMovieVideoListener;
import genyus.com.whichmovie.task.listener.OnMoviesListener;
import genyus.com.whichmovie.task.listener.OnNewMoviesListener;
import genyus.com.whichmovie.utils.AppUtils;
import genyus.com.whichmovie.utils.PreferencesUtils;
import genyus.com.whichmovie.utils.UnitsUtils;

/**
 * Created by genyus on 28/11/15.
 */
public class RequestManager {

    private static final int ATTEMPT_MAX = 2;

    private Context context = null;
    private int currentAttempt = 0;
    private static RequestManager instance = null;

    public static synchronized RequestManager getInstance(Context context) {
        if (null == instance) {
            instance = new RequestManager(context);
        }
        return instance;
    }

    private RequestManager(Context context) {
        this.context = context;
    }

    public void getConfigurations(OnConfigurationListener callback){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_CONFIGURATION, nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            Log.d(genyus.com.whichmovie.classes.Log.TAG, "configuration code = " + 200);
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "configuration json = " + returnedCode.json);
                ConfigurationSerializer.fillConfigurationObject(returnedCode.json);
                currentAttempt = 0;
                callback.OnConfigurationGet();
                return;
            } else {
                this.getConfigurations(callback);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnConfigurationFailed(null);
                return;
            } else {
                this.getConfigurations(callback);
            }
        }
    }

    public void getAllCategories(OnCategoriesListener callback){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_LIST_CATEGORIES, nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "categories json = " + returnedCode.json);
                CategoriesSerializer.fillCategoriesObject(returnedCode.json);
                currentAttempt = 0;
                callback.OnCategoriesGet();
                return;
            } else {
                this.getAllCategories(callback);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnCategoriesFailed(null);
                return;
            } else {
                this.getAllCategories(callback);
            }
        }
    }

    public void getMoviesFromCategory(Context context, OnMoviesListener callback){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        nameValuePairs.add(new BasicNameValuePair("with_genres", String.valueOf(PreferencesUtils.getDefaultCategory(context))));
        nameValuePairs.add(new BasicNameValuePair("include_adult", "true"));
        nameValuePairs.add(new BasicNameValuePair("release_date.lte", UnitsUtils.getNowTime()));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }

        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_LIST_MOVIES_CATEGORY, nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies json = " + returnedCode.json);

                MovieSerializer.fillMoviesObject(returnedCode.json, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getMoviesFromCategory(context, callback);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnMoviesFailed(null);
                return;
            } else {
                this.getMoviesFromCategory(context, callback);
            }
        }
    }

    public void getNewMoviesFromCategory(Context context, OnNewMoviesListener callback){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        nameValuePairs.add(new BasicNameValuePair("with_genres", String.valueOf(PreferencesUtils.getDefaultCategory(context))));
        nameValuePairs.add(new BasicNameValuePair("include_adult", "true"));
        nameValuePairs.add(new BasicNameValuePair("page", String.valueOf(GlobalVars.getPage())));
        nameValuePairs.add(new BasicNameValuePair("release_date.lte", UnitsUtils.getNowTime()));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }

        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_LIST_MOVIES_CATEGORY, nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies json = " + returnedCode.json);

                MovieSerializer.fillNewMoviesObject(returnedCode.json, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getNewMoviesFromCategory(context, callback);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnNewMoviesFailed(null);
                return;
            } else {
                this.getNewMoviesFromCategory(context, callback);
            }
        }
    }

    public void getMovieInfos(Context context, OnMovieInfoListener callback, int movieId){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_INFO_MOVIE(movieId), nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies info json = " + returnedCode.json);

                MovieInfosSerializer.fillConfigurationObject(returnedCode.json, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getMovieInfos(context, callback, movieId);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnMovieInfosFailed(null);
                return;
            } else {
                this.getMovieInfos(context, callback, movieId);
            }
        }
    }

    /**
     * Using date ranges to avoid many calls
     * @param context
     * @param callback
     */
    @Deprecated
    public void getMoviePlaying(Context context, OnMoviesListener callback){
        /*currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        nameValuePairs.add(new BasicNameValuePair("page", String.valueOf(GlobalVars.getPage())));
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_NOW_PLAYING, nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies info json = " + returnedCode.json);

                MovieSerializer.fillListMoviesObject(returnedCode.json);
                currentAttempt = 0;
                if(GlobalVars.page < GlobalVars.totalPlayingPages){
                    GlobalVars.page += 1;
                    this.getMoviePlaying(context, callback);
                } else {
                    getMoviesFromCategory(context, callback);
                }
                return;
            } else {
                this.getMoviePlaying(context, callback);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                return;
            } else {
                this.getMoviePlaying(context, callback);
            }
        }*/
    }

    public void getMovieCrew(OnMovieCrewListener callback, int movieId){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_CREW_MOVIE(movieId), nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies crew json = " + returnedCode.json);

                CrewSerializer.fillCategoriesObject(returnedCode.json, movieId, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getMovieCrew(callback, movieId);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnMovieCrewFailed(null);
                return;
            } else {
                this.getMovieCrew(callback, movieId);
            }
        }
    }

    public void getMovieImages(OnMovieImageListener callback, int movieId){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_IMAGES_MOVIE(movieId), nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies image json = " + returnedCode.json);

                ImageSerializer.fillImagesObject(returnedCode.json, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getMovieImages(callback, movieId);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnMovieImageFailed(null);
                return;
            } else {
                this.getMovieImages(callback, movieId);
            }
        }
    }

    public void getMovieVideos(OnMovieVideoListener callback, int movieId){
        currentAttempt += 1;
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("api_key", APIConst.API_TOKEN));
        if(AppUtils.isDeviceInFrench()){
            nameValuePairs.add(new BasicNameValuePair("language", "fr"));
        }
        RequestReturn returnedCode = RequestSender.sendRequestGet(APIConst.API_BASE_URL, APIConst.API_VIDEOS_MOVIE(movieId), nameValuePairs);
        if (null != returnedCode && !returnedCode.json.contains("Authentication error")) {
            if (200 == returnedCode.code) {
                Log.d(genyus.com.whichmovie.classes.Log.TAG, "movies video json = " + returnedCode.json);

                VideoSerializer.fillVideosObject(returnedCode.json, callback);
                currentAttempt = 0;
                return;
            } else {
                this.getMovieVideos(callback, movieId);
            }
        } else {
            if (ATTEMPT_MAX == currentAttempt) {
                currentAttempt = 0;
                callback.OnMovieVideoFailed(null);
                return;
            } else {
                this.getMovieVideos(callback, movieId);
            }
        }
    }
}
