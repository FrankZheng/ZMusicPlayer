package com.example.xzheng.zmusicplayer;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by xzheng on 14/12/30.
 */
public class SongListAdapter extends ArrayAdapter<Song> {

    private List<Song> _songList;

    public SongListAdapter(Context context, List<Song> songList) {
        super(context, R.layout.rowlayout);
        _songList = songList;
    }


}
