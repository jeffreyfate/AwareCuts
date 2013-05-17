package com.jeffthefate.awarecuts_dev.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
/**
 * Creates a button to be used in preference screen, in this case to finish
 * the activity and save the preferences and start the widget.
 * 
 * @author Jeff Fate
 */
public class ButtonPreference extends Preference {
    /**
     * Global button object
     */
    Button mButton;
    /**
     * Global layout object
     */
    LinearLayout mLayout;
    /**
     * Sets up the object with a layout, seekbar and textview.  Also grabs any
     * persisted values that may exist from the shared preferences.
     * @param context    the parent to create this object under
     * @param attrs        the attribute set for a preference
     */
    public ButtonPreference(Context context, AttributeSet attrs) { 
        super(context,attrs);
        mLayout = new LinearLayout(context);
        mButton = new Button(context);
    }
    @Override 
    protected View onCreateView(ViewGroup parent) {
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setGravity(Gravity.CENTER);
        mButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        if (mLayout.indexOfChild(mButton) == -1)
            mLayout.addView(mButton, 0, new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, 
                    LayoutParams.WRAP_CONTENT));        
        return mLayout;
    }
    @Override 
    protected void onBindView(View view) {
        super.onBindView(view);
    }
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, 
            Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }
    /**
     * Get this preference's button object.
     * @return button object in this preference
     */
    public Button getButton() {
        return mButton;
    }

}