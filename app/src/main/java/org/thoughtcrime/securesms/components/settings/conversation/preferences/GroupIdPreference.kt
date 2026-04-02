package org.thoughtcrime.securesms.components.settings.conversation.preferences

import android.view.View
import android.widget.TextView
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.PreferenceModel
import org.thoughtcrime.securesms.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.util.adapter.mapping.MappingViewHolder

/**
 * Displays the group ID with a copy button in the conversation settings.
 */
object GroupIdPreference {

  private const val MAX_DISPLAY_LENGTH = 24

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_group_id_preference))
  }

  class Model(
    val groupId: String,
    val onCopyClick: () -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model) = true
    override fun areContentsTheSame(newItem: Model) = super.areContentsTheSame(newItem) && groupId == newItem.groupId
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {
    private val label: TextView = itemView.findViewById(R.id.group_id_label)
    private val copy: TextView = itemView.findViewById(R.id.group_id_copy)

    override fun bind(model: Model) {
      val truncated = if (model.groupId.length > MAX_DISPLAY_LENGTH) {
        val half = MAX_DISPLAY_LENGTH / 2
        "${model.groupId.take(half)}...${model.groupId.takeLast(half)}"
      } else {
        model.groupId
      }
      label.text = "Group ID: $truncated"
      copy.setOnClickListener { model.onCopyClick() }
    }
  }
}
