package com.example.hallmorgan_quickalert.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.Iterator;

public class ContactListFragment extends ListFragment {

    private static final String TAG = "ContactListFragment";
    private static final String ARG_CONTACTS = "ARG_CONTACTS";

    private ContactsAdapter mAdapter;
    private ArrayList<Contacts> contacts = new ArrayList<>();
    private boolean isEditable = false;
    public OnEditListener mListener;
    private LinearLayout emptyViewLayout;
    private TextView emptyTextView;
    private TextView instructionsTextView;
    private ListView listView;
    private DatabaseReference userContactsRef;

    public interface OnEditListener{
        void onEdit (int titleResource, int instructionResource, boolean isEdit);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnEditListener){
            mListener = (OnEditListener) context;
        }
    }

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

        userContactsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("contacts");
        listView = view.findViewById(android.R.id.list);


        view.findViewById(R.id.emptyViewLayout);

        emptyViewLayout = view.findViewById(R.id.emptyViewLayout);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        instructionsTextView = view.findViewById(R.id.instructionsTextView);

        if (getArguments() != null) {
            //noinspection unchecked
            contacts = (ArrayList<Contacts>) getArguments().getSerializable(ARG_CONTACTS);
        }

        updateUI();
    }

    private void updateUI(){
        if (contacts == null || contacts.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            instructionsTextView.setVisibility(View.VISIBLE);
            emptyViewLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            contacts.size();
            emptyTextView.setVisibility(View.GONE);
            instructionsTextView.setVisibility(View.GONE);
            emptyViewLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            mAdapter = new ContactsAdapter(getActivity(), contacts, userContactsRef, isEditable);
            setListAdapter(mAdapter);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.profile_edit_item) {
            if (contacts == null){
                new AlertDialog.Builder(getActivity())
                        .setTitle("There are no contacts to edit.")
                        .setMessage("You cannot edit an empty contacts list. \nSelect 'Add Contact' to add an emergency contact to the list")
                        .setPositiveButton("OK", (dialog, which) -> {
                            //close the box
                        }).show();
                return false;
            }
            // Toggle the edit mode
            toggleEditMode();

            if (isEditable) {
                // Update the UI to show checkboxes
                updateList();
            } else {
                // Show alert asking if the user wants to delete selected items
                new AlertDialog.Builder(getActivity())
                        .setTitle("Delete Selected Items")
                        .setMessage("Are you sure you want to delete the selected items?\nThis action cannot be undone.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Delete selected items
                            deleteSelectedItems();
                            // Reset editable state
                            toggleEditMode();
                            mAdapter.setEditable(false);
                            // Refresh the list UI
                            updateList();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Reset editable state
                            toggleEditMode();
                            // Refresh the list UI
                            updateList();
                        })
                        .show();
            }

            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();  // Refresh the menu
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem editItem = menu.findItem(R.id.profile_edit_item);
        if (isEditable){
            editItem.setIcon(R.drawable.ic_delete);
        } else {
            editItem.setIcon(R.drawable.ic_edit);
        }
    }

    public void toggleEditMode(){
        isEditable = !isEditable;
        mAdapter.notifyDataSetChanged();
    }

    public void updateList(){

        if (isEditable){
            //change instructions and title
            mListener.onEdit(R.string.delete_emergency_contacts, R.string.instructions_delete_contacts, true);
            //Make list editable and show checkbox
            mAdapter.setEditable(true);

        } else {
            //Make list items non-editable
            mListener.onEdit(R.string.add_emergency_contacts, R.string.instructions_add_contacts, false);
            mAdapter.setEditable(false);
            if (contacts == null){
                emptyViewLayout.setVisibility(View.VISIBLE);
            } else {
                emptyViewLayout.setVisibility(View.GONE);
            }
        }

        //Notify adapter to refresh the list
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View view, int position, long id) {
        super.onListItemClick(l, view, position, id);
        Log.i(TAG, "onListItemClick");

        Contacts contact = (Contacts) mAdapter.getItem(position);
        if (contact != null){
            contact.setSelected(!contact.isSelected()); //Toggle the selected state
            //notify the adapter
            mAdapter.notifyDataSetChanged();
        }
    }

    public void deleteSelectedItems(){
        Iterator<Contacts> iterator = contacts.iterator();
        while (iterator.hasNext()){
            Contacts contact = iterator.next();
            if (contact.isSelected()){
                iterator.remove();

                //Remove from Firebase
                String contactId = contact.getId();
                userContactsRef.child(contactId).removeValue();
            }
        }
        updateUI();
        mAdapter.notifyDataSetChanged();
        mListener.onEdit(R.string.add_emergency_contacts, R.string.instructions_add_contacts, false);
        toggleEditMode();
    }



    public static class ContactsAdapter extends BaseAdapter {

        //Base ID
        private static final long Base_ID = 0x1011;
        private final DatabaseReference userContactsRef;

        private final Context context;
        private final ArrayList<Contacts> contacts;
        private boolean isEditable;


        public ContactsAdapter(Context _context, ArrayList<Contacts> _contacts, DatabaseReference _userContactsRef, boolean _isEditable) {
            context = _context;
            contacts = _contacts;
            userContactsRef = _userContactsRef;
            isEditable = _isEditable;
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


        public void setEditable(boolean editable){
            isEditable = editable;
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
            String contactName;

            if (convertView == null){
                convertView = LayoutInflater.from(context).inflate(R.layout.list_layout, parent, false);
                vh = new ViewHolder(convertView);
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

                if (isEditable) {
                    vh.checkBox.setVisibility(View.VISIBLE);
                    vh.checkBox.setChecked(contact.isSelected());
                } else {
                    vh.checkBox.setVisibility(View.GONE);
                }

                //Handle checkbox clicks
                vh.checkBox.setOnClickListener(view -> contact.setSelected(((CheckBox) view).isChecked()));
            }

            int clickedPosition = -1;
            Log.d(TAG, "Clicked Position: " + clickedPosition);


            return convertView;
        }
    }

    static class ViewHolder{
        TextView contact_name;
        TextView contact_number;
        CheckBox checkBox;

        public ViewHolder(View _layout){
            contact_name = _layout.findViewById(R.id.contact_name);
            contact_number = _layout.findViewById(R.id.contact_number);
            checkBox = _layout.findViewById(R.id.item_checkbox);
        }
    }

}
