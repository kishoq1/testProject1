package com.example.testproject.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@SuppressLint("ContextCastToActivity")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    player: Player?,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lấy context hiện tại và ép kiểu về Activity để kiểm tra chế độ PiP
    val context = LocalContext.current as Activity
    val inPipMode = context.isInPictureInPictureMode

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    // Chỉ hiển thị các nút điều khiển khi KHÔNG ở trong chế độ PiP
                    useController = !inPipMode
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    setFullscreenButtonClickListener {
                        onToggleFullScreen()
                    }
                }
            },
            update = { view ->
                // Cập nhật lại trình phát và trạng thái hiển thị của các nút điều khiển
                view.player = player
                view.useController = !context.isInPictureInPictureMode
                if (isFullScreen) {
                    view.showController()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}