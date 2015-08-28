/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.chat.single;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputFilter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {

    private final static String EXTRA_CONTACT = "contact";

    private ContactId mContact;

    private OneToOneChat mChat;

    /**
     * ContactId of the displayed single chat
     */
    /* private package */static ContactId contactOnForeground;

    /**
     * List of items for contextual menu
     */
    private final static int CHAT_MENU_ITEM_DELETE = 0;

    private final static int CHAT_MENU_ITEM_RESEND = 1;

    private final static int CHAT_MENU_ITEM_REVOKE = 2;

    private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());

    /**
     * Chat_id is set to contact id for one to one chat messages.
     */
    private static final String WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID)
            .append("=? AND (").append(Message.MIME_TYPE).append("='")
            .append(Message.MimeType.GEOLOC_MESSAGE).append("' OR ").append(Message.MIME_TYPE)
            .append("='").append(Message.MimeType.TEXT_MESSAGE).append("')").toString();

    private final static String UNREADS_WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID)
            .append("=? AND ").append(Message.READ_STATUS).append("=")
            .append(ReadStatus.UNREAD.toInt()).append(" AND (").append(Message.MIME_TYPE)
            .append("='").append(Message.MimeType.GEOLOC_MESSAGE).append("' OR ")
            .append(Message.MIME_TYPE).append("='").append(Message.MimeType.TEXT_MESSAGE)
            .append("')").toString();

    private final static String[] PROJECTION_MSG_ID = new String[] {
        Message.MESSAGE_ID
    };

    /**
     * Single Chat listener
     */
    private OneToOneChatListener mListener = new OneToOneChatListener() {

        // Callback called when an Is-composing event has been received
        @Override
        public void onComposingEvent(ContactId contact, boolean status) {
            // Discard event if not for current contact
            if (!mContact.equals(contact)) {
                return;

            }
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onComposingEvent contact=").append(contact.toString())
                                .append(" status=").append(status).toString());
            }
            displayComposingEvent(contact, status);
        }

        @Override
        public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                Content.Status status, Content.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onMessageStatusChanged contact=")
                                .append(contact.toString()).append(" mime-type=").append(mimeType)
                                .append(" msgId=").append(msgId).append(" status=").append(status)
                                .toString());
            }
        }

        @Override
        public void onMessagesDeleted(ContactId contact, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onMessagesDeleted contact=" + contact + " for message IDs="
                                + Arrays.toString(msgIds.toArray()));
            }
        }

    };

    private CapabilityService mCapabilityService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mExitOnce.isLocked()) {
            return;
        }
        try {
            addChatEventListener(mChatService);
            ChatServiceConfiguration configuration = mChatService.getConfiguration();
            // Set max label length
            int maxMsgLength = configuration.getOneToOneChatMessageMaxLength();
            if (maxMsgLength > 0) {
                // Set the message composer max length
                InputFilter[] filterArray = new InputFilter[1];
                filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
                mComposeText.setFilters(filterArray);
            }
            // Instantiate the composing manager
            mComposingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                    getNotifyComposing());
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate");
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onDestroy");
        }
        if (mChat != null) {
            try {
                mChat.setComposingStatus(false);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "onComposing failed", e);
                }
            }
        }
        super.onDestroy();
        contactOnForeground = null;
    }

    @Override
    public boolean processIntent() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent");
        }
        // Open chat
        ContactId newContact = (ContactId) getIntent().getParcelableExtra(EXTRA_CONTACT);
        if (newContact == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: contact is null");
            }
            return false;

        }
        try {
            if (!newContact.equals(mContact) || mChat == null) {
                boolean firstLoad = (mChat == null);
                /* Save contact ID */
                mContact = newContact;
                /*
                 * Open chat so that if the parameter IM SESSION START is 0 then the session is
                 * accepted now.
                 */
                mChat = mChatService.getOneToOneChat(mContact);
                if (firstLoad) {
                    /*
                     * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
                     */
                    getSupportLoaderManager().initLoader(LOADER_ID, null, this);
                } else {
                    /* We switched from one contact to another: reload history since */
                    getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
                }
                if (mCapabilityService == null) {
                    mCapabilityService = mCnxManager.getCapabilityApi();
                }
                /* Request options for this new contact */
                mCapabilityService.requestContactCapabilities(mContact);
            }
            /*
             * Open chat to accept session if the parameter IM SESSION START is 0. Client
             * application is not aware of the one to one chat session state nor of the IM session
             * start mode so we call the method systematically.
             */
            mChat.openChat();

            contactOnForeground = mContact;
            // Set activity title with display name
            String from = RcsDisplayName.getInstance(this).getDisplayName(mContact);
            setTitle(getString(R.string.title_chat, from));
            // Mark as read messages if required
            Set<String> msgIdUnreads = getUnreadMessageIds(mContact);
            for (String msgId : msgIdUnreads) {
                mChatService.markMessageAsRead(msgId);
            }
            return true;

        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, Message.CONTENT_URI, PROJ_CHAT_MSG, WHERE_CLAUSE,
                new String[] {
                    mContact.toString()
                }, ORDER_CHAT_MSG);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the list item position
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        // Adapt the contextual menu according to the selected item
        menu.add(0, CHAT_MENU_ITEM_DELETE, CHAT_MENU_ITEM_DELETE, R.string.menu_delete_message);
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.DIRECTION)));
        if (Direction.OUTGOING != direction) {
            return;

        }
        Content.Status status = Content.Status.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.STATUS)));
        switch (status) {
            case FAILED:
                menu.add(0, CHAT_MENU_ITEM_RESEND, CHAT_MENU_ITEM_RESEND,
                        R.string.menu_resend_message);
                break;
            case DISPLAY_REPORT_REQUESTED:
            case DELIVERED:
            case SENT:
            case SENDING:
            case QUEUED:
                menu.add(0, CHAT_MENU_ITEM_REVOKE, CHAT_MENU_ITEM_REVOKE,
                        R.string.menu_revoke_message);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        String messageId = cursor.getString(cursor.getColumnIndexOrThrow(Message.MESSAGE_ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected msgId=".concat(messageId));
        }
        switch (item.getItemId()) {
            case CHAT_MENU_ITEM_RESEND:
                // TODO
                return true;
            case CHAT_MENU_ITEM_REVOKE:
                // TODO
                return true;
            case CHAT_MENU_ITEM_DELETE:
                try {
                    mChatService.deleteMessage(messageId);
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "delete message failed", e);
                    }
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Forge intent to start SingleChatView activity
     * 
     * @param context The context
     * @param contact The contact ID
     * @return intent
     */
    public static Intent forgeIntentToStart(Context context, ContactId contact) {
        Intent intent = new Intent(context, SingleChatView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Get unread messages for contact
     * 
     * @param contact
     * @return set of unread message IDs
     */
    private Set<String> getUnreadMessageIds(ContactId contact) {
        Set<String> unReadMessageIDs = new HashSet<String>();
        String[] where_args = new String[] {
            contact.toString()
        };

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(Message.CONTENT_URI, PROJECTION_MSG_ID,
                    UNREADS_WHERE_CLAUSE, where_args, ORDER_CHAT_MSG);
            int columIndex = cursor.getColumnIndexOrThrow(Message.MESSAGE_ID);
            while (cursor.moveToNext()) {
                unReadMessageIDs.add(cursor.getString(columIndex));
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Exception occurred", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return unReadMessageIDs;
    }

    @Override
    public ChatMessage sendMessage(String message) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        // Send text message
        try {
            // Send the text to remote
            return mChat.sendMessage(message);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "sendTextMessage failed", e);
            }
            return null;
        }
    }

    @Override
    public ChatMessage sendMessage(Geoloc geoloc) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendGeolocMessage: ".concat(geoloc.toString()));
        }
        // Send geoloc message
        try {
            // Send the text to remote
            return mChat.sendMessage(geoloc);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "sendMessage failed", e);
            }
            return null;
        }
    }

    @Override
    public void addChatEventListener(ChatService chatService) throws RcsServiceException {
        mChatService.addEventListener(mListener);
    }

    @Override
    public void removeChatEventListener(ChatService chatService) throws RcsServiceException {
        mChatService.removeEventListener(mListener);
    }

    @Override
    public INotifyComposing getNotifyComposing() {
        INotifyComposing notifyComposing = new IsComposingManager.INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mChat == null) {
                        return;

                    }
                    mChat.setComposingStatus(isTyping);
                    if (LogUtils.isActive) {
                        Boolean _isTyping = Boolean.valueOf(isTyping);
                        Log.d(LOGTAG, "sendIsComposingEvent ".concat(_isTyping.toString()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        return notifyComposing;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_insert_smiley:
                Smileys.showSmileyDialog(this, mComposeText, getResources(),
                        getString(R.string.menu_insert_smiley));
                break;

            case R.id.menu_quicktext:
                addQuickText();
                break;

            case R.id.menu_send_geoloc:
                getGeoLoc();
                break;

            case R.id.menu_showus_map:
                Set<String> contacts = new HashSet<String>();
                contacts.add(mContact.toString());
                showUsInMap(contacts);
                break;

            case R.id.menu_send_file:
                SendSingleFile.startActivity(this, mContact);
                break;
        }
        return true;
    }

    @Override
    public boolean isSingleChat() {
        return true;
    }
}
