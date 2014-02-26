/*
 * Tigase XMPP Client Library
 * Copyright (C) 2006-2012 "Bartosz Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.jaxmpp.core.client.xmpp.modules.chat;

import java.util.ArrayList;
import java.util.List;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Context;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public abstract class AbstractChatManager {

	protected final ArrayList<Chat> chats = new ArrayList<Chat>();

	protected ChatSelector chatSelector;

	protected Context context;

	protected AbstractChatManager() {
		ChatSelector x = UniversalFactory.createInstance(ChatSelector.class.getName());
		this.chatSelector = x == null ? new DefaultChatSelector() : x;
	}

	public boolean close(Chat chat) throws JaxmppException {
		boolean x = this.chats.remove(chat);
		if (x) {
			MessageModule.ChatClosedHandler.ChatClosedEvent event = new MessageModule.ChatClosedHandler.ChatClosedEvent(
					context.getSessionObject(), chat);
			context.getEventBus().fire(event);
		}
		return x;
	}

	public Chat createChat(JID jid) throws JaxmppException {
		final String threadId = UIDGenerator.next();
		Chat chat = createChatInstance(jid, threadId);

		this.chats.add(chat);

		MessageModule.ChatCreatedHandler.ChatCreatedEvent event = new MessageModule.ChatCreatedHandler.ChatCreatedEvent(
				context.getSessionObject(), chat, null);

		context.getEventBus().fire(event);

		return chat;
	}

	protected abstract Chat createChatInstance(final JID fromJid, final String threadId);

	protected Chat getChat(JID jid, String threadId) {
		return chatSelector.getChat(chats, jid, threadId);
	}

	public List<Chat> getChats() {
		return this.chats;
	}

	public Context getContext() {
		return context;
	}

	protected void initialize() {
	}

	public boolean isChatOpenFor(final BareJID jid) {
		for (Chat chat : this.chats) {
			if (chat.getJid().getBareJid().equals(jid))
				return true;
		}
		return false;
	}

	public Chat process(Message message) throws JaxmppException {
		final JID interlocutorJid = message.getFrom();
		return process(message, interlocutorJid);
	}

	public Chat process(Message message, JID interlocutorJid) throws JaxmppException {
		if (message.getType() != StanzaType.chat && message.getType() != StanzaType.error
				&& message.getType() != StanzaType.headline)
			return null;
		final String threadId = message.getThread();

		Chat chat = getChat(interlocutorJid, threadId);

		if (chat == null && message.getBody() == null) {
			return null;
		}

		if (chat == null) {
			chat = createChatInstance(interlocutorJid, threadId);
			chat.setJid(interlocutorJid);
			chat.setThreadId(threadId);
			this.chats.add(chat);
			MessageModule.ChatCreatedHandler.ChatCreatedEvent event = new MessageModule.ChatCreatedHandler.ChatCreatedEvent(
					context.getSessionObject(), chat, message);
			context.getEventBus().fire(event);
		} else {
			update(chat, interlocutorJid, threadId);
		}

		return chat;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	protected boolean update(final Chat chat, final JID fromJid, final String threadId) throws JaxmppException {
		boolean changed = false;

		if (!chat.getJid().equals(fromJid)) {
			chat.setJid(fromJid);
			changed = true;
		}

		if (chat.getThreadId() == null && threadId != null) {
			chat.setThreadId(threadId);
			changed = true;
		}

		if (changed) {
			MessageModule.ChatUpdatedHandler.ChatUpdatedEvent event = new MessageModule.ChatUpdatedHandler.ChatUpdatedEvent(
					context.getSessionObject(), chat);
			context.getEventBus().fire(event);
		}

		return changed;
	}

}