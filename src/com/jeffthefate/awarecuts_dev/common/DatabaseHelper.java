package com.jeffthefate.awarecuts_dev.common;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.ApplicationEx;
/**
 * Executes all the database actions, including many helper functions and
 * constants.
 * 
 * @author Jeff Fate
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    /**
     * Name of the saved database
     */
    private static final String DB_NAME = "awareCutsDb";
    /**
     * Name of the apps table
     */
    public static final String APP_TABLE = "Apps";
    /**
     * Name of the bookmarks table
     */
    public static final String BOOKMARK_TABLE = "Bookmarks";
    /**
     * Name of the contacts table
     */
    public static final String CONTACT_TABLE = "Contacts";
    /**
     * Name of the contexts table
     */
    public static final String CONTEXT_TABLE = "Contexts";
    /**
     * Name of the app ID column
     */
    public static final String COL_APP_ID = "AppID";
    /**
     * Name of the app task column
     */
    public static final String COL_TASK = "TaskBytes";
    /**
     * Name of the app name column
     */
    public static final String COL_NAME = "AppName";
    /**
     * Name of the app package name column
     */
    public static final String COL_PACKAGE = "PackageName";
    /**
     * Name of the app activity name column
     */
    public static final String COL_ACTIVITY = "ActivityName";
    /**
     * Name of the bookmark ID column
     */
    static final String COL_BOOK_ID = "BookmarkID";
    /**
     * Name of the bookmark address column
     */
    public static final String COL_ADDRESS = "BookmarkAddress";
    /**
     * Name of the bookmark title column
     */
    public static final String COL_BOOK_TITLE = "BookmarkTitle";
    /**
     * Name of the bookmark icon column
     */
    static final String COL_BOOK_ICON = "BookmarkIcon";
    /**
     * Name of the bookmark visits column
     */
    static final String COL_BOOK_VISITS = "BookmarkVisits";
    /**
     * Name of the contact id column
     */
    static final String COL_CONTACT_ID = "ContactID";
    /**
     * Name of the contact lookup key column
     */
    public static final String COL_LOOKUP = "ContactLookupKey";
    /**
     * Name of the contact name column
     */
    public static final String COL_CONTACT_NAME = "ContactName";
    /**
     * Name of the contact last time contacted column
     */
    static final String COL_CONTACT_TIME = "ContactTime";
    /**
     * Name of the context ID column
     */
    static final String COL_CONTEXT_ID = "COL_CONTEXT_ID";
    /**
     * Name of the context latitude column
     */
    static final String COL_LAT = "Latitude";
    /**
     * Name of the context longitude column
     */
    static final String COL_LONG = "Longitude";
    /**
     * Name of the context time column
     */
    static final String COL_TIME = "Time";
    /**
     * Name of the context bluetooth column
     */
    static final String COL_BLUE = "Bluetooth";
    /**
     * Name of the context wifi column
     */
    static final String COL_WIFI = "Wifi";
    /**
     * Name of the context battery column
     */
    static final String COL_BATT = "Battery";
    /**
     * Name of the context day column
     */
    static final String COL_DAY = "Day";
    /**
     * Name of the context headphone column
     */
    static final String COL_HPHONE = "Headphone";
    /**
     * Name of the context duration, for apps
     */
    public static final String COL_DUR = "Duration";
    /**
     * Create app table string
     */
    private static final String CREATE_APP_TABLE = "CREATE TABLE " + APP_TABLE + 
        " (" + COL_APP_ID + " INTEGER PRIMARY KEY, " + COL_NAME + " TEXT, "+ 
        COL_TASK + " BLOB, " + COL_PACKAGE + " TEXT, " + COL_ACTIVITY + 
        " TEXT)";
    /**
     * Create bookmark table string
     */
    private static final String CREATE_BOOKMARK_TABLE = "CREATE TABLE " + 
        BOOKMARK_TABLE + " (" + COL_BOOK_ID + " INTEGER PRIMARY KEY, " + 
        COL_ADDRESS + " TEXT, " + COL_BOOK_TITLE + " TEXT, " + 
        COL_BOOK_ICON + " BLOB, " + COL_BOOK_VISITS + " INTEGER)";
    /**
     * Create contact table string
     */
    private static final String CREATE_CONTACT_TABLE = "CREATE TABLE " + 
        CONTACT_TABLE + " (" + COL_CONTACT_ID + " INTEGER PRIMARY KEY, " + 
        COL_LOOKUP + " TEXT, " + COL_CONTACT_NAME + " TEXT, " + 
        COL_CONTACT_TIME + " INTEGER)";
    /**
     * Create context table string
     */
    private static final String CREATE_CONTEXT_TABLE = "CREATE TABLE " + 
        CONTEXT_TABLE + " (" + COL_CONTEXT_ID + " INTEGER PRIMARY KEY, " + 
        COL_TIME + " INTEGER, " + COL_LAT + " DOUBLE, " + COL_LONG + 
        " DOUBLE," + COL_DAY + " INTEGER, " + COL_BATT + " INTEGER, " + 
        COL_WIFI + " INTEGER, " + COL_BLUE + " INTEGER, " + COL_HPHONE + 
        " INTEGER, " + COL_DUR + " INTEGER, " + COL_APP_ID + " INTEGER, " + 
        COL_BOOK_ID + " INTEGER, " + COL_CONTACT_ID + " INTEGER)";
    /**
     * Start of selection text to find relevant contexts, for apps 
     */
    public static final String APP_JOIN_TEXT = "SELECT " + COL_NAME + "," +
        CONTEXT_TABLE + "." + COL_APP_ID + "," + CONTEXT_TABLE + "." + 
        COL_BOOK_ID + "," + CONTEXT_TABLE + "." + COL_CONTACT_ID + "," + 
        COL_CONTEXT_ID + "," + CONTEXT_TABLE + "." + COL_DUR + " FROM " + 
        APP_TABLE + "," + CONTEXT_TABLE + " WHERE " + APP_TABLE + "." + 
        COL_APP_ID + "=" + CONTEXT_TABLE + "." + COL_APP_ID;
    /**
     * Start of selection text to find relevant contexts, for bookmarks 
     */
    public static final String BOOKMARK_JOIN_TEXT = "SELECT " + COL_ADDRESS + 
        "," + CONTEXT_TABLE + "." + COL_APP_ID + "," + CONTEXT_TABLE + "." + 
        COL_BOOK_ID + "," + CONTEXT_TABLE + "." + COL_CONTACT_ID + "," + 
        COL_CONTEXT_ID + "," + CONTEXT_TABLE + "." + COL_DUR + " FROM " + 
        BOOKMARK_TABLE + "," + CONTEXT_TABLE + " WHERE " + BOOKMARK_TABLE + 
        "." + COL_BOOK_ID + "=" + CONTEXT_TABLE + "." + COL_BOOK_ID;
    /**
     * Start of selection text to find relevant contexts, for contacts 
     */
    public static final String CONTACT_JOIN_TEXT = "SELECT " + COL_LOOKUP + 
        "," + CONTEXT_TABLE + "." + COL_APP_ID + "," + CONTEXT_TABLE + "." + 
        COL_BOOK_ID + "," + CONTEXT_TABLE + "." + COL_CONTACT_ID + "," + 
        COL_CONTEXT_ID + "," + CONTEXT_TABLE + "." + COL_DUR + " FROM " + 
        CONTACT_TABLE + "," + CONTEXT_TABLE + " WHERE " + CONTACT_TABLE + "." + 
        COL_CONTACT_ID + "=" + CONTEXT_TABLE + "." + COL_CONTACT_ID;
    /**
     * Max number of context table entries (ensure a shorter db query)
     */
    private final int MAX_CONTEXTS = 3000;
    /**
     * Device's current time of day
     */
    private long mCurrTime;
    /**
     * Device's current time of day plus the range
     */
    private long mTopRange;
    /**
     * Device's current time of day minus the range
     */
    private long mBottomRange;
    /**
     * Device's current day of the week
     */
    private int mCurrDay;
    /**
     * Device's current location latitude
     */
    private double mCurrLat;
    /**
     * Device's current location longitude
     */
    private double mCurrLong;
    /**
     * Device's current location latitude plus the range
     */
    private double mMaxLat;
    /**
     * Device's current location longitude plus the range
     */
    private double mMaxLong;
    /**
     * Device's current location latitude minus the range
     */
    private double mMinLat;
    /**
     * Device's current location longitude minus the range
     */
    private double mMinLong;
    /**
     * Device's current battery level
     */
    private int mCurrBatt;
    /**
     * Device's current battery level plus the range
     */
    private int mMaxBatt;
    /**
     * Device's current battery level minus the range
     */
    private int mMinBatt;
    /**
     * Device's current headphone status (wired and wireless)
     */
    private int mCurrHphone;
    /**
     * Device's current wifi status
     */
    private int mCurrWifi;
    /**
     * Device's current bluetooth status
     */
    private int mCurrBlue;
    /**
     * Set of sql queries to filter the contexts based on attributes
     */
    private String[] mQueries = {
            CONTEXT_TABLE + "." + COL_TIME + "<=" + mTopRange + " AND " + 
                CONTEXT_TABLE + "." + COL_TIME + ">=" + mBottomRange,
            CONTEXT_TABLE + "." + COL_DAY + "=" + mCurrDay,
            CONTEXT_TABLE + "." + COL_LAT + "<=" + mMaxLat + " AND " +
                CONTEXT_TABLE + "." + COL_LAT + ">=" + mMinLat + " AND " +
                CONTEXT_TABLE + "." + COL_LONG + "<=" + mMaxLong + " AND " +
                CONTEXT_TABLE + "." + COL_LONG + ">=" + mMinLong,
            CONTEXT_TABLE + "." + COL_BATT + "<=" + mMaxBatt + " AND " +
                CONTEXT_TABLE + "." + COL_BATT + ">=" + mMinBatt,
            CONTEXT_TABLE + "." + COL_HPHONE + "=" + mCurrHphone,
            CONTEXT_TABLE + "." + COL_WIFI + "=" + mCurrWifi,
            CONTEXT_TABLE + "." + COL_BLUE + "=" + mCurrBlue};
    /**
     * Create the helper object that creates and manages the database.
     * @param context   the context used to create this object
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_APP_TABLE);
        db.execSQL(CREATE_BOOKMARK_TABLE);
        db.execSQL(CREATE_CONTACT_TABLE);
        db.execSQL(CREATE_CONTEXT_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + APP_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + BOOKMARK_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONTACT_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONTEXT_TABLE);
        onCreate(db);
    }
    /**
     * Creates a query that calculates how strong each context is.
     * @param text  the root query to use as a base to query from
     * @param level what relevant level the contexts are in
     * @return  query used to calculate how strong the contexts are
     */
    public String getFullQueryText(String text, int level) {
        return "SELECT " + COL_APP_ID + "," + COL_BOOK_ID + "," + 
            COL_CONTACT_ID + ", COUNT(" + COL_APP_ID + ")*" + 
            Math.pow(10, level) + " AS numAppId, COUNT(" + COL_BOOK_ID + ")*" + 
            Math.pow(10, level) + " AS numBookmarkId, COUNT(" + COL_CONTACT_ID + 
            ")*" + Math.pow(10, level) + " AS numContactId, TOTAL(" + 
            COL_DUR + ")*" + Math.pow(2, level) + " AS totalDuration " +
            "FROM (" + text + ") GROUP BY " + COL_APP_ID + "," + COL_BOOK_ID + 
            "," + COL_CONTACT_ID;
    }
    /**
     * Creates a query that counts how many contexts there are for each
     * app/bookmark/contact.
     * @param text  the root query to use as a base to query from
     * @return  query used to get the total context for each 
     *          app/bookmark/contact
     */
    public String getSumQueryText(String text) {
        return "SELECT " + COL_APP_ID + "," + COL_BOOK_ID + "," + COL_CONTACT_ID
            + ", SUM(numAppId) AS numAppId, SUM(numBookmarkId) AS " +
            "numBookmarkId, SUM(numContactId) AS numContactId, " + 
            "SUM(totalDuration) AS totalDuration FROM (" + text + 
            ") GROUP BY " + COL_APP_ID + "," + COL_BOOK_ID + "," + 
            COL_CONTACT_ID;
    }
    /**
     * Deletes any contexts that are older than 15 days.
     * @param mCurrTime device's current time
     */
    private void deleteStaleContexts(long mCurrTime) {
        String query = "SELECT " + COL_CONTEXT_ID + " FROM " + CONTEXT_TABLE + 
            " ORDER BY " + COL_CONTEXT_ID + " ASC";
        Cursor cur = ApplicationEx.db.rawQuery(query, null);
        int count = cur.getCount();
        if (count > MAX_CONTEXTS) {
            if (cur.moveToFirst()) {
                for (int i = 0; i < count-MAX_CONTEXTS; i++) {
                    ApplicationEx.dbHelper.deleteContext(
                            cur.getInt(0), COL_CONTEXT_ID);
                    cur.moveToNext();
                }
            }
        }
        cur.close();
    }
    /**
     * Put together the query string based on the type and the separate queries.
     * @param types which contexts to include
     * @param sbAppsQuery   the already created query for app contexts
     * @param sbBookmarksQuery  the already created query for bookmark contexts
     * @param sbContactsQuery   the already created query for contact contexts
     * @return  completed query
     */
    private String getQueryString(HashMap<String,Boolean> types,
            StringBuilder sbAppsQuery, StringBuilder sbBookmarksQuery,
            StringBuilder sbContactsQuery) {
        StringBuilder sbQuery = new StringBuilder(0);
        if (types.get("apps")) {
            sbQuery.append(sbAppsQuery);
            if (types.get("bookmarks"))
                sbQuery.append(" UNION ").append(sbBookmarksQuery);
            if (types.get("contacts"))
                sbQuery.append(" UNION ").append(sbContactsQuery);
        }
        else if (types.get("bookmarks")) {
            sbQuery.append(sbBookmarksQuery);
            if (types.get("contacts"))
                sbQuery.append(" UNION ").append(sbContactsQuery);
        }
        else if (types.get("contacts"))
            sbQuery.append(sbContactsQuery);
        return sbQuery.toString();
    }
    /**
     * Clear out the query strings and set them back up with the base.
     * @param sBuilders set of query strings
     * @return  set of query strings after they are reset
     */
    private StringBuilder[] setupStrings(StringBuilder[] sBuilders) {
        sBuilders[0].setLength(0);
        sBuilders[0].append(APP_JOIN_TEXT);
        sBuilders[1].setLength(0);
        sBuilders[1].append(BOOKMARK_JOIN_TEXT);
        sBuilders[2].setLength(0);
        sBuilders[2].append(CONTACT_JOIN_TEXT);
        return sBuilders;
    }
    /**
     * Top level of composing the entire query, navigating through all levels.
     * @param queries   set of queries for each attribute
     * @param level current level of query
     * @param sqlQueries    the already created app/bookmark/contact queries
     * @param lastQuery the query from the last level
     * @param types which types to include in the query
     * @return  query for the current level
     */
    private StringBuilder composeQuery(String[] queries, int level, 
            StringBuilder[] sqlQueries, StringBuilder lastQuery,
            HashMap<String,Boolean> types) {
        HashMap<Integer, Integer> iterators = new HashMap<Integer, Integer>();
        int temp = level;
        for (int i = queries.length-1; i >= 0; i--) {
            iterators.put(level, i);
            composeInnerQuery(++temp, i, queries, sqlQueries, iterators);
        }
        return lastQuery.append(" UNION ALL ").append(getFullQueryText(
                getQueryString(
                        types, sqlQueries[0], sqlQueries[1], sqlQueries[2]), 
                        level-1));
    }
    /**
     * Recursive algorithm to get the current level query and the following
     * levels.
     * @param level current level
     * @param lastValue the last index of query
     * @param queries   set of queries for each attribute
     * @param sqlQueries    the already created app/bookmark/contact queries
     * @param iterators keeps track of which index the top level loop is on
     */
    private void composeInnerQuery(int level, int lastValue, String[] queries,
            StringBuilder[] sqlQueries, HashMap<Integer, Integer> iterators) {
        if (level < 7) {
            for (int i = lastValue-1; i >= 0; i--) {
                iterators.put(level, i);
                composeInnerQuery(++level, i, queries, sqlQueries, iterators);
            }
        }
        compareValues(queries, sqlQueries, iterators);
    }
    /**
     * Create the comparison query parts.
     * @param queries   set of queries for each attribute
     * @param sqlQueries    the already created app/bookmark/contact queries
     * @param iterators keeps track of which index the top level loop is on
     */
    private void compareValues(String[] queries, StringBuilder[] sqlQueries,
            HashMap<Integer, Integer> iterators) {
        boolean and = true;
        boolean union = false;
        for (int o = 0; o < queries.length; o++) {
            and = true;
            union = false;
            for (Map.Entry<Integer, Integer> entry : iterators.entrySet()) {
                if (o == entry.getValue().intValue())
                    and = false;
            }
            if (and) {
                sqlQueries[0].append(" AND ").append(queries[o]);
                sqlQueries[1].append(" AND ").append(queries[o]);
                sqlQueries[2].append(" AND ").append(queries[o]);
            }
            else {
                sqlQueries[0].append(" AND NOT (").append(queries[o]).
                        append(")");
                sqlQueries[1].append(" AND NOT (").append(queries[o]).
                        append(")");
                sqlQueries[2].append(" AND NOT (").append(queries[o]).
                        append(")");
            }
        }
        for (Map.Entry<Integer, Integer> entry : iterators.entrySet()) {
            if (0 != entry.getValue().intValue())
                union = true;
        }
        if (union) {
            sqlQueries[0].append(" UNION ").append(APP_JOIN_TEXT);
            sqlQueries[1].append(" UNION ").append(BOOKMARK_JOIN_TEXT);
            sqlQueries[2].append(" UNION ").append(CONTACT_JOIN_TEXT);
        }
    }
    /**
     * Get the list of relevant contexts.
     * @param sqlQueries    the already created app/bookmark/contact queries
     * @param lastQuery the query from the last level
     * @param types which types to include in the query
     * @param perfTime  start time to keep track of performance
     * @param currPerf  current time to compare against perfTime
     * @param timeout   the max time allowed for the query action
     * @param size  how big the widget group is
     * @param level current query level
     * @return  cursor for the query level requested
     */
    private Cursor getRelevantList(StringBuilder[] sqlQueries, 
            StringBuilder lastQuery, HashMap<String, Boolean> types,
            long perfTime, long currPerf, int timeout, int size, int level) {
        sqlQueries = setupStrings(sqlQueries);
        StringBuilder currentQuery = composeQuery(
                mQueries, level, sqlQueries, lastQuery, types);
        Cursor cur = ApplicationEx.db.rawQuery(getSumQueryText(
                currentQuery.toString()), null);
        currPerf = System.currentTimeMillis()-perfTime;
        if (cur.getCount() >= size || currPerf > timeout || level == 1) {
            Log.d(Constants.LOG_TAG, "level " + level + 
                    " getRelevantApps end:" + currPerf);
            return cur;
        }
        else {            
            cur.close();
            return getRelevantList(sqlQueries, currentQuery, types, perfTime, 
                    currPerf, timeout, size, --level);
        }
    }
    /**
     * Set the values for the current attributes of the device.
     */
    private void setupConstraints() {
        mCurrTime = ApplicationEx.getTimeOfDay();
        mTopRange = mCurrTime + Util.TIME_RANGE;
        mBottomRange = mCurrTime - Util.TIME_RANGE;
        mCurrDay = ApplicationEx.getDay();
        mCurrLat = ApplicationEx.getLatLong()[0];
        mCurrLong = ApplicationEx.getLatLong()[1];
        mMaxLat = mCurrLat + Util.LAT_LON_RANGE;
        mMaxLong = mCurrLong + Util.LAT_LON_RANGE;
        mMinLat = mCurrLat - Util.LAT_LON_RANGE;
        mMinLong = mCurrLong - Util.LAT_LON_RANGE;
        mCurrBatt = ApplicationEx.getBattery();
        mMaxBatt = mCurrBatt + Util.BATT_RANGE;
        mMinBatt = mCurrBatt - Util.BATT_RANGE;
        mCurrHphone = (ApplicationEx.isHeadphonePlugged() == 1 || 
                ApplicationEx.isA2DPOn() == 1) ? 1 : 0;
        mCurrWifi = ApplicationEx.getWifi();
        mCurrBlue = ApplicationEx.getBluetooth();
    }
    /**
     * Gets all the relevant contexts based on the device's current aspects.
     * @param size  how many contexts to get
     * @param types which types to include in the query
     * @return  cursor pointing to all the relevant contexts
     */
    public Cursor getRelevantContexts(int size, HashMap<String,Boolean> types) {
        long perfTime = System.currentTimeMillis();
        long currPerf;     
        int timeout = 5000;
        
        deleteStaleContexts(mCurrTime);
        
        setupConstraints();
        // 7
        StringBuilder[] sqlQueries = {new StringBuilder(0),
                new StringBuilder(0), new StringBuilder(0)};
        sqlQueries = setupStrings(sqlQueries);
        for (String queryPart : mQueries) {
            sqlQueries[0].append(" AND ").append(queryPart);
            sqlQueries[1].append(" AND ").append(queryPart);
            sqlQueries[2].append(" AND ").append(queryPart);
        }
        StringBuilder levelSevenQuery = new StringBuilder(getFullQueryText(
                getQueryString(types, sqlQueries[0], sqlQueries[1], 
                        sqlQueries[2]), 6));
        Cursor cur = ApplicationEx.db.rawQuery(levelSevenQuery.toString(), 
                null);
        currPerf = System.currentTimeMillis()-perfTime;
        if (cur.getCount() >= size || currPerf > timeout) {
            Log.d(Constants.LOG_TAG, "level 7 getRelevantApps end:" + currPerf);
            return cur;
        }
        cur.close();
        // 6-1
        return getRelevantList(sqlQueries, levelSevenQuery, types, perfTime,
                currPerf, timeout, size, 6);
    }
    /**
     * Look for an item in a specific table.
     * @param name  identifier for the item to lookup
     * @param table the table to look in
     * @param column    the column to look under
     * @return  if the item is found
     */
    public boolean inDb(String name, String table, String column) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT * FROM " + table + 
                " WHERE " + column + "=?", new String[] { name });
        boolean inDb = false;
        if (cur.moveToFirst())
            inDb = true;
        cur.close();
        return inDb;
    }
    /**
     * Get the id of an item in a table.
     * @param name  identifier for the item to lookup
     * @param columnId  the name of the id column
     * @param table the table to look in
     * @param columnName    the name of the name column
     * @return  the found id
     */
    public int getRecordId(String name, String columnId, String table, 
            String columnName) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + columnId + " FROM " + 
                table + " WHERE " + columnName + "=?", new String[] { name });
        int bookmarkId = -1;
        if (cur.moveToFirst())
            bookmarkId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return bookmarkId;
    }
    /**
     * Get the id of a context.
     * @param id    item id in the app/bookmark/contact table
     * @param columnId  the name of the id column
     * @param table the table to look in
     * @param columnName    the name of the name column
     * @return  the found id
     */
    public int getCOL_CONTEXT_ID(int id, String columnId, String table, 
            String columnName) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + columnId + " FROM " + 
                table + " WHERE " + columnName + "='" + 
                id + "'", null);
        int resultId = -1;
        if (cur.moveToFirst())
            resultId = cur.getInt(cur.getColumnIndex(columnId));
        cur.close();
        return resultId;
    }
    /**
     * Get the name of an item in a table.
     * @param id    id for the item to lookup
     * @param columnId  the name of the id column
     * @param table the table to look in
     * @param columnName    the name of the name column
     * @return  the found name
     */
    public String getRecordName(int id, String columnId, String table, 
            String columnName) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + columnName + 
                " FROM " + table + " WHERE " + columnId + "=" + id, null);
        String name = "";
        if (cur.moveToFirst())
            name = cur.getString(cur.getColumnIndex(columnName));
        cur.close();
        return name;
    }
    /**
     * Get the value from a column that is bytes.
     * @param name  identifier for the item to lookup
     * @param columnBytes   the name of the column that has the bytes
     * @param columnName    the name of the name column
     * @param table the table to look in
     * @return  the found bytes array
     */
    public byte[] getBytes(String name, String columnBytes, String columnName, 
            String table) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + columnBytes + 
                " FROM " + table + " WHERE " + columnName + "=?", 
                new String[] { name });
        byte[] task = null;
        if (cur.moveToFirst())
            task = cur.getBlob(cur.getColumnIndex(columnBytes));
        cur.close();
        return task;
    }
    /**
     * Get the value from a column that is a string.
     * @param name  identifier for the item to lookup
     * @param columnString  the name of the column that has the string
     * @param table the table to look in
     * @param columnName    the name of the name column
     * @return  the found string
     */
    public String getString(String name, String columnString, String table, 
            String columnName) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + columnString + 
                " FROM " + table + " WHERE " + columnName + "=?", 
                new String[] { name });
        String appPackage = null;
        if (cur.moveToFirst())
            appPackage = cur.getString(cur.getColumnIndex(columnString));
        cur.close();
        return appPackage;
    }
    /**
     * Get the visits of a bookmark.
     * @param address   identifier for the item to lookup
     * @return  visits of the bookmark, -1 if not found
     */
    public int getBookmarkVisits(String address) {
        Cursor cur = ApplicationEx.db.rawQuery("SELECT " + COL_BOOK_VISITS + 
                " FROM " + BOOKMARK_TABLE + " WHERE " + COL_ADDRESS + "=?", 
                new String[] { address });
        int visits = -1;
        if (cur.moveToFirst())
            visits = cur.getInt(cur.getColumnIndex(COL_BOOK_VISITS));
        cur.close();
        return visits;
    }
    /**
     * Delete the record for an item (app/bookmark/contact).
     * @param name  identifier for the item to lookup
     * @param table where the item is
     * @param column    which column to look under
     */
    public void deleteRecord(String name, String table, String column) {
        ApplicationEx.db.delete(table, column + "='" + name + "'", null);
    }
    /**
     * Delete the record for an item (context).
     * @param id    id of the context in the table
     * @param idCol the column name where the id is
     */
    public void deleteContext(int id, String idCol) {
        ApplicationEx.db.delete(CONTEXT_TABLE, idCol + "=" + id, null);
    }
    /**
     * Insert a new record into a table in the database.
     * @param cv    list of content values to be entered
     * @param tableName the table name to be inserted into
     * @param columnName    the column that isn't null if the rest are null
     * @return  the row id of the inserted row
     */
    public long insertRecord(ContentValues cv, String tableName, 
            String columnName) {
        long result = ApplicationEx.db.insert(tableName, columnName, cv);
        return result;
    }
    /**
     * Update a record in a table in the database.
     * @param cv    list of content values to be entered
     * @param tableName the table name to be inserted into
     * @param whereClause   what to look for
     * @return  the row id of the updated row
     */
    public long updateRecord(ContentValues cv, String tableName, 
            String whereClause) {
        long result = ApplicationEx.db.update(tableName, cv, whereClause, null);
        return result;
    }

}
