package com.dpain.paras.static_frame_test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

public class NewValueFragment extends DialogFragment {

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, Date time, int value);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        // There is a good reason why I've used null, can't remember why atm
        View rootView = inflater.inflate(R.layout.dialog_new_val, null);

        final Calendar cal = Calendar.getInstance();
        final EditText txtVal = (EditText) rootView.findViewById(R.id.edit_txt_new_val);
        TextView txtMsg = (TextView) rootView.findViewById(R.id.txt_new_val_msg);

        // To fetch the last value in the list
        cal.setTime(MainActivity.dateIndex.get(MainActivity.dateIndex.size() - 1));
        // Add one day
        cal.add(Calendar.DATE, 1);
        txtMsg.setText(MainActivity.formatter.format(cal.getTime()));

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(rootView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // To avoid empty submissions (parsed value is "")
                        try{
                            mListener.onDialogPositiveClick(NewValueFragment.this, cal.getTime(), Integer.parseInt(txtVal.getText().toString()));
                        } catch(NumberFormatException nf){
                            mListener.onDialogNegativeClick(NewValueFragment.this);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(NewValueFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
