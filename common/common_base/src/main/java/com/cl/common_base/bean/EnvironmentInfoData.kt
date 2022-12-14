package com.cl.common_base.bean

import com.cl.common_base.BaseBean

/**
 * 获取植物的环境信息
 *
 * @author 李志军 2022-08-11 18:04
 */
class EnvironmentInfoData(
    val detectionValue: String? = null,
    val healthStatus: String? = null,
    val value: String? = null,
): BaseBean() {
}