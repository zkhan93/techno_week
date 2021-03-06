package com.app.iw.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.app.iw.model.dao.DaoSession;
import com.app.iw.model.dao.PhaseDao;
import com.app.iw.model.dao.RuleDao;
import com.google.firebase.database.IgnoreExtraProperties;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zeeshan on 3/13/2017.
 */
@Entity
@IgnoreExtraProperties
public class Phase implements Parcelable, Comparable<Phase> {
    public static final Creator<Phase> CREATOR = new Creator<Phase>() {
        @Override
        public Phase createFromParcel(Parcel in) {
            return new Phase(in);
        }

        @Override
        public Phase[] newArray(int size) {
            return new Phase[size];
        }
    };
    @ToMany(referencedJoinProperty = "phaseId")
    List<Rule> rules;
    @Id(autoincrement = true)
    private Long id;
    private long startDate;
    private String eventId;
    private String name;
    private String leaderboardId; //merely eventId
    private String leaderboardType;
    private int sortOrder;
    /**
     * Used to resolve relations
     */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /**
     * Used for active entity operations.
     */
    @Generated(hash = 1887545964)
    private transient PhaseDao myDao;


    protected Phase(Parcel in) {
        id = in.readLong();
        startDate = in.readLong();
        eventId = in.readString();
        leaderboardId = in.readString();
        leaderboardType = in.readString();
        sortOrder = in.readInt();
        rules = new ArrayList<>();
        in.readTypedList(rules, Rule.CREATOR);
    }


    @Generated(hash = 123237717)
    public Phase(Long id, long startDate, String eventId, String name,
                 String leaderboardId, String leaderboardType, int sortOrder) {
        this.id = id;
        this.startDate = startDate;
        this.eventId = eventId;
        this.name = name;
        this.leaderboardId = leaderboardId;
        this.leaderboardType = leaderboardType;
        this.sortOrder = sortOrder;
    }


    @Generated(hash = 184327826)
    public Phase() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(startDate);
        parcel.writeString(eventId);
        parcel.writeString(leaderboardId);
        parcel.writeString(leaderboardType);
        parcel.writeInt(sortOrder);
        parcel.writeTypedList(rules);
    }

    @Override
    public int compareTo(@NonNull Phase o) {
        return this.sortOrder - o.sortOrder;
    }


    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public long getStartDate() {
        return this.startDate;
    }


    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }


    public String getEventId() {
        return this.eventId;
    }


    public void setEventId(String eventId) {
        this.eventId = eventId;
    }


    public String getName() {
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getLeaderboardId() {
        return this.leaderboardId;
    }


    public void setLeaderboardId(String leaderboardId) {
        this.leaderboardId = leaderboardId;
    }


    public String getLeaderboardType() {
        return this.leaderboardType;
    }


    public void setLeaderboardType(String leaderboardType) {
        this.leaderboardType = leaderboardType;
    }


    public int getSortOrder() {
        return this.sortOrder;
    }


    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }


    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 148447646)
    public List<Rule> getRules() {
        if (rules == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            RuleDao targetDao = daoSession.getRuleDao();
            List<Rule> rulesNew = targetDao._queryPhase_Rules(id);
            synchronized (this) {
                if (rules == null) {
                    rules = rulesNew;
                }
            }
        }
        return rules;
    }


    /**
     * Resets a to-many relationship, making the next get call to query for a fresh result.
     */
    @Generated(hash = 53842526)
    public synchronized void resetRules() {
        rules = null;
    }


    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }


    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }


    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }


    /**
     * called by internal mechanisms, do not call yourself.
     */
    @Generated(hash = 1510019989)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getPhaseDao() : null;
    }

}
