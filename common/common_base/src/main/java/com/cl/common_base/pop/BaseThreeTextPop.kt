package com.cl.common_base.pop

import android.content.Context
import androidx.core.view.isNotEmpty
import androidx.databinding.DataBindingUtil
import com.cl.common_base.R
import com.cl.common_base.databinding.BaseThreePopBinding
import com.cl.common_base.util.ViewUtils
import com.lxj.xpopup.core.CenterPopupView

/**
 * 三行文字弹窗
 */
class BaseThreeTextPop(
    context: Context,
    val content: String? = null,
    val oneLineText: String? = null,
    val twoLineText: String? = null,
    val threeLineText: String? = null,
    private val oneLineCLickEventAction: (() -> Unit)? = null,
    private val twoLineCLickEventAction: (() -> Unit)? = null,
    private val threeLineCLickEventAction: (() -> Unit)? = null,
) : CenterPopupView(context) {
    override fun getImplLayoutId(): Int {
        return R.layout.base_three_pop
    }

    private var binding: BaseThreePopBinding? = null
    override fun onCreate() {
        super.onCreate()

        binding = DataBindingUtil.bind<BaseThreePopBinding>(popupImplView)?.apply {
            tvContent.text = content
            tvOne.text = oneLineText
            tvTwo.text = twoLineText
            tvThree.text = threeLineText

            tvOne.setOnClickListener {
                oneLineCLickEventAction?.invoke()
                dismiss()
            }
            tvTwo.setOnClickListener {
                twoLineCLickEventAction?.invoke()
                dismiss()
            }
            tvThree.setOnClickListener {
                threeLineCLickEventAction?.invoke()
                dismiss()
            }
        }
    }
}