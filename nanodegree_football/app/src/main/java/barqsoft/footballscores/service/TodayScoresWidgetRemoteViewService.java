package barqsoft.footballscores.service;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by webteam on 10/12/15.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TodayScoresWidgetRemoteViewService extends RemoteViewsService {
    public final String LOG_TAG = TodayScoresWidgetRemoteViewService.class.getSimpleName();

    private static final String[] MATCH_COLUMNS = {
            DatabaseContract.SCORES_TABLE + "." + DatabaseContract.scores_table._ID,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.TIME_COL
    };

    static final int INDEX_SCORES_ID = 0;
    static final int INDEX_HOME_TEAM = 1;
    static final int INDEX_AWAY_TEAM = 2;
    static final int INDEX_HOME_GOALS = 3;
    static final int INDEX_AWAY_GOALS = 4;
    static final int INDEX_MATCH_TIME = 5;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                Date fragmentdate = new Date(System.currentTimeMillis());
                SimpleDateFormat mformat = new SimpleDateFormat(getString(R.string.match_date_format));
                String formattedDate = mformat.format(fragmentdate);
                Uri scoresForDateUri = DatabaseContract.scores_table.buildScoreWithDate();
                data = getContentResolver().query(scoresForDateUri,
                        MATCH_COLUMNS,
                        null,
                        new String[] {formattedDate},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String homeName = data.getString(INDEX_HOME_TEAM);
                String awayName = data.getString(INDEX_AWAY_TEAM);
                int homeGoals = data.getInt(INDEX_HOME_GOALS);
                int awayGoals = data.getInt(INDEX_AWAY_GOALS);
                String matchTime = data.getString(INDEX_MATCH_TIME);

                views.setTextViewText(R.id.widget_home_name, homeName);

                views.setTextViewText(R.id.widget_away_name, awayName);


                views.setTextViewText(R.id.widget_data_textview, matchTime);

                views.setTextViewText(R.id.widget_score_textview, Utilies.getScores(homeGoals, awayGoals));

                //set crests
                views.setImageViewResource(R.id.widget_home_crest,Utilies.getTeamCrestByTeamName(homeName));
                views.setImageViewResource(R.id.widget_away_crest, Utilies.getTeamCrestByTeamName(awayName));

                //set content descriptions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    String homeNameWithVersus = getString(R.string.first_team_name_description, homeName, awayName);
                    views.setContentDescription(R.id.widget_home_name, homeNameWithVersus);
                    String matchTimeDescription = getString(R.string.match_time_description, matchTime);
                    views.setContentDescription(R.id.widget_data_textview, matchTimeDescription);
                    String scoreDescription = getString(R.string.score_description, homeName + homeGoals, awayName + awayGoals);
                    views.setContentDescription(R.id.widget_score_textview, scoreDescription);
                }

                final Intent fillInIntent = new Intent();
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_SCORES_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
