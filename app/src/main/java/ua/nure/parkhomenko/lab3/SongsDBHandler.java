package ua.nure.parkhomenko.lab3;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class SongsDBHandler extends SQLiteOpenHelper {
    private ContentResolver contentResolver;
    private static final String DATABASE_NAME = "songs.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SONGS           = "songs";
    public static final String COLUMN_ID             = "songID";
    public static final String COLUMN_TITLE          = "title";
    public static final String COLUMN_ARTIST       = "artist";
    //public static final String COLUMN_PATH           = "songPath";

    public SongsDBHandler(Context context, ContentResolver contentResolver) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.contentResolver=contentResolver;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SONGS + " (" + COLUMN_ID
                + " INTEGER PRIMARY KEY, " + COLUMN_TITLE + " TEXT, " + COLUMN_ARTIST
                + /*" TEXT, " + COLUMN_PATH +*/ " TEXT " + ")");
        ArrayList<Song> songList = getSongsList();
        insertSongsToDB(db, songList);
    }

    private void insertSongsToDB(SQLiteDatabase db, ArrayList<Song> songList) {
        //SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues;
        for (Song song : songList) {
            contentValues = new ContentValues();
            contentValues.put(COLUMN_ID, song.getId());
            contentValues.put(COLUMN_TITLE, song.getTitle());
            contentValues.put(COLUMN_ARTIST, song.getArtist());
            //contentValues.put(COLUMN_PATH, song.getPath());

            db.insert(TABLE_SONGS, null, contentValues);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("CREATE TABLE " + TABLE_SONGS + " (" + COLUMN_ID
                + " INTEGER, " + COLUMN_TITLE + " TEXT, " + COLUMN_ARTIST
                + /*" TEXT, " + COLUMN_PATH + */" TEXT PRIMARY KEY " + ")");
    }

    public ArrayList<Song> getSongsList() {
        //retrieve song info
        ArrayList<Song> songList = new ArrayList<>();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;    //"/storage/emulated/0/Music/Telegram/"
        //String sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " ASC";
        Cursor songCursor = contentResolver.query(musicUri, null, null, null, null/*sortOrder*/);

        if (songCursor != null && songCursor.moveToFirst()) {
            //get columns
            int titleColumn = songCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = songCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = songCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            /*int pathColumn = songCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);*/
            //add songs to list
            do {
                long id = songCursor.getLong(idColumn);
                String title = songCursor.getString(titleColumn);
                String artist = songCursor.getString(artistColumn);
                /*String path = songCursor.getString(pathColumn);*/
                songList.add(new Song(id, title, artist/*, path*/));
            }
            while (songCursor.moveToNext());
            songCursor.close();
        }
        return songList;
    }

    public ArrayList<Song> populateNoteListArray(){
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Song song;
        Cursor result = sqLiteDatabase.rawQuery("SELECT * FROM "+ TABLE_SONGS, null);
        ArrayList<Song> songs = new ArrayList<Song>();
        if(result.getCount()!=0) {
            while ((result.moveToNext())) {
                int id = result.getInt(0);
                String title = result.getString(1);
                String artist = result.getString(2);
                /*String path = result.getString(3);*/
                song = new Song(id, title, artist/*, path*/);
                songs.add(song);
            }
        }
        result.close();
        return songs;
    }
}
