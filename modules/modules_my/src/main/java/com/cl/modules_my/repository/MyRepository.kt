package com.cl.modules_my.repository

import com.cl.common_base.BaseBean
import com.cl.common_base.bean.*
import com.cl.modules_my.request.ModifyUserDetailReq
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import javax.inject.Inject

@ActivityRetainedScoped
class MyRepository @Inject constructor(private var remoteRepository: MyRemoteRepository) {

    /**
     * 上传图片
     */
    fun uploadImg(body: List<MultipartBody.Part>): Flow<HttpResult<String>> {
        return remoteRepository.uploadImg(body)
    }

    /**
     * 更新用户信息
     */
    fun modifyUserDetail(body: ModifyUserDetailReq): Flow<HttpResult<Boolean>> {
        return remoteRepository.modifyUserDetail(body)
    }

    /**
     * 更新植物信息
     */
    fun updatePlantInfo(body: UpPlantInfoReq): Flow<HttpResult<BaseBean>> {
        return remoteRepository.updatePlantInfo(body)
    }

    /**
     * 获取植物信息
     */
    fun plantInfo(): Flow<HttpResult<PlantInfoData>> {
        return remoteRepository.plantInfo()
    }

    /**
     * 获取用户信息
     */
    fun userDetail(): Flow<HttpResult<UserinfoBean.BasicUserBean>> {
        return remoteRepository.userDetail()
    }


    /**
     * 删除设备
     */
    fun deleteDevice(): Flow<HttpResult<BaseBean>> {
        return remoteRepository.deleteDevice()
    }

    /**
     * 删除植物
     */
    fun plantDelete(uuid: String): Flow<HttpResult<Boolean>> {
        return remoteRepository.plantDelete(uuid)
    }

    /**
     * 是否种植
     */
    fun checkPlant(uuid: String): Flow<HttpResult<CheckPlantData>> {
        return remoteRepository.checkPlant(uuid)
    }

    /**
     * 更新app
     */
    fun getAppVersion(): Flow<HttpResult<AppVersionData>> {
        return remoteRepository.getAppVersion()
    }

    /**
     * 换水获取图文
     */
    fun advertising(type: String): Flow<HttpResult<MutableList<AdvertisingData>>> {
        return remoteRepository.advertising(type)
    }

    /**
     * 换水获取图文
     */
    fun getGuideInfo(type: String): Flow<HttpResult<GuideInfoData>> {
        return remoteRepository.getGuideInfo(type)
    }


    /**
     * 获取疑问信息
     */
    fun troubleShooting(): Flow<HttpResult<MyTroubleData>> {
        return remoteRepository.troubleShooting()
    }

    /**
     * 获取图文接口
     */
    fun getDetailByLearnMoreId(learnMoreId: String): Flow<HttpResult<DetailByLearnMoreIdData>> {
        return remoteRepository.getDetailByLearnMoreId(learnMoreId)
    }

    /**
     * HowTo
     */
    fun howTo(): Flow<HttpResult<MutableList<MyTroubleData.Bean>>> {
        return remoteRepository.howTo()
    }

    /**
     *  获取日历任务
     */
    fun getCalendar(startDate: String, endDate: String): Flow<HttpResult<MutableList<CalendarData>>> {
        return remoteRepository.getCalendar(startDate, endDate)
    }

    /**
     * 更新日历任务
     */
    fun updateTask(body: UpdateReq): Flow<HttpResult<String>> {
        return remoteRepository.updateTask(body)
    }

    /**
     * 解锁花期
     */
    fun unlockJourney(name: String, weight: String? = null): Flow<HttpResult<BaseBean>> {
        return remoteRepository.unlockJourney(name, weight)
    }

    /**
     * 日历-完成任务
     */
    fun finishTask(body: FinishTaskReq): Flow<HttpResult<String>> {
        return remoteRepository.finishTask(body)
    }

    /**
     * 跳过换水记录上报接口
     */
    fun deviceOperateStart(businessId: String, type: String): Flow<HttpResult<BaseBean>> {
        return remoteRepository.deviceOperateStart(businessId, type)
    }

    /**
     * 上报排水成功
     */
    fun deviceOperateFinish(type:String): Flow<HttpResult<BaseBean>> {
        return remoteRepository.deviceOperateFinish(type)
    }
}