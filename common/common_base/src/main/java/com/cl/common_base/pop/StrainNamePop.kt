package com.cl.common_base.pop

import android.content.Context
import android.graphics.Typeface
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import com.cl.common_base.R
import com.cl.common_base.databinding.StrainNameBinding
import com.cl.common_base.widget.toast.ToastUtil
import com.lxj.xpopup.core.BottomPopupView

/**
 * StrainName Pop
 */
class StrainNamePop(
    context: Context,
    private val onConfirmAction: ((strainName: String) -> Unit)? = null,
    private val onCancelAction: (() -> Unit)? = null,
) : BottomPopupView(context) {
    override fun getImplLayoutId(): Int {
        return R.layout.strain_name
    }

    override fun onCreate() {
        super.onCreate()

        DataBindingUtil.bind<StrainNameBinding>(popupImplView)?.apply {
            tvHow.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)

            ivClose.setOnClickListener {
                onCancelAction?.invoke()
                dismiss()
                btnSuccess.isEnabled = false
            }

            strainName.addTextChangedListener {
                if (it.isNullOrEmpty()) return@addTextChangedListener

                // 点击按钮状态监听
                btnSuccess.isEnabled = !it.isNullOrEmpty()
            }

            // 清空输入内容
            curingDelete.setOnClickListener {
                strainName.setText("")
            }

            tvHow.setOnClickListener {
                // todo 跳转固定的图文介绍界面
            }

            clNotKnow.setOnClickListener {
                // 跳过的话，默认名字
                onConfirmAction?.invoke("I don’t know")
            }

            btnSuccess.setOnClickListener {
                // 输入范围为1～24字节
                if (getTextLength(strainName.text.toString()) < 1 || getTextLength(strainName.text.toString()) > 24) {
                    ToastUtil.shortShow(context.getString(R.string.strain_name_desc))
                    return@setOnClickListener
                }
                onConfirmAction?.invoke(strainName.text.toString())
            }

        }
    }

    override fun onDismiss() {
        onCancelAction?.invoke()
        super.onDismiss()
    }

    private fun getTextLength(text: CharSequence): Int {
        var length = 0
        for (element in text) {
            if (element.code > 255) {
                length += 2
            } else {
                length++
            }
        }
        return length
    }
}