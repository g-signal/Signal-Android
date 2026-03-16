package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import org.signal.core.util.DimensionUnit;
import org.signal.glide.Log;
import org.thoughtcrime.securesms.recipients.GextTag;

/**
 * 文字标签组件（tagType = 0）。
 * 调用 {@link #bind(GextTag)} 传入标签数据即完成样式渲染。
 */
public class GExtTextTagView extends AppCompatTextView {

  public GExtTextTagView(@NonNull Context context) {
    this(context, null);
  }

  public GExtTextTagView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public GExtTextTagView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void bind(@NonNull GextTag tag,float textSize,int height) {
    setText(tag.getText());
    setSingleLine(true);
      Log.d("GExtTextTagView","textSize"+textSize);
    setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    setGravity(Gravity.CENTER);

//    int paddingH = (int) DimensionUnit.DP.toPixels(6f);
//    int paddingV = (int) DimensionUnit.DP.toPixels(2f);
    setPadding(5, 0, 5, 0);

    if (tag.getCssColor() != null) {
      try {
        setTextColor(Color.parseColor(tag.getCssColor()));
      } catch (Exception ignored) {
        setTextColor(Color.WHITE);
      }
    }

    GradientDrawable bg = new GradientDrawable();
    if (tag.getCssBackgroundColor() != null) {
      try {
        int baseColor = Color.parseColor(tag.getCssBackgroundColor());
        int alpha     = Math.round(tag.getCssOpacity() * 255f);
        bg.setColor(Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
      } catch (Exception ignored) {}
    }
    if (tag.getCssBorderRadius() > 0) {
      bg.setCornerRadius(DimensionUnit.DP.toPixels((float) tag.getCssBorderRadius()));
    }
    if (tag.getCssBorderWidth() > 0 && tag.getCssBorderColor() != null && "solid".equals(tag.getCssBorderStyle())) {
      try {
        int borderColor = Color.parseColor(tag.getCssBorderColor());
        int borderWidth = (int) DimensionUnit.DP.toPixels((float) tag.getCssBorderWidth());
        bg.setStroke(borderWidth, borderColor);
      } catch (Exception ignored) {}
    }
    setBackground(bg);

    setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, height
    ));
  }
}
