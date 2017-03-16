package com.app.innovationweek.util;

import android.os.AsyncTask;
import android.util.Log;

import com.app.innovationweek.callbacks.DaoOperationComplete;
import com.app.innovationweek.model.Leaderboard;
import com.app.innovationweek.model.LeaderboardEntry;
import com.app.innovationweek.model.User;
import com.app.innovationweek.model.dao.DaoSession;
import com.app.innovationweek.model.dao.LeaderboardDao;
import com.app.innovationweek.model.dao.LeaderboardEntryDao;
import com.app.innovationweek.model.dao.UserDao;
import com.google.firebase.database.DataSnapshot;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by zeeshan on 3/14/2017.
 */

public class LeaderboardEntryUpdateTask extends AsyncTask<DataSnapshot, Void, LeaderboardEntry> {
    public static final String TAG = LeaderboardEntryUpdateTask.class.getSimpleName();
    private WeakReference<DaoOperationComplete> daoOperationCompleteWeakReference;
    private WeakReference<DaoSession> daoSessionWeakReference;
    private String quizId;

    public LeaderboardEntryUpdateTask(DaoSession daoSession, String quizId, DaoOperationComplete
                                              daoOperationComplete) {
        daoOperationCompleteWeakReference = new WeakReference<>(daoOperationComplete);
        daoSessionWeakReference = new WeakReference<>(daoSession);
        this.quizId = quizId;
    }

    @Override
    protected LeaderboardEntry doInBackground(DataSnapshot... dataSnapshots) {
        Leaderboard leaderboard;
        List<Leaderboard> leaderboardList;
        List<User> userList;
        String userId;
        LeaderboardEntry leaderboardEntry=null;
        List<LeaderboardEntry> leaderboardEntryList;
        for(DataSnapshot entrySnapshot:dataSnapshots) {
            leaderboardList = daoSessionWeakReference.get().getLeaderboardDao().queryBuilder().where(LeaderboardDao
                    .Properties.Id.eq(quizId)).list();
            if (leaderboardList != null && leaderboardList.size() > 0) {
                leaderboard = leaderboardList.get(0);
            } else {
                leaderboard = new Leaderboard(quizId);
                if(daoSessionWeakReference.get()!=null) daoSessionWeakReference.get().getLeaderboardDao().insert(leaderboard);
            }
            userId = entrySnapshot.getKey();//or entrySnapshot.child("userId").getValue(String
            // .class);
            User user;
            userList = daoSessionWeakReference.get().getUserDao().queryBuilder().where
                    (UserDao.Properties.Id.eq(userId)).list();
            if (userList != null && userList.size() > 0) {
                user = userList.get(0);
            } else {
                //fetch and insert the user
                Log.d(TAG, "invalid user found");
                return null;
            }

            leaderboardEntryList = daoSessionWeakReference.get()
                    .getLeaderboardEntryDao().queryBuilder().where(LeaderboardEntryDao.Properties
                            .LeaderboardId.eq(leaderboard.getId()), LeaderboardEntryDao.Properties
                            .UserId.eq(user.getId())).list();
            if (leaderboardEntryList != null && leaderboardEntryList.size() > 0) {
                //update
                leaderboardEntry = leaderboardEntryList.get(0);
                leaderboardEntry.setScore(entrySnapshot.child("totalScore").getValue(Float.class));
                leaderboardEntry.setTotalTime(entrySnapshot.child("totalTime").getValue(Long.class));
                leaderboardEntry.update();
            } else {
                //insert
                leaderboardEntry = new LeaderboardEntry();
                leaderboardEntry.setLeaderboard(leaderboard);
                leaderboardEntry.setUser(user);
                leaderboardEntry.setScore(entrySnapshot.child("totalScore").getValue(Float.class));
                leaderboardEntry.setTotalTime(entrySnapshot.child("totalTime").getValue(Long.class));
                daoSessionWeakReference.get().getLeaderboardEntryDao().insert(leaderboardEntry);
            }
        }
        return dataSnapshots.length>1?null:leaderboardEntry;
    }

    @Override
    protected void onPostExecute(LeaderboardEntry leaderboardEntry) {
        if (daoOperationCompleteWeakReference != null && daoOperationCompleteWeakReference.get() != null)
            daoOperationCompleteWeakReference.get().onDaoOperationComplete(leaderboardEntry);
        super.onPostExecute(leaderboardEntry);
    }
}