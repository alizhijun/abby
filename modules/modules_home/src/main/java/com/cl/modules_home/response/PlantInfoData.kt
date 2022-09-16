package com.cl.modules_home.response

import com.cl.common_base.BaseBean

/**
 * 获取植物基本信息
 *
 * @author 李志军 2022-08-08 16:23
 */
data class PlantInfoData(
    var attribute: String? = null, // 种植属性(Photo、Auto)
    var strainName: String? = null, // 种植名字
    var plantWay: String? = null, // 种植方式
    var day: Int? = null,
    var flushingWeight: Int? = null, // 称重重量
    var healthStatus: String? = null,
    var name: String? = null,
    var heigh: Int? = null,
    var id: Int? = null,
    var oxygen: Int? = null,
    var plantStatus: Int? = null,
    var week: Int? = null,
    var list: MutableList<InfoList>? = null
) : BaseBean() {

    data class InfoList(
        var day: String? = null,
        var journeyName: String? = null,
        var guideId: Int? = null,
        var journeyStatus: Int? = null,
        var week: String? = null,
    ) : com.joketng.timelinestepview.bean.BaseBean()
}