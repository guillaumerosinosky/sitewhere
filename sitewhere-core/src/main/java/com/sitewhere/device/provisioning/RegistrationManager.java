/*
 * RegistrationManager.java 
 * --------------------------------------------------------------------------------------
 * Copyright (c) Reveal Technologies, LLC. All rights reserved. http://www.reveal-tech.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.provisioning;

import org.apache.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.rest.model.device.request.DeviceAssignmentCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceCreateRequest;
import com.sitewhere.rest.model.search.SearchCriteria;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.DeviceAssignmentType;
import com.sitewhere.spi.device.IDevice;
import com.sitewhere.spi.device.IDeviceSpecification;
import com.sitewhere.spi.device.ISite;
import com.sitewhere.spi.device.event.request.IDeviceRegistrationRequest;
import com.sitewhere.spi.device.provisioning.IRegistrationManager;
import com.sitewhere.spi.search.ISearchResults;

/**
 * Base logic for {@link IRegistrationManager} implementations.
 * 
 * @author Derek
 */
public abstract class RegistrationManager implements IRegistrationManager {

	/** Static logger instance */
	private static Logger LOGGER = Logger.getLogger(RegistrationManager.class);

	/** Indicates if new devices can register with the system */
	private boolean allowNewDevices;

	/** Indicates if devices can be auto-assigned if no site token is passed */
	private boolean autoAssignSite;

	/** Token used if autoAssignSite is enabled */
	private String autoAssignSiteToken;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.spi.device.provisioning.IRegistrationManager#handleDeviceRegistration
	 * (com.sitewhere.spi.device.event.request.IDeviceRegistrationRequest)
	 */
	@Override
	public void handleDeviceRegistration(IDeviceRegistrationRequest request) throws SiteWhereException {
		LOGGER.debug("Handling device registration request.");
		IDevice device =
				SiteWhere.getServer().getDeviceManagement().getDeviceByHardwareId(request.getHardwareId());
		IDeviceSpecification specification =
				SiteWhere.getServer().getDeviceManagement().getDeviceSpecificationByToken(
						request.getSpecificationToken());
		// Create device if it does not already exist.
		if (device == null) {
			LOGGER.debug("Creating new device as part of registration.");
			if (specification == null) {
				sendInvalidSpecification(request.getHardwareId());
				return;
			}
			DeviceCreateRequest deviceCreate = new DeviceCreateRequest();
			deviceCreate.setHardwareId(request.getHardwareId());
			deviceCreate.setSpecificationToken(request.getSpecificationToken());
			deviceCreate.setComments("Device created by on-demand registration.");
			for (String key : request.getMetadata().keySet()) {
				String value = request.getMetadata(key);
				deviceCreate.addOrReplaceMetadata(key, value);
			}
			device = SiteWhere.getServer().getDeviceManagement().createDevice(deviceCreate);
		} else if (!device.getSpecificationToken().equals(request.getSpecificationToken())) {
			sendInvalidSpecification(request.getHardwareId());
			return;
		}
		// Make sure device is assigned.
		if (device.getAssignmentToken() == null) {
			if (!isAutoAssignSite()) {
				sendSiteTokenRequired(request.getHardwareId());
				return;
			}
			LOGGER.debug("Handling unassigned device for registration.");
			DeviceAssignmentCreateRequest assnCreate = new DeviceAssignmentCreateRequest();
			assnCreate.setSiteToken(getAutoAssignSiteToken());
			assnCreate.setDeviceHardwareId(device.getHardwareId());
			assnCreate.setAssignmentType(DeviceAssignmentType.Unassociated);
			SiteWhere.getServer().getDeviceManagement().createDeviceAssignment(assnCreate);
		}
		boolean isNewRegistration = (device != null);
		sendRegistrationAck(request.getHardwareId(), isNewRegistration);
	}

	/**
	 * Send a registration ack message.
	 * 
	 * @param hardwareId
	 * @param newRegistration
	 * @throws SiteWhereException
	 */
	protected abstract void sendRegistrationAck(String hardwareId, boolean newRegistration)
			throws SiteWhereException;

	/**
	 * Send a message indicating invalid specification id or one that does not match
	 * existing device.
	 * 
	 * @param hardwareId
	 * @throws SiteWhereException
	 */
	protected abstract void sendInvalidSpecification(String hardwareId) throws SiteWhereException;

	/**
	 * Send information indicating a site token must be passed (if not auto-assigned).
	 * 
	 * @param hardwareId
	 * @throws SiteWhereException
	 */
	protected abstract void sendSiteTokenRequired(String hardwareId) throws SiteWhereException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.ISiteWhereLifecycle#start()
	 */
	@Override
	public void start() throws SiteWhereException {
		LOGGER.info("Device registration manager starting.");
		if (isAutoAssignSite()) {
			if (getAutoAssignSiteToken() == null) {
				ISearchResults<ISite> sites =
						SiteWhere.getServer().getDeviceManagement().listSites(new SearchCriteria(1, 1));
				if (sites.getResults().isEmpty()) {
					throw new SiteWhereException(
							"Registration manager configured for auto-assign site, but no sites were found.");
				}
				setAutoAssignSiteToken(sites.getResults().get(0).getToken());
			} else {
				ISite site =
						SiteWhere.getServer().getDeviceManagement().getSiteByToken(getAutoAssignSiteToken());
				if (site == null) {
					throw new SiteWhereException(
							"Registration manager auto assignment site token is invalid.");
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.ISiteWhereLifecycle#stop()
	 */
	@Override
	public void stop() throws SiteWhereException {
		LOGGER.info("Device registration manager stopping.");
	}

	public boolean isAllowNewDevices() {
		return allowNewDevices;
	}

	public void setAllowNewDevices(boolean allowNewDevices) {
		this.allowNewDevices = allowNewDevices;
	}

	public boolean isAutoAssignSite() {
		return autoAssignSite;
	}

	public void setAutoAssignSite(boolean autoAssignSite) {
		this.autoAssignSite = autoAssignSite;
	}

	public String getAutoAssignSiteToken() {
		return autoAssignSiteToken;
	}

	public void setAutoAssignSiteToken(String autoAssignSiteToken) {
		this.autoAssignSiteToken = autoAssignSiteToken;
	}
}