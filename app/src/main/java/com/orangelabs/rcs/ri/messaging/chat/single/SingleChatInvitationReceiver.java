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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * CHAT invitation receiver
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class SingleChatInvitationReceiver extends BroadcastReceiver {

    /**
     * Action New One to One CHAT Message
     */
    /* package private */static final String ACTION_NEW_121_CHAT_MSG = "NEW_121_CHAT_MSG";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Send intent to service

        Intent receiverIntent = new Intent(context, SingleChatIntentService.class);
        receiverIntent.putExtras(intent);
        receiverIntent.setAction(ACTION_NEW_121_CHAT_MSG);
        context.startService(receiverIntent);
    }

}
