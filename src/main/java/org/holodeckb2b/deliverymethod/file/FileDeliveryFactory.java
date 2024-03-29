/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.deliverymethod.file;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.holodeckb2b.backend.file.NotifyAndDeliverOperation;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;

/**
 * @deprecated This class is only included for back-ward compatibility. Configurations <b>should</b> be changed asap.
 */
@Deprecated
public class FileDeliveryFactory extends NotifyAndDeliverOperation {
	
	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
		LogManager.getLogger().warn("org.holodeckb2b.deliverymethod.file.FileDeliveryFactory is deprecated! Use org.holodeckb2b.backend.file.NotifyAndDeliverOperation instead");
		super.init(settings);
	}
}
