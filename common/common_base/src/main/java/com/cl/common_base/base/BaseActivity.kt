package com.cl.common_base.base

import android.app.Activity
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ViewDataBinding
import com.cl.common_base.BaseBinding
import com.cl.common_base.R
import com.cl.common_base.constants.Constants
import com.cl.common_base.util.ActivityCollector
import com.cl.common_base.util.StatusBarUtil
import com.cl.common_base.util.livedatabus.LiveEventBus
import com.orhanobut.logger.Logger
import java.lang.ref.WeakReference
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity(), BaseBinding<VB> {

    /**
     * 日志输出标志
     */
    protected val TAG: String = this.javaClass.simpleName

    protected lateinit var binding: VB

    private val inflate = "inflate"

    /**
     * 当前Activity的实例。
     */
    private var activity: Activity? = null

    /** 当前Activity的弱引用，防止内存泄露  */
    private var activityWR: WeakReference<Activity>? = null

    /**
     * dialog
     */
    private val loadingDialog by lazy {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_loading_common)
        dialog.window?.setBackgroundDrawable(null)
        dialog
    }

    /**
     * theme color
     */
//    private var themeColor: Int = SettingUtil.getColor()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initColor()

        activity = this
        activityWR = WeakReference(activity!!)
        ActivityCollector.pushTask(activityWR)
        initVb()
        initSplash()
        if (this::binding.isInitialized) {
            setContentView(binding.root)
            binding.initBinding()
            initView()
            observe()
            initData()
        }
    }

    override fun onStart() {
        super.onStart()
        // 蓝牙状态监听变化
        LiveEventBus.get().with(Constants.Ble.KEY_BLE_STATE, String::class.java)
            .observe(this) {
                onBleChange(it)
            }
        // 设备状态监听变化
        LiveEventBus.get().with(Constants.Device.KEY_DEVICE_TO_APP, String::class.java)
            .observe(this) {
                onDeviceChange(it)
            }
        // 涂鸦发送给app数据监听
        LiveEventBus.get().with(Constants.Tuya.KEY_TUYA_DEVICE_TO_APP, String::class.java)
            .observe(this) {
                onTuYaToAppDataChange(it)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    open fun initSplash() {

    }

    private fun initVb() {
        kotlin.runCatching {
            val type = javaClass.genericSuperclass
            if (type is ParameterizedType) {
                val vbCls = type.actualTypeArguments[0] as Class<VB>
                when {
                    // 当页面中填写的是ViewBinding则表示不需要使用ViewBinding功能
                    ViewDataBinding::class.java.isAssignableFrom(vbCls) && vbCls != ViewDataBinding::class.java -> {
                        vbCls.getDeclaredMethod(inflate, LayoutInflater::class.java).let {
                            binding = it.invoke(null, layoutInflater) as VB
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Parameter err! Generic ViewDataBinding fail!")
            }
        }.onFailure {
            Logger.e("$this initVb error, ${it.message} ")
        }
    }

    abstract fun initView()
    abstract fun observe()
    abstract fun initData()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    open fun initColor() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isLightStatusBar()) {
//            window.decorView.apply {
//                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//            }
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLightNavigationBar()) {
//            window.decorView.apply {
//                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//            }
//        }
//        window.statusBarColor = getStatusBarColor()
//        window.navigationBarColor = getNavigationBarColor()

        StatusBarUtil.setColor(this, Color.TRANSPARENT, 0)
        StatusBarUtil.setLightMode(this)
        StatusBarUtil.transparentNavBar(this)
    }

    open fun isLightStatusBar(): Boolean = true
    open fun isLightNavigationBar(): Boolean = true

    /**
     * 状态栏默认白底，该功能需要系统版本23，因为需要状态栏内容变灰，低版本为主题色
     */
    open fun getStatusBarColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            ResourcesCompat.getColor(resources, android.R.color.white, theme)
        else
            ResourcesCompat.getColor(resources, R.color.mainColor, theme)
    }

    /**
     * 导航栏默认白底，该功能需要系统版本26，因为需要导航栏内容变灰。低版本为黑色
     */
    open fun getNavigationBarColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getWindowBackgroundColor()
        else
            ResourcesCompat.getColor(resources, android.R.color.black, theme)
    }

    private fun getWindowBackgroundColor(): Int {
        val a = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return a.data
        }
        return ResourcesCompat.getColor(resources, android.R.color.black, theme)
    }


    /**
     * 设置状态栏颜色
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setStatusBarColor(color: Int) {
        window?.statusBarColor = color
    }

    /**
     * 设置虚拟导航栏颜色
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setNavigationBarColor(color: Int) {
        window?.navigationBarColor = color
    }

    fun lightNavigationBar(light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window?.decorView?.apply {
                systemUiVisibility = if (light) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
        }
    }

    fun lightStatusBar(light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window?.decorView?.apply {
                systemUiVisibility = if (light) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    fun showProgressLoading(
        text: String? = getString(R.string.loading),
        cancelable: Boolean = true
    ) {
        val textView = loadingDialog.findViewById<TextView>(R.id.text_loading_message)
        textView?.apply {
            visibility = if (text == null) View.GONE else View.VISIBLE
            this.text = text
        }
        loadingDialog.setCancelable(cancelable)

        if (!loadingDialog.isShowing) {
            loadingDialog.show()
        }
    }

    fun hideProgressLoading() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    override fun VB.initBinding() {

    }

    /**
     * 监听蓝牙的开关
     */
    open fun connect(): Boolean {
        return true
    }

    open fun close(): Boolean = false


    /**
     * 蓝牙状态改变回调
     *
     * @param status
     */
    open fun onBleChange(status: String) {}

    /**
     * 设备状态改变回调
     */
    open fun onDeviceChange(status: String) {}

    /**
     * 涂鸦发送给App的信息
     */
    open fun onTuYaToAppDataChange(status: String) {}


    override fun onDestroy() {
        super.onDestroy()
        activity = null
        ActivityCollector.removeTask(activityWR)
        binding.let {
            it.unbind()
        }
    }
}