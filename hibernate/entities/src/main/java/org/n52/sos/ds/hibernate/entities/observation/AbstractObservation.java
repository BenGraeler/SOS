/*
 * Copyright (C) 2012-2018 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ds.hibernate.entities.observation;

import org.n52.sos.ds.hibernate.entities.ObservableProperty;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.entities.Unit;
import org.n52.sos.ds.hibernate.entities.feature.AbstractFeatureOfInterest;

/**
 *
 */
public abstract class AbstractObservation<T>
        extends AbstractTemporalReferencedObservation
        implements Observation<T> {

    private static final long serialVersionUID = -5638600640028433573L;
    private AbstractFeatureOfInterest featureOfInterest;
    private ObservableProperty observableProperty;
    private Procedure procedure;
    private Offering offering;
    private Unit unit;

    @Override
    public AbstractFeatureOfInterest getFeatureOfInterest() {
        return featureOfInterest;
    }

    @Override
    public void setFeatureOfInterest(AbstractFeatureOfInterest featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    @Override
    public ObservableProperty getObservableProperty() {
        return observableProperty;
    }

    @Override
    public void setObservableProperty(ObservableProperty observableProperty) {
        this.observableProperty = observableProperty;
    }

    @Override
    public Procedure getProcedure() {
        return procedure;
    }

    @Override
    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    @Override
    public Offering getOffering() {
        return offering;
    }

    @Override
    public void setOffering(Offering offering) {
       this.offering = offering;
    }

    @Override
    public boolean isSetOffering() {
        return getOffering() != null;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public void setUnit(final Unit unit) {
        this.unit = unit;
    }


    @Override
    public String getDiscriminator() {
        return null;
    }

}
