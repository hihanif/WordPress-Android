package org.wordpress.android.ui.posts.adapters

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.ui.posts.PostListItemViewHolder
import org.wordpress.android.ui.posts.PostViewHolderConfig
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.posts.PostListItemType
import org.wordpress.android.viewmodel.posts.PostListItemType.EndListIndicatorItem
import org.wordpress.android.viewmodel.posts.PostListItemType.LoadingItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

private const val VIEW_TYPE_POST = 0
private const val VIEW_TYPE_ENDLIST_INDICATOR = 1
private const val VIEW_TYPE_LOADING = 2

class PostListAdapter(
    context: Context,
    private val postViewHolderConfig: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : PagedListAdapter<PostListItemType, ViewHolder>(PostListDiffItemCallback) {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EndListIndicatorItem -> VIEW_TYPE_ENDLIST_INDICATOR
            is LoadingItem -> VIEW_TYPE_LOADING
            is PostListItemUiState -> VIEW_TYPE_POST
            null -> VIEW_TYPE_LOADING // Placeholder by paged list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ENDLIST_INDICATOR -> {
                val view = layoutInflater.inflate(R.layout.endlist_indicator, parent, false)
                view.layoutParams.height = postViewHolderConfig.endlistIndicatorHeight
                EndListViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = layoutInflater.inflate(R.layout.post_list_item_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_POST -> {
                PostListItemViewHolder(R.layout.post_list_item, parent, postViewHolderConfig, uiHelpers)
            }
            else -> {
                // Fail fast if a new view type is added so the we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // The only holder type that requires a special setup is the PostViewHolder
        if (holder is PostListItemViewHolder) {
            val item = getItem(position)
            assert(item is PostListItemUiState) {
                "If we are presenting PostViewHolder, the item has to be of type PostListItemUiState " +
                        "for position: $position"
            }
            holder.onBind((item as PostListItemUiState))
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class EndListViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

private val PostListDiffItemCallback = object : DiffUtil.ItemCallback<PostListItemType>() {
    override fun areItemsTheSame(oldItem: PostListItemType, newItem: PostListItemType): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.localOrRemoteId == newItem.localOrRemoteId
        }
        if (oldItem is PostListItemUiState && newItem is PostListItemUiState) {
            return oldItem.data.localPostId == newItem.data.localPostId
        }
        if (oldItem is LoadingItem && newItem is PostListItemUiState) {
            return when (oldItem.localOrRemoteId) {
                is LocalId -> oldItem.localOrRemoteId == newItem.data.localPostId.id
                is RemoteId -> oldItem.localOrRemoteId == newItem.data.remotePostId.id
            }
        }
        return false
    }

    override fun areContentsTheSame(oldItem: PostListItemType, newItem: PostListItemType): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.localOrRemoteId == newItem.localOrRemoteId
        }
        if (oldItem is PostListItemUiState && newItem is PostListItemUiState) {
            return oldItem.data == newItem.data
        }
        return false
    }
}
