package com.example.hallmorgan_quickalert.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.example.hallmorgan_quickalert.R;
import com.example.hallmorgan_quickalert.user.Contacts;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ContactListFragment extends ListFragment {

    private static final String TAG = "ContactListFragment";
    private static final String ARG_CONTACTS = "ARG_CONTACTS";

    private ContactsAdapter mAdapter;
    private ArrayList<Contacts> contacts = new ArrayList<>();


    public static ContactListFragment newInstance(ArrayList<Contacts> _contacts) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACTS, _contacts);
        ContactListFragment fragment = new ContactListFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "";

        DatabaseReference userContactsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("contacts");
        ListView listView = view.findViewById(android.R.id.list);


        view.findViewById(R.id.emptyViewLayout);
        LinearLayout emptyViewLayout;
        emptyViewLayout = view.findViewById(R.id.emptyViewLayout);
        TextView emptyTextView = view.findViewById(R.id.emptyTextView);
        TextView instructionsTextView = view.findViewById(R.id.instructionsTextView);

        if (getArguments() != null) {
            //noinspection unchecked
            contacts = (ArrayList<Contacts>) getArguments().getSerializable(ARG_CONTACTS);
        }

        if (contacts == null || contacts.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            instructionsTextView.setVisibility(View.VISIBLE);
            emptyViewLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            instructionsTextView.setVisibility(View.GONE);
            emptyViewLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            mAdapter = new ContactsAdapter(getActivity(), contacts, userContactsRef);
            setListAdapter(mAdapter);
        }


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View view, int position, long id) {
        super.onListItemClick(l, view, position, id);
        Log.i(TAG, "onListItemClick");

        for (int i = 0; i < mAdapter.getCount(); i++) {
            Contacts contact = (Contacts) mAdapter.getItem(i);
            if (contact != null) {
                if (i == position) {
                    contact.setShowDeleteButton(!contact.isShowDeleteButton());
                } else {
                    contact.setShowDeleteButton(false);
                }
            }
        }

        mAdapter.notifyDataSetChanged();

    }



    public static class ContactsAdapter extends BaseAdapter {

        //Base ID
        private static final long Base_ID = 0x1011;
        private final DatabaseReference userContactsRef;

        private final Context context;
        private final ArrayList<Contacts> contacts;


        public ContactsAdapter(Context _context, ArrayList<Contacts> _contacts, DatabaseReference _userContactsRef) {
            context = _context;
            contacts = _contacts;
            userContactsRef = _userContactsRef;
        }

        @Override
        public int getCount() {
            if (contacts != null){
                return contacts.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (contacts != null && position >= 0 && position < contacts.size()){
                return contacts.get(position);
            }
            return null;
        }



        @Override
        public long getItemId(int position) {
            return Base_ID + position;
        }

        public void removeItem(int position){
            contacts.remove(position);
            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            userContactsRef.setValue(contacts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            Contacts contact = contacts.get(position);
            String contactName = "";

            if (convertView == null){
                convertView = LayoutInflater.from(context).inflate(R.layout.list_layout, parent, false);
                vh = new ViewHolder(convertView);
                vh.delete_button = convertView.findViewById(R.id.deleteButton);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            if (contact != null){
                vh.contact_name.setText(contact.getName());
                //vh.contact_name.setTextColor(Color.BLACK);
                vh.contact_number.setText(contact.getNumber());
                //vh.contact_number.setTextColor(Color.BLACK);
                contactName = contact.getName();
                Log.d(TAG, "Contact Name: " + contactName + " Contact Number: " + contact.getNumber());

                if (contact.isShowDeleteButton()) {
                    vh.delete_button.setVisibility(View.VISIBLE);
                    vh.delete_button.setOnClickListener(view -> {
                        // Handle the delete button click
                        removeItem(position);
                        notifyDataSetChanged();

                        String deletedContact = contact.getName() + " was deleted from your designated emergency contacts";
                        Toast.makeText(context, deletedContact, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    vh.delete_button.setVisibility(View.GONE);
                }
            }

            int clickedPosition = -1;
            Log.d(TAG, "Clicked Position: " + clickedPosition);
            Log.d(TAG, "Delete Button Visibility: " + vh.delete_button.getVisibility());


            return convertView;
        }
    }

    static class ViewHolder{
        TextView contact_name;
        TextView contact_number;
        Button delete_button;

        public ViewHolder(View _layout){
            contact_name = _layout.findViewById(R.id.contact_name);
            contact_number = _layout.findViewById(R.id.contact_number);
        }
    }

}
