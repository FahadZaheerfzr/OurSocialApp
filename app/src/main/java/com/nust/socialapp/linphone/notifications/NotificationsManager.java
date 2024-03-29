/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nust.socialapp.linphone.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;


import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatMessageListenerStub;
import org.linphone.core.ChatRoom;
import org.linphone.core.ChatRoomCapabilities;
import org.linphone.core.Content;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;

import java.io.File;
import java.util.HashMap;

import com.nust.socialapp.R;
import com.nust.socialapp.linphone.CallInterfaceActivity;
import com.nust.socialapp.linphone.Manager;
import com.nust.socialapp.linphone.call.CallIncomingActivity;
import com.nust.socialapp.linphone.call.CallOutgoingActivity;
import com.nust.socialapp.linphone.compatibility.Compatibility;
import com.nust.socialapp.linphone.contacts.ContactsManager;
import com.nust.socialapp.linphone.contacts.LinphoneContact;
import com.nust.socialapp.linphone.dialer.DialerActivity;
import com.nust.socialapp.linphone.service.LinphoneService;
import com.nust.socialapp.linphone.settings.LinphonePreferences;
import com.nust.socialapp.linphone.utils.DeviceUtils;
import com.nust.socialapp.linphone.utils.FileUtils;
import com.nust.socialapp.linphone.utils.ImageUtils;
import com.nust.socialapp.linphone.utils.LinphoneUtils;
import com.nust.socialapp.linphone.utils.MediaScannerListener;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationsManager {
    private static final int SERVICE_NOTIF_ID = 1;
    private static final int MISSED_CALLS_NOTIF_ID = 2;

    private final Context mContext;
    private final NotificationManager mNM;
    private final HashMap<String, Notifiable> mChatNotifMap;
    private final HashMap<String, Notifiable> mCallNotifMap;
    private int mLastNotificationId;
    private final Notification mServiceNotification;
    private int mCurrentForegroundServiceNotification;
    private String mCurrentChatRoomAddress;
    private CoreListenerStub mListener;
    private ChatMessageListenerStub mMessageListener;

   
    public NotificationsManager(Context context) {
        mContext = context;
        mChatNotifMap = new HashMap<>();
        mCallNotifMap = new HashMap<>();
        mCurrentForegroundServiceNotification = 0;
        mCurrentChatRoomAddress = null;

        mNM = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

      /*  if (mContext.getResources().getBoolean(R.bool.keep_missed_call_notification_upon_restart)) {
            StatusBarNotification[] notifs = Compatibility.getActiveNotifications(mNM);
            if (notifs != null && notifs.length > 1) {
                for (StatusBarNotification notif : notifs) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        if (notif.getId() != MISSED_CALLS_NOTIF_ID) {
                            dismissNotification(notif.getId());
                        }
                    }
                }
            }
        } else {
            mNM.cancelAll();
        }

       */

        mLastNotificationId = 5; // Do not conflict with hardcoded notifications ids !

        Compatibility.createNotificationChannels(mContext);

        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
        } catch (Exception e) {
            Log.e(e);
        }

        Intent notifIntent = new Intent(mContext, DialerActivity.class);
        notifIntent.putExtra("Notification", true);
        addFlagsToIntent(notifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext, SERVICE_NOTIF_ID, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mServiceNotification =
                Compatibility.createNotification(
                        mContext,
                        "Linphone Service",
                        "",
                        R.drawable.linphone_logo,
                        R.mipmap.ic_launcher,
                        bm,
                        pendingIntent,
                        Notification.PRIORITY_MIN,
                        true);

        mListener =
                new CoreListenerStub() {
                    public void onMessageSent(Core core, ChatRoom room, ChatMessage message) {
                        if (room.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
                            Compatibility.createChatShortcuts(mContext);
                        }
                    }

                    @Override
                    public void onMessageReceived(
                            Core core, final ChatRoom cr, final ChatMessage message) {
                        if (message.isOutgoing()
                                || mContext.getResources().getBoolean(R.bool.disable_chat)
                                || mContext.getResources()
                                        .getBoolean(R.bool.disable_chat_message_notification)) {
                            return;
                        }

                        if (mCurrentChatRoomAddress != null
                                && mCurrentChatRoomAddress.equals(
                                        cr.getPeerAddress().asStringUriOnly())) {
                            Log.i(
                                    "[Notifications Manager] Message received for currently displayed chat room, do not make a notification");
                            return;
                        }

                        if (message.getErrorInfo() != null
                                && message.getErrorInfo().getReason()
                                        == Reason.UnsupportedContent) {
                            Log.w(
                                    "[Notifications Manager] Message received but content is unsupported, do not notify it");
                            return;
                        }

                        if (!message.hasTextContent()
                                && message.getFileTransferInformation() == null) {
                            Log.w(
                                    "[Notifications Manager] Message has no text or file transfer information to display, ignoring it...");
                            return;
                        }

                        final Address from = message.getFromAddress();
                        final LinphoneContact contact =
                                ContactsManager.getInstance().findContactFromAddress(from);
                        final String textMessage =
                                (message.hasTextContent())
                                        ? message.getTextContent()
                                        : mContext.getString(
                                                R.string.content_description_incoming_file);

                        String file = null;
                        for (Content c : message.getContents()) {
                            if (c.isFile()) {
                                file = c.getFilePath();
                                Manager.getInstance()
                                        .getMediaScanner()
                                        .scanFile(
                                                new File(file),
                                                new MediaScannerListener() {
                                                    @Override
                                                    public void onMediaScanned(
                                                            String path, Uri uri) {
                                                        createNotification(
                                                                cr,
                                                                contact,
                                                                from,
                                                                textMessage,
                                                                message.getTime(),
                                                                uri,
                                                                FileUtils.getMimeFromFile(path));
                                                    }
                                                });
                                break;
                            }
                        }

                        if (file == null) {
                            createNotification(
                                    cr, contact, from, textMessage, message.getTime(), null, null);
                        }

                        if (cr.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
                            Compatibility.createChatShortcuts(mContext);
                        }
                    }
                };

        mMessageListener =
                new ChatMessageListenerStub() {
                    @Override
                    public void onMsgStateChanged(ChatMessage msg, ChatMessage.State state) {
                        if (msg.getUserData() == null) return;
                        int notifId = (int) msg.getUserData();
                        Log.i(
                                "[Notifications Manager] Reply message state changed ("
                                        + state.name()
                                        + ") for notif id "
                                        + notifId);

                        if (state != ChatMessage.State.InProgress) {
                            // There is no need to be called here twice
                            msg.removeListener(this);
                        }

                        if (state == ChatMessage.State.Delivered
                                || state == ChatMessage.State.Displayed) {
                            Notifiable notif =
                                    mChatNotifMap.get(
                                            msg.getChatRoom().getPeerAddress().asStringUriOnly());
                            if (notif == null) {
                                Log.e(
                                        "[Notifications Manager] Couldn't find message notification for SIP URI "
                                                + msg.getChatRoom()
                                                        .getPeerAddress()
                                                        .asStringUriOnly());
                                dismissNotification(notifId);
                                return;
                            } else if (notif.getNotificationId() != notifId) {
                                Log.w(
                                        "[Notifications Manager] Notif ID doesn't match: "
                                                + notifId
                                                + " != "
                                                + notif.getNotificationId());
                            }

                        } else if (state == ChatMessage.State.NotDelivered) {
                            Log.e(
                                    "[Notifications Manager] Couldn't send reply, message is not delivered");
                            dismissNotification(notifId);
                        }
                    }
                };
    }

    public void onCoreReady() {
        Core core = Manager.getCore();
        if (core != null) {
            core.addListener(mListener);
        }
    }

    public void destroy() {
        // mNM.cancelAll();
        // Don't use cancelAll to keep message notifications !
        // When a message is received by a push, it will create a LinphoneService
        // but it might be getting killed quite quickly after that
        // causing the notification to be missed by the user...
        Log.i("[Notifications Manager] Getting destroyed, clearing Service & Call notifications");

        if (mCurrentForegroundServiceNotification > 0) {
            mNM.cancel(mCurrentForegroundServiceNotification);
        }

        for (Notifiable notifiable : mCallNotifMap.values()) {
            mNM.cancel(notifiable.getNotificationId());
        }

        Core core = Manager.getCore();
        if (core != null) {
            core.removeListener(mListener);
        }
    }

    private void addFlagsToIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    public void startForeground() {
        if (LinphoneService.isReady()) {
            Log.i("[Notifications Manager] Starting Service as foreground");
            LinphoneService.instance().startForeground(SERVICE_NOTIF_ID, mServiceNotification);
            mCurrentForegroundServiceNotification = SERVICE_NOTIF_ID;
        }
    }

    private void startForeground(Notification notification, int id) {
        if (LinphoneService.isReady()) {
            Log.i("[Notifications Manager] Starting Service as foreground while in caller");
            LinphoneService.instance().startForeground(id, notification);
            mCurrentForegroundServiceNotification = id;
        }
    }

    public void stopForeground() {
        if (LinphoneService.isReady()) {
            Log.i("[Notifications Manager] Stopping Service as foreground");
            LinphoneService.instance().stopForeground(true);
            mCurrentForegroundServiceNotification = 0;
        }
    }

    public void removeForegroundServiceNotificationIfPossible() {
        if (LinphoneService.isReady()) {
            if (mCurrentForegroundServiceNotification == SERVICE_NOTIF_ID
                    && !isServiceNotificationDisplayed()) {
                Log.i(
                        "[Notifications Manager] Linphone has started after device boot, stopping Service as foreground");
                stopForeground();
            }
        }
    }

    public void setCurrentlyDisplayedChatRoom(String address) {
        mCurrentChatRoomAddress = address;
        if (address != null) {
            resetMessageNotifCount(address);
        }
    }

    public void dismissMissedCallNotification() {
        dismissNotification(MISSED_CALLS_NOTIF_ID);
    }

    public void sendNotification(int id, Notification notif) {
        Log.i("[Notifications Manager] Notifying " + id);
        mNM.notify(id, notif);
    }

    public void dismissNotification(int notifId) {
        Log.i("[Notifications Manager] Dismissing " + notifId);
        mNM.cancel(notifId);
    }

    public void resetMessageNotifCount(String address) {
        Notifiable notif = mChatNotifMap.get(address);
        if (notif != null) {
            notif.resetMessages();
            mNM.cancel(notif.getNotificationId());
        }
    }

    public ChatMessageListenerStub getMessageListener() {
        return mMessageListener;
    }

    private boolean isServiceNotificationDisplayed() {
        return LinphonePreferences.instance().getServiceNotificationVisibility();
    }

    public String getSipUriForNotificationId(int notificationId) {
        for (String addr : mChatNotifMap.keySet()) {
            if (mChatNotifMap.get(addr).getNotificationId() == notificationId) {
                return addr;
            }
        }
        return null;
    }

    public void displayGroupChatMessageNotification(
            String subject,
            String conferenceAddress,
            String fromName,
            Uri fromPictureUri,
            String message,
            Address localIdentity,
            long timestamp,
            Uri filePath,
            String fileMime) {

        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, fromPictureUri);
        Notifiable notif = mChatNotifMap.get(conferenceAddress);
        NotifiableMessage notifMessage =
                new NotifiableMessage(message, fromName, timestamp, filePath, fileMime);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mChatNotifMap.put(conferenceAddress, notif);
        }
        Log.i("[Notifications Manager] Creating group chat message notifiable " + notif);

        notifMessage.setSenderBitmap(bm);
        notif.addMessage(notifMessage);
        notif.setIsGroup(true);
        notif.setGroupTitle(subject);
        notif.setMyself(LinphoneUtils.getAddressDisplayName(localIdentity));
        notif.setLocalIdentity(localIdentity.asString());
    }

    public void displayMessageNotification(
            String fromSipUri,
            String fromName,
            Uri fromPictureUri,
            String message,
            Address localIdentity,
            long timestamp,
            Uri filePath,
            String fileMime) {
        if (fromName == null) {
            fromName = fromSipUri;
        }

        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, fromPictureUri);
        Notifiable notif = mChatNotifMap.get(fromSipUri);
        NotifiableMessage notifMessage =
                new NotifiableMessage(message, fromName, timestamp, filePath, fileMime);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mChatNotifMap.put(fromSipUri, notif);
        }
        Log.i("[Notifications Manager] Creating chat message notifiable " + notif);

        notifMessage.setSenderBitmap(bm);
        notif.addMessage(notifMessage);
        notif.setIsGroup(false);
        notif.setMyself(LinphoneUtils.getAddressDisplayName(localIdentity));
        notif.setLocalIdentity(localIdentity.asString());
    }
    public void displayCallNotification(Call call) {
        if (call == null) return;

        Class callNotifIntentClass = CallInterfaceActivity.class;
        if (call.getState() == Call.State.IncomingReceived
                || call.getState() == Call.State.IncomingEarlyMedia) {
            callNotifIntentClass = CallIncomingActivity.class;
        } else if (call.getState() == Call.State.OutgoingInit
                || call.getState() == Call.State.OutgoingProgress
                || call.getState() == Call.State.OutgoingRinging
                || call.getState() == Call.State.OutgoingEarlyMedia) {
            callNotifIntentClass = CallOutgoingActivity.class;
        }
        Intent callNotifIntent = new Intent(mContext, callNotifIntentClass);
        callNotifIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, callNotifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Address address = call.getRemoteAddress();
        String addressAsString = address.asStringUriOnly();
        Notifiable notif = mCallNotifMap.get(addressAsString);
        if (notif == null) {
            notif = new Notifiable(mLastNotificationId);
            mLastNotificationId += 1;
            mCallNotifMap.put(addressAsString, notif);
        }

        int notificationTextId;
        int iconId;
        switch (call.getState()) {
            case Released:
            case End:
                if (mCurrentForegroundServiceNotification == notif.getNotificationId()) {
                    Log.i(
                            "[Notifications Manager] Call ended, stopping notification used to keep service alive");
                    // Call is released, remove service notification to allow for an other caller to
                    // be service notification
                    stopForeground();
                }
                mNM.cancel(notif.getNotificationId());
                mCallNotifMap.remove(addressAsString);
                return;
            case Paused:
            case PausedByRemote:
            case Pausing:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_paused;
                break;
            case IncomingEarlyMedia:
            case IncomingReceived:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_incoming;
                break;
            case OutgoingEarlyMedia:
            case OutgoingInit:
            case OutgoingProgress:
            case OutgoingRinging:
                iconId = R.drawable.topbar_call_notification;
                notificationTextId = R.string.incall_notif_outgoing;
                break;
            default:
                if (call.getCurrentParams().videoEnabled()) {
                    iconId = R.drawable.topbar_videocall_notification;
                    notificationTextId = R.string.incall_notif_video;
                } else {
                    iconId = R.drawable.topbar_call_notification;
                    notificationTextId = R.string.incall_notif_active;
                }
                break;
        }

        if (notif.getIconResourceId() == iconId
                && notif.getTextResourceId() == notificationTextId) {
            // Notification hasn't changed, do not "update" it to avoid blinking
            return;
        } else if (notif.getTextResourceId() == R.string.incall_notif_incoming) {
            // If previous notif was incoming caller, as we will switch channels, dismiss it first
            dismissNotification(notif.getNotificationId());
        }

        notif.setIconResourceId(iconId);
        notif.setTextResourceId(notificationTextId);
        Log.i(
                "[Notifications Manager] Call notification notifiable is "
                        + notif
                        + ", pending intent "
                        + callNotifIntentClass);

        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        Uri pictureUri = contact != null ? contact.getThumbnailUri() : null;
        Bitmap bm = ImageUtils.getRoundBitmapFromUri(mContext, pictureUri);
        String name =
                contact != null
                        ? contact.getFullName()
                        : LinphoneUtils.getAddressDisplayName(address);
        boolean isIncoming = callNotifIntentClass == CallIncomingActivity.class;

        Notification notification;
        if (isIncoming) {
            notification =
                    Compatibility.createIncomingCallNotification(
                            mContext,
                            notif.getNotificationId(),
                            bm,
                            name,
                            addressAsString,
                            pendingIntent);
        } else {
            notification =
                    Compatibility.createInCallNotification(
                            mContext,
                            notif.getNotificationId(),
                            mContext.getString(notificationTextId),
                            iconId,
                            bm,
                            name,
                            pendingIntent);
        }

        // Don't use incoming caller notification as foreground service notif !
        if (!isServiceNotificationDisplayed() && !isIncoming) {
            if (call.getCore().getCallsNb() == 0) {
                Log.i(
                        "[Notifications Manager] Foreground service mode is disabled, stopping caller notification used to keep it alive");
                stopForeground();
            } else {
                if (mCurrentForegroundServiceNotification == 0) {
                    if (DeviceUtils.isAppUserRestricted(mContext)) {
                        Log.w(
                                "[Notifications Manager] App has been restricted, can't use caller notification to keep service alive !");
                        sendNotification(notif.getNotificationId(), notification);
                    } else {
                        Log.i(
                                "[Notifications Manager] Foreground service mode is disabled, using caller notification to keep it alive");
                        startForeground(notification, notif.getNotificationId());
                    }
                } else {
                    sendNotification(notif.getNotificationId(), notification);
                }
            }
        } else {
            sendNotification(notif.getNotificationId(), notification);
        }
    }

    public String getSipUriForCallNotificationId(int notificationId) {
        for (String addr : mCallNotifMap.keySet()) {
            if (mCallNotifMap.get(addr).getNotificationId() == notificationId) {
                return addr;
            }
        }
        return null;
    }

    private void createNotification(
            ChatRoom cr,
            LinphoneContact contact,
            Address from,
            String textMessage,
            long time,
            Uri file,
            String mime) {
        if (cr.hasCapability(ChatRoomCapabilities.OneToOne.toInt())) {
            if (contact != null) {
                displayMessageNotification(
                        cr.getPeerAddress().asStringUriOnly(),
                        contact.getFullName(),
                        contact.getThumbnailUri(),
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            } else {
                displayMessageNotification(
                        cr.getPeerAddress().asStringUriOnly(),
                        from.getUsername(),
                        null,
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            }
        } else {
            String subject = cr.getSubject();
            if (contact != null) {
                displayGroupChatMessageNotification(
                        subject,
                        cr.getPeerAddress().asStringUriOnly(),
                        contact.getFullName(),
                        contact.getThumbnailUri(),
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            } else {
                displayGroupChatMessageNotification(
                        subject,
                        cr.getPeerAddress().asStringUriOnly(),
                        from.getUsername(),
                        null,
                        textMessage,
                        cr.getLocalAddress(),
                        time,
                        file,
                        mime);
            }
        }
    }
}
