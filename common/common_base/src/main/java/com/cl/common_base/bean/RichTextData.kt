package com.cl.common_base.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import com.cl.common_base.BaseBean

data class RichTextData(
    val flushingWeigh: String? = null, // 冲刷期重量
    val name: String? = null, // 文档名称
    val txtId: String? = null, // 文本ID
    val testType: String? = null, // 文本ID
    val bar: String? = null, // 文本标题
    val page: MutableList<Page>? = null,
    val topPage: MutableList<TopPage>? = null,
) : BaseBean() {

    data class Page(
        val id: String? = null,
        val type: String? = null,
        val value: Value? = null,
        var videoTag: Boolean = false,
        var videoPosition: Long? = null,
    ) : BaseBean(), MultiItemEntity {
        override val itemType: Int
            get() = when (type) {
                "Bar" -> KEY_TYPE_BAR
                "title" -> KEY_TYPE_TITLE
                "txt" -> KEY_TYPE_TXT
                "picture" -> KEY_TYPE_PICTURE
                "url" -> KEY_TYPE_URL
                "video" -> KEY_TYPE_VIDEO
                "pageDown" -> KEY_TYPE_PAGE_DOWN
                "pageClose" -> KEY_TYPE_PAGE_CLOSE
                "customerService" -> KEY_TYPE_CUSTOMER_SERVICE
                "imageTextJump" -> KEY_TYPE_IMAGE_TEXT_JUMP
                "Discord" -> KEY_TYPE_DISCORD
                "finishTask" -> KEY_TYPE_FINISH_TASK
                "flushingWeigh" -> KEY_TYPE_FLUSHING_WEIGH
                "dryingWeigh" -> KEY_TYPE_DRYING_WEIGH
                "buttonJump" -> KEY_TYPE_BUTTON_JUMP
                else -> KEY_TYPE_BAR
            }
    }

    data class TopPage(
        val id: String? = null,
        val type: String? = null,
        val value: Value? = null,
    )

    data class Value(
        val height: String? = null, // 高度
        val taskCategory: String? = null, // 任务种类
        val taskId: String? = null, // 任务ID
        val taskType: String? = null, // 任务类型
        val title: String? = null, // 标题
        val top: String? = null, // 是否悬浮
        val txt: String? = null, // 文本
        val txtId: String? = null, // 文本ID
        val url: String? = null, // URL
        val width: String? = null, // 宽度
        val icon: String? = null, // 按钮图标
        val autoplay: Boolean? = null, // 自动播放
        ) : BaseBean()


    companion object {
        // 这是Activity页面的标题
        const val KEY_TYPE_BAR = 1

        // 这是内容的标题
        const val KEY_TYPE_TITLE = 2

        // 这是内容文本
        const val KEY_TYPE_TXT = 3

        // 图片
        const val KEY_TYPE_PICTURE = 4

        // 设置视频的url类型、以文本的形式展示
        const val KEY_TYPE_URL = 5

        // 这是视频展示类型、以视频样式展示
        const val KEY_TYPE_VIDEO = 6

        // 跳下页
        const val KEY_TYPE_PAGE_DOWN = 7

        // 关闭本页
        const val KEY_TYPE_PAGE_CLOSE = 8

        // 客服类型
        const val KEY_TYPE_CUSTOMER_SERVICE = 9

        // 图文跳转
        const val KEY_TYPE_IMAGE_TEXT_JUMP = 10

        // Discord 类型
        const val KEY_TYPE_DISCORD = 11

        // 任务完成
        const val KEY_TYPE_FINISH_TASK = 12

        // 清洗期
        const val KEY_TYPE_FLUSHING_WEIGH = 13

        // 干燥器重量
        const val KEY_TYPE_DRYING_WEIGH = 14

        // 跳转到webView
        const val KEY_TYPE_BUTTON_JUMP = 15

        // 自己创建的类型
        // 与商民的无关
        const val KEY_BAR = "Bar"
    }
}