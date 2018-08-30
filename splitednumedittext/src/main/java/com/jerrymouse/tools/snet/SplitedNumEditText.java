package com.jerrymouse.tools.snet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * 自定义分段输入控件，支持银行卡、身份证、手机号分段输入。
 * @author 程序园中猿
 */

public class SplitedNumEditText extends LinearLayout {
    private EditText inputEt;
    private ImageView deleteIv;
    /**
     * 普通数字(含小数点)，不需要分隔
     */
    public static final int TYPE_COMMON = 0;
    public static final int TYPE_BANK_CARD = 1;
    public static final int TYPE_ID_CARD = 2;
    public static final int TYPE_PHONE = 3;
    private int start, count, before;
    private int contentType;
    private int maxLength = 50;
    private String digits;
    private TextChangeListener textChangeListener;

    public SplitedNumEditText(Context context) {
        this(context, null, 0);
    }

    public SplitedNumEditText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SplitedNumEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_num_input_item, this);
        inputEt = (EditText) findViewById(R.id.et_input);
        deleteIv = (ImageView) findViewById(R.id.iv_delete);
        init(context, attrs);
    }

    public void setInputType(int type) {
        if (contentType == TYPE_COMMON || contentType == TYPE_PHONE || contentType == TYPE_BANK_CARD) {
            type = InputType.TYPE_CLASS_NUMBER;
        } else if (contentType == TYPE_ID_CARD) {
            type = InputType.TYPE_CLASS_TEXT;
        }
        inputEt.setInputType(type);
        /* 非常重要:setKeyListener要在setInputType后面调用，否则无效。*/
        if (!TextUtils.isEmpty(digits)) {
            inputEt.setKeyListener(DigitsKeyListener.getInstance(digits));
        }
    }

    protected void init(final Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SplitedNumEditText);
        String hint = typedArray.getString(R.styleable.SplitedNumEditText_item_hint);
        ColorStateList textColor = typedArray.getColorStateList(R.styleable.SplitedNumEditText_item_text_color);
        float textSize = typedArray.getDimensionPixelOffset(R.styleable.SplitedNumEditText_item_text_size, 36);
        contentType = typedArray.getInt(R.styleable.SplitedNumEditText_item_type, 0);
        int maxCount = typedArray.getInteger(R.styleable.SplitedNumEditText_item_max_len, 30);
        typedArray.recycle();
        initType();
        inputEt.setHint(hint);
        inputEt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxCount)});
        inputEt.setTextSize(px2dip(context, textSize));
        if (textColor != null) {
            inputEt.setTextColor(textColor);
        }

        inputEt.setSingleLine();
        inputEt.addTextChangedListener(watcher);
        inputEt.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    deleteIv.setVisibility(GONE);
                } else {
                    if (inputEt.getText().length() > 0) {
                        deleteIv.setVisibility(VISIBLE);
                    } else {
                        deleteIv.setVisibility(GONE);
                    }
                }
            }
        });
        deleteIv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                inputEt.setText("");
                inputEt.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(inputEt, InputMethodManager.SHOW_FORCED);
                if (textChangeListener != null) {
                    textChangeListener.onChanged(inputEt.getText().toString());
                }
            }
        });

    }

    private void showDeleteIcon(Editable s) {
        if (s.toString().length() > 0) {
            deleteIv.setVisibility(VISIBLE);
        } else {
            deleteIv.setVisibility(GONE);
        }
    }

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            SplitedNumEditText.this.start = start;
            SplitedNumEditText.this.before = before;
            SplitedNumEditText.this.count = count;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s == null) {
                return;
            }
            showDeleteIcon(s);
            if (textChangeListener != null) {
                textChangeListener.onChanged(s.toString());
            }
            //判断是否是在中间输入，需要重新计算
            boolean isMiddle = (start + count) < (s.length());
            //在末尾输入时，是否需要加入空格
            boolean isNeedSpace = false;
            if (!isMiddle && isSpace(s.length())) {
                isNeedSpace = true;
            }
            if (isMiddle || isNeedSpace || count > 1) {
                String newStr = s.toString();
                newStr = newStr.replace(" ", "");
                StringBuilder sb = new StringBuilder();
                int spaceCount = 0;
                for (int i = 0; i < newStr.length(); i++) {
                    sb.append(newStr.substring(i, i + 1));
                    //如果当前输入的字符下一位为空格(i+1+1+spaceCount)，因为i是从0开始计算的，所以一开始的时候需要先加1
                    if (isSpace(i + 2 + spaceCount)) {
                        sb.append(" ");
                        spaceCount += 1;
                    }
                }
                inputEt.removeTextChangedListener(watcher);
                s.replace(0, s.length(), sb);
                //如果是在末尾的话,或者加入的字符个数大于零的话（输入或者粘贴）
                if (!isMiddle || count > 1) {
                    inputEt.setSelection(s.length() <= maxLength ? s.length() : maxLength);
                } else {
                    //如果是删除
                    if (count == 0) {
                        //如果删除时，光标停留在空格的前面，光标则要往前移一位
                        if (isSpace(start - before + 1)) {
                            inputEt.setSelection((start - before) > 0 ? start - before : 0);
                        } else {
                            inputEt.setSelection((start - before + 1) > s.length() ? s.length() : (start - before + 1));
                        }
                    }
                    //如果是增加
                    else {
                        if (isSpace(start - before + count)) {
                            inputEt.setSelection((start + count - before + 1) < s.length() ? (start + count - before + 1) : s.length());
                        } else {
                            inputEt.setSelection(start + count - before);
                        }
                    }
                }
                inputEt.addTextChangedListener(watcher);
            }
        }
    };

    private void initType() {
        if (contentType == TYPE_COMMON) {
            digits = ".0123456789 ";
            setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if (contentType == TYPE_PHONE) {
            digits = "0123456789 ";
            setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if (contentType == TYPE_BANK_CARD) {
            digits = "0123456789 ";
            setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if (contentType == TYPE_ID_CARD) {
            digits = null;
            setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }


    private boolean isSpace(int length) {
        if (contentType == TYPE_PHONE) {
            return isSpacePhone(length);
        } else if (contentType == TYPE_BANK_CARD) {
            return isSpaceCard(length);
        } else if (contentType == TYPE_ID_CARD) {
            return isSpaceIDCard(length);
        }
        return false;
    }

    private boolean isSpacePhone(int length) {
        return length >= 4 && (length == 4 || (length + 1) % 5 == 0);
    }

    private boolean isSpaceCard(int length) {
        return length % 5 == 0;
    }

    private boolean isSpaceIDCard(int length) {
        return length > 6 && (length == 7 || (length - 2) % 5 == 0);
    }

    public String getText() {
        return inputEt.getText().toString().replaceAll(" ", "");
    }

    public void setText(String text) {
        inputEt.setText(text);
    }

    public void setTextChangeListener(TextChangeListener textChangeListener) {
        this.textChangeListener = textChangeListener;
    }


    public interface TextChangeListener {
        /**
         * 监听文本内容版画
         *
         * @Parameter text 监听文本
         */
        void onChanged(String text);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

}
