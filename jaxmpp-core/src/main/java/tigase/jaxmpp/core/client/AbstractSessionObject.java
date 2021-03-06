/*
 * AbstractSessionObject.java
 *
 * Tigase XMPP Client Library
 * Copyright (C) 2004-2018 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
package tigase.jaxmpp.core.client;

import tigase.jaxmpp.core.client.eventbus.EventBus;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.EventBusAware;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Default representation of {@linkplain SessionObject}
 */
public abstract class AbstractSessionObject
		implements SessionObject, EventBusAware {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	protected Map<String, Entry> properties;
	private EventBus eventBus;

	protected AbstractSessionObject() {
	}

	public void addClearedHandler(ClearedHandler handler) {
		eventBus.addHandler(ClearedHandler.ClearedEvent.class, handler);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws JaxmppException {
		clear((Set<Scope>) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(final Scope... scopes) throws JaxmppException {
		final Set<Scope> scopesSet = new HashSet<Scope>();
		if (scopes != null) {
			for (Scope scope : scopes) {
				scopesSet.add(scope);
			}
		}
		clear(scopesSet);
	}

	public synchronized void clear(Set<Scope> scopes) throws JaxmppException {
		if (scopes == null || scopes.isEmpty()) {
			scopes = new HashSet<SessionObject.Scope>();
			scopes.add(Scope.session);
			scopes.add(Scope.stream);
		}
		log.fine("Clearing properties in scopes " + scopes);

		Iterator<java.util.Map.Entry<String, Entry>> iterator = this.properties.entrySet().iterator();
		while (iterator.hasNext()) {
			java.util.Map.Entry<String, Entry> entry = iterator.next();
			if (scopes.contains(entry.getValue().scope)) {
				iterator.remove();
			}
		}

		ClearedHandler.ClearedEvent event = new ClearedHandler.ClearedEvent(this, scopes);
		eventBus.fire(event);
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	@Override
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(Scope scope, String key) {
		Entry entry = this.properties.get(key);
		if (entry == null) {
			return null;
		} else if (scope == null || scope == entry.scope) {
			return (T) entry.value;
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getProperty(String key) {
		return getProperty(null, key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BareJID getUserBareJid() {
		return this.getProperty(USER_BARE_JID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getUserProperty(String key) {
		return getProperty(Scope.user, key);
	}

	public void removeClearedHandler(ClearedHandler handler) {
		eventBus.remove(ClearedHandler.ClearedEvent.class, handler);
	}

	@Override
	public SessionObject setProperty(Scope scope, String key, Object value) {
		if (value == null) {
			this.properties.remove(key);
		} else {
			Entry e = this.properties.get(key);
			if (e == null) {
				e = new Entry();
				this.properties.put(key, e);
			}
			e.scope = scope;
			e.value = value;
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SessionObject setProperty(String key, Object value) {
		return setProperty(Scope.session, key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserProperties setUserProperty(String key, Object value) {
		return setProperty(Scope.user, key, value);
	}

	@Override
	public String toString() {
		return "AbstractSessionObject{" + "properties=" + properties + '}';
	}

	public static class Entry {

		public Scope scope;
		public Object value;

		public Entry() {
		}

		@Override
		public String toString() {
			return "Entry{" + "scope=" + scope + ", value=" + value + '}';
		}
	}

}