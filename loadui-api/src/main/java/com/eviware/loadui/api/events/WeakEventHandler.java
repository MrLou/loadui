/*
 * Copyright 2010 eviware software ab
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.api.events;

import java.util.EventObject;

/**
 * A marker interface which tells EventFirers to only hold weak references to
 * the EventHandler when it is added. A WeakEventHandler may be garbage
 * collected even though it has been registered with one or more EventFirers.
 * 
 * @author dain.nilsson
 * 
 * @param <T>
 *           The type of event to listen for.
 */
public interface WeakEventHandler<T extends EventObject> extends EventHandler<T>
{
}