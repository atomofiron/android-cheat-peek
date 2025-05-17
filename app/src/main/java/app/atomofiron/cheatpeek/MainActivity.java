package app.atomofiron.cheatpeek;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.*;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

import androidx.annotation.NonNull;

public class MainActivity
    extends
        Activity
    implements
        CompoundButton.OnCheckedChangeListener
      , SeekBar.OnSeekBarChangeListener
{

    private static final float ALPHA_ENABLED = 1f;
    private static final float ALPHA_DISABLED = .5f;

    private static final String KEY_PATH = "KEY_PATH";
    private static final String KEY_ZERO_CODE = "KEY_ZERO_CODE";
    private static final String KEY_ONE_CODE = "KEY_ONE_CODE";
    private static final String KEY_SUBMIT_CODE = "KEY_SUBMIT_CODE";
    private static final String KEY_PREV_CODE = "KEY_PREV_CODE";
    private static final String KEY_NEXT_CODE = "KEY_NEXT_CODE";

    private static final int REQUEST_CODE = 777;
    private static final int FIRST = 0;
    private static final int MAX_PROGRESS = 100;
    private static final int UNDEFINED = -1;
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int NO_REPEAT = -1;

    private TextView tvLastCode;
    private TextView tvCurrent;
    private TextView tvCurrentPath;
    private EditText etPath;
    private TextView tvZero;
    private TextView tvOne;
    private TextView tvSubmit;
    private TextView tvPrev;
    private TextView tvNext;
    private ToggleButton btnZeroEdit;
    private ToggleButton btnOneEdit;
    private ToggleButton btnSubmitEdit;
    private ToggleButton btnPrevEdit;
    private ToggleButton btnNextEdit;
    private TextView tvTip;
    private SeekBar sbUnlock;
    private Button btnLock;
    private Button btnPermission;
    private ImageView imageView;
    private View blockView;

    private static final Pattern imagePattern = Pattern.compile(".+\\.(png|jpe?g)$", Pattern.CASE_INSENSITIVE);
    private SharedPreferences sp;
    private Vibrator vibrator;

    private int zeroCode = 24;
    private int oneCode = 25;
    private int submitCode = 79;
    private int prevCode = 88;
    private int nextCode = 87;
    private final long[] patternZero = {0, 50};
    private final long[] patternOne = {0, 50, 100, 50};
    private final long[] patternSuccess = {0, 200};
    private final long[] patternError = {0, 200, 100, 200};

    private int[] array = null;
    private boolean seekAllowed = false;
    private boolean locked = true;
    private String cursorPath = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View controls = findViewById(R.id.controls);
        controls.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets windowInsets) {
                int left = windowInsets.getSystemWindowInsetLeft();
                int top = windowInsets.getSystemWindowInsetTop();
                int right = windowInsets.getSystemWindowInsetRight();
                int bottom = windowInsets.getSystemWindowInsetBottom();
                controls.setPadding(left, top, right, bottom);
                return windowInsets;
            }
        });

        tvLastCode = findViewById(R.id.tv_code);
        tvCurrent = findViewById(R.id.tv_current);
        etPath = findViewById(R.id.et_path);
        tvCurrentPath = findViewById(R.id.tv_current_file);
        tvZero = findViewById(R.id.tv_zero);
        tvOne = findViewById(R.id.tv_one);
        tvSubmit = findViewById(R.id.tv_submit);
        tvPrev = findViewById(R.id.tv_prev);
        tvNext = findViewById(R.id.tv_next);
        btnZeroEdit = findViewById(R.id.btn_edit_zero);
        btnOneEdit = findViewById(R.id.btn_edit_one);
        btnSubmitEdit = findViewById(R.id.btn_edit_submit);
        btnPrevEdit = findViewById(R.id.btn_edit_prev);
        btnNextEdit = findViewById(R.id.btn_edit_next);
        tvTip = findViewById(R.id.tv_tip);
        sbUnlock = findViewById(R.id.sb_unlock);
        btnLock = findViewById(R.id.btn_lock);
        btnPermission = findViewById(R.id.btn_permission);
        imageView = findViewById(R.id.iv_image);
        blockView = findViewById(R.id.v_block);

        tvCurrent.setText(getString(R.string.current, ""));
        tvCurrentPath.setText(getString(R.string.current_path, ""));

        //noinspection deprecation
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        etPath.setText(sp.getString(KEY_PATH, etPath.getText().toString()));
        zeroCode = sp.getInt(KEY_ZERO_CODE, zeroCode);
        oneCode = sp.getInt(KEY_ONE_CODE, oneCode);
        submitCode = sp.getInt(KEY_SUBMIT_CODE, submitCode);
        prevCode = sp.getInt(KEY_PREV_CODE, prevCode);
        nextCode = sp.getInt(KEY_NEXT_CODE, nextCode);

        btnZeroEdit.setOnCheckedChangeListener(this);
        btnOneEdit.setOnCheckedChangeListener(this);
        btnSubmitEdit.setOnCheckedChangeListener(this);
        btnPrevEdit.setOnCheckedChangeListener(this);
        btnNextEdit.setOnCheckedChangeListener(this);
        btnLock.setOnClickListener(v -> lockControls(true));
        btnPermission.setOnClickListener(v -> requestPermission());
        sbUnlock.setOnSeekBarChangeListener(this);

        printCodes();
        lockControls(true);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        hideNavigationBar();
    }

    private void hideNavigationBar() {
        Window window = getWindow();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN;
        window.getDecorView().setSystemUiVisibility(uiOptions);
        window.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
    }

    private void vibrate(long[] pattern) {
        if (SDK_INT >= VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT));
        } else {
            vibrator.vibrate(pattern, NO_REPEAT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        btnPermission.setVisibility(havePermission() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sp.edit().putString(KEY_PATH, etPath.getText().toString()).apply();
    }

    @Override
    public void onBackPressed() {
        if (imageView.getVisibility() == View.VISIBLE) {
            imageView.setVisibility(View.GONE);
            blockView.setVisibility(View.GONE);
        } else if (!locked)
            super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;

        int id = buttonView.getId();

        if (btnZeroEdit.isChecked() && btnZeroEdit.getId() != id) {
            btnZeroEdit.setChecked(false);
        }
        if (btnOneEdit.isChecked() && btnOneEdit.getId() != id) {
            btnOneEdit.setChecked(false);
        }
        if (btnSubmitEdit.isChecked() && btnSubmitEdit.getId() != id) {
            btnSubmitEdit.setChecked(false);
        }
        if (btnPrevEdit.isChecked() && btnPrevEdit.getId() != id) {
            btnPrevEdit.setChecked(false);
        }
        if (btnNextEdit.isChecked() && btnNextEdit.getId() != id) {
            btnNextEdit.setChecked(false);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekAllowed = false;
        tvTip.setVisibility(View.GONE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.isEnabled()) {
            seekBar.setProgress(0);
            tvTip.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        seekAllowed = seekAllowed || progress < MAX_PROGRESS / 2;

        if (seekAllowed && progress == MAX_PROGRESS) {
            lockControls(false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }

        tvLastCode.setText(getString(R.string.last_code, keyCode));
        processKey(keyCode);

        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[FIRST] == PackageManager.PERMISSION_GRANTED) {
            btnPermission.setVisibility(View.GONE);
        }
    }

    private void processKey(int keyCode) {
        boolean changed = false;

        if (btnZeroEdit.isChecked()) {
            btnZeroEdit.setChecked(false);
            zeroCode = keyCode;
            changed = true;
        } else if (btnOneEdit.isChecked()) {
            btnOneEdit.setChecked(false);
            oneCode = keyCode;
            changed = true;
        } else if (btnSubmitEdit.isChecked()) {
            btnSubmitEdit.setChecked(false);
            submitCode = keyCode;
            changed = true;
        } else if (btnPrevEdit.isChecked()) {
            btnPrevEdit.setChecked(false);
            prevCode = keyCode;
            changed = true;
        } else if (btnNextEdit.isChecked()) {
            btnNextEdit.setChecked(false);
            nextCode = keyCode;
            changed = true;
        }
        if (changed) {
            saveCodes();
            printCodes();
        } else if (locked) {
            parse(keyCode);
        }
    }

    private void parse(int keyCode) {
        if (array == null) {
            array = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        }

        if (keyCode == submitCode) {
            submit();
        } else if (keyCode == zeroCode) {
            vibrate(patternZero);
            updateCurrent(ZERO);
        } else if (keyCode == oneCode) {
            vibrate(patternOne);
            updateCurrent(ONE);
        } else if (keyCode == prevCode) {
            goTo(false);
        } else if (keyCode == nextCode) {
            goTo(true);
        }
    }

    private void updateCurrent(int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == UNDEFINED) {
                array[i] = value;
                break;
            }
        }

        setColor(R.color.colorNormal);
        printCurrent();
        printCurrentPath();
    }

    private void printCurrent() {
        StringBuilder arrStr = new StringBuilder("[ ");
        for (int x : array) {
            if (x == UNDEFINED) break;
            arrStr.append(x);
            arrStr.append(" ");
        }
        arrStr.append("] = ");
        arrStr.append(decodeCurrent());

        tvCurrent.setText(getString(R.string.current, arrStr));
    }

    private void printCurrentPath() {
        String path = getFilePathIfExists();
        path = (path == null) ? "null" : path.substring(path.lastIndexOf('/') + 1);
        tvCurrentPath.setText(getString(R.string.current_path, path));
    }

    private String decodeCurrent() {
        int length = 0;
        for (int x : array) {
            if (x == UNDEFINED) break;
            length++;
        }
        if (length == 0) {
            return null;
        }
        int sum = 0;
        for (int i = 0; i < length; i++) {
            int q = array[i];
            for (int j = i + 1; j < length; j++) {
                q *= 2;
            }
            sum += q;
        }
        return String.valueOf(sum);
    }

    private boolean isEmpty() {
        return array[0] == UNDEFINED;
    }

    private void submit() {
        String path = getFilePathIfExists();
        if (!isEmpty() && path != null) {
            showImage(path);
            blockView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            vibrate(patternSuccess);

            setColor(R.color.colorReady);
            array = null;
            cursorPath = path;
        } else {
            if (blockView.getVisibility() == View.VISIBLE) {
                blockView.setVisibility(View.GONE);
            } else if (imageView.getVisibility() == View.VISIBLE) {
                blockView.setVisibility(View.VISIBLE);
            } else {
                setColor(R.color.colorWrong);
                vibrate(patternError);

                printCurrent();
                printCurrentPath();
                array = null;
                cursorPath = null;
            }
        }
        hideNavigationBar();
    }

    private void showImage(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        imageView.setImageBitmap(bitmap);
    }

    private String getFilePathIfExists() {
        String name = decodeCurrent();
        if (name != null) {
            String path = etPath.getText().toString();
            String[] list = getFiles(path);
            if (list != null)
                for (String file : list)
                    if (isImage(file) && file.startsWith(name + "."))
                        return path + "/" + file;
        }
        return null;
    }

    private void setColor(int color) {
        color = getResources().getColor(color);
        tvCurrent.setTextColor(color);
        tvCurrentPath.setTextColor(color);
    }

    private void goTo(boolean goNext) {
        if (cursorPath == null ||
                imageView.getVisibility() == View.GONE ||
                blockView.getVisibility() == View.VISIBLE) {
            return;
        }

        String path = etPath.getText().toString() + "/";
        String[] list = getFiles(path);
        String fileName = null;
        if (list != null) {
            String prev = null;
            boolean stopNext = false;
            for (String f : list) {
                if (!isImage(f)) continue;

                if (stopNext) {
                    fileName = f;
                    break;
                }
                if (cursorPath.endsWith("/" + f)) {
                    if (goNext)
                        stopNext = true;
                    else {
                        fileName = prev;
                        break;
                    }
                }
                prev = f;
            }
        }
        if (fileName != null) {
            path += fileName;
            showImage(path);
            cursorPath = path;
        }
    }

    private void saveCodes() {
        sp.edit()
                .putInt(KEY_ZERO_CODE, zeroCode)
                .putInt(KEY_ONE_CODE, oneCode)
                .putInt(KEY_SUBMIT_CODE, submitCode)
                .putInt(KEY_PREV_CODE, prevCode)
                .putInt(KEY_NEXT_CODE, nextCode)
                .apply();
    }

    private void printCodes() {
        tvZero.setText(getString(R.string.zero_code, zeroCode));
        tvOne.setText(getString(R.string.one_code, oneCode));
        tvSubmit.setText(getString(R.string.submit_code, submitCode));
        tvPrev.setText(getString(R.string.prev_code, prevCode));
        tvNext.setText(getString(R.string.next_code, nextCode));
    }

    private void lockControls(boolean lock) {
        locked = lock;

        tvCurrent.setAlpha(lock ? ALPHA_ENABLED : ALPHA_DISABLED);
        tvCurrentPath.setAlpha(lock ? ALPHA_ENABLED : ALPHA_DISABLED);

        etPath.setEnabled(!lock);
        etPath.clearFocus();

        float alpha = lock ? ALPHA_DISABLED : ALPHA_ENABLED;
        tvZero.setAlpha(alpha);
        tvOne.setAlpha(alpha);
        tvSubmit.setAlpha(alpha);
        tvPrev.setAlpha(alpha);
        tvNext.setAlpha(alpha);

        btnZeroEdit.setEnabled(!lock);
        btnOneEdit.setEnabled(!lock);
        btnSubmitEdit.setEnabled(!lock);
        btnPrevEdit.setEnabled(!lock);
        btnNextEdit.setEnabled(!lock);
        btnLock.setEnabled(!lock);

        sbUnlock.setEnabled(lock);
        sbUnlock.setProgress(lock ? 0 : sbUnlock.getMax());
        tvTip.setVisibility(lock ? View.VISIBLE : View.GONE);
    }

    private boolean havePermission() {
        if (SDK_INT > VERSION_CODES.R) {
            return Environment.isExternalStorageManager() || checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (SDK_INT > VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (SDK_INT > VERSION_CODES.R) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
            );
            startActivity(intent);
        } else if (SDK_INT > VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    private static boolean isImage(String file) {
        return imagePattern.matcher(file).matches();
    }

    private static String[] getFiles(String dirPath) {
        File dir = new File(dirPath);
        String[] list = (dir.list() == null) ? new String[]{} : dir.list();
        assert list != null;
        Arrays.sort(list, Sorter.INSTANCE);
        return list;
    }

    private static class Sorter implements Comparator<String> {
        static final Sorter INSTANCE = new Sorter();

        @Override
        public int compare(String first, String second) {
            return first.compareTo(second);
        }
    }
}
