package com.cl.common_base.easeui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.cl.common_base.BaseApplication;
import com.cl.common_base.BuildConfig;
import com.cl.common_base.R;
import com.cl.common_base.bean.UserinfoBean;
import com.cl.common_base.constants.Constants;
import com.cl.common_base.easeui.receiver.CallReceiver;
import com.cl.common_base.easeui.ui.EaseUiActivity;
import com.cl.common_base.util.Prefs;
import com.cl.common_base.util.json.GSON;
import com.cl.common_base.util.livedatabus.LiveEventBus;
import com.hyphenate.chat.AgoraMessage;
import com.hyphenate.chat.ChatClient;
import com.hyphenate.chat.ChatManager;
import com.hyphenate.chat.Conversation;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.Message;
import com.hyphenate.chat.OfficialAccount;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.helpdesk.easeui.Notifier;
import com.hyphenate.helpdesk.easeui.UIProvider;
import com.hyphenate.helpdesk.easeui.util.CommonUtils;
import com.hyphenate.helpdesk.easeui.util.Config;
import com.hyphenate.helpdesk.model.AgentInfo;
import com.hyphenate.helpdesk.model.MessageHelper;

import org.json.JSONObject;

import java.util.List;

public class EaseUiHelper {

    private static final String TAG = "DemoHelper";

    public static EaseUiHelper instance = new EaseUiHelper();

    /**
     * kefuChat.MessageListener
     */
    protected ChatManager.MessageListener messageListener = null;

    /**
     * ChatClient.ConnectionListener
     */
    private ChatClient.ConnectionListener connectionListener;

    private UIProvider _uiProvider;

    private CallReceiver callReceiver;
    private Context appContext;

    public static int sNavHeight;

    private EaseUiHelper() {
    }

    public synchronized static EaseUiHelper getInstance() {
        return instance;
    }

    /**
     * init helper
     *
     * @param context application context
     */
    public void init(final Context context) {
        appContext = context;

        // 客服Options
//        String string = Prefs.INSTANCE.getString(Constants.Login.KEY_LOGIN_DATA, "");
//        UserinfoBean userinfoBean = GSON.parseObject(string, UserinfoBean.class);
        ChatClient.Options options = new ChatClient.Options();
        options.setAppkey(Constants.EaseUi.DEFAULT_CUSTOMER_APPKEY);
        options.setTenantId(Constants.EaseUi.DEFAULT_TENANTID); // 租户ID
        //  options.setConfigId(Preferences.getInstance().getConfigId()); //
        options.showAgentInputState().showVisitorWaitCount().showMessagePredict();

        // 你需要设置自己申请的账号来使用三方推送功能，详见集成文档
//        EMPushConfig.Builder builder = new EMPushConfig.Builder(context);
//        builder.enableVivoPush() // 需要在AndroidManifest.xml中配置appId和appKey
//                .enableMeiZuPush("119943", "91163267c8784687804af6dd8e8fcf37")
//                .enableMiPush("2882303761517507836", "5631750729836")
//                .enableOppoPush("b08eb4a4b43f49799f45d136a5e2eabe", "52d5f8b887c14987bd306f6ffcd33044")
//                .enableHWPush() // 需要在AndroidManifest.xml中配置appId
//                .enableFCM("570662061026");

//        options.setPushConfig(builder.build());
        // TODO 沙箱测试，只为测试
        // options.setKefuRestServer("https://helps.live");
        // options.setKefuRestServer("https://sandbox.kefu.easemob.com");

        //设为调试模式，打成正式包时，最好设为false，以免消耗额外的资源
        options.setConsoleLog(BuildConfig.DEBUG);
//	    options.setUse2channel(true);
//        options.setAutoLogin(false);

        options.setAppVersion("1.3.2.9");
        // 环信客服 SDK 初始化, 初始化成功后再调用环信下面的内容
        /*if (ChatClient.getInstance().init(context, options)){
            _uiProvider = UIProvider.getInstance();
            //初始化EaseUI
            _uiProvider.init(context);
            //调用easeui的api设置providers
            setEaseUIProvider(context);
            //设置全局监听
            setGlobalListeners();
        }*/

        // IM EMOptions
        com.hyphenate.chat.EMOptions emoptions = new com.hyphenate.chat.EMOptions();
        if (ChatClient.getInstance().init(context, options, emoptions)) {
            _uiProvider = UIProvider.getInstance();
            //初始化EaseUI
            _uiProvider.init(context);
            //调用easeui的api设置providers
            setEaseUIProvider(context);
            //设置全局监听
            setGlobalListeners();
        }

    }


    private void setEaseUIProvider(final Context context) {
        //设置头像和昵称 某些控件可能没有头像和昵称，需要注意
        UIProvider.getInstance().setUserProfileProvider(new UIProvider.UserProfileProvider() {
            @Override
            public void setNickAndAvatar(Context context, Message message, ImageView userAvatarView, TextView usernickView) {
                if (message.direct() == Message.Direct.RECEIVE) {
                    //设置客服的昵称和头像
                    AgentInfo agentInfo = MessageHelper.getAgentInfo(message);
                    OfficialAccount officialAccount = message.getOfficialAccount();
                    if (usernickView != null) {
                        usernickView.setText(message.from());
                        if (agentInfo != null) {
                            if (!TextUtils.isEmpty(agentInfo.getNickname())) {
                                usernickView.setText(agentInfo.getNickname());
                            }
                        }
                    }
                    if (userAvatarView != null) {
                        // 默认发送方都是为这一个头像
                        userAvatarView.setImageResource(R.mipmap.head);

                        // 如果设置了个人头像则优先于企业logo显示
//                        if (officialAccount != null) {
//                            if (!TextUtils.isEmpty(officialAccount.getImg())) {
//                                String imgUrl = officialAccount.getImg();
//                                // 设置客服头像
//                                if (!TextUtils.isEmpty(imgUrl)) {
//                                    if (!imgUrl.startsWith("http")) {
//                                        imgUrl = "http:" + imgUrl;
//                                    }
//                                    //正常的string路径
//                                    // Glide.with(context).load(imgUrl).apply(RequestOptions.placeholderOf(com.hyphenate.helpdesk.R.drawable.hd_default_avatar).diskCacheStrategy(DiskCacheStrategy.ALL)).into(userAvatarView);
//                                    Glide.with(context).load(imgUrl).apply(RequestOptions.placeholderOf(R.mipmap.head).diskCacheStrategy(DiskCacheStrategy.ALL)).into(userAvatarView);
//                                }
//                            }
//                        }
//
//                        if (agentInfo != null) {
//                            if (!TextUtils.isEmpty(agentInfo.getAvatar())) {
//                                String strUrl = agentInfo.getAvatar();
//                                // 设置客服头像
//                                if (!TextUtils.isEmpty(strUrl)) {
//                                    if (!strUrl.startsWith("http")) {
//                                        strUrl = "http:" + strUrl;
//                                    }
//                                    //正常的string路径
//                                    Glide.with(context).load(strUrl).apply(RequestOptions.placeholderOf(R.mipmap.head).diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop()).into(userAvatarView);
//                                    return;
//                                }
//                            }
//                        }
                    }
                } else {
                    // 这是发送消息方的头像设置
                    //此处设置当前登录用户的头像，
                    if (userAvatarView != null) {
                        // 默认显示用户头像
                        String string = Prefs.INSTANCE.getString(Constants.Login.KEY_USER_INFO, "");
                        UserinfoBean.BasicUserBean basicUserBean = GSON.parseObject(string, UserinfoBean.BasicUserBean.class);
                        Glide.with(context).load(basicUserBean.getAvatarPicture()).apply(RequestOptions.placeholderOf(R.mipmap.head).diskCacheStrategy(DiskCacheStrategy.ALL).bitmapTransform(new CircleCrop())).into(userAvatarView);
                    }
                }
            }
        });


        //设置通知栏样式
        _uiProvider.getNotifier().setNotificationInfoProvider(new Notifier.NotificationInfoProvider() {
            @Override
            public String getTitle(Message message) {
                //修改标题,这里使用默认
                return null;
            }

            @Override
            public int getSmallIcon(Message message) {
                //设置小图标，这里为默认
                return 0;
            }

            @Override
            public String getDisplayedText(Message message) {
                // 设置状态栏的消息提示，可以根据message的类型做相应提示
                String ticker = CommonUtils.getMessageDigest(message, context);
                if (message.getType() == Message.Type.TXT) {
                    ticker = ticker.replaceAll("\\[.{2,3}\\]", context.getString(R.string.noti_text_expression));
                }
                return message.from() + ": " + ticker;
            }

            @Override
            public String getLatestText(Message message, int fromUsersNum, int messageNum) {
                return null;
                // return fromUsersNum + "contacts send " + messageNum + "messages to you";
            }

            @Override
            public Intent getLaunchIntent(Message message) {
                Intent intent;
                try {
                    String type = message.getStringAttribute("type");
                    if ("agorartcmedia/video".equalsIgnoreCase(type)) {
                        return null;
                    }
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }

                //设置点击通知栏跳转事件
                Conversation conversation = ChatClient.getInstance().chatManager().getConversation(message.from());
                String titleName = null;
                if (conversation.officialAccount() != null) {
                    titleName = conversation.officialAccount().getName();
                }
//                intent = new IntentBuilder(context)
//                        .setTargetClass(ChatActivity.class)
//                        .setServiceIMNumber(conversation.conversationId())
//                        .setVisitorInfo(DemoMessageHelper.createVisitorInfo())
//                        .setTitleName(titleName)
//                        .setShowUserNick(true)
//                        .build();
//                return intent;
                return null;
            }
        });

        //不设置,则使用默认, 声音和震动设置
//        _uiProvider.setSettingsProvider(new UIProvider.SettingsProvider() {
//            @Override
//            public boolean isMsgNotifyAllowed(Message message) {
//                return false;
//            }
//
//            @Override
//            public boolean isMsgSoundAllowed(Message message) {
//                return false;
//            }
//
//            @Override
//            public boolean isMsgVibrateAllowed(Message message) {
//                return false;
//            }
//
//            @Override
//            public boolean isSpeakerOpened() {
//                return false;
//            }
//        });
//        ChatClient.getInstance().getChat().addMessageListener(new MessageListener() {
//            @Override
//            public void onMessage(List<Message> msgs) {
//
//            }
//
//            @Override
//            public void onCmdMessage(List<Message> msgs) {
//
//            }
//
//            @Override
//            public void onMessageSent() {
//
//            }
//
//            @Override
//            public void onMessageStatusUpdate() {
//
//            }
//        });
    }


    private void setGlobalListeners() {
        // create the global connection listener
        /*connectionListener = new ChatClient.ConnectionListener(){

            @Override
            public void onConnected() {
                //onConnected
            }

            @Override
            public void onDisconnected(int errorcode) {
                if (errorcode == Error.USER_REMOVED){
                    //账号被移除
                }else if (errorcode == Error.USER_LOGIN_ANOTHER_DEVICE){
                    //账号在其他地方登陆
                }
            }
        };

        //注册连接监听
        ChatClient.getInstance().addConnectionListener(connectionListener);*/

        //注册消息事件监听
        registerEventListener();

        IntentFilter callFilter = new IntentFilter(ChatClient.getInstance().callManager().getIncomingCallBroadcastAction());
        callFilter.addAction("calling.state");
        if (callReceiver == null) {
            callReceiver = new CallReceiver();
        }
        // register incoming call receiver
        appContext.registerReceiver(callReceiver, callFilter);
    }

    /**
     * 全局事件监听
     * 因为可能会有UI页面先处理到这个消息，所以一般如果UI页面已经处理，这里就不需要再次处理
     * activityList.size() <= 0 意味着所有页面都已经在后台运行，或者已经离开Activity Stack
     */
    protected void registerEventListener() {
        messageListener = new ChatManager.MessageListener() {
            /**
             * 后台消息，关闭聊天界面之后也会在这接收到
             */
            @Override
            public void onMessage(List<Message> msgs) {
                for (Message message : msgs) {
                    Log.d(TAG, "onMessageReceived id : " + message.messageId());
                    try {
                        Log.d(TAG, "onMessageReceived id : " + message.getStringAttribute("weichat"));
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                    // 有消息推送过来了。
                    // 直接下发状态-MainActivity
                    LiveEventBus.get()
                            .with(Constants.Global.KEY_MAIN_SHOW_BUBBLE, Boolean.class)
                            .postEvent(true);

                    //这里全局监听通知类消息,通知类消息是通过普通消息的扩展实现
                    if (MessageHelper.isNotificationMessage(message)) {
                        // 检测是否为留言的通知消息
                        String eventName = getEventNameByNotification(message);
                        if (!TextUtils.isEmpty(eventName)) {
                            if (eventName.equals("TicketStatusChangedEvent") || eventName.equals("CommentCreatedEvent")) {
                                // 检测为留言部分的通知类消息,刷新留言列表
                                JSONObject jsonTicket = null;
                                try {
                                    jsonTicket = message.getJSONObjectAttribute("weichat").getJSONObject("event").getJSONObject("ticket");
                                } catch (Exception ignored) {
                                }

                                ListenerManager.getInstance().sendBroadCast(eventName, jsonTicket);
                            }
                        }
                    } else if (message.isNeedToScore()) {
                        MessageHelper.createInviteCommentMsg(message, "");
                        message.setIsNeedToScore(false);
                    }
                }
            }

            @Override
            public void onCmdMessage(List<Message> msgs) {
                for (Message message : msgs) {
                    Log.d(TAG, "收到透传消息");
                    //获取消息body
                    EMCmdMessageBody cmdMessageBody = (EMCmdMessageBody) message.body();
                    String action = cmdMessageBody.action(); //获取自定义action
                    Log.d(TAG, String.format("透传消息: action:%s,message:%s", action, message.toString()));
                }
            }

            @Override
            public void onMessageStatusUpdate() {

            }

            @Override
            public void onMessageSent() {

            }
        };

        ChatClient.getInstance().chatManager().addMessageListener(messageListener);
    }


    /**
     * 获取EventName
     *
     * @param message
     * @return
     */
    public String getEventNameByNotification(Message message) {

        try {
            JSONObject weichatJson = message.getJSONObjectAttribute("weichat");
            if (weichatJson != null && weichatJson.has("event")) {
                JSONObject eventJson = weichatJson.getJSONObject("event");
                if (eventJson != null && eventJson.has("eventName")) {
                    return eventJson.getString("eventName");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void pushActivity(Activity activity) {
        _uiProvider.pushActivity(activity);
    }

    public void popActivity(Activity activity) {
        _uiProvider.popActivity(activity);
    }

    public Notifier getNotifier() {
        return _uiProvider.getNotifier();
    }

    /**
     * 展示通知设置页面
     */
//    public void showNotificationPermissionDialog() {
//        EMPushType pushType = EMPushHelper.getInstance().getPushType();
//        // oppo
//        if(pushType == EMPushType.OPPOPUSH && HeytapPushManager.isSupportPush(appContext)) {
//            HeytapPushManager.requestNotificationPermission();
//        }
//    }


    /**
     * 获取未读消息数量
     */
    public int getUnReadMessage() {
        // todo 需要判断先登录
        if (ChatClient.getInstance().isLoggedInBefore()) {
            // im 服务号
            Conversation conversation = ChatClient.getInstance().chatManager().getConversation(Constants.EaseUi.DEFAULT_CUSTOMER_ACCOUNT);
            return conversation.unreadMessagesCount();
        }
        return 0;
    }

    /**
     * 未读消息清零、变成已读
     */
    public void UnreadMessagesCleared() {
        Conversation conversation = ChatClient.getInstance().chatManager().getConversation(Constants.EaseUi.DEFAULT_CUSTOMER_ACCOUNT);
        //指定会话消息未读数清零
        conversation.markAllMessagesAsRead();
        ChatClient.getInstance().chatManager().markAllConversationsAsRead();
    }

    /**
     * 跳转到聊天界面
     *
     * @param message 附带消息过去
     */
    public void startChat(String message) {
        Intent intent = new Intent(BaseApplication.getContext(), EaseUiActivity.class);
        if (!TextUtils.isEmpty(message)) {
            intent.putExtra(Config.EXTRA_MESSAGE, message);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        BaseApplication.getContext().startActivity(intent);
    }
}