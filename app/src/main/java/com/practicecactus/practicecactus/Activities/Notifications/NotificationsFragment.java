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
import java.util.List;

/**
 * Created by Muna on 2017-04-06.
 */

public class NotificationsFragment extends DialogFragment {


    ArrayAdapter<String> adapter;
    ArrayList<String> notificationsList;

    public static NotificationsFragment newInstance(ArrayList<String> notificationsList) {

        NotificationsFragment frag = new NotificationsFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("list", notificationsList);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationsList = getArguments().getStringArrayList("list");
//        System.out.println("LIST***:" + notificationsList);

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        getDialog().setTitle("Notifications");

        View v = inflater.inflate(R.layout.notifications_fragment, container, false);

        ArrayList<String> listContact = notificationsList;
        ListView lv = (ListView) v.findViewById(R.id.whole_list);

        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_expandable_list_item_1,
                listContact);

        lv.setAdapter(adapter);


        return v;
    }


    private ArrayList<String> GetlistContact(){

        ArrayList<String> contactlist = new ArrayList<String>();

        contactlist.add("Topher");
        contactlist.add("Jean");
        contactlist.add("Andrew");

        return contactlist;
    }
}
