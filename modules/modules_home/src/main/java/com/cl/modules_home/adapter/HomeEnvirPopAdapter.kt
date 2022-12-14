package com.cl.modules_home.adapter

import androidx.databinding.DataBindingUtil
import com.cl.common_base.bean.EnvironmentInfoData
import com.bbgo.module_home.R
import com.bbgo.module_home.databinding.HomeEnvirItemPopBinding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * pop初始化数据
 *
 * @author 李志军 2022-08-06 18:44
 */
class HomeEnvirPopAdapter(data: MutableList<EnvironmentInfoData>?) :
    BaseQuickAdapter<EnvironmentInfoData, BaseViewHolder>(R.layout.home_envir_item_pop, data) {

    override fun onItemViewHolderCreated(viewHolder: BaseViewHolder, viewType: Int) {
        DataBindingUtil.bind<HomeEnvirItemPopBinding>(viewHolder.itemView);
    }

    override fun convert(helper: BaseViewHolder, item: EnvironmentInfoData) {
        // 获取 Binding
        val binding: HomeEnvirItemPopBinding? = helper.getBinding()
        if (binding != null) {
            // 设置数据
            binding.data = item
            binding.executePendingBindings()
        }
    }
}
