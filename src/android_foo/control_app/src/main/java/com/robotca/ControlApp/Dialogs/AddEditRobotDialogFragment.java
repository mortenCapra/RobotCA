package com.robotca.ControlApp.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.robotca.ControlApp.Core.RobotInfo;
import com.robotca.ControlApp.Fragments.SimpleFragment;
import com.robotca.ControlApp.R;

import java.util.UUID;

/**
 * Dialog for adding or editing a Robot.
 * <p/>
 * Created by Michael Brunson on 1/23/16.
 *
 * Not a dialog anymore but merely a fragment.
 * 28/05/20
 */
public class AddEditRobotDialogFragment extends Fragment {

    /**
     * Bundle key for position
     */
    public static final String POSITION_KEY = "POSITION_KEY";

    // Temporary RobotInfo
    private RobotInfo mInfo = new RobotInfo();

    // Use this instance of the interface to deliver action events
    private DialogListener mListener;

    // EditTexts for editing the RobotInfo
    private EditText mNameEditTextView;
    private EditText mMasterUriEditTextView;
    private View mAdvancedOptionsView;
    private EditText mJoystickTopicEditTextView;
    private EditText mLaserScanTopicEditTextView;
    private EditText mCameraTopicEditTextView;
    private EditText mNavSatTopicEditTextView;
    private EditText mOdometryTopicEditTextView;
    private EditText mPoseTopicEditTextView;
    private EditText mImuTopicEditTextView;

    private CheckBox mReverseLaserScanCheckBox;
    private CheckBox mInvertXAxisCheckBox;
    private CheckBox mInvertYAxisCheckBox;
    private CheckBox mInvertAngularVelocityCheckBox;

    private Button addButton;
    private Button cancelButton;


    // Position of the RobotInfo in the list of RobotInfos
    private int mPosition = -1;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mPosition = args.getInt(POSITION_KEY, -1);
            mInfo.load(args);
        }
    }

    // Override the Fragment.onAttach() method to instantiate the DialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DialogListener so we can send events to the host
            mListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            //throw new ClassCastException(activity.toString()  + " must implement DialogListener");
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_add_robot, null);
        mNameEditTextView = (EditText) v.findViewById(R.id.robot_name_edit_text);
        mMasterUriEditTextView = (EditText) v.findViewById(R.id.master_uri_edit_view);

        CheckBox mAdvancedOptionsCheckbox = (CheckBox) v.findViewById(R.id.advanced_options_checkbox_view);
        mAdvancedOptionsView = v.findViewById(R.id.advanced_options_view);
        mJoystickTopicEditTextView = (EditText) v.findViewById(R.id.joystick_topic_edit_text);
        mLaserScanTopicEditTextView = (EditText) v.findViewById(R.id.laser_scan_edit_view);
        mCameraTopicEditTextView = (EditText) v.findViewById(R.id.camera_topic_edit_view);
        mNavSatTopicEditTextView = (EditText) v.findViewById(R.id.navsat_topic_edit_view);
        mOdometryTopicEditTextView = (EditText) v.findViewById(R.id.odometry_topic_edit_view);
        mPoseTopicEditTextView = (EditText) v.findViewById(R.id.pose_topic_edit_view);
        mImuTopicEditTextView = (EditText) v.findViewById(R.id.Imu_topic_edit_view);
        mReverseLaserScanCheckBox = (CheckBox) v.findViewById(R.id.reverse_laser_scan_check_box);
        mInvertXAxisCheckBox = (CheckBox) v.findViewById(R.id.invert_x_axis_check_box);
        mInvertYAxisCheckBox = (CheckBox) v.findViewById(R.id.invert_y_axis_check_box);
        mInvertAngularVelocityCheckBox = (CheckBox) v.findViewById(R.id.invert_angular_velocity_check_box);

        mNameEditTextView.setText(mInfo.getName());
        mMasterUriEditTextView.setText(mInfo.getMasterUri());

        mAdvancedOptionsCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    mAdvancedOptionsView.setVisibility(View.VISIBLE);
                } else {
                    mAdvancedOptionsView.setVisibility(View.GONE);
                }
            }
        });

        mJoystickTopicEditTextView.setText(mInfo.getJoystickTopic());
        mLaserScanTopicEditTextView.setText(mInfo.getLaserTopic());
        mCameraTopicEditTextView.setText(mInfo.getCameraTopic());
        mNavSatTopicEditTextView.setText(mInfo.getNavSatTopic());
        mOdometryTopicEditTextView.setText(mInfo.getOdometryTopic());
        mPoseTopicEditTextView.setText(mInfo.getPoseTopic());
        mImuTopicEditTextView.setText(mInfo.getImuTopic());
        mReverseLaserScanCheckBox.setChecked(mInfo.isReverseLaserScan());
        mInvertXAxisCheckBox.setChecked(mInfo.isInvertX());
        mInvertYAxisCheckBox.setChecked(mInfo.isInvertY());
        mInvertAngularVelocityCheckBox.setChecked(mInfo.isInvertAngularVelocity());

        addButton = v.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mNameEditTextView.getText().toString().trim();
                String masterUri = mMasterUriEditTextView.getText().toString().trim();
                String joystickTopic = mJoystickTopicEditTextView.getText().toString().trim();
                String laserScanTopic = mLaserScanTopicEditTextView.getText().toString().trim();
                String cameraTopic = mCameraTopicEditTextView.getText().toString().trim();
                String navsatTopic = mNavSatTopicEditTextView.getText().toString().trim();
                String odometryTopic = mOdometryTopicEditTextView.getText().toString().trim();
                String poseTopic = mPoseTopicEditTextView.getText().toString().trim();
                String imuTopic = mImuTopicEditTextView.getText().toString().trim();
                boolean reverseLaserScan = mReverseLaserScanCheckBox.isChecked();
                boolean invertX = mInvertXAxisCheckBox.isChecked();
                boolean invertY = mInvertYAxisCheckBox.isChecked();
                boolean invertAngVel = mInvertAngularVelocityCheckBox.isChecked();

                if (masterUri.equals("")) {
                    Toast.makeText(getActivity(), "Master URI required", Toast.LENGTH_SHORT).show();
                } else if (joystickTopic.equals("") || laserScanTopic.equals("") || cameraTopic.equals("")
                        || navsatTopic.equals("") || odometryTopic.equals("") || poseTopic.equals("")) {
                    Toast.makeText(getActivity(), "All topic names are required", Toast.LENGTH_SHORT).show();
                } else if (!name.equals("")) {
                    mListener.onAddEditDialogPositiveClick(new RobotInfo(mInfo.getId(), name,
                            masterUri, joystickTopic, laserScanTopic, cameraTopic, navsatTopic,
                            odometryTopic, poseTopic, imuTopic, reverseLaserScan, invertX, invertY, invertAngVel), mPosition, AddEditRobotDialogFragment.this);
                } else {
                    Toast.makeText(getActivity(), "Robot name required", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancelButton = v.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                mListener.onAddEditDialogNegativeClick(AddEditRobotDialogFragment.this);
            }
        });

        /*
        builder.setTitle(R.string.add_edit_robot)
                .setView(v)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String name = mNameEditTextView.getText().toString().trim();
                        String masterUri = mMasterUriEditTextView.getText().toString().trim();
                        String joystickTopic = mJoystickTopicEditTextView.getText().toString().trim();
                        String laserScanTopic = mLaserScanTopicEditTextView.getText().toString().trim();
                        String cameraTopic = mCameraTopicEditTextView.getText().toString().trim();
                        String navsatTopic = mNavSatTopicEditTextView.getText().toString().trim();
                        String odometryTopic = mOdometryTopicEditTextView.getText().toString().trim();
                        String poseTopic = mPoseTopicEditTextView.getText().toString().trim();
                        String imuTopic = mImuTopicEditTextView.getText().toString().trim();
                        boolean reverseLaserScan = mReverseLaserScanCheckBox.isChecked();
                        boolean invertX = mInvertXAxisCheckBox.isChecked();
                        boolean invertY = mInvertYAxisCheckBox.isChecked();
                        boolean invertAngVel = mInvertAngularVelocityCheckBox.isChecked();

                        if (masterUri.equals("")) {
                            Toast.makeText(getActivity(), "Master URI required", Toast.LENGTH_SHORT).show();
                        } else if (joystickTopic.equals("") || laserScanTopic.equals("") || cameraTopic.equals("")
                                || navsatTopic.equals("") || odometryTopic.equals("") || poseTopic.equals("")) {
                            Toast.makeText(getActivity(), "All topic names are required", Toast.LENGTH_SHORT).show();
                        } else if (!name.equals("")) {
                            mListener.onAddEditDialogPositiveClick(new RobotInfo(mInfo.getId(), name,
                                    masterUri, joystickTopic, laserScanTopic, cameraTopic, navsatTopic,
                                    odometryTopic, poseTopic, imuTopic, reverseLaserScan, invertX, invertY, invertAngVel), mPosition);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getActivity(), "Robot name required", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onAddEditDialogNegativeClick(AddEditRobotDialogFragment.this);
                dialog.cancel();
            }
        });

        return builder.create();

         */
        return v;
    }

    public void show(){
        getFragmentManager().beginTransaction().show(this).commit();
    }

    public void hide(){
        getFragmentManager().beginTransaction().hide(this).commit();
    }


    public interface DialogListener {
        void onAddEditDialogPositiveClick(RobotInfo info, int position, AddEditRobotDialogFragment fragment);

        void onAddEditDialogNegativeClick(AddEditRobotDialogFragment fragment);
    }

}
