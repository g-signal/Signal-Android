package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Base64;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import org.signal.core.util.DimensionUnit;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.recipients.GextTag;

/**
 * 图片标签组件（tagType = 1）。
 * 调用 {@link #bind(GextTag)} 传入标签数据；返回 false 表示 base64 数据无效，调用方应跳过此视图。
 */
public class GExtImageTagView extends AppCompatImageView {

  private static final String TAG = Log.tag(GExtImageTagView.class);

  public GExtImageTagView(@NonNull Context context) {
    this(context, null);
  }

  public GExtImageTagView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public GExtImageTagView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * @return true 绑定成功；false base64 数据为空或解码失败，调用方应丢弃此视图
   */
  public boolean bind(@NonNull GextTag tag,int wh) {
    String base64 = tag.getImgBase64();
    if (base64 == null || base64.isEmpty()) return false;

    int commaIdx = base64.indexOf(',');
    if (commaIdx >= 0) base64 = base64.substring(commaIdx + 1).trim();
    if (base64.isEmpty()) return false;

    try {
      byte[] bytes  = Base64.decode(base64, Base64.DEFAULT);
      Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      if (bitmap == null) return false;

      setImageBitmap(bitmap);
      setScaleType(ScaleType.CENTER_CROP);

      int size = (int) DimensionUnit.DP.toPixels(20f);
      setLayoutParams(new LinearLayout.LayoutParams(wh==0?size:wh, wh==0?size:wh));
      return true;
    } catch (Exception e) {
      Log.w(TAG, "bind: failed to decode image for tag " + tag.getTagId(), e);
      return false;
    }
  }
}
