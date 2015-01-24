package pt.gu.zclock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Preference for picking a color which is persisted as an integer.
 *
 * @author Jake Wharton
 */
public class ColorPreference extends DialogPreference implements OnSeekBarChangeListener, View.OnLongClickListener {

    /**
     * Color preview at top of dialog.
     */
    private SurfaceView mPreview;

    private SeekBar mA, mH, mS, mV;
    private int resA = 10, resH = 10, resS = 10, resV = 10;

    private TextView mAValue, mHValue, mSValue, mVValue;

    private int mColor;

    private Integer mTempColor;


    /**
     * Create a new instance of the ColorPreference.
     *
     * @param context Context.
     * @param attrs   Attributes.
     */
    public ColorPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        this.setPersistent(true);
        this.setDialogLayoutResource(R.layout.color_preference);
        setAttrs(attrs);
    }

    private void setAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPreference);
        int res = a.getInt(R.styleable.ColorPreference_Res, 10);
        resA = a.getInt(R.styleable.ColorPreference_aRes, res);
        resH = a.getInt(R.styleable.ColorPreference_hRes, res);
        resS = a.getInt(R.styleable.ColorPreference_sRes, res);
        resV = a.getInt(R.styleable.ColorPreference_vRes, res);
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        this.mPreview = (SurfaceView) view.findViewById(R.id.preview);
        this.mPreview.setBackgroundColor(this.mColor);

        float hsv[] = new float[3];
        Color.colorToHSV(this.mColor, hsv);

        this.mA = (SeekBar) view.findViewById(R.id.colpref_a);
        this.mA.setMax((int) (250 / resA));
        this.mA.setProgress((int) (Color.alpha(this.mColor) / resA));
        this.mA.setOnSeekBarChangeListener(this);

        this.mH = (SeekBar) view.findViewById(R.id.colpref_h);
        this.mH.setMax((int) (360 / resH));
        this.mH.setProgress((int) (hsv[0] / resH));
        this.mH.setOnSeekBarChangeListener(this);

        this.mS = (SeekBar) view.findViewById(R.id.colpref_s);
        this.mS.setMax((int) (100 / resS));
        this.mS.setProgress((int) (hsv[1] * 100 / resS));
        this.mS.setOnSeekBarChangeListener(this);

        this.mV = (SeekBar) view.findViewById(R.id.colpref_v);
        this.mV.setMax((int) (100 / resV));
        this.mV.setProgress((int) (hsv[2] * 100 / resV));
        this.mV.setOnSeekBarChangeListener(this);

        this.mAValue = (TextView) view.findViewById(R.id.colpref_a_value);
        this.mAValue.setText(String.valueOf(Color.alpha(this.mColor)));
        this.mAValue.setOnLongClickListener(this);
        this.mHValue = (TextView) view.findViewById(R.id.colpref_h_value);
        this.mHValue.setText(String.valueOf((int) hsv[0]));
        this.mSValue = (TextView) view.findViewById(R.id.colpref_s_value);
        this.mSValue.setText(String.valueOf((int) (hsv[1] * 100)));
        this.mVValue = (TextView) view.findViewById(R.id.colpref_v_value);
        this.mVValue.setText(String.valueOf((int) (hsv[2] * 100)));
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        final int a = ColorPreference.this.mA.getProgress() * resA;
        final float h = ColorPreference.this.mH.getProgress() * resH;
        final float s = ColorPreference.this.mS.getProgress() * resS;
        final float v = ColorPreference.this.mV.getProgress() * resV;

        final int color = Color.HSVToColor(a, new float[]{h, s / 100, v / 100});

        ColorPreference.this.mAValue.setText(String.valueOf(a));
        ColorPreference.this.mHValue.setText(String.valueOf(h));
        ColorPreference.this.mSValue.setText(String.valueOf(s));
        ColorPreference.this.mVValue.setText(String.valueOf(v));

        ColorPreference.this.setValue(color);
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(final boolean restore, final Object defaultValue) {
        final int color = this.getPersistedInt(defaultValue == null ? 0 : (Integer) defaultValue);
        this.mColor = color;
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            this.mTempColor = this.mColor;
            if (this.callChangeListener(this.mTempColor)) {
                this.saveValue(this.mTempColor);
            }
        }
    }

    /**
     * Set the value of the color and update the preview.
     *
     * @param color Color value.
     */
    public void setValue(final int color) {
        this.mColor = color;
        this.mPreview.setBackgroundColor(color);
    }

    /**
     * Set and persist the value of the color.
     *
     * @param color Color value.
     */
    public void saveValue(final int color) {
        this.setValue(color);
        this.persistInt(color);
    }

    public int getColor() {
        return mColor;
    }
}