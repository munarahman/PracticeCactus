package com.practicecactus.practicecactus.Activities.Notifications;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.practicecactus.practicecactus.R;

import java.util.ArrayList;

/**
 * Created by Muna on 2017-04-06.
 */

public class NotificationsFragment extends DialogFragment {


    ArrayAdapter<String> adapter;
    ArrayList<String> notificationsList;

    public static NotificationsFragment newInstance(ArrayList<String> notificationsList) {

        // this static function creates a new instance of the class and passes in the list as an argument

        NotificationsFragment frag = new NotificationsFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("list", notificationsList);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set notificationsList to be equal to the list of new notifications sent by the server
        notificationsList = getArguments().getStringArrayList("list");

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        // set the dialogs title
        getDialog().setTitle("Notifications");

        // inflate the view to be notifications_fragment
        View v = inflater.inflate(R.layout.notifications_fragment, container, false);

        // get the listView in the notifications_fragment
        ListView lv = (ListView) v.findViewById(R.id.whole_list);

        // set an adapter to the listview so it pulls each cells data from notificationsList
        adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item, notificationsList);

        lv.setAdapter(adapter);

        // return the view
        return v;
    }

}
