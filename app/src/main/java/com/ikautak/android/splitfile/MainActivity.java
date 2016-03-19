package com.ikautak.android.splitfile;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "SplitFile";

    private TextView mFileNameText;
    private EditText mSizeEditText;
    private EditText mShiftNumEditText;
    private Button mSplitButton;

    private Runnable mRunnable;
    private ProgressDialog mDialog;

    private String mFilePath;
    private int mFileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        mRunnable = this;

        String file_name = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (DEBUG) Log.i(TAG, "file " + file_name);
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        mFilePath = uri.getPath();

        // Display target file name.
        mFileNameText = (TextView)findViewById(R.id.textview_filename);
        mFileSize = get_file_size(mFilePath);
        mFileNameText.setText(String.format("%s : %s", mFilePath, get_size_str(mFileSize)));

        // Split file size.
        mSizeEditText = (EditText)findViewById(R.id.edittext_mb);
        if (mSizeEditText != null) {
            mSizeEditText.setText("3");
        }

        // Data shift num.
        mShiftNumEditText = (EditText)findViewById(R.id.edittext_shift);
        if (mShiftNumEditText != null) {
            mShiftNumEditText.setText("3");
        }

        // Dialog
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("Split");
        mDialog.setMessage("wait...");
        mDialog.setIndeterminate(false);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mSplitButton = (Button)findViewById(R.id.button_split);
        mSplitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.show();

                Thread thread = new Thread(mRunnable);
                thread.start();
            }
        });
    }

    @Override
    public void run() {
        split();

        mDialog.dismiss();

        // Close Activity.
        finish();
    }

    private void split() {
        if (DEBUG) Log.i(TAG, "split");

        final int SPLIT_SIZE = Integer.parseInt(mSizeEditText.getText().toString()) * 1024 * 1024;
        final int SPLIT_NUM = (mFileSize + SPLIT_SIZE - 1) / SPLIT_SIZE;
        final int last = mFileSize % SPLIT_SIZE;
        final byte SHIFT_NUM = (byte)(Integer.parseInt(mShiftNumEditText.getText().toString()) & 0xff);
        if (DEBUG) Log.i(TAG, "split size " + SPLIT_SIZE + " num " + SPLIT_NUM + " last " + last);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mFilePath);
            BufferedInputStream in = new BufferedInputStream(fis);
            byte buf[] = new byte[SPLIT_SIZE];

            for (int i = 0; i < SPLIT_NUM; i++) {
                int read_size = SPLIT_SIZE;
                if (i == SPLIT_NUM - 1) {
                    read_size = last;
                }

                // Read
                int l = in.read(buf, 0, read_size);
                if (DEBUG) Log.i(TAG, "read " + l + " bytes");

                if (SHIFT_NUM > 0) {
                    shift_byte(buf, l, SHIFT_NUM);
                }

                // Write 1 file.
                FileOutputStream fos = new FileOutputStream(mFilePath + ".frac" + i);
                BufferedOutputStream out = new BufferedOutputStream(fos);
                out.write(buf, 0, l);
                if (DEBUG) Log.i(TAG, "write " + l + " bytes");

                out.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int get_file_size(String file_name) {
        int size = 0;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file_name);
            size = fis.available();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return size;
    }

    private String get_size_str(int size) {
        String size_str;

        if (size > 1024 * 1024) {
            size_str = Float.toString(size / 1024f / 1024f) + "MB";
            size_str += " (" + Integer.toString(size) + "B)";
        } else if (size > 1024) {
            size_str = Float.toString(size / 1024f) + "KB";
            size_str += " (" + Integer.toString(size) + "B)";
        } else {
            size_str = Integer.toString(size) + "B";
        }

        return size_str;
    }

    private void shift_byte(byte[] d, int len, byte shift) {
        for (int i = 0; i < len; i++) {
            d[i] = (byte)((d[i] + shift) & 0xff);
        }
    }
}
