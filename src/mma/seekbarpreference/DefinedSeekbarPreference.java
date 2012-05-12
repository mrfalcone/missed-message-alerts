/*
 * Copyright 2011 Michael R. Falcone
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mma.seekbarpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.mma.missedmessagealerts.R;


/**
 * A Preference that provides a DefinedSeekbar component and
 * displays its value. This preference will store an integer into
 * the SharedPreferences.
 *
 * @author Michael R. Falcone
 */
public class DefinedSeekbarPreference extends Preference implements OnSeekBarChangeListener {


    // MEMBER VARIABLES ----------------------------


    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    private static final int ATTR_POSSIBLE_VALUES = 0;
    private static final int ATTR_IS_FREE = 1;


    private String mLabel = "DefinedSeekbarPreference";
    private int mDefaultMaxValue = 100;
    private int mDefaultValue = 0;
    private int mCurrentValue = 0;
    private int mPossibleValuesResId = -1;
    private boolean mDefaultIsFree = true;
    private String mSummary = "";


    private DefinedSeekbar mSeekbar;
    private TextView mLabelTextView;
    private TextView mSummaryTextView;
    private EditText mValueText;

    private View mLayoutView = null;


    private boolean mIsInitialized = false;


    // CONSTRUCTORS ----------------------------------------

    public DefinedSeekbarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initAttrs(context, attrs);
        initView();
        initComponents();
    }

    public DefinedSeekbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        initAttrs(context, attrs);
        initView();
        initComponents();
    }


    // OVERRIDE METHODS ------------------------------

    @Override
    protected View onCreateView(ViewGroup parent) {

        if (mLayoutView == null)
            initView();

        return mLayoutView;

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);


        mDefaultValue = (defaultValue == null) ? mDefaultValue : ((Integer) defaultValue).intValue();

        if (restorePersistedValue)
            mCurrentValue = getPersistedInt(mDefaultValue);


        else {
            mCurrentValue = mDefaultValue;

            if (shouldPersist())
                persistInt(mCurrentValue);
        }

        mSeekbar.setProgress(mCurrentValue, true, false);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (mIsInitialized) {

            if (!callChangeListener(new Integer(progress))) {

                mSeekbar.setProgress(mCurrentValue, true, true);
                mValueText.setText(String.valueOf(mCurrentValue));
                return;
            }

            mCurrentValue = progress;

            if (shouldPersist())
                persistInt(mCurrentValue);
        }


        mValueText.setText(String.valueOf(mCurrentValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }


    // PUBLIC METHODS --------------------------------

    /**
     * Sets the progress of the underlying DefinedSeekbar.
     */
    public void setProgress(int progress) {
        mSeekbar.setProgress(progress);
    }


    /**
     * Returns the progress of the underlying DefinedSeekbar.
     */
    public int getProgress() {

        return mCurrentValue;
    }


    /**
     * Returns the default value of the DefinedSeekbarPreference.
     */
    public int getDefaultValue() {
        return mDefaultValue;
    }


    /**
     * Sets the maximum value of the underlying DefinedSeekbar when in free mode.
     */
    public void setMax(int max) {
        mSeekbar.setMax(max);
    }


    /**
     * Returns the maximum value of the underlying DefinedSeekbar when in free mode.
     */
    public int getMax() {
        return mSeekbar.getMax();
    }


    /**
     * Returns whether the underlying DefinedSeekbar is in free mode.
     *
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public boolean isFree() {
        return mSeekbar.isFree();
    }


    /**
     * Sets whether the underlying DefinedSeekbar is in free mode.
     *
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public void setIsFree(boolean isFree) {
        mSeekbar.setIsFree(isFree);
    }


    /**
     * Sets the array of values that the underlying DefinedSeekbar can assume
     * when not in free mode.
     *
     * @param resId id of an integer array resource
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public void setPossibleValues(int resId) {
        mSeekbar.setPossibleValues(resId);
    }


    /**
     * Sets the array of values that the underlying DefinedSeekbar can assume
     * when not in free mode.
     *
     * @param possibleValues set of possible values for the underlying DefinedSeekbar
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public void setPossibleValues(int[] possibleValues) {
        mSeekbar.setPossibleValues(possibleValues);
    }


    /**
     * Sets the array of values that the underlying DefinedSeekbar can assume
     * when not in free mode. Only uses the first numValues integers.
     *
     * @param possibleValues set of possible values for the underlying DefinedSeekbar
     * @param numValues      the number of possible values in possibleValues to use
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public void setPossibleValues(int[] possibleValues, int numValues) {
        mSeekbar.setPossibleValues(possibleValues, numValues);
    }


    /**
     * Returns the values possible for the underlying DefinedSeekbar when not
     * in free mode.
     *
     * @see com.mma.seekbarpreference.DefinedSeekbar
     */
    public int[] getPossibleValues() {
        return mSeekbar.getPossibleValues();
    }


    /**
     * Resets the DefinedSeekbarPreference to its default value.
     */
    public void reset() {

        mSeekbar.setDefaultProgress(mDefaultValue);
        mSeekbar.reset();
    }


    // PRIVATE METHODS --------------------------------


    private void initAttrs(Context context, AttributeSet attrs) {


        // handle default values from a resource or defined directly
        int def_res_value = attrs.getAttributeResourceValue(ANDROID_NAMESPACE, "defaultValue", -1);

        mDefaultValue = (def_res_value >= 0) ? context.getResources().getInteger(def_res_value)
                : attrs.getAttributeIntValue(ANDROID_NAMESPACE, "defaultValue", mDefaultValue);

        mCurrentValue = mDefaultValue;

        int title_res_value = attrs.getAttributeResourceValue(ANDROID_NAMESPACE, "title", -1);
        String title = (title_res_value >= 0) ? context.getResources().getString(title_res_value)
                : attrs.getAttributeValue(ANDROID_NAMESPACE, "title");
        mLabel = (title == null) ? mLabel : title;


        int summary_res_value = attrs.getAttributeResourceValue(ANDROID_NAMESPACE, "summary", -1);
        String summary = (summary_res_value >= 0) ? context.getResources().getString(summary_res_value)
                : attrs.getAttributeValue(ANDROID_NAMESPACE, "summary");
        mSummary = (summary == null) ? mSummary : summary;


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DefinedSeekbar);

        mDefaultIsFree = a.getBoolean(ATTR_IS_FREE, mDefaultIsFree);

        mPossibleValuesResId = a.getResourceId(ATTR_POSSIBLE_VALUES, -1);

        a.recycle();
    }


    private void initView() {

        mLayoutView = View.inflate(getContext(), R.layout.definedseekbar_preference, null);

        mSeekbar = (DefinedSeekbar) mLayoutView.findViewById(R.id.defSeekbar);
        mLabelTextView = (TextView) mLayoutView.findViewById(R.id.labelTextView);
        mSummaryTextView = (TextView) mLayoutView.findViewById(R.id.summaryTextView);
        mValueText = (EditText) mLayoutView.findViewById(R.id.valueText);
    }


    private void initComponents() {

        mLabelTextView.setText(mLabel);
        mSummaryTextView.setText(mSummary);
        mSeekbar.setMax(mDefaultMaxValue);
        mSeekbar.setPossibleValues(mPossibleValuesResId);
        mSeekbar.setIsFree(mDefaultIsFree);
        mSeekbar.setOnSeekBarChangeListener(this);
        mSeekbar.setProgress(mCurrentValue, true, false);

        mIsInitialized = true;
    }


}
