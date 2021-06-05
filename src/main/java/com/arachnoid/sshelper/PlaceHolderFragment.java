package com.arachnoid.sshelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lutusp on 9/16/17.
 */

public final class PlaceHolderFragment extends android.support.v4.app.Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    private final int[] fragment_array;
    //private final int[] label_array;

    public PlaceHolderFragment() {
        fragment_array = new int[]{R.layout.fragment_log, R.layout.fragment_config, R.layout.fragment_terminal};
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int index = getArguments().getInt("index");
        return inflater.inflate(fragment_array[index], container, false);
        //Log.e("onCreateView", "" + this + "," + index);
        //TextView textView = (TextView) rootView.findViewById(label_array[index]);
        //textView.setText(getString(R.string.section_format, index));
        //return rootView;
    }
}
