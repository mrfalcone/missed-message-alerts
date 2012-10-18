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
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.mma.missedmessagealerts.R;


/**
 * DefinedSeekbar is an extension of SeekBar that supports using a
 * set of defined integer values for which the seekbar can assume. If
 * no set of values is defined or the DefinedSeekbar is in free mode,
 * the DefinedSeekbar acts as a normal SeekBar. Secondary progress of
 * SeekBar is unaffected.
 *
 * @author Michael R. Falcone
 * @see android.widget.SeekBar
 */
public class DefinedSeekbar extends SeekBar implements OnSeekBarChangeListener {


    // MEMBER VARIABLES ----------------------------


    private static final int ATTR_POSSIBLE_VALUES = 0;
    private static final int ATTR_IS_FREE = 1;

    private boolean mAllowNextInvalidValue = false; // required for resetting with invalid values
    private int mDefaultProgress = 0;
    private int mFreeMax = 100;
    private int[] mPossibleValues = null;
    private int mMaxPossibleValuesIndex = 0;
    private boolean mIsFree = true;        // if false, seekbar can only have defined values

    private OnSeekBarChangeListener mListener = null;


    // CONSTRUCTORS -------------------------------------------

    public DefinedSeekbar(Context context) {
        super(context);

        super.setOnSeekBarChangeListener(this);
    }


    public DefinedSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);

        super.setOnSeekBarChangeListener(this);
        loadAttrs(context, attrs);
    }

    public DefinedSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        super.setOnSeekBarChangeListener(this);
        loadAttrs(context, attrs);
    }


    // METHOD OVERRIDES ----------------------------------------


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (mAllowNextInvalidValue) {

            if (mListener != null)
                mListener.onProgressChanged(this, progress, false);

            return;
        }


        if (mAllowNextInvalidValue || mIsFree || mPossibleValues == null) {

            mAllowNextInvalidValue = true;
            setProgress(progress);
            mAllowNextInvalidValue = false;

            if (mListener != null)
                mListener.onProgressChanged(this, progress, false);

            return;
        }

        int final_progress = mPossibleValues[0];

        for (int i = 1; i <= mMaxPossibleValuesIndex; ++i) {

            if (progress >= mPossibleValues[i])
                final_progress = mPossibleValues[i];
            else
                break;
        }

        mAllowNextInvalidValue = true;
        setProgress(final_progress);
        mAllowNextInvalidValue = false;

        if (mListener != null)
            mListener.onProgressChanged(this, final_progress, false);

    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }


    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {

        mListener = l;
    }


    @Override
    public void setMax(int max) {

        mFreeMax = max;

        if (mIsFree)
            super.setMax(max);


    }


    // PUBLIC METHODS -------------------------------------------


    /**
     * Sets the progress of the DefinedSeekbar
     *
     * @param progress new progress of the DefinedSeekbar
     * @param force    if true, causes the DefinedSeekbar to force acceptance of the new progress
     * @param silently if true, supresses any OnSeekBarChangeListener callbacks
     */
    public void setProgress(int progress, boolean force, boolean silently) {

        OnSeekBarChangeListener l = mListener;

        if (silently)
            mListener = null;

        mAllowNextInvalidValue = force;

        setProgress(progress);

        mAllowNextInvalidValue = false;

        mListener = l;

    }


    /**
     * Sets the array of values that the DefinedSeekbar can assume
     * when not in free mode. The values do not have to be sorted.
     *
     * @param possibleValues array of values the DefinedSeekbar can assume
     */
    public void setPossibleValues(int[] possibleValues) {

        setPossibleValues(possibleValues, possibleValues.length);
    }


    /**
     * Sets the array of values that the DefinedSeekbar can assume
     * when not in free mode. The values do not have to be sorted.
     * Only uses the first numValues integers.
     *
     * @param possibleValues array of values the DefinedSeekbar can assume
     * @param numValues      the number of possible values in possibleValues to use
     */
    public void setPossibleValues(int[] possibleValues, int numValues) {

        if (possibleValues == null) {

            mPossibleValues = null;

            return;
        }

        mMaxPossibleValuesIndex = numValues - 1;


        if (!(mPossibleValues != null && mPossibleValues.length >= numValues))
            mPossibleValues = new int[numValues];


        mPossibleValues[0] = possibleValues[0];
        for (int i = 1; i <= mMaxPossibleValuesIndex; ++i) {

            int cur_value = possibleValues[i];
            int hole = i;

            for (int j = 0; j < i; ++j) {

                if (cur_value < mPossibleValues[j]) {

                    hole = j;

                    for (int k = i; k > hole; --k)
                        mPossibleValues[k] = mPossibleValues[k - 1];

                    break;
                }
            }

            mPossibleValues[hole] = cur_value;
        }


        if (!mIsFree) {
            super.setMax(1);
            super.setMax(mPossibleValues[mMaxPossibleValuesIndex]);
            setProgress(mPossibleValues[0]);
        } else
            setProgress(mDefaultProgress);

    }


    /**
     * Sets the array of values that the DefinedSeekbar can assume
     * when not in free mode. The values do not have to be sorted.
     *
     * @param resId id of an integer array resource
     */
    public void setPossibleValues(int resId) {

        int[] values = null;

        try {
            values = getResources().getIntArray(resId);
        } catch (Exception e) {
        }

        if (values != null)
            setPossibleValues(values, values.length);
    }


    /**
     * Returns the values possible for the DefinedSeekbar when not
     * in free mode.
     */
    public int[] getPossibleValues() {
        return mPossibleValues;
    }


    /**
     * Sets whether the DefinedSeekbar is in free mode. If in free mode,
     * the DefinedSeekbar acts as a regular SeekBar. If not in free mode,
     * the DefinedSeekbar can only assume a set of specified possible values.
     * If no set possible values values is defined, setting free mode has
     * no effect.
     */
    public void setIsFree(boolean isFree) {

        int progress = getProgress();

        if (isFree) {

            super.setMax(1);    // used to update the position of the thumb
            super.setMax(mFreeMax);
            mIsFree = true;

            setProgress(progress);
        } else {

            if (mPossibleValues != null) {

                super.setMax(1);    // used to update the position of the thumb
                super.setMax(mPossibleValues[mMaxPossibleValuesIndex]);
            }

            mIsFree = false;

            if (mPossibleValues != null && progress < mPossibleValues[0])
                setProgress(mPossibleValues[0]);
            else if (mPossibleValues != null && progress > mPossibleValues[mMaxPossibleValuesIndex])
                setProgress(mPossibleValues[mMaxPossibleValuesIndex]);
            else
                setProgress(progress);
        }


    }


    /**
     * Returns whether the DefinedSeekbar is in free mode.
     */
    public boolean isFree() {
        return mIsFree;
    }


    /**
     * Sets the default progress value of the DefinedSeekbar. This
     * does not have to be one of the possible values defined.
     */
    public void setDefaultProgress(int progress) {
        mDefaultProgress = progress;
    }


    /**
     * Returns the value of the default progress.
     */
    public int getDefaultProgress() {
        return mDefaultProgress;
    }


    /**
     * Resets the DefinedSeekbar to the default progress.
     */
    public void reset() {
        mAllowNextInvalidValue = true;
        setProgress(mDefaultProgress);
        mAllowNextInvalidValue = false;
    }


    // PRIVATE METHODS --------------------------------

    private void loadAttrs(Context context, AttributeSet attrs) {


        mFreeMax = super.getMax();


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DefinedSeekbar);


        mIsFree = a.getBoolean(ATTR_IS_FREE, true);

        int values_id = a.getResourceId(ATTR_POSSIBLE_VALUES, -1);

        if (values_id < 0) {

            mPossibleValues = null;
        } else
            setPossibleValues(values_id);


        a.recycle();
    }


}
