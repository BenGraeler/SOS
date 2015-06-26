/**
 * Copyright (C) 2012-2015 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.ds.hibernate.util.procedure.enrich;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.iceland.exception.ows.OwsExceptionReport;
import org.n52.iceland.ogc.ows.OwsServiceProvider;
import org.n52.iceland.util.LocalizedProducer;
import org.n52.sos.iso.CodeList;
import org.n52.sos.ogc.sensorML.AbstractSensorML;
import org.n52.sos.ogc.sensorML.Role;
import org.n52.sos.ogc.sensorML.SmlResponsibleParty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann <c.autermann@52north.org>
 */
public class ContactsEnrichment extends SensorMLEnrichment {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactsEnrichment.class);

    private final LocalizedProducer<OwsServiceProvider> serviceProvider;
    private final Locale locale;

    public ContactsEnrichment(Locale locale, LocalizedProducer<OwsServiceProvider> serviceProvider) {
        this.serviceProvider = serviceProvider;
        this.locale = locale;
    }


    @Override
    protected void enrich(AbstractSensorML description) throws OwsExceptionReport {
        // set contacts --> take from service information
        if (!description.isSetContact()) {
            Optional<SmlResponsibleParty> contact = createContactFromServiceContact();
            if (contact.isPresent()) {
                description.addContact(contact.get());
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return super.isApplicable() && procedureSettings().isEnrichWithDiscoveryInformation()
                && procedureSettings().isUseServiceContactAsProcedureContact();
    }

    /**
     * Get SerivceProvider object,
     *
     * @return SerivceProvider object
     */
    @VisibleForTesting
    Optional<OwsServiceProvider> getServiceProvider() {
        return Optional.of(this.serviceProvider.get());
    }

    /**
     * Create SensorML Contact form service contact informations
     *
     * @return SensorML Contact
     */
    private Optional<SmlResponsibleParty> createContactFromServiceContact() {
        Optional<OwsServiceProvider> serviceProvider = getServiceProvider();
        if (!serviceProvider.isPresent()) {
            return Optional.absent();
        }
        SmlResponsibleParty rp = new SmlResponsibleParty();
        OwsServiceProvider sp = serviceProvider.get();
        if (sp.hasIndividualName()) {
            rp.setIndividualName(sp.getIndividualName());
        }
        if (sp.hasName()) {
            rp.setOrganizationName(sp.getName());
        }
        if (sp.hasSite()) {
            rp.addOnlineResource(sp.getSite());
        }
        if (sp.hasPositionName()) {
            rp.setPositionName(sp.getPositionName());
        }
        if (sp.hasDeliveryPoint()) {
            rp.addDeliveryPoint(sp.getDeliveryPoint());
        }
        if (sp.hasPhone()) {
            rp.addPhoneVoice(sp.getPhone());
        }
        if (sp.hasCity()) {
            rp.setCity(sp.getCity());
        }
        if (sp.hasCountry()) {
            rp.setCountry(sp.getCountry());
        }
        if (sp.hasPostalCode()) {
            rp.setPostalCode(sp.getPostalCode());
        }
        if (sp.hasMailAddress()) {
            rp.setEmail(sp.getMailAddress());
        }
        rp.setRole(createRole());
        return Optional.of(rp);
    }

    private Role createRole() {
        Role role =
                new Role("Point of Contact").setCodeList(CodeList.CI_ROLE_CODE_URL)
                        .setCodeListValue(CodeList.CiRoleCodes.CI_RoleCode_pointOfContact.name());
        return role;
    }
}
