package com.cl.modules_home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cl.common_base.BaseBean
import com.cl.common_base.bean.AdvertisingData
import com.cl.common_base.bean.AppVersionData
import com.cl.common_base.bean.EnvironmentInfoData
import com.cl.common_base.bean.UnreadMessageData
import com.cl.common_base.constants.Constants
import com.cl.common_base.ext.Resource
import com.cl.common_base.ext.logD
import com.cl.common_base.ext.logI
import com.cl.common_base.util.Prefs
import com.cl.common_base.util.device.TuYaDeviceConstants
import com.cl.common_base.util.json.GSON
import com.cl.modules_home.repository.HomeRepository
import com.cl.modules_home.request.AutomaticLoginReq
import com.cl.modules_home.response.AutomaticLoginData
import com.cl.modules_home.response.GuideInfoData
import com.cl.modules_home.response.PlantInfoData
import com.tuya.smart.android.device.bean.UpgradeInfoBean
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.sdk.api.IGetOtaInfoCallback
import com.tuya.smart.sdk.bean.DeviceBean
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@ActivityRetainedScoped
class HomeViewModel @Inject constructor(private val repository: HomeRepository) : ViewModel() {
    // 账号
    val account by lazy {
        Prefs.getString(Constants.Login.KEY_LOGIN_ACCOUNT)
    }

    // 密码
    val psd by lazy {
        Prefs.getString(Constants.Login.KEY_LOGIN_PSD)
    }

    /**
     * 设备信息
     */
    val tuyaDeviceBean by lazy {
        val homeData = Prefs.getString(Constants.Tuya.KEY_DEVICE_DATA)
        GSON.parseObject(homeData, DeviceBean::class.java)
    }

    /**
     * 返回当前设备所有的dps
     * 这里面的dps都是会变化的，需要实时更新
     * 不能直接用
     */
    private val getDeviceDps by lazy {
        tuyaDeviceBean?.dps
    }

    // 水的容积。=， 多少升
    private val _getWaterVolume =
        MutableLiveData(getDeviceDps?.filter { status -> status.key == TuYaDeviceConstants.KEY_DEVICE_WATER_STATUS }
            ?.get(TuYaDeviceConstants.KEY_DEVICE_WATER_STATUS).toString())
    val getWaterVolume: LiveData<String> = _getWaterVolume
    fun setWaterVolume(volume: String) {
        _getWaterVolume.value = volume
    }

    // 是否需要修复SN
    // 需要在设备在线的情况下才展示修复
    private val _repairSN = MutableLiveData(
        if (tuyaDeviceBean?.isOnline == true) {
            getDeviceDps?.filter { status -> status.key == TuYaDeviceConstants.KEY_DEVICE_REPAIR_SN }
                ?.get(TuYaDeviceConstants.KEY_DEVICE_REPAIR_SN).toString()
        } else {
            "OK"
        }
    )
    val repairSN: LiveData<String> = _repairSN
    fun setRepairSN(sn: String) {
        _repairSN.value = sn
    }

    /**
     * refreshToken
     */
    private val _refreshToken = MutableLiveData<Resource<AutomaticLoginData>>()
    val refreshToken: LiveData<Resource<AutomaticLoginData>> = _refreshToken
    fun refreshToken(req: AutomaticLoginReq) {
        viewModelScope.launch {
            repository.automaticLogin(req)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _refreshToken.value = it
                }
        }
    }


    /**
     * 获取图文引导
     *
     * 引导类型:0-种植、1-开始种植、2-开始花期、3-开始清洗期、5-开始烘干期、6-完成种植
     */
    private val _getGuideInfo = MutableLiveData<Resource<GuideInfoData>>()
    val getGuideInfo: LiveData<Resource<GuideInfoData>> = _getGuideInfo
    fun getGuideInfo(req: String) {
        viewModelScope.launch {
            repository.getGuideInfo(req)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _getGuideInfo.value = it
                }
        }
    }


    /**
     * 上报引导
     */
    private val _saveOrUpdate = MutableLiveData<Resource<BaseBean>>()
    val saveOrUpdate: LiveData<Resource<BaseBean>> = _saveOrUpdate
    fun saveOrUpdate(req: String) {
        viewModelScope.launch {
            repository.saveOrUpdate(req)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _saveOrUpdate.value = it
                }
        }
    }

    /**
     * 开始种植植物
     */
    private val _startRunning = MutableLiveData<Resource<Boolean>>()
    val startRunning: LiveData<Resource<Boolean>> = _startRunning
    fun startRunning(botanyId: String?, goon: Boolean) {
        viewModelScope.launch {
            repository.startRunning(botanyId, goon)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _startRunning.value = it
                }
        }
    }


    /**
     * 获取植物基本信息
     */
    private val _plantInfo = MutableLiveData<Resource<PlantInfoData>>()
    val plantInfo: LiveData<Resource<PlantInfoData>> = _plantInfo
    fun plantInfo() {
        viewModelScope.launch {
            repository.plantInfo()
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _plantInfo.value = it
                }
        }
    }

    /**
     * 获取图文广告
     */
    private val _advertising = MutableLiveData<Resource<MutableList<AdvertisingData>>>()
    val advertising: LiveData<Resource<MutableList<AdvertisingData>>> = _advertising
    fun advertising(type: String? = "0") {
        viewModelScope.launch {
            repository.advertising(type ?: "0")
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _advertising.value = it
                }
        }
    }


    /**
     * 获取植物的环境信息
     */
    private val _environmentInfo = MutableLiveData<Resource<MutableList<EnvironmentInfoData>>>()
    val environmentInfo: LiveData<Resource<MutableList<EnvironmentInfoData>>> = _environmentInfo
    fun environmentInfo(type: String) {
        viewModelScope.launch {
            repository.environmentInfo(type)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _environmentInfo.value = it
                }
        }
    }

    /**
     * 获取未读消息
     */
    private val _getUnread = MutableLiveData<Resource<MutableList<UnreadMessageData>>>()
    val getUnread: LiveData<Resource<MutableList<UnreadMessageData>>> = _getUnread
    fun getUnread() {
        viewModelScope.launch {
            repository.getUnread()
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _getUnread.value = it
                }
        }
    }

    /**
     * 标记已读消息
     */
    private val _getRead = MutableLiveData<Resource<BaseBean>>()
    val getRead: LiveData<Resource<BaseBean>> = _getRead
    fun getRead(messageId: String) {
        viewModelScope.launch {
            repository.getRead(messageId)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        // 删除第一条信息
                        removeFirstUnreadMessage()
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _getRead.value = it
                }
        }
    }

    /**
     * 解锁花期
     */
    private val _unlockJourney = MutableLiveData<Resource<BaseBean>>()
    val unlockJourney: LiveData<Resource<BaseBean>> = _unlockJourney
    fun unlockJourney(name: String) {
        viewModelScope.launch {
            repository.unlockJourney(name)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        // 删除第一条信息
                        removeFirstUnreadMessage()
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _unlockJourney.value = it
                }
        }
    }


    /**
     * 检查app版本
     */
    private val _getAppVersion = MutableLiveData<Resource<AppVersionData>>()
    val getAppVersion: LiveData<Resource<AppVersionData>> = _getAppVersion
    fun getAppVersion() {
        viewModelScope.launch {
            repository.getAppVersion()
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _getAppVersion.value = it
                }
        }
    }

    /**
     * 设备操作结束
     */
    private val _deviceOperateFinish = MutableLiveData<Resource<BaseBean>>()
    val deviceOperateFinish: LiveData<Resource<BaseBean>> = _deviceOperateFinish
    fun deviceOperateFinish(type: String) {
        viewModelScope.launch {
            repository.deviceOperateFinish(type)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _deviceOperateFinish.value = it
                }
        }
    }

    /**
     * 设备操作开始
     */
    private val _deviceOperateStart = MutableLiveData<Resource<BaseBean>>()
    val deviceOperateStart: LiveData<Resource<BaseBean>> = _deviceOperateStart
    fun deviceOperateStart(business: String, type: String) {
        viewModelScope.launch {
            repository.deviceOperateStart(business, type)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _deviceOperateStart.value = it
                }
        }
    }

    /**
     * 用户操作到哪一步了，上报 continues1 continues2 continues3
     * 这个是用来当用户取消弹窗时上报的。
     */
    private val _userMessageFlag = MutableLiveData<Resource<BaseBean>>()
    val userMessageFlag: LiveData<Resource<BaseBean>> = _userMessageFlag
    fun userMessageFlag(flagId: String, messageId: String) {
        viewModelScope.launch {
            repository.userMessageFlag(flagId, messageId)
                .map {
                    if (it.code != Constants.APP_SUCCESS) {
                        Resource.DataError(
                            it.code,
                            it.msg
                        )
                    } else {
                        Resource.Success(it.data)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    emit(Resource.Loading())
                }
                .catch {
                    logD("catch $it")
                    emit(
                        Resource.DataError(
                            -1,
                            "$it"
                        )
                    )
                }.collectLatest {
                    _userMessageFlag.value = it
                }
        }
    }


    /**
     * 设置当前获取图文引导的状态，默认为s0
     */
    private val _currentReqStatus = MutableLiveData(0)
    val currentReqStatus: LiveData<Int> = _currentReqStatus
    fun setCurrentReqStatus(state: Int) {
        _currentReqStatus.value = state
    }

    /**
     * 当前获取引导图文详情到状态，默认为0
     *      * type	引导类型:0-种植、1-开始种植、2-开始花期、3-开始清洗期、5-开始烘干期、6-完成种植
     */
    private val _typeStatus = MutableLiveData<Int>()
    val typeStatus: LiveData<Int> = _typeStatus
    fun setTypeStatus(status: Int) {
        _typeStatus.value = status
    }

    /**
     * 当前周期信息
     */
    private val _periodData = MutableLiveData<MutableList<PlantInfoData.InfoList>>()
    val periodData: LiveData<MutableList<PlantInfoData.InfoList>> = _periodData
    fun setPeriodList(data: MutableList<PlantInfoData.InfoList>) {
        _periodData.value = data
    }

    /**
     * 未读消息列表
     */
    private val _unreadMessageList = MutableLiveData<MutableList<UnreadMessageData>>()
    val unreadMessageList: LiveData<MutableList<UnreadMessageData>> = _unreadMessageList
    fun setUnreadMessageList(list: MutableList<UnreadMessageData>) {
        _unreadMessageList.value = list
    }

    // 删除当前第一条消息
    fun removeFirstUnreadMessage() {
        kotlin.runCatching {
            if (_unreadMessageList.value.isNullOrEmpty()) return
            _unreadMessageList.value?.let { unreadMessage ->
                if (unreadMessage.size == 0) return
                unreadMessage.removeFirst()
            }
        }
    }

    // 固件信息
    // 获取当前设备信息
    private val tuYaDeviceBean by lazy {
        val homeData = Prefs.getString(Constants.Tuya.KEY_DEVICE_DATA)
        GSON.parseObject(homeData, DeviceBean::class.java)
    }

    private val tuYaHomeSdk by lazy {
        TuyaHomeSdk.newOTAInstance(tuYaDeviceBean?.devId)
    }

    /**
     * 查询固件升级信息
     */
    fun checkFirmwareUpdateInfo(
        onOtaInfo: ((upgradeInfoBeans: MutableList<UpgradeInfoBean>?, isShow: Boolean) -> Unit)? = null,
    ) {
        tuYaHomeSdk.getOtaInfo(object : IGetOtaInfoCallback {
            override fun onSuccess(upgradeInfoBeans: MutableList<UpgradeInfoBean>?) {
                logI("getOtaInfo:  ${GSON.toJson(upgradeInfoBeans?.first { it.type == 9 })}")
                // 如果可以升级
                if (hasHardwareUpdate(upgradeInfoBeans)) {
                    onOtaInfo?.invoke(upgradeInfoBeans, true)
                } else {
                    // 如果不可以升级过
                    onOtaInfo?.invoke(upgradeInfoBeans, false)
                }
            }

            override fun onFailure(code: String?, error: String?) {
                logI(
                    """
                        getOtaInfo:
                        code: $code
                        error: $error
                    """.trimIndent()
                )
            }
        })
    }

    /**
     * 检查固件是否可以升级
     */
    private fun hasHardwareUpdate(list: MutableList<UpgradeInfoBean>?): Boolean {
        if (null == list || list.size == 0) return false
        return list.first { it.type == 9 }.upgradeStatus == 1
    }

    /**
     * 继承之后选择的状态
     */
}