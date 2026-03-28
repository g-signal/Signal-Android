package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.DimensionUnit;
import org.thoughtcrime.securesms.recipients.GextTag;

import java.util.List;

/**
 * 标签容器组件，横向排列多个 {@link GExtTextTagView} / {@link GExtImageTagView}。
 *
 * <p>调用 {@link #bind(List, int)} 传入标签列表与高度（px）即可完成动态渲染；
 * 调用 {@link #clear()} 可清空并隐藏。</p>
 */
public class GExtTagsView extends LinearLayout {

  private static final int TAG_SPACING_DP = 4;

  public GExtTagsView(@NonNull Context context) {
    this(context, null);
  }

  public GExtTagsView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setOrientation(HORIZONTAL);
  }

  public GExtTagsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(HORIZONTAL);
  }

  /**
   * 根据标签列表渲染子 View。
   *
   * @param tags   标签数据列表
   * @param height 每个标签的高度（px），通常传入父级 TextView 的 textSize（像素）
   */
  public void bind(@NonNull List<GextTag> tags, int height) {
    removeAllViews();

    if (tags.isEmpty()) {
      setVisibility(View.GONE);
      return;
    }

    int tagSpacing = (int) DimensionUnit.DP.toPixels(TAG_SPACING_DP);
    int added = 0;

    for (GextTag tag : tags) {
      View tagView = null;

      if (tag.getTagType() == 1) {
        GExtImageTagView imageTag = new GExtImageTagView(getContext());
        if (imageTag.bind(tag, (int)(height* 0.75f))) tagView = imageTag;
      } else {
        String text = tag.getText();
        if (text != null && !text.isEmpty()) {
          GExtTextTagView textTag = new GExtTextTagView(getContext());
          textTag.bind(tag, height * 0.75f, height);
          tagView = textTag;
        }
      }

      if (tagView == null) continue;

      LayoutParams lp = tagView.getLayoutParams() instanceof LayoutParams
          ? (LayoutParams) tagView.getLayoutParams()
          : new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      if (added > 0) lp.setMarginStart(tagSpacing);
      tagView.setLayoutParams(lp);
      addView(tagView);
      added++;
    }

    setVisibility(added > 0 ? View.VISIBLE : View.GONE);
  }

  /** 清空所有标签并隐藏容器。 */
  public void clear() {
    removeAllViews();
    setVisibility(View.GONE);
  }
}
