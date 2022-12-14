package com.cl.modules_my.ui

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.text.TextPaint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.cl.common_base.base.BaseActivity
import com.cl.common_base.bean.CalendarData
import com.cl.common_base.bean.FinishTaskReq
import com.cl.common_base.bean.UpdateReq
import com.cl.common_base.constants.Constants
import com.cl.common_base.constants.RouterPath
import com.cl.common_base.constants.UnReadConstants
import com.cl.common_base.ext.DateHelper
import com.cl.common_base.ext.dp2px
import com.cl.common_base.ext.logI
import com.cl.common_base.ext.resourceObserver
import com.cl.common_base.help.PermissionHelp
import com.cl.common_base.help.SeedGuideHelp
import com.cl.common_base.pop.*
import com.cl.common_base.util.Prefs
import com.cl.common_base.util.ViewUtils
import com.cl.common_base.util.calendar.Calendar
import com.cl.common_base.util.calendar.CalendarEventUtil
import com.cl.common_base.util.calendar.CalendarUtil
import com.cl.common_base.util.device.DeviceControl
import com.cl.common_base.util.device.TuYaDeviceConstants
import com.cl.common_base.util.json.GSON
import com.cl.common_base.widget.AbTextViewCalendar
import com.cl.common_base.widget.SvTextView
import com.cl.common_base.widget.toast.ToastUtil
import com.cl.modules_my.R
import com.cl.modules_my.adapter.MyCalendarAdapter
import com.cl.modules_my.databinding.MyCalendayActivityBinding
import com.cl.modules_my.viewmodel.CalendarViewModel
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.goldentec.android.tools.util.let
import com.goldentec.android.tools.util.letMultiple
import com.joketng.timelinestepview.LayoutType
import com.joketng.timelinestepview.OrientationShowType
import com.joketng.timelinestepview.adapter.TimeLineStepAdapter
import com.joketng.timelinestepview.view.TimeLineStepView
import com.lxj.xpopup.XPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.util.*
import javax.inject.Inject


@Route(path = RouterPath.My.PAGE_MY_CALENDAR)
@AndroidEntryPoint
class CalendarActivity : BaseActivity<MyCalendayActivityBinding>() {

    @Inject
    lateinit var mViewMode: CalendarViewModel

    private val adapter by lazy {
        MyCalendarAdapter(mutableListOf())
    }

    private val pop by lazy {
        XPopup.Builder(this@CalendarActivity)
            .isDestroyOnDismiss(false)
            .dismissOnTouchOutside(false)
    }

    override fun initView() {
        // ????????????????????????????????????
        binding.title.setTitle(getString(com.cl.common_base.R.string.my_calendar))
            .setTitleColor(com.cl.common_base.R.color.mainColor)
            .setQuickClickListener {
                // ?????????????????????
                val data = adapter.data
                if (data.isEmpty()) return@setQuickClickListener
                val layoutManager = binding.rvList.layoutManager as GridLayoutManager
                val findFirstVisibleItemPosition =
                    layoutManager.findFirstCompletelyVisibleItemPosition()
                val currentPosition = data.indexOfFirst { it.isCurrentDay }
                if (currentPosition > findFirstVisibleItemPosition) {
                    // ???????????????
                    binding.rvList.scrollToPosition(currentPosition + 7 * 3)
                } else if (currentPosition < findFirstVisibleItemPosition) {
                    binding.rvList.scrollToPosition(currentPosition - 7)
                }
                // ?????????????????????
                binding.abMonth.text = CalendarUtil.getMonthFromLocation(Date().time)
                binding.tvTodayDate.text = mViewMode.getYmdForEn(Date())
            }.setLeftClickListener {
                setResult(RESULT_OK)
                finish()
            }

        // ???????????????????????????
        initCalendarData()
        // ?????????
        binding.rvList.layoutManager = GridLayoutManager(this@CalendarActivity, 7)
        binding.rvList.adapter = adapter

        // help
        val snapHelper = GravitySnapHelper(Gravity.TOP)
        snapHelper.attachToRecyclerView(binding.rvList)

        // ????????????
        adapter.addChildClickViewIds(R.id.ll_root)
        // ??????????????????
        setMaxFlingVelocity(binding.rvList, 2000)
        // ??????????????????
        binding.tvTodayDate.text = mViewMode.getYmdForEn(Date())
    }

    private fun initCalendarData() {
        // ????????????12???????????????
        mViewMode.getLocalCalendar(
            year = CalendarUtil.getFormat("yyyy").format(Date().time).toInt()
        )
    }

    override fun MyCalendayActivityBinding.initBinding() {
        binding.apply {
            lifecycleOwner = this@CalendarActivity
            viewModel = mViewMode
            executePendingBindings()
        }
    }

    private val basePumpWaterFinishPop by lazy {
        BasePumpWaterFinishedPop(
            this@CalendarActivity,
            onSuccessAction = {
                // ???????????????????????????OK??????
                // ???????????????????????????
                mViewMode.deviceOperateFinish(UnReadConstants.StatusManager.VALUE_STATUS_PUMP_WATER)
            })
    }

    override fun observe() {
        mViewMode.apply {
            // ???????????????
            showCompletePage.observe(this@CalendarActivity) {
                if (it) {
                    // ????????????????????????????????????????????????
                    setResult(RESULT_OK, Intent().putExtra(Constants.Global.KEY_IS_SHOW_COMPLETE, true))
                    finish()
                }
            }

            // ?????????????????????
            plantInfo.observe(this@CalendarActivity, resourceObserver {
                loading { showProgressLoading() }
                error { errorMsg, code ->
                    ToastUtil.shortShow(errorMsg)
                    hideProgressLoading()
                }
                success {
                    hideProgressLoading()
                    // ????????????FlushWeight???????????????
                    when (mViewMode.guideInfoStatus.value) {
                        UnReadConstants.PlantStatus.TASK_TYPE_CHECK_CHECK_CURING -> {
                            if ((data?.flushingWeight ?: 0) <= 0) {
                                mViewMode.taskId.value?.let { mViewMode.finishTask(FinishTaskReq(taskId = it)) }
                                // ????????????????????????????????????????????????
                                setResult(RESULT_OK, Intent().putExtra(Constants.Global.KEY_IS_SHOW_COMPLETE, true))
                                finish()
                            } else {
                                // ????????????
                                mViewMode.guideInfoStatus.value?.let {
                                    mViewMode.getGuideInfo(it)
                                }
                            }
                        }
                    }
                }
            })
            // ???????????????????????????
            advertising.observe(this@CalendarActivity, resourceObserver {
                loading { showProgressLoading() }
                error { errorMsg, code ->
                    ToastUtil.shortShow(errorMsg)
                    hideProgressLoading()
                }
                success {
                    hideProgressLoading()
                    pop
                        .enableDrag(false)
                        .maxHeight(dp2px(700f))
                        .dismissOnTouchOutside(false)
                        .asCustom(
                            BasePumpWaterPop(
                                this@CalendarActivity,
                                { status ->
                                    // ?????????????????????????????????
                                    DeviceControl.get()
                                        .success {
                                            // todo ????????????????????????
                                        }
                                        .error { code, error ->
                                            ToastUtil.shortShow(
                                                """
                                                pumpWater: 
                                                code-> $code
                                                errorMsg-> $error
                                            """.trimIndent()
                                            )
                                        }
                                        .pumpWater(status)
                                },
                                onWaterFinishedAction = {
                                    // ?????????????????????????????????
                                    if (basePumpWaterFinishPop.isShow) return@BasePumpWaterPop
                                    pop
                                        .isDestroyOnDismiss(false)
                                        .enableDrag(false)
                                        .maxHeight(dp2px(600f))
                                        .dismissOnTouchOutside(false)
                                        .asCustom(
                                            basePumpWaterFinishPop
                                        ).show()
                                },
                                data = this.data,
                            )
                        ).show()
                }
            })

            // ?????????????????? - deviceOperateFinish
            deviceOperateFinish.observe(this@CalendarActivity, resourceObserver {
                loading { showProgressLoading() }
                error { errorMsg, code ->
                    ToastUtil.shortShow(errorMsg)
                    hideProgressLoading()
                }
                success {
                    hideProgressLoading()
                    // ???????????????????????????
                    when (mViewMode.getWaterVolume.value) {
                        // ????????????
                        "0L" -> {
                            // ????????????
                            mViewMode.taskId.value?.let {
                                mViewMode.deviceOperateStart(
                                    it,
                                    UnReadConstants.StatusManager.VALUE_STATUS_ADD_WATER
                                )
                            }
                            pop
                                .isDestroyOnDismiss(false)
                                .enableDrag(false)
                                .dismissOnTouchOutside(false)
                                .asCustom(
                                    HomePlantFourPop(
                                        context = this@CalendarActivity,
                                        onNextAction = {
                                            // ????????????
                                            pop
                                                .isDestroyOnDismiss(false)
                                                .enableDrag(false)
                                                .dismissOnTouchOutside(false)
                                                .asCustom(
                                                    HomePlantFivePop(
                                                        context = this@CalendarActivity,
                                                        onCancelAction = {},
                                                        onNextAction = {
                                                            // ?????????????????????????????????
                                                            mViewMode.taskId.value?.let {
                                                                mViewMode.deviceOperateStart(
                                                                    it,
                                                                    UnReadConstants.StatusManager.VALUE_STATUS_ADD_MANURE
                                                                )
                                                            }

                                                            pop
                                                                .isDestroyOnDismiss(false)
                                                                .maxHeight(dp2px(600f))
                                                                .enableDrag(false)
                                                                .dismissOnTouchOutside(false)
                                                                .asCustom(
                                                                    // ????????????
                                                                    HomePlantSixPop(
                                                                        context = this@CalendarActivity,
                                                                        onNextAction = {
                                                                            // ???????????????????????????
                                                                            DeviceControl.get()
                                                                                .success {
                                                                                    // ?????????????????????????????????????????????????????????
                                                                                    // ????????????????????????
                                                                                    mViewMode.taskId.value?.let {
                                                                                        mViewMode.finishTask(
                                                                                            FinishTaskReq(it)
                                                                                        )
                                                                                    }
                                                                                }
                                                                                .error { code, error ->
                                                                                    ToastUtil.shortShow(
                                                                                        """
                                                                                        feedAbby:
                                                                                        code-> $code
                                                                                        errorMsg-> $error
                                                                                    """.trimIndent()
                                                                                    )
                                                                                }
                                                                                .feedAbby(true)
                                                                        }
                                                                    )
                                                                ).show()

                                                        }
                                                    )
                                                ).show()
                                        }
                                    )
                                ).show()
                        }
                        else -> {
                            // ?????????????????????????????????
                            mViewMode.taskId.value?.let {
                                mViewMode.deviceOperateStart(
                                    it,
                                    UnReadConstants.StatusManager.VALUE_STATUS_ADD_MANURE
                                )
                            }
                            pop
                                .isDestroyOnDismiss(false)
                                .maxHeight(dp2px(600f))
                                .enableDrag(false)
                                .dismissOnTouchOutside(false)
                                .asCustom(
                                    // ????????????
                                    HomePlantSixPop(
                                        context = this@CalendarActivity,
                                        onNextAction = {
                                            // ???????????????????????????
                                            DeviceControl.get()
                                                .success {
                                                    // ??????????????????
                                                    if (Prefs.getBoolean(Constants.Global.KEY_IS_SHOW_FEET_POP, true)) {
                                                        pop
                                                            .isDestroyOnDismiss(false)
                                                            .maxHeight(dp2px(600f))
                                                            .enableDrag(false)
                                                            .dismissOnTouchOutside(false)
                                                            .asCustom(
                                                                BaseBottomPop(
                                                                    this@CalendarActivity,
                                                                    backGround = ContextCompat.getDrawable(
                                                                        this@CalendarActivity,
                                                                        com.cl.common_base.R.mipmap.base_feet_fall_bg
                                                                    ),
                                                                    text = getString(com.cl.common_base.R.string.base_feet_fall),
                                                                    buttonText = getString(com.cl.common_base.R.string.base_feet_fall_button_text),
                                                                    bottomText = getString(com.cl.common_base.R.string.base_dont_show),
                                                                    onNextAction = {
                                                                        // ?????????????????????????????????????????????????????????
                                                                        // ????????????????????????
                                                                        mViewMode.taskId.value?.let {
                                                                            mViewMode.finishTask(
                                                                                FinishTaskReq(it)
                                                                            )
                                                                        }
                                                                    },
                                                                    bottomTextAction = {
                                                                        // ?????????????????????????????????????????????????????????
                                                                        // ????????????????????????
                                                                        mViewMode.taskId.value?.let {
                                                                            mViewMode.finishTask(
                                                                                FinishTaskReq(it)
                                                                            )
                                                                        }
                                                                    }
                                                                )
                                                            ).show()
                                                    } else {
                                                        // ?????????????????????????????????????????????????????????
                                                        // ????????????????????????
                                                        mViewMode.taskId.value?.let {
                                                            mViewMode.finishTask(
                                                                FinishTaskReq(it)
                                                            )
                                                        }
                                                    }
                                                }
                                                .error { code, error ->
                                                    ToastUtil.shortShow(
                                                        """
                                                              feedAbby:
                                                              code-> $code
                                                              errorMsg-> $error
                                                        """.trimIndent()
                                                    )
                                                }
                                                .feedAbby(true)
                                        }
                                    )
                                ).show()
                        }
                    }
                }
            })

            // guideInfo
            getGuideInfo.observe(this@CalendarActivity, resourceObserver {
                loading { showProgressLoading() }
                error { errorMsg, code ->
                    ToastUtil.shortShow(errorMsg)
                    hideProgressLoading()
                }
                success {
                    hideProgressLoading()
                    // ?????????????????????
                    pop
                        .enableDrag(true)
                        .maxHeight(dp2px(700f))
                        .dismissOnTouchOutside(false)
                        .isDestroyOnDismiss(false)
                        .asCustom(
                            BasePlantUsuallyGuidePop(
                                this@CalendarActivity,
                                onNextAction = { weight ->
                                    // ???????????????????????????
                                    val status = mViewMode.guideInfoStatus.value
                                    if (status.isNullOrEmpty()) return@BasePlantUsuallyGuidePop
                                    when (status) {
                                        CalendarData.TASK_TYPE_CHANGE_WATER -> {
                                        }
                                        CalendarData.TASK_TYPE_CHANGE_CUP_WATER -> {
                                            mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                        }
                                        CalendarData.TASK_TYPE_LST -> {
                                            mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                        }
                                        CalendarData.TASK_TYPE_TOPPING -> {
                                            mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                        }
                                        CalendarData.TASK_TYPE_TRIM -> {
                                            mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                        }
                                        CalendarData.TASK_TYPE_CHECK_TRANSPLANT -> {
                                            // todo ???????????????????????????????????????????????????????????????
                                            // seed to veg
                                            SeedGuideHelp(this@CalendarActivity).showGuidePop {
                                                mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                            }
                                        }
                                        else -> {
                                            mViewMode.taskId.value?.let { taskId -> mViewMode.finishTask(FinishTaskReq(taskId, weight)) }
                                        }
                                    }
                                },
                                isShowRemindMe = mViewMode.guideInfoTaskTime.value?.isNotEmpty(),
                                onRemindMeAction = {
                                    // ????????????
                                    // ??????
                                    // ???????????? & ???????????????????????????
                                    // todo ????????????
                                    // 1667864539000 + 172800000
                                    mViewMode.updateTask(
                                        UpdateReq(
                                            taskId = mViewMode.taskId.value,
                                            taskTime = "${(mViewMode.guideInfoTaskTime.value?.toLong() ?: 0L) + (0L + 60 * 60 * 1000 * 48)}"
                                        )
                                    )
                                }
                            ).setData(data)
                        ).show()
                }
            })

            // ????????????
            localCalendar.observe(this@CalendarActivity) {
                if (it.isNullOrEmpty()) return@observe
                // ????????????????????????????????????????????????????????????????????????
                adapter.setList(it)
                // ??????
                // ?????????????????????????????????
                // todo ???????????????????????????????????????????????????
                binding.rvList.scrollToPosition(it.indexOf(mViewMode.mCurrentDate) - 7)
                // todo  ???????????????,, ??????????????????7??????????????????????????????
                binding.abMonth.text =
                    CalendarUtil.getMonthFromLocation(adapter.data[it.indexOf(mViewMode.mCurrentDate) + 7].timeInMillis)
                // ??????????????????
                letMultiple(it.firstOrNull()?.ymd, it.lastOrNull()?.ymd) { first, last ->
                    mViewMode.getCalendar(first, last)
                }
            }

            // ??????????????????
            getCalendar.observe(this@CalendarActivity, resourceObserver {
                loading { }
                error { errorMsg, code ->
                    hideProgressLoading()
                    errorMsg?.let { ToastUtil.shortShow(it) }
                }
                success {
                    hideProgressLoading()
                    // ???????????????``
                    // todo ????????????
                    if (data.isNullOrEmpty()) return@success
                    if (mViewMode.localCalendar.value.isNullOrEmpty()) return@success
                    lifecycleScope.launch(Dispatchers.IO) {
                        // ????????????
                        val local = mViewMode.localCalendar.value
                        local?.let { localData ->
                            // ?????????????????????
                            // ???????????????
                            kotlin.runCatching {
                                // ??????????????????????????????????????????????????????????????????
                                localData.forEachIndexed { index, calendar ->
                                    calendar.calendarData = data?.get(index)
                                }
                                withContext(Dispatchers.Main) {
                                    // ????????????
                                    adapter.setList(local)
                                    if (mViewMode.onlyRefreshLoad.value == true) {
                                        // ?????????????????????
                                        // ????????????isChooser = true ?????????
                                        adapter.data.indexOfFirst { it.isChooser }.let { index ->
                                            showTaskList(adapter.data[index])
                                        }
                                    } else {
                                        showTaskList(mViewMode.mCurrentDate)
                                    }
                                }

                            }
                        }
                    }
                }
            })

            updateTask.observe(this@CalendarActivity, resourceObserver {
                error { errorMsg, _ ->
                    ToastUtil.shortShow(errorMsg)
                }
            })
            finishTask.observe(this@CalendarActivity, resourceObserver {
                error { errorMsg, _ ->
                    ToastUtil.shortShow(errorMsg)
                }
            })
        }
    }

    override fun initData() {
        // ??????????????????
        binding.tvCycle.setOnClickListener {
            // ???????????????
            adapter.data.firstOrNull { it.isChooser }?.apply {
                this.calendarData.epochExplain?.let { it1 -> mViewMode.getGuideInfo(it1) }
            }
        }

        adapter.setOnItemChildClickListener { adapter, view, position ->
            val list = adapter.data as? MutableList<com.cl.common_base.util.calendar.Calendar>
            val data = adapter.data[position] as? com.cl.common_base.util.calendar.Calendar
            when (view.id) {
                R.id.ll_root -> {
                    if (list.isNullOrEmpty()) return@setOnItemChildClickListener
                    val rlDay = view.findViewById<RelativeLayout>(R.id.tv_content_day)
                    rlDay.background = ContextCompat.getDrawable(
                        this@CalendarActivity,
                        com.cl.common_base.R.drawable.base_dot_main_color
                    )

                    // ?????????true
                    // ??????????????????????????????
                    list.indexOfFirst { it.isChooser }.let {
                        if (it != -1) {
                            list[it].isChooser = false
                            adapter.notifyItemChanged(it)
                        }
                    }

                    data?.isChooser = true
                    adapter.notifyItemChanged(position)

                    // ??????????????????
                    //??????
                    val animation = ScaleAnimation(
                        1.0f, 0.5f, 1.0f, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                    )
                    animation.duration = 600 //????????????
                    animation.repeatCount = 0 //??????????????????
                    rlDay.startAnimation(animation) //??????View????????????

                    // todo ???????????????????????????????????????
                    data?.timeInMillis?.let {
                        binding.tvTodayDate.text = mViewMode.getYmdForEn(time = it)
                    }

                    // ???????????????taskList
                    showTaskList(data, true)
                }


            }
        }


        // ??????????????????
        binding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            /**
             *??? RecyclerView????????????????????????????????????????????????
             *
             * @param recyclerView
             * @param newState     ????????????????????????????????????
             * 						RecyclerView.SCROLL_STATE_IDLE
             *                      RecyclerView.SCROLL_STATE_DRAGGING
             *                      RecyclerView.SCROLL_STATE_SETTLING
             *
             *                      findFirstVisibleItemPositions(int[]) ????????????????????????span???items?????????
            findLastVisibleItemPositions(int[]) ???????????????????????????span???items?????????
            findFirstCompletelyVisibleItemPositions(int[]) ??????????????????????????????span???items?????????
            findLastCompletelyVisibleItemPositions(int[]) ?????????????????????????????????span???items?????????

             */
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                /**
                 * ?????????????????????????????????????????????
                 */
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        layoutManager.findFirstVisibleItemPosition()
                        layoutManager.findFirstCompletelyVisibleItemPosition()
                        layoutManager.findLastVisibleItemPosition()
                        layoutManager.findLastCompletelyVisibleItemPosition()

                        //                        logI(
                        //                            """
                        //                               ${layoutManager.findFirstVisibleItemPosition()}
                        //                               ${layoutManager.findFirstCompletelyVisibleItemPosition()}
                        //                               ${layoutManager.findLastVisibleItemPosition()}
                        //                               ${layoutManager.findLastCompletelyVisibleItemPosition()}
                        //                            """.trimIndent()
                        //                        )

                        // ?????????????????????????????????
                        scrollByDate()
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        //                        logI(
                        //                            """
                        //                            SCROLL_STATE_DRAGGING:
                        //                            ${layoutManager.findFirstVisibleItemPosition()}
                        //                               ${layoutManager.findFirstCompletelyVisibleItemPosition()}
                        //                               ${layoutManager.findLastVisibleItemPosition()}
                        //                               ${layoutManager.findLastCompletelyVisibleItemPosition()}
                        //                        """.trimIndent()
                        //                        )
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        //                        logI(
                        //                            """
                        //                            SCROLL_STATE_SETTLING:
                        //                            ${layoutManager.findFirstVisibleItemPosition()}
                        //                               ${layoutManager.findFirstCompletelyVisibleItemPosition()}
                        //                               ${layoutManager.findLastVisibleItemPosition()}
                        //                               ${layoutManager.findLastCompletelyVisibleItemPosition()}
                        //                        """.trimIndent()
                        //                        )


                    }
                }

            }

            /**
             * ??? RecyclerView ?????????????????????????????????????????????????????????????????????????????????
             * ???????????????????????????????????????????????????item range changes??????????????????????????????
             * ?????????????????? dx ??? dy ?????? 0.
             *
             * @param recyclerView
             * @param dx ?????????horizontal scroll?????????????????????
             * @param dy ?????????vertical scroll????????????
             */
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // todo ???????????????????????????
                // val layoutManager = recyclerView.layoutManager as GridLayoutManager
                //                logI(
                //                    """
                //                            onScrolled:
                //                            ${layoutManager.findFirstVisibleItemPosition()}
                //                               ${layoutManager.findFirstCompletelyVisibleItemPosition()}
                //                               ${layoutManager.findLastVisibleItemPosition()}
                //                               ${layoutManager.findLastCompletelyVisibleItemPosition()}
                //                        """.trimIndent()
                //                )
            }
        })


        // ????????????
        clickEvent()
    }


    /**
     * ????????????????????????????????????5??????????????????????????????????????????
     */
    private fun scrollByDate() {
        val layoutManager = binding.rvList.layoutManager as? GridLayoutManager
        val thirdLineFirst =
            layoutManager?.findFirstCompletelyVisibleItemPosition()?.plus(17)
        if (adapter.data.isEmpty()) return
        // ??????????????????
        thirdLineFirst?.let {
            binding.abMonth.text =
                CalendarUtil.getMonthFromLocation(adapter.data[it].timeInMillis)
        }
    }

    private fun showTaskList(
        data: Calendar?,
        isExecutionAlphaAni: Boolean? = false
    ) {
        val getCalendarDate = mViewMode.getCalendar.value?.data
        if (getCalendarDate?.isEmpty() == true) return
        val calendarData = getCalendarDate?.firstOrNull { it.date == data?.ymd }
        // ????????????????????????
        val startTime = calendarData?.epochStartTime ?: ""
        val endTime = calendarData?.epochEndTime ?: ""
        // ?????????????????????????????????
        val diffDay = CalendarUtil.getDatePoor(
            DateHelper.formatToLong(endTime, "yyyy-MM-dd"),
            DateHelper.formatToLong(startTime, "yyyy-MM-dd")
        )
        val currentPosition = adapter.data.indexOfFirst { it.ymd == startTime }
        adapter.data.filter { it.isShowBg }.forEach {
            val i = adapter.data.indexOf(it)
            adapter.data[i].isShowBg = false
            adapter.notifyItemChanged(i)
        }
        // ???????????????????????????????????????????????????????????????????????????
        // ?????????????????????????????????isChooser ??????????????????
        adapter.data.firstOrNull { it.isChooser }.apply {
            if (null == this) {
                adapter.data.indexOfFirst { it.isCurrentDay }.apply {
                    adapter.data[this].isChooser = true
                    adapter.notifyItemChanged(this)
                }
            }
        }
        logI(
            """
            diffDay???
            $diffDay
            $currentPosition
        """.trimIndent()
        )

        // ?????????????????????
        // ????????????????????????????????????????????????
        if (currentPosition != -1) {
            for (i in currentPosition..(currentPosition + diffDay)) {
                adapter.data[i].isShowBg = true
                // ????????????????????????
                when (i) {
                    currentPosition -> {
                        adapter.data[i].bgFlag = Calendar.KEY_START
                    }
                    currentPosition + diffDay -> {
                        adapter.data[i].bgFlag = Calendar.KEY_END
                    }
                    else -> {
                        adapter.data[i].bgFlag = Calendar.KEY_NORMAL
                    }
                }
                adapter.notifyItemChanged(i)
            }
        }

        if (isExecutionAlphaAni == true) {
            // ?????????????????????0-1???alpha??????
            // Alpha?????? ?????????
            val animation = AlphaAnimation(
                0f, 1f
            )
            animation.duration = 1000 //????????????
            animation.repeatCount = 0 //??????????????????
            binding.llRoot.startAnimation(animation) //??????View????????????
        }

        // ?????????????????????????????????????????????????????????????????????????????????
        ViewUtils.setVisible(
            null == calendarData,
            binding.svtDayBg,
            binding.svtPeriodBg,
            binding.svtTaskListBg
        )
        // ?????????????????????????????????????????????????????????????????????????????????
        ViewUtils.setGone(binding.timeLine, null == calendarData)

        // ??????????????????????????????????????????????????????
        calendarData?.let {
            // ???????????????????????????
            // ???null?????????????????????
            ViewUtils.setVisible(!it.epoch.isNullOrEmpty(), binding.tvCycle, binding.ivAsk)
            ViewUtils.setVisible(!it.day.isNullOrEmpty(), binding.tvDay)
            // ??????
            binding.tvCycle.text = it.epoch
            // ??????
            binding.tvDay.text = "Day${it.day}"
            // ??????????????????????????????, ??????????????? or ???????????????
            ViewUtils.setVisible(it.taskList.isNullOrEmpty(), binding.rlEmpty)
            ViewUtils.setVisible(!it.taskList.isNullOrEmpty(), binding.timeLine)
            initTime(it.taskList ?: mutableListOf())
        }
    }

    private fun clickEvent() {
        binding.rlCycle.setOnClickListener {

        }
    }

    // ???????????????????????????????????????
    // ??????RecyclerView??????????????????
    private fun setMaxFlingVelocity(rv: RecyclerView, velocity: Int) {
        try {
            val field: Field = rv.javaClass.getDeclaredField("mMaxFlingVelocity")
            field.isAccessible = true
            field.set(rv, velocity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ??????????????????
     */
    private fun initTime(data: MutableList<CalendarData.TaskList>) {
        val listContent = mutableListOf<CalendarData.TaskList>()
        listContent.addAll(data)
        listContent.add(0, CalendarData.TaskList())
        binding.timeLine.initData(
            listContent,
            OrientationShowType.CENTER_VERTICAL,
            object : TimeLineStepView.OnInitDataCallBack {
                override fun onBindDataViewHolder(
                    holder: TimeLineStepAdapter.CustomViewHolder,
                    position: Int
                ) {
                    if (position == 0) {
                        holder.rightLayout.visibility = View.GONE
                        holder.leftLayout.visibility = View.GONE
                        val layoutParams = holder.imgMark.layoutParams as LinearLayout.LayoutParams
                        layoutParams.width = dp2px(0f)
                        layoutParams.height = dp2px(0f)
                        holder.llLine.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                        if (position == 0) holder.imgMark.setImageDrawable(null) else holder.imgMark.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@CalendarActivity,
                                R.mipmap.my_iv_red_circle
                            )
                        )
                        return
                    }
                    val layoutParams = holder.imgMark.layoutParams as LinearLayout.LayoutParams
                    holder.llLine.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    holder.imgLineEnd.layoutParams.width = dp2px(0.8f)
                    holder.imgLineStart.layoutParams.width = dp2px(0.8f)
                    holder.imgMark.scaleType = ImageView.ScaleType.CENTER_CROP

                    // ????????????
                    logI(
                        """
                        initTime ->  time:
                        ${Date().time}
                        ${listContent[position].taskTime?.toLong() ?: 0L}
                        ${
                            DateHelper.after(
                                Date(),
                                Date(listContent[position].taskTime?.toLong() ?: 0L)
                            )
                        }
                        ${
                            CalendarUtil.getFormat("yyyy-MM-dd")
                                .format(listContent[position].taskTime?.toLong() ?: 0L)
                        }
                        ${CalendarUtil.getFormat("yyyy-MM-dd").format(Date().time)}
                    """.trimIndent()
                    )
                    if (DateHelper.after(
                            Date(),
                            Date(listContent[position].taskTime?.toLong() ?: 0L)
                        )
                    ) {
                        // ??????????????????taskTime(????????????)
                        holder.imgMark.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@CalendarActivity,
                                com.cl.common_base.R.drawable.base_dot_gray
                            )
                        )
                    } else if (DateHelper.after(
                            Date(
                                listContent[position].taskTime?.toLong() ?: 0L
                            ), Date()
                        ) || CalendarUtil.getFormat("yyyy-MM-dd").format(
                            listContent[position].taskTime?.toLong() ?: 0L
                        ) == CalendarUtil.getFormat("yyyy-MM-dd").format(Date().time)
                    ) {
                        // ??????????????????????????????taskTime(????????????)
                        when (listContent[position].taskCategory) {
                            CalendarData.TYPE_CHANGE_WATER -> {
                                holder.imgMark.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@CalendarActivity,
                                        com.cl.common_base.R.drawable.base_dot_change_water
                                    )
                                )
                            }
                            CalendarData.TYPE_PERIOD_CHECK -> {
                                holder.imgMark.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@CalendarActivity,
                                        com.cl.common_base.R.drawable.base_dot_change_period
                                    )
                                )
                            }
                            CalendarData.TYPE_TRAIN -> {
                                holder.imgMark.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@CalendarActivity,
                                        com.cl.common_base.R.drawable.base_dot_change_train
                                    )
                                )
                            }
                        }

                    }



                    logI(
                        """
                        task:
                        ${listContent[position].taskTime}
                        ${listContent[position].taskName}
                    """.trimIndent()
                    )
                    val tvTaskTime = holder.leftLayout.findViewById<TextView>(R.id.tv_task_time)
                    tvTaskTime.text = listContent[position].taskTime?.toLong()
                        ?.let { DateHelper.formatTime(it, "HH:mm", Locale.US) }

                    val tvTaskName = holder.rightLayout.findViewById<TextView>(R.id.tv_task_name)
                    // ??????
                    val svtWaitUnlock =
                        holder.rightLayout.findViewById<AbTextViewCalendar>(R.id.svt_wait_unlock)
                    val svtUnlock =
                        holder.rightLayout.findViewById<SvTextView>(R.id.svt_unlock)
                    val svtGrayUnlock =
                        holder.rightLayout.findViewById<SvTextView>(R.id.svt_gray_unlock)
                    when (listContent[position].taskStatus) {
                        // (1-????????????0-?????????????????????2-?????????????????????)
                        "1" -> {
                            ViewUtils.setGone(svtUnlock)
                            ViewUtils.setGone(svtGrayUnlock)
                            ViewUtils.setVisible(svtWaitUnlock)
                            svtWaitUnlock.text = "Done"
                        }
                        "0" -> {
                            ViewUtils.setVisible(svtUnlock)
                            ViewUtils.setGone(svtWaitUnlock)
                            ViewUtils.setGone(svtGrayUnlock)
                            svtUnlock.text = "GO"
                        }
                        "2" -> {
                            ViewUtils.setGone(svtWaitUnlock)
                            ViewUtils.setVisible(svtGrayUnlock)
                            ViewUtils.setGone(svtUnlock)
                            svtGrayUnlock.text = "Go"
                        }
                    }
                    tvTaskName.text = listContent[position].taskName

                    svtUnlock.setOnClickListener {
                        when (listContent[position].taskStatus) {
                            "0" -> {
                                //  ????????????
                                XPopup
                                    .Builder(this@CalendarActivity)
                                    .asCustom(
                                        BaseThreeTextPop(
                                            this@CalendarActivity,
                                            content = getString(
                                                com.cl.common_base.R.string.my_to_do,
                                                listContent[position].taskName
                                            ),
                                            oneLineText = getString(com.cl.common_base.R.string.my_go),
                                            twoLineText = getString(com.cl.common_base.R.string.my_remind_me),
                                            threeLineText = getString(com.cl.common_base.R.string.my_cancel),
                                            oneLineCLickEventAction = {
                                                // ??????taskId
                                                listContent[position].taskId?.let { taskId ->
                                                    mViewMode.setTaskId(
                                                        taskId
                                                    )
                                                }
                                                // ??????taskTime
                                                listContent[position].taskTime?.let { mViewMode.setGuideInfoTime(it) }
                                                // ??????TaskType
                                                listContent[position].taskType?.let { mViewMode.setGuideInfoStatus(it) }
                                                // todo ??????????????????????????????
                                                when (listContent[position].taskType) {
                                                    CalendarData.TASK_TYPE_CHANGE_WATER -> {
                                                        // todo ????????????????????????????????????
                                                        // ?????????????????????????????????
                                                        changWaterAddWaterAddpump()
                                                    }
                                                    CalendarData.TASK_TYPE_CHECK_CHECK_CURING -> {
                                                        // ????????????plantInfo?????????????????????????????????
                                                        mViewMode.plantInfo()
                                                    }
                                                    else -> {
                                                        listContent[position].taskType?.let { type -> mViewMode.getGuideInfo(type) }
                                                    }
                                                }
                                            },
                                            twoLineCLickEventAction = {
                                                if (PermissionHelp().hasPermissions(
                                                        this@CalendarActivity,
                                                        Manifest.permission.READ_CALENDAR,
                                                        Manifest.permission.WRITE_CALENDAR
                                                    )
                                                ) {
                                                    pop.asCustom(
                                                        BaseTimeChoosePop(
                                                            this@CalendarActivity,
                                                            currentTime = listContent[position].taskTime?.toLong(),
                                                            onConfirmAction = { time, timeMis ->
                                                                // ??????
                                                                // ???????????? & ???????????????????????????
                                                                // todo ????????????
                                                                mViewMode.updateTask(
                                                                    UpdateReq(
                                                                        taskId = listContent[position].taskId,
                                                                        taskTime = timeMis.toString()
                                                                    )
                                                                )
                                                                // todo ???????????????????????????
                                                                CalendarEventUtil.addCalendarEvent(
                                                                    this@CalendarActivity,
                                                                    listContent[position].taskName,
                                                                    listContent[position].taskName,
                                                                    timeMis, 2
                                                                )
                                                            })
                                                    ).show()
                                                    return@BaseThreeTextPop
                                                }
                                                // remind me
                                                // ??????????????????????????????
                                                pop.asCustom(BaseCenterPop(
                                                    this@CalendarActivity,
                                                    content = getString(com.cl.common_base.R.string.my_calendar_permisson),
                                                    confirmText = getString(com.cl.common_base.R.string.my_confirm),
                                                    onConfirmAction = {
                                                        // ????????????????????????????????????
                                                        // ??????????????????
                                                        PermissionHelp().applyPermissionHelp(
                                                            this@CalendarActivity,
                                                            getString(com.cl.common_base.R.string.my_calendar_permisson),
                                                            object :
                                                                PermissionHelp.OnCheckResultListener {
                                                                override fun onResult(result: Boolean) {
                                                                    if (!result) return
                                                                    // ????????????????????????
                                                                    pop.asCustom(
                                                                        BaseTimeChoosePop(
                                                                            this@CalendarActivity,
                                                                            currentTime = listContent[position].taskTime?.toLong(),
                                                                            onConfirmAction = { time, timeMis ->
                                                                                // ??????
                                                                                // ???????????? & ???????????????????????????
                                                                                // todo ????????????
                                                                                mViewMode.updateTask(
                                                                                    UpdateReq(
                                                                                        taskId = listContent[position].taskId,
                                                                                        taskTime = timeMis.toString()
                                                                                    )
                                                                                )
                                                                                // todo ???????????????????????????
                                                                                CalendarEventUtil.addCalendarEvent(
                                                                                    this@CalendarActivity,
                                                                                    listContent[position].taskName,
                                                                                    listContent[position].taskName,
                                                                                    timeMis, 2
                                                                                )
                                                                            })
                                                                    ).show()
                                                                }
                                                            },
                                                            Manifest.permission.READ_CALENDAR,
                                                            Manifest.permission.WRITE_CALENDAR,
                                                        )

                                                    }
                                                )).show()

                                            },
                                            threeLineCLickEventAction = {
                                                // ???????????????
                                            },
                                        )
                                    ).show()
                            }
                        }
                    }

                }

                override fun createCustomView(
                    leftLayout: ViewGroup,
                    rightLayout: ViewGroup,
                    holder: TimeLineStepAdapter.CustomViewHolder
                ) {
                    LayoutInflater.from(this@CalendarActivity)
                        .inflate(R.layout.my_item_custom, rightLayout, true)

                    LayoutInflater.from(this@CalendarActivity)
                        .inflate(R.layout.my_item_lef_custom, leftLayout, true)
                }

            }).setLayoutType(LayoutType.ALL)
            .setIsCustom(true)
    }

    // ???????????????
    private fun changWaterAddWaterAddpump() {
        // ??????taskId
        // ???????????????
        pop
            .isDestroyOnDismiss(false)
            .enableDrag(false)
            .maxHeight(dp2px(600f))
            .asCustom(
                HomePlantDrainPop(
                    context = this@CalendarActivity,
                    onNextAction = {
                        // ????????????
                        mViewMode.advertising()
                    },
                    onCancelAction = {

                    },
                    onTvSkipAddWaterAction = {
                        pop
                            .isDestroyOnDismiss(false)
                            .enableDrag(false)
                            .maxHeight(dp2px(600f))
                            .dismissOnTouchOutside(false)
                            .asCustom(
                                HomeSkipWaterPop(this@CalendarActivity, onConfirmAction = {
                                    mViewMode.taskId.value?.let {
                                        // ????????????
                                        mViewMode.deviceOperateStart(
                                            it,
                                            UnReadConstants.StatusManager.VALUE_STATUS_SKIP_CHANGING_WATERE
                                        )
                                        // ???????????????????????????????????????????????????????????????
                                        mViewMode.deviceOperateFinish(UnReadConstants.StatusManager.VALUE_STATUS_PUMP_WATER)

                                    }
                                })
                            ).show()
                    }
                ).setData(true)
            ).show()
    }

    /**
     * ??????????????????
     */
    override fun onTuYaToAppDataChange(status: String) {
        val map = GSON.parseObject(status, Map::class.java)
        map?.forEach { (key, value) ->
            when (key) {
                // ???????????????????????????????????????????????????????????????
                TuYaDeviceConstants.DeviceInstructions.KEY_DEVICE_WATER_STATUS_INSTRUCTIONS -> {
                    logI("KEY_DEVICE_WATER_STATUS??? $value")
                    mViewMode.setWaterVolume(value.toString())
                }

                // ????????????
                TuYaDeviceConstants.DeviceInstructions.KAY_PUMP_WATER_FINISHED_INSTRUCTION -> {
                }

                // SN???????????????
                TuYaDeviceConstants.DeviceInstructions.KEY_DEVICE_REPAIR_SN_INSTRUCTION -> {
                    logI("KEY_DEVICE_REPAIR_SN??? $value")
                }
            }
        }
    }

    // ????????????
    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }
}


