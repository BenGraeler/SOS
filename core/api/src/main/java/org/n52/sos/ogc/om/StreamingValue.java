/*
 * Copyright (C) 2012-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.sos.ogc.om;

import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.NamedValue;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.TimeValuePair;
import org.n52.shetland.ogc.om.values.Value;
import org.n52.shetland.ogc.ows.OWSConstants.AdditionalRequestParams;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.AbstractStreaming;
import org.n52.sos.util.GeometryHandler;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Abstract streaming value class
 *
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 * @since 4.1.0
 *
 * @param <S>
 *            Entity type
 */
public abstract class StreamingValue<S> extends AbstractStreaming {
    private static final long serialVersionUID = -884370769373807775L;
    private Time phenomenonTime;
    private TimeInstant resultTime;
    private Time validTime;
    private String unit;
    private boolean unitQueried = false;
    protected OmObservation observationTemplate;

    /**
     * Get the next entity
     *
     * @return next entity object
     * @throws OwsExceptionReport
     *             If an error occurs
     */
    public abstract S nextEntity() throws OwsExceptionReport;

    /**
     * Query times
     */
    protected abstract void queryTimes();

    /**
     * query unit
     */
    protected abstract void queryUnit();

    /**
     * Get next {@link TimeValuePair} from entity
     *
     * @return Next {@link TimeValuePair}
     * @throws OwsExceptionReport
     *             If an error occurs
     */
    public abstract TimeValuePair nextValue() throws OwsExceptionReport;

    /**
     * Set the observation template which contains all metadata
     *
     * @param observationTemplate
     *            Observation template to set
     */
    public void setObservationTemplate(OmObservation observationTemplate) {
        this.observationTemplate = observationTemplate;
    }

    @Override
    public Time getPhenomenonTime() {
        isSetPhenomenonTime();
        return phenomenonTime;
    }

    @Override
    public void setPhenomenonTime(Time phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    public boolean isSetPhenomenonTime() {
        if (phenomenonTime == null) {
            queryTimes();
        }
        return phenomenonTime != null;
    }

    @Override
    public Value<OmObservation> getValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setValue(Value<OmObservation> value) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getUnit() {
        isSetUnit();
        return unit;
    }

    @Override
    public void setUnit(String unit) {
        this.unit = unit;
        unitQueried = true;
    }

    @Override
    public boolean isSetUnit() {
        if (!unitQueried && unit == null) {
            queryUnit();
            unitQueried = true;
        }
        return unit != null;
    }

    public TimeInstant getResultTime() {
        return resultTime;
    }

    protected void setResultTime(TimeInstant resultTime) {
        this.resultTime = resultTime;
    }

    public boolean isSetResultTime() {
        return getResultTime() != null;
    }

    public Time getValidTime() {
        return validTime;
    }

    protected void setValidTime(Time validTime) {
        this.validTime = validTime;
    }

    public boolean isSetValidTime() {
        return getValidTime() != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void checkForModifications(OmObservation observation) throws OwsExceptionReport {
        if (isSetAdditionalRequestParams() && contains(AdditionalRequestParams.crs)) {
            Object additionalRequestParam = getAdditionalRequestParams(AdditionalRequestParams.crs);
            int targetCRS = -1;
            if (additionalRequestParam instanceof Integer) {
                targetCRS = (Integer) additionalRequestParam;
            } else if (additionalRequestParam instanceof String) {
                targetCRS = Integer.parseInt((String) additionalRequestParam);
            }
            if (observation.isSetParameter()) {
                for (NamedValue<?> namedValue : observation.getParameter()) {
                    if (Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE.equals(namedValue.getName().getHref())) {
                        NamedValue<Geometry> spatialFilteringProfileParameter = (NamedValue<Geometry>) namedValue;
                        spatialFilteringProfileParameter.getValue().setValue(
                                GeometryHandler.getInstance().transform(
                                        spatialFilteringProfileParameter.getValue().getValue(), targetCRS));
                    }
                }
            }
        }
    }

}
