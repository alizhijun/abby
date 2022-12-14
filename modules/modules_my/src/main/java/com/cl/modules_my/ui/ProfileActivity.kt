package com.cl.modules_my.ui

import android.Manifest
import android.app.PictureInPictureUiState
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.cl.common_base.base.BaseActivity
import com.cl.common_base.bean.UserinfoBean
import com.cl.common_base.constants.Constants
import com.cl.common_base.constants.RouterPath
import com.cl.common_base.ext.logE
import com.cl.common_base.ext.logI
import com.cl.common_base.ext.resourceObserver
import com.cl.common_base.help.PermissionHelp
import com.cl.common_base.report.Reporter
import com.cl.common_base.util.Prefs
import com.cl.common_base.util.file.FileUtil
import com.cl.common_base.util.file.SDCard
import com.cl.common_base.util.glide.GlideEngine
import com.cl.common_base.util.json.GSON
import com.cl.common_base.util.mesanbox.MeSandboxFileEngine
import com.cl.common_base.widget.toast.ToastUtil
import com.cl.modules_my.R
import com.cl.modules_my.databinding.MyProfileActivityBinding
import com.cl.modules_my.request.ModifyUserDetailReq
import com.cl.modules_my.viewmodel.ProfileViewModel
import com.cl.modules_my.widget.ChooserOptionPop
import com.cl.modules_my.widget.LoginOutPop
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.language.LanguageConfig
import com.luck.picture.lib.style.BottomNavBarStyle
import com.luck.picture.lib.style.PictureSelectorStyle
import com.luck.picture.lib.style.SelectMainStyle
import com.luck.picture.lib.utils.MediaUtils
import com.luck.picture.lib.utils.PictureFileUtils
import com.lxj.xpopup.XPopup
import com.permissionx.guolindev.PermissionX
import com.tuya.smart.android.user.api.ILogoutCallback
import com.tuya.smart.home.sdk.TuyaHomeSdk
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject


/**
 * ??????????????????
 */
@AndroidEntryPoint
class ProfileActivity : BaseActivity<MyProfileActivityBinding>() {

    @Inject
    lateinit var mViewModel: ProfileViewModel

    // ????????????
    private val userInfo by lazy {
        val bean = Prefs.getString(Constants.Login.KEY_LOGIN_DATA)
        val parseObject = GSON.parseObject(bean, UserinfoBean::class.java)
        parseObject
    }

    /**
     * ??????????????????
     */
    private val confirm by lazy {
        XPopup.Builder(this@ProfileActivity)
            .isDestroyOnDismiss(false)
            .dismissOnTouchOutside(false)
            .asCustom(LoginOutPop(this) {
                TuyaHomeSdk.getUserInstance().logout(object : ILogoutCallback {
                    override fun onSuccess() {
                        // ??????????????????
                        Prefs.removeKey(Constants.Login.KEY_LOGIN_DATA_TOKEN)
                        // ?????????????????????Activity
                        // ?????????Login??????
                        ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN)
                            .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            .navigation()
                    }

                    override fun onError(code: String?, error: String?) {
                        logE(
                            """
                           logout -> onError:
                            code: $code
                            error: $error
                        """.trimIndent()
                        )
                        Reporter.reportTuYaError("getUserInstance", error, code)
                    }
                })
            })
    }

    private val modifyUserDetailReq by lazy {
        ModifyUserDetailReq()
    }

    /**
     * ????????????????????????
     */
    private val chooserOptionPop by lazy {
        XPopup.Builder(this@ProfileActivity)
            .hasStatusBar(true)
            .isDestroyOnDismiss(false)
            .asCustom(
                ChooserOptionPop(
                    context = this@ProfileActivity,
                    onPhotoAction = {
                        PermissionHelp().applyPermissionHelp(
                            this@ProfileActivity,
                            getString(com.cl.common_base.R.string.profile_request_camera),
                            object : PermissionHelp.OnCheckResultListener{
                                override fun onResult(result: Boolean) {
                                    if (!result) return
                                    //???????????????????????????
                                    gotoCamera()
                                }
                            },
                            Manifest.permission.CAMERA
                        )
                    },
                    onLibraryAction = {
                        // ????????????
                        // ??????????????????????????????
                        val style = PictureSelectorStyle()
                        val ss = BottomNavBarStyle()
                        ss.isCompleteCountTips = false
                        style.bottomBarStyle = ss
                        PictureSelector.create(this@ProfileActivity)
                            .openGallery(SelectMimeType.ofImage())
                            .setImageEngine(GlideEngine.createGlideEngine())
//                            .setCompressEngine(ImageFileCompressEngine()) //????????????
                            .setSandboxFileEngine(MeSandboxFileEngine()) // Android10 ????????????
                            .isOriginalControl(false)// ????????????
                            .isDisplayTimeAxis(true)// ?????????
                            .setEditMediaInterceptListener(null)// ??????????????????????????????
                            .isMaxSelectEnabledMask(true) // ??????????????????
                            .isDisplayCamera(false)//??????????????????
                            .setLanguage(LanguageConfig.ENGLISH) //????????????
                            .setMaxSelectNum(1)
                            .setSelectorUIStyle(style)
                            .forResult(PictureConfig.CHOOSE_REQUEST)
                    })
            )
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        // ??????nikeName
        binding.ftNickName.setItemValueWithColor(userInfo?.nickName, "#000000")
        // ??????abbyID
        binding.ftId.itemValue = userInfo?.abbyId
        binding.ftId.setHideArrow(true)
        // ????????????
        // ????????????????????????
        val headUrl = userInfo?.avatarPicture ?: userInfo?.userDetailData?.avatarPicture
        if (headUrl.isNullOrEmpty()) {
            binding.ftHead.setTvItemImage(userInfo?.nickName?.substring(0, 1))
        } else {
            binding.ftHead.setImageForUrl(headUrl, true)
        }
        // ????????????
        binding.ftHead.setItemTitle(getString(com.cl.common_base.R.string.profile_photo), true)
        binding.ftNickName.setItemTitle(getString(com.cl.common_base.R.string.profile_name), true)
        binding.ftId.setItemTitle(getString(com.cl.common_base.R.string.profile_abby_id), true)
    }

    override fun observe() {
        mViewModel.apply {
            // ????????????????????????
            uploadImg.observe(this@ProfileActivity, resourceObserver {
                success {
                    hideProgressLoading()
                    binding.ftHead.setImageForUrl(data, true)

                    // ??????????????????
                    // ?????????????????????????????????
                    if (data.isNullOrEmpty()) return@success
                    data?.let {
                        val oneArray = it.split("com/")
                        if (oneArray.isNotEmpty()) {
                            if (oneArray.isNotEmpty()) {
                                val result = oneArray[1].split("?")
                                if (result.isNotEmpty()) {
                                    logI(result[0])
                                    // ??????????????????
                                    modifyUserDetailReq.avatarPicture = result[0]
                                    mViewModel.modifyUserDetail(modifyUserDetailReq)
                                }
                            }
                        }
                    }
                }
                error { msg, code ->
                    hideProgressLoading()
                    msg?.let { it1 -> ToastUtil.shortShow(it1) }
                }
                loading {
                    showProgressLoading()
                }
            })

            /**
             * ??????????????????
             */
            modifyUserDetail.observe(this@ProfileActivity, resourceObserver {
                success {
                    hideProgressLoading()
                }
                error { errorMsg, code ->
                    hideProgressLoading()
                    errorMsg?.let { it1 -> ToastUtil.shortShow(it1) }
                }
                loading {
                    hideProgressLoading()
                }
            })

            /**
             * ????????????????????????
             */
            userDetail.observe(this@ProfileActivity, resourceObserver {
                success {
                    hideProgressLoading()
                    // ??????nikeName
                    binding.ftNickName.setItemValueWithColor(data?.nickName, "#000000")
                    // ????????????
                    val headUrl = data?.avatarPicture ?: ""
                    if (headUrl.isNullOrEmpty()) {
                        binding.ftHead.setTvItemImage(userInfo?.nickName?.substring(0, 1))
                    } else {
                        binding.ftHead.setImageForUrl(headUrl, true)
                    }
                    // ??????abbyID
                    binding.ftId.itemValue = data?.abbyId

                    // ????????????
                    userInfo?.userDetailData = data
                }
                loading { }
                error { errorMsg, code ->
                    hideProgressLoading()
                    errorMsg?.let { it1 -> ToastUtil.shortShow(it1) }
                }
            })

        }
    }

    override fun initData() {
        // ??????
        binding.tvLoginOut.setOnClickListener {
            confirm.show()
        }

        binding.ftHead.setOnClickListener {
            chooserOptionPop.show()
        }

        binding.ftNickName.setOnClickListener {
            val intent = Intent(this@ProfileActivity, EditNickNameActivity::class.java)
            intent.putExtra(KEY_NICK_NAME, binding.ftNickName.itemValue)
            startActivity(intent)
        }
    }

    /**
     * ????????????
     */
    private var imageUri: Uri? = null
    private fun gotoCamera() {
        imageUri = createImageUri()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_CAPTURE)
    }

    /**
     * ??????????????????uri,?????????????????????????????? Android 10????????????????????????
     */
    private fun createImageUri(): Uri? {
        //Android 10??????
        val photoUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = Environment.getExternalStorageState()
            // ???????????????SD???,????????????SD?????????,?????????SD????????????????????????
            if (status == Environment.MEDIA_MOUNTED) {
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues()
                )
            } else {
                contentResolver.insert(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    ContentValues()
                )
            }
        } else {
            val tempFile: File = FileUtil.createFileIfNotExists(
                SDCard.getContextPictureDir(this@ProfileActivity)
                    .toString() + File.separator + System.currentTimeMillis() + ".jpg"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //??????Android 7.0?????????????????????FileProvider????????????content?????????Uri
                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider", tempFile
                )
            } else {
                Uri.fromFile(tempFile)
            }
        }
        return photoUri
    }


    /**
     * ????????????
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CAPTURE -> {
                if (resultCode == RESULT_OK && imageUri != null) {
                    gotoClipActivity(imageUri)
                }
            }

            REQUEST_CROP_PHOTO -> {
                // ???????????????URi
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    uri?.let {
                        val cropImagePath = getRealFilePathFromUri(applicationContext, uri)
                        // ??????????????????
                        // ???????????????head-?????????trend-?????????
                        mViewModel.uploadImg(upLoadImage(cropImagePath ?: ""))
                    }
                    // ???????????????
                }
            }

            PictureConfig.CHOOSE_REQUEST -> {
                val result = PictureSelector.obtainSelectorList(data)
                if (result.isNullOrEmpty()) return
                analyticalSelectResults(result)
            }
        }
    }


    /**
     * ????????????????????????
     */
    private fun analyticalSelectResults(result: ArrayList<LocalMedia>) {
        for (media in result) {
            if (media.width == 0 || media.height == 0) {
                // ???????????????
                if (PictureMimeType.isHasImage(media.mimeType)) {
                    val imageExtraInfo = MediaUtils.getImageSize(this, media.path)
                    media.width = imageExtraInfo.width
                    media.height = imageExtraInfo.height
                } else if (PictureMimeType.isHasVideo(media.mimeType)) {
                    val videoExtraInfo = MediaUtils.getVideoSize(this, media.path)
                    media.width = videoExtraInfo.width
                    media.height = videoExtraInfo.height
                }
            }
            logI(
                """
                "?????????: " + ${media.fileName}
                "????????????:" + ${media.isCompressed}
                "??????:" + ${media.compressPath}
                "????????????:" + ${media.path}
                "????????????:" + ${media.realPath}
                "????????????:" + ${media.isCut}
                "????????????:" + ${media.cutPath}
                "??????????????????:" + ${media.isOriginal}
                "????????????:" + ${media.originalPath}
                "????????????:" + ${media.sandboxPath}
                "????????????:" + ${media.watermarkPath}
                "???????????????:" + ${media.videoThumbnailPath}
                "????????????: " + ${media.width} + "x" + ${media.height}
                "????????????: " + ${media.cropImageWidth} + "x" + ${media.cropImageHeight}
                "????????????: " + ${PictureFileUtils.formatAccurateUnitFileSize(media.size)}
                "????????????: " + ${media.duration}
                "????????????: " + ${media.availablePath}
            """.trimIndent()
            )
        }
        runOnUiThread {
            // ????????????
            val media = result[0]
            val path = media.availablePath
            if (PictureMimeType.isContent(path)) {
                // ?????????????????????
                gotoClipActivity(
                    Uri.parse(
                        path
                    )
                )
            } else {
                // ?????????????????????
                gotoClipActivity(
                    Uri.parse(
                        media.path
                    )
                )
            }

        }
    }

    /**
     * ??????uri??????????????????
     */
    private fun getRealFilePathFromUri(context: Context, uri: Uri?): String? {
        if (null == uri) {
            return null
        }
        val scheme = uri.scheme
        var data: String? = null
        if (scheme == null) {
            data = uri.path
        } else if (ContentResolver.SCHEME_FILE == scheme) {
            data = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.DATA),
                null,
                null,
                null
            )
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    if (index > -1) {
                        data = cursor.getString(index)
                    }
                }
                cursor.close()
            }
        }
        return data
    }

    private fun gotoClipActivity(uri: Uri?) {
        if (uri == null) {
            return
        }
        val intent = Intent(this, ClipImageActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("type", 1)
        bundle.putString("uri", uri.toString())
        intent.putExtras(bundle)
        startActivityForResult(intent, REQUEST_CROP_PHOTO)
    }

    /**
     * ????????????
     */
    private fun upLoadImage(path: String): List<MultipartBody.Part> {
        //1.??????MultipartBody.Builder??????
        val builder = MultipartBody.Builder()
            //????????????
            .setType(MultipartBody.FORM)

        //2.??????????????????????????????
        val file: File = File(path)

        //?????????x???
        //????????????
        val body: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)

        //3.??????MultipartBody.Builder???addFormDataPart()????????????????????????
        /**
         * ps:builder.addFormDataPart("code","123456");
         * ps:builder.addFormDataPart("file",file.getName(),body);
         */
        builder.addFormDataPart("imgType", "head") //????????????????????????key????????????value???
        builder.addFormDataPart("file", file.name, body) //?????????????????????body??????????????????

        //4.??????List<MultipartBody.Part> ?????????
        //  ??????MultipartBody.Builder???build()?????????????????????????????????MultipartBody
        //  ?????????MultipartBody???parts()????????????MultipartBody.Part??????
        return builder.build().parts
    }

    override fun onResume() {
        super.onResume()
        // ??????????????????
        mViewModel.userDetail()
    }

    override fun onDestroy() {
        super.onDestroy()
        // ????????????
        GSON.toJson(userInfo)
            ?.let {
                logI("refreshData: $it")
                Prefs.putStringAsync(Constants.Login.KEY_LOGIN_DATA, it)
            }
    }

    companion object {
        // ????????????
        private const val REQUEST_CAPTURE = 100

        //????????????
        private const val REQUEST_PICK = 101

        // ??????????????????
        private const val REQUEST_CROP_PHOTO = 102

        // ??????nickName
        const val KEY_NICK_NAME = "key_nick_name"
    }

}